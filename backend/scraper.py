import requests, json, sys, os
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
    for panel in tqdm(doc.select(".panel.panel-default.row")):

       

        locations = len(panel.select(".fa-location-arrow"))
        summer = len(panel.select(".fa-calendar")) != 0

        name = panel.select("a")[0].text.replace("\xa0\xa0\xa0", ' ').strip()

        course = name.split(" - ")
        primary = course[0].split(" ", maxsplit=1)
        secondary = course[1].split(" ", maxsplit=1)

        department = primary[0].strip()
        course_number = primary[1].strip()

        section_number = secondary[0].strip()
        description = secondary[1].strip()

        #edge cases

        #ignore credit by petitions
        #if section_number == "CBP":
        #    continue
        

        section = {
            "id": int(panel.select("div > a")[0].text) if panel.select("div > a")[0].text.isdigit() else 0,
            "term": term,
            "department": department,
            "course_number": course_number,
            "section_number": section_number,
            "description": description,
            "instructor": panel.select(".col-xs-6:nth-child(2)")[0].text.split(": ")[1].replace(",", ", ").strip(),
            "location": panel.select(".col-xs-6:nth-child(1)")[1].text.split(": ", 1)[1].strip(),
            "time": panel.select(".col-xs-6:nth-child(2)")[1].text.split(": ")[1].strip() if len(panel.select(".col-xs-6:nth-child(2)")[1].text.split(": ")) > 1 else "None",
            "alt_location": panel.select(".col-xs-6:nth-child(3)")[0].text.split(": ", 1)[1].strip() if locations > 1 else "None",
            "alt_time": panel.select(".col-xs-6:nth-child(4)")[0].text.split(": ")[1].strip() if locations > 1 else "None",
            "enrolled": panel.select(".col-xs-6:nth-child({})".format(5 if summer else 4))[locations - 1].text.strip(),
            "type": panel.select("b")[0].text.strip(),
            "summer_session": panel.select(".col-xs-6:nth-child(4)")[0].text.split(": ")[1].strip() if summer else "None",
            "url": panel.select("a")[0]['href'].strip(),
            "status": panel.select("h2 .sr-only")[0].text.strip()
        }

        if gened:
            pisaApiResponse = json.loads(requests.get(PISA_API + f'{term}/{section["id"]}').text)
            section["gen_ed"] = pisaApiResponse["primary_section"]["gened"]

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

term_list = [2238, 2232, 2230, 2228]

with concurrent.futures.ThreadPoolExecutor() as executor:
    results = list(executor.map(lambda term: queryPisa(str(term), True), term_list))