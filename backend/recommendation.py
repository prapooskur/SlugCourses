import os, psycopg2, json
#from haystack import Pipeline
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.embedders import SentenceTransformersTextEmbedder
from haystack.components.builders.prompt_builder import PromptBuilder
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold
import google.ai.generativelanguage as glm
from pydantic import BaseModel
from typing import List
from enum import Enum
from haystack_integrations.document_stores.pgvector import PgvectorDocumentStore
from haystack_integrations.components.retrievers.pgvector import PgvectorKeywordRetriever, PgvectorEmbeddingRetriever
from haystack.utils import Secret
from haystack.components.rankers import TransformersSimilarityRanker
from fastapi.middleware.cors import CORSMiddleware

# create app and load .env
classRecommender = FastAPI(
    docs_url=None, # Disable docs (Swagger UI)
    redoc_url=None, # Disable redoc
)


origins = os.getenv("CORS_URLS")
classRecommender.add_middleware(
    CORSMiddleware,
    allow_origins=[origins],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# load API keys from .env
load_dotenv()
GEMINI_KEY = os.getenv("GEMINI_KEY")
PG_CONN_STRING = os.getenv("PG_CONN_STRING")
SUPABASE_CONN_STRING = os.getenv("SUPABASE_CONN_STRING")


prompt_template = '''
You are SlugBot, a helpful course assistant for UCSC students. 
Given these documents, answer the question. 
Assume the user is an undergraduate student and cannot take graduate classes without instructor permission.
Documents:
{% for doc in documents %}
    {{ doc.content }}
{% endfor %}
Question: {{question}}
Answer:'''

# Beware of the 7-stage [redacted] pipeline:
# 1) Load data about all classes into document store
# 2) Generate text embedding for user input
# 3) Create a list of documents that match the input (keyword based search) 
# 4) Create a list of documents that match the input (vector/semantic based search) 
# 5) Join vector/keyword documents into one list of documents, ranked by score
# 6) Insert merged document list and user input into prompt
# 7) Query LLM and return response. 

# (1) intialize store
document_store = PgvectorDocumentStore(
    connection_string = Secret.from_env_var("PG_CONN_STRING"),
    embedding_dimension=1024,
    vector_function="cosine_similarity",
    search_strategy="hnsw",
)


# create and warm up sentence embedder
embeddings_model = os.getenv("EMBEDDINGS_MODEL")
embeddings_query = os.getenv("EMBEDDINGS_QUERY")
# query_instruction = "Represent this sentence for searching relevant passages: "
text_embedder = SentenceTransformersTextEmbedder(model=embeddings_model, prefix=embeddings_query)
text_embedder.warm_up()

# create retrievers
keyword_retriever = PgvectorKeywordRetriever(document_store=document_store)
embedding_retriever = PgvectorEmbeddingRetriever(document_store=document_store)

# create document joiner using reciprocal rank fusion method
document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")

# create and warm up reranker
document_ranker = TransformersSimilarityRanker("BAAI/bge-reranker-v2-m3")
document_ranker.warm_up()


# instantiate llm tools
def retrieve_general(input: str):
    """
    Performs a search across course vectors to retrieve relevant courses based on a general query.
    Does not return a comprehensive list of courses.
    
    Use this function for queries like:
    - "What courses about data science are available?"
    - "Tell me about art history classes"
    - "Which courses cover environmental sustainability?"
    """
    # (2) generate text embeddings based on user input
    text_embeddings = text_embedder.run(input)

    # (3) keyword based document search
    keyword_docs = keyword_retriever.run(query=input)

    # (4) create embeddings for vector based document search
    embedding_docs = embedding_retriever.run(query_embedding=text_embeddings['embedding'])

    # (5) merge vector-based docs and keyword-based docs, then rerank
    merged_docs = document_joiner.run([keyword_docs["documents"], embedding_docs["documents"]])
    ranked_docs = document_ranker.run(query = input, documents = merged_docs["documents"], top_k = 7)

    print(ranked_docs)

    prompt_template = """
    Documents:
    {% for doc in documents %}
        {{ doc.content }}
    {% endfor %}
    """
    prompt_builder = PromptBuilder(template=prompt_template)
    finalizedPrompt = prompt_builder.run(documents=ranked_docs["documents"])["prompt"]
    # print(mergedDocs["documents"])
    return finalizedPrompt


def retrieve_specific(sql_input: str):
    """
    Queries the course SQL database (read-only). Use for queries about a specific class, instructor, department, or location.

    Example User Inputs:
    1. "Find all Computer Science courses for Spring 2024"
       SQL: SELECT * FROM llm_view WHERE department = 'CSE' AND quarter = "Spring 2024"

    2. "What are the details for PSYC 1?"
       SQL: SELECT * FROM llm_view WHERE department = 'PSYC' AND course_number = 1 AND course_letter = ''

    3. "Who's teaching Organic Chemistry this Fall?"
       SQL: SELECT name, instructor FROM llm_view WHERE department = 'CHEM' AND name LIKE '%Organic Chemistry%' AND quarter = "Fall 2024"

    4. "Show me all courses taught by Professor Johnson in the History department"
       SQL: SELECT * FROM llm_view WHERE department = 'HIS' AND instructor LIKE '%Johnson%'

    5. "What are the available evening classes for the upcoming Fall quarter?"
       SQL: SELECT * FROM llm_view WHERE quarter = "Fall 2024" AND time LIKE '%PM%'

    6. "Describe all BME courses in detail."
       SQL: SELECT * FROM llm_view WHERE department = 'BME'


    Table: llm_view
    quarter	        The quarter a course is taught.
    department	    Abbreviation for the academic department offering the course (e.g., AM, ANTH, ART).
    course_number	The numerical part of the course code (e.g., 10, 130, 158).
    course_letter	Optional letter suffix for course number. Always uppercase. If no letter was specified, do not include in query.
    section_number	Numerical code to distinguish different sections of the same course within a quarter. Stored as text. One-digit numbers have leading zeros.
    name	        The title of the course.
    instructor	    Name(s) of the professor(s) teaching the course.
    location	    Building and room where the course is held (LEC: Lecture, STU: Studio, SEM: Seminar, FLD: Field).
    time	        Regular meeting days and times for the course.
    alt_location	If applicable, a secondary location for the course (sometimes indicates separate sections).
    alt_time	    If applicable, a secondary meeting time for the course.
    gen_ed	        General education code(s) the course fulfills (e.g., MF, CC, ER).
    type	        Instruction mode (e.g., In Person, Asynchronous Online, Synchronous Online, Hybrid).
    enrolled	    Current enrollment status, showing the number of students enrolled and the capacity.
    status	        Whether the course is Open or Closed for enrollment.
    url	            Link to the course enrollment page.
    summer_session	Indicates which summer session a course belongs to (if applicable).
    description     A description of the course and the topics it covers.
    requirements    The prerequisites for the course. If empty, the course has no prerequisites.
    notes           Notes about the course. If empty, the course has no notes.
    """

    if not sql_input or sql_input is None or sql_input == "" or sql_input == "None":
        return "Function requires an input"

    conn = psycopg2.connect(SUPABASE_CONN_STRING)
    conn.set_session(readonly=True)

    cur = conn.cursor()
    # sanitize input
    sql_input = sql_input.replace("\"","'").replace("\\","").replace("\'","'")
    # why does this happen?
    sql_input = sql_input.replace('course_letter = " "', 'course_letter = ""').replace("course_letter = ' '","course_letter = ''")

    # force sorting order if not specified, unless it would break the query
    if "ORDER BY" not in sql_input and "DISTINCT" not in sql_input:
        sql_input+=" ORDER BY term DESC, department ASC, course_number ASC, course_LETTER ASC, section_number ASC"
    # print("executing: "+sql_input)

    try:
        cur.execute(sql_input)
    except psycopg2.Error as e:
        print (f"Database error: {e}")

    output = cur.fetchall()
    print(f"retrieved: {output}")
    cur.close()

    return output

# create LLM
init_message = """
You are SlugBot, a helpful course assistant for UCSC students. Answer questions with the provided documents.
Use markdown in your replies. Do not use markdown tables.
Quarters are ordered Fall 2024 -> Winter 2025 -> Spring 2025 -> Summer 2025. Use this order in your responses.
The earliest quarter you have access to is Spring 2022. The current quarter is Spring 2025.
If asked a general question about courses, include all quarters in generated queries. If asked a specific question about a course (ie whether it's full), only include quarters that are open for enrollment.
Use single quotes in generated queries.
Fall 2025 is currently open for enrollment. All other quarters are not.
All data you have access to is public. Do not refuse to share data because of privacy concerns. 
"""
tool_list=[retrieve_general, retrieve_specific]
tool_dict = {
    "retrieve_general": retrieve_general,
    "retrieve_specific": retrieve_specific
}
genai.configure(api_key=GEMINI_KEY)
model = genai.GenerativeModel(
    'gemini-2.5-flash-preview-05-20',
    #'gemini-1.5-flash-002',
    tools=tool_list, 
    system_instruction = init_message,
    generation_config = genai.types.GenerationConfig(
        # Only one candidate for now.
        candidate_count=1,
        # limit temp for more deterministic responses
        temperature=0.1
    ),
)

class Author(str, Enum):
    USER = "USER"
    SYSTEM = "SYSTEM"
    FUNCTION = "FUNCTION"

class ChatMessage(BaseModel):
    message: str
    author: Author

class Chat(BaseModel):
    messages: List[ChatMessage]

#new endpoint (exposes tools to llm, both general and specific search)
@classRecommender.post("/chat/")
def get_stream_history(chat: Chat):

    messages = []
    
    for message in chat.messages[1:]:
        match (message.author):
            case Author.USER:
                messages.append({"role":"user", "parts": [message.message]})
            case Author.FUNCTION:
                fcall = json.loads(message.message)
                if "input" in fcall:
                    messages.append({
                        "role": "model",
                        "parts": [
                            genai.protos.Part(function_call=genai.protos.FunctionCall(name=fcall["name"], args={"input": fcall["input"]}))
                        ]
                    })
                elif "response" in fcall:
                    messages.append({
                        "role": "model",
                        "parts": [
                            genai.protos.Part(function_response=genai.protos.FunctionResponse(name=fcall["name"], response={"result": fcall["response"]}))
                        ]
                    })
                else:
                    raise ValueError("Invalid function call type")
            case _:
                messages.append({"role":"model", "parts": [message.message]})
            
    # finalized_messages[-1]["parts"] = [finalized_prompt]

    print("messages: "+str(messages))
    # (7) Send prompt to LLM

    # gemini's safety settings often trip on random content, so make them less aggressive
    model_safety_settings = {
            HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_ONLY_HIGH,
            HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
            HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
            HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_ONLY_HIGH
        }
    response = model.generate_content(
        messages, 
        tools = tool_list, 
        stream = True,
        safety_settings=model_safety_settings
    )

    def stream_data():
        new_response = None
        try:
            for chunk in response: 
                fcall = False
                for part in chunk.parts:
                    if fn := part.function_call:
                        fcall = True
                        
                        # print(fn) 
                        # human-readable
                        # args = ", ".join(f"{key}={val}" for key, val in fn.args.items())
                        # print(f"{fn.name}({args})")

                        # since there's only one argument for both functions, just hardcode it
                        # first item is always the input name so ignore
                        func_args = ", ".join(f"{val}" for key, val in fn.args.items())
                        print(f"{fn.name}({func_args})")

                        call_parts = [
                            genai.protos.Part(function_call=genai.protos.FunctionCall(name=fn.name, args={"input": func_args}))
                        ]

                        call_message = {"role":"model", "parts": call_parts}
                        messages.append(call_message)

                        # yield str(call_message)
                        # print(FuncCall(name=fn.name, input=func_args))
                        yield json.dumps({"name": fn.name, "input": str(func_args)})+"\n"

                        func_response = tool_dict[fn.name](func_args)
                        # print(f"received: {func_response}") 
                        response_parts = [
                            genai.protos.Part(function_response=genai.protos.FunctionResponse(name=fn.name, response={"result": func_response}))
                        ]
                        #print(response_parts)

                        response_message = {"role":"model", "parts": response_parts}
                        messages.append(response_message)

                        # yield str(response_message)
                        # print(FuncResponse(name=fn.name, response=str(func_response)))
                        yield json.dumps({"name": fn.name, "response": str(func_response)})+"\n"
                if fcall:
                    new_response = model.generate_content(messages, tools = tool_list, stream = True, request_options={"timeout": 600}, safety_settings=model_safety_settings)
                    for new_chunk in new_response:
                        if new_chunk.text:
                            yield new_chunk.text
                if not fcall and chunk.text:
                    yield chunk.text
        except Exception as e:
            print(f"exception: {e}")
            #print(response)
            if new_response:
                print(new_response)
                print(new_response.candidates[0])
                if new_response.candidates and new_response.candidates[0].finish_reason != 1:
                    yield f"An error occured: {e}."
                else:
                    print(new_response.candidates[0].finish_reason, "ignoring previous stop code")

    return StreamingResponse(stream_data())

# suggestions for chat screen
import random
@classRecommender.get("/suggestions")
def get_suggestions():
    suggested_messages = [
        "Courses that fulfill the IM gen ed",
        "Courses taught by Prof. Tantalo this quarter",
        "Open CSE courses this winter",
        "Is CSE 115a still open this winter?",
        "How many people are currently enrolled in CSE 130?",
        "Which professors are teaching CSE 30 in Winter 2025?",
        "What are the prerequisites for CSE 101?",
        "What time is ECON 1 held?",
        "Who teaches ECE 101?",
        "What are the prerequisites for LING 50?",
        "What is LING 80K?",
        "MATH 100 course description",
        "Courses about ethics",
        "List all quarters PHIL 9 was taught.",
        "List all professors for JRLC 1.",
        "Find artificial intelligence courses",
        "Show available online courses for the current quarter."
    ]
    return suggested_messages

#old endpoint (injects documents into prompt directly, only general search)
@classRecommender.get("/")
async def get_stream(userInput : str):
    # (2) generate text embeddings based on user input
    textEmbeddings = text_embedder.run(userInput)

    # (3) keyword based document search
    bm25Docs = keyword_retriever.run(query=userInput)

    # (4) create embeddings for vector based document search
    embeddingDocs = embedding_retriever.run(query_embedding=textEmbeddings['embedding'])

    # (5) merge vector-based docs and keyword-based docs
    mergedDocs = document_joiner.run([bm25Docs["documents"], embeddingDocs["documents"]])

    # return only the top 5 results (or length of the list, whichever is shortest)
    #maxLength = min(len(mergedDocs["documents"]), 5)
    #mergedDocs["documents"] = [mergedDocs["documents"][i] for i in range(maxLength)]


    # (6) put user input and list of documents into the prompt template
    # finalizedPrompt -> string
    prompt_builder = PromptBuilder(template=prompt_template)
    finalizedPrompt = prompt_builder.run(documents=mergedDocs["documents"], question=userInput)["prompt"]

    # (7) Send prompt to LLM
    response = model.generate_content(finalizedPrompt, stream=True)

    def stream_data():
        for chunk in response:
            yield chunk.text
        
    return StreamingResponse(stream_data())
