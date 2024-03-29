import requests, json, sys, os, re
from dotenv import load_dotenv
from bs4 import BeautifulSoup
from supabase import create_client, Client
from tqdm import tqdm # optional, shows progress bar
import concurrent.futures

URL = "https://pisa.ucsc.edu/class_search/index.php"
PISA_API = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_DETAIL.v1/"
MAX_RESULTS = "2000"

def queryPisa(term: str, gened: bool = False) -> list[dict]:
    load_dotenv()

    info = {
        "action": "results",
        "binds[:term]": term,
        "binds[:reg_status]": "all",
        "binds[:subject]": "",
        "binds[:catalog_nbr_op]": "",
        "binds[:catalog_nbr]": "",
        "binds[:title]": "",
        "binds[:instr_name_op]": "=",
        "binds[:instructor]": "",
        "binds[:ge]": "",
        "binds[:crse_units_op]": "=",
        "binds[:crse_units_from]": "",
        "binds[:crse_units_to]": "",
        "binds[:crse_units_exact]": "",
        "binds[:day]": "",
        "binds[:times]": "",
        "binds[:acad_career]": "",
        "binds[:asynch]": "A",
        "binds[:hybrid]": "H",
        "binds[:synch]": "S",
        "binds[:person]": "P",
        "rec_start": "0",
        "rec_dur": MAX_RESULTS
    }

    response = requests.post(URL, data=info)
    doc = BeautifulSoup(response.text, 'html.parser')

    sections = []

    # debug - disable tqdm
    #for panel in doc.select(".panel.panel-default.row"):
    for panel in tqdm(doc.select(".panel.panel-default.row")):

       

        locations = len(panel.select(".fa-location-arrow"))
        summer = len(panel.select(".fa-calendar")) != 0

        name = panel.select("a")[0].text.replace("\xa0\xa0\xa0", ' ').strip()

        course = name.split(" - ")
        primary = course[0].split(" ", maxsplit=1)
        secondary = course[1].split(" ", maxsplit=1)

        department = primary[0].strip()
        full_course_number = primary[1].strip()
        course_number = full_course_number
        #course_number, course_letter = re.match(r'(\d+)([a-zA-Z]*)', course_number).groups() if re.match(r'(\d+)([a-zA-Z]*)', course_number) else (course_number, '')

        course_match = re.match(r'(\d+)(\D*)', full_course_number)
        course_letter = " "

        if course_match:
            course_number = course_match.group(1)
            course_letter = course_match.group(2)

        section_number = secondary[0].strip()
        description = secondary[1].strip()

        # edge cases
        #some classes have three locations - this causes enrollment to break if left unhandled
        #in summer, enrollment is always the only itme in the array

        if summer:
            enrolled_index = 0
        else:
            enrolled_index = min(locations-1, 1)
        
        id = int(panel.select("div > a")[0].text) if panel.select("div > a")[0].text.isdigit() else 0,
        combined_id = str(term)+"_"+str(id)

        section = {
            "id": combined_id,
            "term": term,
            "department": department,
            "course_number": course_number,
            "course_letter": course_letter,
            "section_number": section_number,
            "short_name": description,
            "instructor": panel.select(".col-xs-6:nth-child(2)")[0].text.split(": ")[1].replace(",", ", ").strip(),
            "location": panel.select(".col-xs-6:nth-child(1)")[1].text.split(": ", 1)[1].strip(),
            "time": panel.select(".col-xs-6:nth-child(2)")[1].text.split(": ")[1].strip() if len(panel.select(".col-xs-6:nth-child(2)")[1].text.split(": ")) > 1 else "None",
            "alt_location": panel.select(".col-xs-6:nth-child(3)")[0].text.split(": ", 1)[1].strip() if locations > 1 else "None",
            "alt_time": panel.select(".col-xs-6:nth-child(4)")[0].text.split(": ")[1].strip() if locations > 1 else "None",
            "enrolled": panel.select(".col-xs-6:nth-child({})".format(5 if summer else 4))[enrolled_index].text.strip(),
            "type": panel.select("b")[0].text.strip(),
            "summer_session": panel.select(".col-xs-6:nth-child(4)")[0].text.split(": ")[1].strip() if summer else "None",
            "url": panel.select("a")[0]['href'].strip(),
            "status": panel.select("h2 .sr-only")[0].text.strip()
        }

        if gened:
            #pisaApiResponse = json.loads(requests.get(PISA_API + f'{term}/{section["id"]}').text)
            #section["gen_ed"] = pisaApiResponse["primary_section"]["gened"]
            pisaApiResponse = json.loads(requests.get(PISA_API + f'{term}/{section["id"]}').text)
            if "primary_section" in pisaApiResponse and "gened" in pisaApiResponse["primary_section"]:
                section["gen_ed"] = pisaApiResponse["primary_section"]["gened"]
            if "primary_section" in pisaApiResponse and "title_long" in pisaApiResponse["primary_section"]:
                section["name"] = pisaApiResponse["primary_section"]["title_long"]

        sections.append(section)
    
    url: str = os.environ.get("SUPABASE_URL")
    key: str = os.environ.get("SUPABASE_KEY")
    supabase: Client = create_client(url, key)

    supabase.table("courses").upsert(sections).execute()
            

        
'''
match(len(sys.argv)):
    case 1:
        print("Run with args pls")
    case 2:
        queryPisa(sys.argv[1], False)
    case _:
        if (sys.argv[2] == "True"):
            queryPisa(sys.argv[1], True)
        else:
            queryPisa(sys.argv[1], False)
'''

#term_list = [2242, 2240, 2238, 2234, 2232, 2230, 2228, 2224]

#with concurrent.futures.ThreadPoolExecutor() as executor:
#    results = list(executor.map(lambda term: queryPisa(str(term), False), term_list))

queryPisa("2242", False)
#queryPisa("2240")

#for term in term_list:
#    queryPisa(str(term))