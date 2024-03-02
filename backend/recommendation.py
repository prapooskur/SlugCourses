import pickle, os, re, asyncio, time, json
#from haystack import Pipeline
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.retrievers.in_memory import InMemoryBM25Retriever, InMemoryEmbeddingRetriever
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack.components.embedders import SentenceTransformersTextEmbedder
from haystack_integrations.components.generators.google_ai import GoogleAIGeminiGenerator
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
