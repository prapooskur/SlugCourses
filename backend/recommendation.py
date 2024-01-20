import sys, pickle
from haystack import Pipeline, Document
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack.document_stores.types import DuplicatePolicy
from haystack.components.joiners.document_joiner import DocumentJoiner
from haystack.components.retrievers.in_memory import InMemoryBM25Retriever, InMemoryEmbeddingRetriever
from haystack.components.embedders import SentenceTransformersDocumentEmbedder, SentenceTransformersTextEmbedder
from google_ai_haystack.generators.gemini import GoogleAIGeminiGenerator
from haystack.components.builders.answer_builder import AnswerBuilder
from haystack.components.builders.prompt_builder import PromptBuilder


document_file = open("backend/combodocumentstore", mode="rb")
document_store = pickle.load(document_file)
document_file.close()

print("documents written")

# Build a RAG pipeline
# prompt_template = """
# You are SlugBot, a helpful course assistant for UCSC students. Given these documents, answer the question. Respond like a person, not a robot. Assume the user is an undergraduate student and cannot take graduate classes without instructor permission.
# Documents:
# {% for doc in documents %}
#     {{ doc.content }}
# {% endfor %}
# Question: {{question}}
# Answer:
# """

question = ""
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

query_instruction = "Represent this sentence for searching relevant passages:"
text_embedder = SentenceTransformersTextEmbedder(model="BAAI/bge-large-en-v1.5", prefix = query_instruction)
embedding_retriever = InMemoryEmbeddingRetriever(document_store = document_store)

bm25_retriever = InMemoryBM25Retriever(document_store=document_store)

document_joiner = DocumentJoiner(join_mode="reciprocal_rank_fusion")

#print("creating ranker...")
#ranker = TransformersSimilarityRanker()
#ranker.warm_up()

prompt_builder = PromptBuilder(template=prompt_template)
llm = GoogleAIGeminiGenerator(model="gemini-pro", api_key="")
#llm = OllamaGenerator(model="dolphin-phi", url="http://localhost:11434/api/generate")


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
query = sys.argv[-1]
print(query)
results = query_pipeline.run(
    {
        "bm25_retriever": {"query": query},
        "text_embedder":{"text": query},
        "prompt_builder": {"question": query},
    }
)

print(str(results["llm"]["answers"]).replace("\\n\\n","\n").replace("\\n","\n"))