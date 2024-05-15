import pickle, os, re, asyncio, time, json
#from haystack import Pipeline
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.retrievers.in_memory import InMemoryBM25Retriever, InMemoryEmbeddingRetriever
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack.components.embedders import SentenceTransformersTextEmbedder
from haystack_integrations.components.generators.google_ai import GoogleAIGeminiGenerator
from haystack.components.builders.prompt_builder import PromptBuilder
import psycopg2
from supabase import create_client, client
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import google.generativeai as genai
import google.ai.generativelanguage as glm
from pydantic import BaseModel
from typing import List
from enum import Enum


# create app and load .env
classRecommender = FastAPI()

# load API keys from .env
load_dotenv()
geminiKey = os.getenv("GEMINIKEY")
supaKey = os.getenv("SUPAKEY")
supaUrl = os.getenv("SUPAURL")
supaPass = os.getenv("SUPABASE_PASSWORD")


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

# (1) load documents into store
bm25_file = open("cache/classdocuments", mode="rb")
bm25_store = pickle.load(bm25_file)
bm25_file.close()

embeddings_file = open("cache/classembeddings", mode="rb")
embeddings_cache = pickle.load(embeddings_file)
embeddings_file.close()


# (2) create text embeddings for instruction
# textEmbeddings -> a list of floats
query_instruction = "Represent this sentence for searching relevant passages: "
text_embedder = SentenceTransformersTextEmbedder(model="WhereIsAI/UAE-Large-V1", prefix=query_instruction)
text_embedder.warm_up()


# (3) find documents based on keyword matches (sparse models)
# bm25Docs -> dictionary of documents
bm25_retriever = InMemoryBM25Retriever(document_store=bm25_store)


# (4) find documents based on documents (dense models)
# embeddingDocs -> dictionary of documents
embeddings_store = InMemoryDocumentStore(embedding_similarity_function="cosine")
embeddings_store.write_documents(embeddings_cache['documents'])
embedding_retriever = InMemoryEmbeddingRetriever(document_store=embeddings_store)


# (5) merge embeddingDocs and bm25Docs via reciprocal rank fusion method
# mergedDocs -> list of documents 
document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")


# instantiate llm tools
def retrieve_courses(input: str):
    """Searches the list of course vectors and returns relevant courses. Use for general queries that are too broad for query_course_database."""
    # (2) generate text embeddings based on user input
    textEmbeddings = text_embedder.run(input)

    # (3) keyword based document search
    bm25Docs = bm25_retriever.run(query=input)

    # (4) create embeddings for vector based document search
    embeddingDocs = embedding_retriever.run(query_embedding=textEmbeddings['embedding'])

    # (5) merge vector-based docs and keyword-based docs
    mergedDocs = document_joiner.run([bm25Docs["documents"], embeddingDocs["documents"]])
    prompt_template = """
    Documents:
    {% for doc in documents %}
        {{ doc.content }}
    {% endfor %}
    """
    prompt_builder = PromptBuilder(template=prompt_template)
    finalizedPrompt = prompt_builder.run(documents=mergedDocs["documents"])["prompt"]
    return finalizedPrompt


conn = psycopg2.connect("user=postgres.cdmaojsmfcuyscmphhjk password="+supaPass+" host=aws-0-us-west-1.pooler.supabase.com port=5432 dbname=postgres")
conn.set_session(readonly=True)
def query_course_database(sql_input: str):
    """
    Queries the course SQL database (read-only). Use for queries about a specific class, instructor, or location.
    Example: SELECT * FROM courses WHERE department = 'BME' AND course_number = 160
    Example: SELECT * FROM courses WHERE department = 'HIS' AND course_number = 80 AND course_letter = 'Y'
    Table: courses
    id	            Unique identifier for each course offering. Combines term and course number.
    term	        The term a course is taught.  (2228 - Fall 2022, 2230 - Winter 2023, 2232 - Spring 2023, 2234 - Summer 2023, 2238 - Fall 2023, 2240 - Winter 2024, 2242 - Spring 2024, 2244 - Summer 2024, 2248 - Fall 2024, etc.)
    department	    Abbreviation for the academic department offering the course (e.g., AM, ANTH, ART).
    course_number	The numerical part of the course code (e.g., 10, 130, 158).
    course_letter	Optional letter suffix for course number, used for further course differentiation.
    section_number	Numerical code to distinguish different sections of the same course within a term.
    short_name	    A concise title or abbreviation of the course (e.g., Math Methods I, Israel-Palestine, Adv Photography).
    name	        The full title of the course.
    instructor	    Name(s) of the professor(s) teaching the course.
    location	    Building and room where the course is held (LEC: Lecture, STU: Studio, SEM: Seminar, FLD: Field).
    time	        Regular meeting days and times for the course.
    alt_location	If applicable, a secondary location for the course (sometimes indicates separate sections).
    alt_time	    If applicable, a secondary meeting time for the course.
    gen_ed	        General education code(s) the course fulfills (e.g., MF, CC, ER).
    type	        Instruction mode (e.g., In Person, Asynchronous Online, Synchronous Online, Hybrid).
    enrolled	    Current enrollment status, showing the number of students enrolled and the capacity.
    status	        Whether the course is Open or Closed for enrollment.
    url	            Link to the detailed course information page. Preprend "https://pisa.ucsc.edu/class_search/".
    summer_session	Indicates which summer session a course belongs to (if applicable).
    """

    cur = conn.cursor()
    sql_input = sql_input.replace("\"","'").replace("\\","")
    print(sql_input)

    cur.execute(sql_input)
    output = cur.fetchall()
    print(output)

    cur.close()

    return output


