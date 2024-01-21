import pickle, os, re, asyncio, time
#from haystack import Pipeline
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.retrievers.in_memory import InMemoryBM25Retriever, InMemoryEmbeddingRetriever
from haystack.components.embedders import SentenceTransformersTextEmbedder
from google_ai_haystack.generators.gemini import GoogleAIGeminiGenerator
from haystack.components.builders.prompt_builder import PromptBuilder
from supabase import create_client, client
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import google.generativeai as genai



# create app and load .env
classRecommender = FastAPI()

# load API keys from .env
load_dotenv()
geminiKey = os.getenv("GEMINIKEY")
supaKey = os.getenv("SUPAKEY")
supaUrl = os.getenv("SUPAURL")


prompt_template = '''
You are a bot that makes recommendations on what class a user should take.

These are the list of possible classes:
{% for doc in documents %}
    {{ doc.content }}
{% endfor %}
Question: {{question}}
Compile a recommendation to the user based on the list of possible classes and the user input.
If you recommend graduate level courses, separate undergraduate and graduate level classes into two sections.
If there are no graduate level courses in your recommendation, keep the courses in one list.
Your response must be formatted in a bullet point list with a blank line after each bullet point.
You must include the class's code and full name in your response.
Write a a couple brief bullet points per class you recommend. Include a brief summary of prerequisites and any enrollment restrictions.
'''


# load documents into store
document_file = open("backend/updatedclasses", mode="rb")
document_store = pickle.load(document_file)
document_file.close()

# create text embeddings for instruction
# textEmbeddings -> a list of floats
query_instruction = "Represent this sentence for searching relevant passages:"
text_embedder = SentenceTransformersTextEmbedder(model="BAAI/bge-large-en-v1.5", prefix=query_instruction)
text_embedder.warm_up()


# find documents based on documents (vector based)
# embeddingDocs -> dictionary of documents
embedding_retriever = InMemoryEmbeddingRetriever(document_store=document_store)


# find documents based on keyword matches
# bm25Docs -> dictionary of documents
bm25_retriever = InMemoryBM25Retriever(document_store=document_store)


# merge embeddingDocs and bm25Docs via reciprocal rank fusion method
# mergedDocs -> list of documents 
document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")


# create LLM
genai.configure(api_key=geminiKey)
model = genai.GenerativeModel('gemini-pro')


# uvicorn backend.recommendation:classRecommender
@classRecommender.get("/")
async def get_stream(userInput : str):


    # generate text embeddings based on user input
    textEmbeddings = text_embedder.run(userInput)

    # create embeddings (vector based document search)
    embeddingDocs = embedding_retriever.run(query_embedding=textEmbeddings['embedding'])

    # keyword based document search
    bm25Docs = bm25_retriever.run(query=userInput)

    # merge vector-based docs and keyword-based docs
    mergedDocs = document_joiner.run([bm25Docs["documents"], embeddingDocs["documents"]])


    # put user input and list of documents into the prompt template
    # finalizedPrompt -> string
    prompt_builder = PromptBuilder(template=prompt_template)
    finalizedPrompt = prompt_builder.run(documents=mergedDocs["documents"], question=userInput)["prompt"]

    response = model.generate_content(finalizedPrompt, stream=True)

    def stream_data():
        for chunk in response:
            yield chunk.text
            time.sleep(1)

    return StreamingResponse(stream_data())


    # recommendations = str(results["llm"]["answers"][0]).replace("\\n", "\n")

    # classRegex = re.compile("[A-Z]{3,4}\s\d{1,3}[A-Z]?")
    # matches = list(set(re.findall(classRegex, recommendations)))
    # print("classes found:----------")
    # print(matches)
    # print("---------------------")


    # supabase = create_client(supaUrl, supaKey, options=client.ClientOptions(
    #     postgrest_client_timeout=10,
    #     storage_client_timeout=10
    # ))

    # for match in matches:
    #     department = match.split(' ')[0]
    #     catalogNum = match.split(' ')[1]
    #     response = supabase.table("courses").select("id").eq("department", department).eq("course_number", catalogNum).execute()
    #     #print(response)
    #     try:
    #         classID = response.data[0]["id"]
    #     except:
    #         print(f"Warning: {department} {catalogNum} does not exist in database.")
    #     else:
    #         recommendations = recommendations.replace(match, f"[{department} {catalogNum}]({classID})"