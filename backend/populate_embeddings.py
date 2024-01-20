import pickle, json
from tqdm import trange, tqdm
import requests
import concurrent.futures
from haystack.components.embedders import SentenceTransformersDocumentEmbedder
from haystack import Document
from course import Course

def termToQuarterName(term : str) -> str:
    match term:
        case "2240":
            return "Winter Quarter 2024"
        case "2238":
            return "Fall Quarter 2023"
        case "2234":
            return "Summer Quarter 2023"
        case "2232":
            return "Spring Quarter 2023"
        case "2230":
            return "Winter Quarter 2023"
        case "2228":
            return "Fall Quarter 2022"
        case "2224":
            return "Summer Quarter 2022"
        case "2222":
            return "Spring Quarter 2022"



def populate():
    print("updating cache...")
    terms = ["2240", "2238", "2234", "2232", "2230", "2228", "2224", "2222"]
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

    with concurrent.futures.ThreadPoolExecutor() as executor:
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
    print(detailedInfo)

    picklefile = open("backend/updatedclasses", mode="wb")
    pickle.dump(detailedInfo, picklefile)
    picklefile.close()    


    jsonfile = open("backend/updatedclasses.json", mode="w")
    json.dump(detailedInfo, jsonfile)
    jsonfile.close()

def populate_embeddings(documents: list[Document]):
    print("updating embeddings...")
    document_embedder = SentenceTransformersDocumentEmbedder(
        model="BAAI/bge-large-en-v1.5")
    document_embedder.warm_up()
    documents_with_embeddings = document_embedder.run(documents)
    picklefile = open("backend/updated", mode="wb")
    pickle.dump(documents_with_embeddings, picklefile)
    picklefile.close()