def get_quarter_from_term(term_input: int):
    """
    Given a term from the database, returns a quarter.
    """
    print("matching "+str(term_input))
    match term_input:
        case 2248:
            quarter = "Fall 2024"
        case 2244:
            quarter = "Summer 2024"
        case 2242:
            quarter = "Spring 2024"
        case 2238:
            quarter = "Fall 2023"
        case 2234:
            quarter = "Summer 2023"
        case 2232:
            quarter = "Spring 2023"
        case 2230:
            quarter = "Winter 2023"
        case 2228:
            quarter = "Fall 2023"
        case _:
            return term_input
    return quarter

# create LLM
genai.configure(api_key=geminiKey)
model = genai.GenerativeModel('gemini-1.5-flash-latest',tools=[retrieve_courses, query_course_database, get_quarter_from_term])

class Author(str, Enum):
    USER = "USER"
    SYSTEM = "SYSTEM"

class ChatMessage(BaseModel):
    message: str
    author: Author

class Chat(BaseModel):
    messages: List[ChatMessage]

post_prompt_template = '''
Given these documents, answer the question. 
Documents:
{% for doc in documents %}
    {{ doc.content }}
{% endfor %}
Question: {{question}}
Answer:'''

init_message = """
You are SlugBot, a helpful course assistant for UCSC students. Answer questions with the provided documents.
The current term is Spring 2024. The upcoming terms are Summer 2024 and Fall 2024.
"""


@classRecommender.post("/chat/")
async def get_stream_history(chat: Chat):

    finalized_messages = []
    
    for message in chat.messages:
        if message == chat.messages[0]:
            finalized_messages.append({"role":"model", "parts": [init_message]})
            
        elif message.author == Author.USER:
            finalized_messages.append({"role":"user", "parts": [message.message]})
        else:
            finalized_messages.append({"role":"model", "parts": [message.message]})
    # finalized_messages[-1]["parts"] = [finalized_prompt]

    print(finalized_messages)
    # (7) Send prompt to LLM
    #response = model.generate_content(finalized_messages, stream=True)
    response = model.generate_content(finalized_messages, tools = [retrieve_courses, query_course_database, get_quarter_from_term], stream = True)
    fcall = False
    response.resolve()
    for part in response.parts:
        function_calls = {}
        if fn := part.function_call:
            fcall = True
            function_calls[fn.name] = globals()[fn.name](**fn.args)
    response_parts = [
        glm.Part(function_response=glm.FunctionResponse(name=fn, response={"result": val}))
        for fn, val in function_calls.items()
    ]
    if fcall:   
        finalized_messages.append({"role":"model", "parts": response_parts})
        response = model.generate_content(finalized_messages, stream = True)

    def stream_data():
        for chunk in response:
            yield chunk.text

    return StreamingResponse(stream_data())

#legacy endpoint
# uvicorn backend.recommendation:classRecommender
@classRecommender.get("/")
async def get_stream(userInput : str):

    # (2) generate text embeddings based on user input
    textEmbeddings = text_embedder.run(userInput)

    # (3) keyword based document search
    bm25Docs = bm25_retriever.run(query=userInput)

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


def docToDict(doc) -> dict:
    return {
        "content": str(doc.content)
    }
