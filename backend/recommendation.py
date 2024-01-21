import pickle, os, re, asyncio, time, json
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
Compile a recommendation of the best classes from the list to the user based on the given list of possible classes and the user input.
If you recommend graduate level courses, separate undergraduate and graduate level classes into two sections.
If there are no graduate level courses in your recommendation, keep the courses in one list.
Your response must be formatted in a bullet point list with a blank line after each bullet point.
You must include the class's code and full name in your response.
Write a couple brief bullet points per class you recommend. Include a brief summary of prerequisites and any enrollment restrictions.
'''

# Beware of the 7-stage [redacted] pipeline:
# 1) Load data about all classes into document store
# 2) Generate text embedding for user input
# 3) Create a list of documents that match the input (keyword based search) 
# 4) Create a list of documents that match the input (vector/semantic based search) 
# 5) Join vector/keyword documents into one list of documents, ranked by score
# 6) Insert merged document list and user input into prompt
# 7) Query LLM and return response. 

# (1) load documents into store
document_file = open("backend/cache/classdocuments", mode="rb")
document_store = pickle.load(document_file)
document_file.close()


# (2) create text embeddings for instruction
# textEmbeddings -> a list of floats
query_instruction = "Represent this sentence for searching relevant passages:"
text_embedder = SentenceTransformersTextEmbedder(model="BAAI/bge-large-en-v1.5", prefix=query_instruction)
text_embedder.warm_up()


# (3) find documents based on keyword matches (sparse models)
# bm25Docs -> dictionary of documents
bm25_retriever = InMemoryBM25Retriever(document_store=document_store)


# (4) find documents based on documents (dense models)
# embeddingDocs -> dictionary of documents
embedding_retriever = InMemoryEmbeddingRetriever(document_store=document_store)


# (5) merge embeddingDocs and bm25Docs via reciprocal rank fusion method
# mergedDocs -> list of documents 
document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")


# create LLM
genai.configure(api_key=geminiKey)
model = genai.GenerativeModel('gemini-pro')


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
    maxLength = min(len(mergedDocs["documents"]), 5)
    mergedDocs["documents"] = [mergedDocs["documents"][i] for i in range(maxLength)]


    # (6) put user input and list of documents into the prompt template
    # finalizedPrompt -> string
    prompt_builder = PromptBuilder(template=prompt_template)
    finalizedPrompt = prompt_builder.run(documents=mergedDocs["documents"], question=userInput)["prompt"]

    # (7) Send prompt to LLM
    response = model.generate_content(finalizedPrompt)#, stream=True)

    #def stream_data():
        #for chunk in response:
        #    yield str({f"response": chunk.text})
        
    #    yield json.dumps(response.text)

        #yield str({"eor": "||"})
        #yield str(mergedDocs)

    #return StreamingResponse(stream_data())

    docList = []
    for i in mergedDocs["documents"]:
        docList.append(docToDict(i))

    response_data = {
        "text": str(response.text),
        "document_list": docList
    }
    return json.dumps(response_data)


def docToDict(doc) -> dict:
    return {
        "content": str(doc.content)
    }
