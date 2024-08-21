import pickle, json, sys, os
from tqdm import trange, tqdm
import requests
import concurrent.futures
from haystack.components.embedders import SentenceTransformersDocumentEmbedder
from haystack import Document
from course import Course
from haystack.document_stores.in_memory import InMemoryDocumentStore
from haystack_integrations.document_stores.pgvector import PgvectorDocumentStore
from haystack.document_stores.types.policy import DuplicatePolicy
from haystack.utils import Secret
import argparse

def termToQuarterName(term : str) -> str:
    match term:
        case "2248":
            return "Fall 2024"
        case "2244":
            return "Summer 2024"
        case "2242":
            return "Spring 2024"
        case "2240":
            return "Winter 2024"
        case "2238":
            return "Fall 2023"
        case "2234":
            return "Summer 2023"
        case "2232":
            return "Spring 2023"
        case "2230":
            return "Winter 2023"
        case "2228":
            return "Fall 2022"
        case "2224":
            return "Summer 2022"
        case "2222":
            return "Spring 2022"

def populate():
    print("updating cache...")
    terms = ["2248", "2244", "2242", "2240", "2238", "2234", "2232", "2230", "2228", "2224", "2222"]
    classNums = []
    addedNums = set()
    for term in terms:
        baseUrl = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_LIST.v1/"+term+"?dept="

        allCourses = requests.get(baseUrl).json()
        for value in allCourses.get('classes', []):

            # Access the 'class_nbr' key
            class_nbr = value.get('class_nbr')
            if class_nbr not in addedNums:
                course = (term, str(class_nbr))
                classNums.append(course)
                addedNums.add(class_nbr)

            
    #print(classNums)
    print("beginning download")

    with concurrent.futures.ThreadPoolExecutor(10) as executor:
        # Using a set for dupe protection
        unique_results = set()

        # Define a function to fetch course data
        def fetch_course_data(course_input):
            term = course_input[0]
            baseurl = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_DETAIL.v1/" + term + "/"
            raw = requests.get(baseurl + course_input[1]).json()
            detailedCourse = raw.get('primary_section')

            # Dupe protection
            courseName = detailedCourse.get('subject')+detailedCourse.get('catalog_nbr')
            if (courseName) not in unique_results:
                unique_results.add(courseName)
                course = Course(
                    termToQuarterName(detailedCourse.get('strm')),
                    detailedCourse.get('acad_career'),
                    detailedCourse.get('subject'),
                    detailedCourse.get('catalog_nbr'),
                    detailedCourse.get('title_long'),
                    detailedCourse.get('description'),
                    detailedCourse.get('gened'),
                    detailedCourse.get('requirements'),
                    raw.get('notes'),
                ).to_dict()
                return course

        # Use tqdm for the progress bar
        results = list(tqdm(executor.map(fetch_course_data, classNums), total=len(classNums)))

    detailedInfo = list(filter(None, results))

    #detailedInfo.extend(result[-1] for result in results)
    print("array created")
    #print(detailedInfo)

    picklefile = open("cache/updatedclasses", mode="wb")
    pickle.dump(detailedInfo, picklefile)
    picklefile.close()    


    jsonfile = open("cache/updatedclasses.json", mode="w")
    json.dump(detailedInfo, jsonfile)
    jsonfile.close()


embeddings_model = "WhereIsAI/UAE-Large-V1"
def populate_embeddings(documents: list[Document]):
    print("updating embeddings...")
    document_embedder = SentenceTransformersDocumentEmbedder(model=embeddings_model)
    document_embedder.warm_up()
    documents_with_embeddings = document_embedder.run(documents)
    picklefile = open("cache/classembeddings", mode="wb")
    pickle.dump(documents_with_embeddings, picklefile)
    picklefile.close()

def populate_supabase_embeddings(documents: list[Document]):
    print("updating supabase embeddings...")
    document_embedder = SentenceTransformersDocumentEmbedder(model=embeddings_model)
    document_embedder.warm_up()
    embeddings = document_embedder.run(documents)
    
    document_store = PgvectorDocumentStore(
        connection_string = Secret.from_env_var("PG_CONN_STRING"),
        embedding_dimension=1024,
        vector_function="cosine_similarity",
        recreate_table=False,
        search_strategy="hnsw",
    )

    document_store.write_documents(documents, policy=DuplicatePolicy.OVERWRITE)
    document_store.write_documents(embeddings['documents'], policy=DuplicatePolicy.OVERWRITE)


if __name__ == "__main__":

    parser = argparse.ArgumentParser(
                prog='SlugCourses Embedding Generator',
                description='Generates course embeddings for LLM backend'
            )
    
    parser.add_argument("-c", "--cache", action='store_true', help='store course data in pickle cache')
    parser.add_argument("-e", "--embed", action='store_true', help='generate embeddings from pickle cache and store locally')
    parser.add_argument("-s", "--supabase-embed", action='store_true', help='generate embeddings from pickle cache and store in supabase')

    args = parser.parse_args()

    if not args.cache and not args.embed and not args.supabase_embed:
        parser.print_help()
        parser.exit()

    if args.cache:
        populate()
    
    if args.embed:
        file = open("cache/updatedclasses", mode="rb")
        detailed_info = pickle.load(file)
        file.close()

        # Write documents to InMemoryDocumentStore  
        document_store = InMemoryDocumentStore()
        documents = []
        for i in detailed_info:
            documents.append(Document(content=str(i)))

        document_store.write_documents(documents)

        doc_picklefile = open("cache/classdocuments", mode="wb")
        pickle.dump(document_store, doc_picklefile)
        
        populate_embeddings(documents)
    
    if args.supabase_embed:
        with open("cache/updatedclasses", "rb") as file:
            detailed_info = pickle.load(file)

        documents = [Document(content=str(item)) for item in detailed_info]
        populate_supabase_embeddings(documents)

