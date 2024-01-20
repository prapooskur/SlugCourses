import sys, pickle, os, json, asyncio
from haystack import Pipeline, Document
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack.document_stores.types import DuplicatePolicy
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.retrievers.in_memory import InMemoryBM25Retriever, InMemoryEmbeddingRetriever
from haystack.components.embedders import SentenceTransformersDocumentEmbedder, SentenceTransformersTextEmbedder
from google_ai_haystack.generators.gemini import GoogleAIGeminiGenerator
from haystack.components.builders.answer_builder import AnswerBuilder
from haystack.components.builders.prompt_builder import PromptBuilder
from dotenv import load_dotenv
from populate_embeddings import populate_embeddings



async def GetRecommendations(userPrompt : str) -> str:
    load_dotenv()
    apiKey = os.getenv("KEY")

    updateCache = False

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
    If you choose to have bullet point descriptions for each class, make sure each line is in a spartan tone.
    '''


    # if (updateCache):
    #     print("loading files...")
    #     pickle_text = open("backend/updatedclasses", mode="rb")
    #     documents_text = pickle.load(pickle_text)
    #     pickle_text.close()

    #     pickle_embeddings = open("backend/updated", mode="rb")
    #     documents_with_embeddings = pickle.load(pickle_embeddings)
    #     pickle_embeddings.close()

    #     print("writing documents to store...")
    #     document_store = InMemoryDocumentStore(embedding_similarity_function="cosine")
    #     #print(type(documents_with_embeddings[0]))
    #     document_store.write_documents(documents_with_embeddings["documents"]) #failed here

    #     # documents = []
    #     # print(documents_text)
    #     # for i in documents_text:
    #     #     documents.append(Document(content=str(i)))
    #     # document_store.write_documents(documents_text, DuplicatePolicy.SKIP) #documents -> documents_text

    #     print("writing document store to backend...")
    #     document_file = open("backend/updatedclasses", mode="wb")
    #     documents_text = pickle.dump(document_store, document_file)
    #     document_file.close()
    # else:
        #print("loading document backend")
    document_file = open("backend/updatedclasses", mode="rb")
    document_store = pickle.load(document_file)
    document_file.close()

    query_instruction = "Represent this sentence for searching relevant passages:"
    text_embedder = SentenceTransformersTextEmbedder(model="BAAI/bge-large-en-v1.5", prefix = query_instruction)
    embedding_retriever = InMemoryEmbeddingRetriever(document_store = document_store)

    bm25_retriever = InMemoryBM25Retriever(document_store=document_store)

    document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")

    prompt_builder = PromptBuilder(template=prompt_template)
    llm = GoogleAIGeminiGenerator(model="gemini-pro", api_key=apiKey)


    #print("creating pipeline...")
    query_pipeline = Pipeline()
    query_pipeline.add_component("text_embedder", text_embedder)
    query_pipeline.add_component("embedding_retriever", embedding_retriever)

    query_pipeline.add_component("bm25_retriever", bm25_retriever)

    query_pipeline.add_component("joiner", document_joiner)

    #query_pipeline.add_component("ranker", ranker)

    query_pipeline.add_component("prompt_builder", prompt_builder)
    query_pipeline.add_component("llm", llm)

    query_pipeline.connect("text_embedder.embedding", "embedding_retriever.query_embedding")
    query_pipeline.connect("embedding_retriever", "joiner")
    query_pipeline.connect("bm25_retriever", "joiner")
    query_pipeline.connect("joiner", "prompt_builder.documents")
    query_pipeline.connect("prompt_builder", "llm")

    #print("asking question...")
    # Ask a question
    #query = "Recommend me courses about machine learning" #sys.argv[-1]
    print(userPrompt)
    results = query_pipeline.run(
        {
            "bm25_retriever": {"query": userPrompt},
            "text_embedder":{"text": userPrompt},
            "prompt_builder": {"question": userPrompt},
        }
    )

    recommendations = str(results["llm"]["answers"]).replace("\\n", "\n")
    with open("shit.txt", "w") as file:
        file.write(json.dumps(recommendations, indent=4))

    return recommendations



thing = asyncio.run(GetRecommendations("Recommend me classes about machine learning"))
print(thing)
