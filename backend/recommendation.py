import pickle, os, re, asyncio
from haystack import Pipeline
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.retrievers.in_memory import InMemoryBM25Retriever, InMemoryEmbeddingRetriever
from haystack.components.embedders import SentenceTransformersTextEmbedder
from google_ai_haystack.generators.gemini import GoogleAIGeminiGenerator
from haystack.components.builders.prompt_builder import PromptBuilder
from supabase import create_client, client
from dotenv import load_dotenv
from fastapi import FastAPI


# create app and load .env
classRecommender = FastAPI()
load_dotenv()
geminiKey = os.getenv("KEY")
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
If you choose to have bullet point descriptions for each class, make sure each line is in a spartan tone.
'''


# load documents into store
document_file = open("backend/updatedclasses", mode="rb")
document_store = pickle.load(document_file)
document_file.close()


# set up embedders and retrievers
query_instruction = "Represent this sentence for searching relevant passages:"
text_embedder = SentenceTransformersTextEmbedder(model="BAAI/bge-large-en-v1.5", prefix = query_instruction)
embedding_retriever = InMemoryEmbeddingRetriever(document_store = document_store)

bm25_retriever = InMemoryBM25Retriever(document_store=document_store)
document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")
prompt_builder = PromptBuilder(template=prompt_template)
llm = GoogleAIGeminiGenerator(model="gemini-pro", api_key=geminiKey)


# set up pipeline
query_pipeline = Pipeline()
query_pipeline.add_component("text_embedder", text_embedder)
query_pipeline.add_component("embedding_retriever", embedding_retriever)
query_pipeline.add_component("bm25_retriever", bm25_retriever)
query_pipeline.add_component("joiner", document_joiner)
query_pipeline.add_component("prompt_builder", prompt_builder)
query_pipeline.add_component("llm", llm)

query_pipeline.connect("text_embedder.embedding", "embedding_retriever.query_embedding")
query_pipeline.connect("embedding_retriever", "joiner")
query_pipeline.connect("bm25_retriever", "joiner")
query_pipeline.connect("joiner", "prompt_builder.documents")
query_pipeline.connect("prompt_builder", "llm")



async def GetRecommendations(userPrompt : str, query_pipeline : Pipeline) -> str:

    print(userPrompt)
    results = query_pipeline.run(
        {
            "bm25_retriever": {"query": userPrompt},
            "text_embedder":{"text": userPrompt},
            "prompt_builder": {"question": userPrompt},
        }
    )

    recommendations = str(results["llm"]["answers"][0]).replace("\\n", "\n")

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
    #         recommendations = recommendations.replace(match, f"[{department} {catalogNum}]({classID})")



    #print(response)

    return recommendations



@classRecommender.get("/")
def response(prompt : str):
    llmResponse = asyncio.run(GetRecommendations(prompt, query_pipeline))
    return {llmResponse}
