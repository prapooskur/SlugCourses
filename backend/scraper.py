import requests, json, sys, os, re, concurrent.futures, time
from bs4 import BeautifulSoup, SoupStrainer
from concurrent.futures import ThreadPoolExecutor
from dotenv import load_dotenv
from multiprocessing import Pool
from supabase import create_client, Client
from tqdm import tqdm # optional, shows progress bar
import argparse

URL = "https://pisa.ucsc.edu/class_search/index.php"
PISA_API = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_DETAIL.v1/"
MAX_RESULTS = "2000"


# takes in a single panel from BS4 and parses it into a dictionary
# delegate used in the multithreading in queryPisa
def parseSinglePanel(panel, term: str, detailed: bool) -> dict:

    try:

        locations = len(panel.select(".fa-location-arrow"))
        summer = len(panel.select(".fa-calendar")) != 0

        name = panel.select("a")[0].text.replace("\xa0\xa0\xa0", ' ').strip()

        course = name.split(" - ")
        primary = course[0].split(" ", maxsplit=1)
        secondary = course[1].split(" ", maxsplit=1)

        department = primary[0].strip()
        full_course_number = primary[1].strip()
        course_number = full_course_number

        course_match = re.match(r'(\d+)(\D*)', full_course_number)
        course_letter = " "

        if course_match:
            course_number = course_match.group(1)
            course_letter = course_match.group(2)

        section_number = secondary[0].strip()
        description = secondary[1].strip()

        # these elements are accessed multiple times
        # store them in variables to reduce calls to select() (which is slow)
        panel_nth_child_2 = panel.select(".col-xs-6:nth-child(2)")
        panel_nth_child_4 = panel.select(".col-xs-6:nth-child(4)")[0]  
        panel_div_A = panel.select("div > a")[0].text

        # edge cases
        #some classes have three locations - this causes enrollment to break if left unhandled
        #in summer, enrollment is always the only itme in the array

        if summer:
            enrolled_index = 0
        else:
            enrolled_index = min(locations-1, 1)

        id = int(panel_div_A) if panel_div_A.isdigit() else 0
        combined_id = str(term)+"_"+str(id)

        section = {
            "id": combined_id,
            "term": term,
            "department": department,
            "course_number": course_number,
            "course_letter": course_letter,
            "section_number": section_number,
            "short_name": description,
            "instructor": panel_nth_child_2[0].text.split(": ")[1].replace(",", ", ").strip(),
            "location": panel.select(".col-xs-6:nth-child(1)")[1].text.split(": ", 1)[1].strip(),
            "time": panel_nth_child_2[1].text.split(": ")[1].strip() if len(panel_nth_child_2[1].text.split(": ")) > 1 else "None",
            "alt_location": panel.select(".col-xs-6:nth-child(3)")[0].text.split(": ", 1)[1].strip() if locations > 1 else "None",
            "alt_time": panel_nth_child_4.text.split(": ")[1].strip() if locations > 1 else "None",
            "enrolled": panel.select(".col-xs-6:nth-child({})".format(5 if summer else 4))[enrolled_index].text.strip(),
            "type": panel.select("b")[0].text.strip(),
            "summer_session": panel_nth_child_4.text.split(": ")[1].strip() if summer else "None",
            "url": panel.select("a")[0]['href'].strip(),
            "status": panel.select("h2 .sr-only")[0].text.strip()
        }

        try:
            int(section["course_number"])
        except ValueError:
            # print("invalid course_number", section["course_number"])
            # only a few classes do this and they're all listed as "external", so skip if it's not valid so the upload doesn't fail.
            return

        if detailed:
            pisa_api_response = json.loads(requests.get(PISA_API + f'{term}/{id}').text)
            if "primary_section" in pisa_api_response:
                if "gened" in pisa_api_response["primary_section"]:
                    section["gen_ed"] = pisa_api_response["primary_section"]["gened"]
                if "title_long" in pisa_api_response["primary_section"]:
                    section["name"] = pisa_api_response["primary_section"]["title_long"]
                if "description" in pisa_api_response["primary_section"]:
                    section["description"] = pisa_api_response["primary_section"]["description"]

                # exp
                if "requirements" in pisa_api_response["primary_section"]:
                    section["requirements"] = pisa_api_response["primary_section"]["requirements"]
            if "notes" in pisa_api_response and pisa_api_response["notes"][0]:
                section["notes"] = pisa_api_response["notes"][0]
            elif "notes" in pisa_api_response:
                section["notes"] = pisa_api_response["notes"]
            else:
                # default to empty string to avoid null error
                section["notes"] = ""

        return section
    except Exception as e:
        print(f"Exception: {e}")
        return None


def queryPisa(term: str, detailed: bool = False) -> list[dict]:
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
    sections = []


    response = requests.post(URL, data=info)
    strainedSoup = SoupStrainer(class_="panel panel-default row") # doesnt help much tbh
    doc = BeautifulSoup(response.content, features='lxml', parse_only=strainedSoup)
    #print(f"Time to make BS4 object: {time.time() - startTime} seconds")

    panels = doc.select(".panel.panel-default.row")
    
    with ThreadPoolExecutor() as executor:
        future_to_section = {executor.submit(parseSinglePanel, panel, term, detailed): panel for panel in panels}

        with tqdm(total=len(panels), desc="Processing panels") as pbar:
            for future in concurrent.futures.as_completed(future_to_section):
                try:
                    section = future.result()
                    # if the course number isn't a number the function returns none, so a check is needed
                    if section:
                        sections.append(section)
                except Exception as e:
                    print(f"Error processing course: {str(e)}")
                finally:
                    pbar.update(1)

    return sections
            


load_dotenv()
url: str = os.environ.get("SUPABASE_URL")
key: str = os.environ.get("SUPABASE_KEY")
supabase: Client = create_client(url, key)


parser = argparse.ArgumentParser(description="Scrape and upsert course data.")
parser.add_argument("term", nargs="?", type=int, help="Specify a term to scrape.")
parser.add_argument("-g", "--get-detail", action="store_true", help="Get detailed info.")
parser.add_argument("-a", "--all-terms", action="store_true", help="Scrape all terms.")

# Parse arguments
args = parser.parse_args()

term_list = [2258, 2254, 2252, 2250, 2248, 2244, 2242, 2240, 2238, 2234, 2232, 2230, 2228, 2224]

if args.get_detail:
    print("getting detailed info")

if args.term and args.all_terms:
    print("specific term and all terms were selected, will scrape all terms")

if args.all_terms:
    print("getting every term")
    for term in term_list:
        print("scraping " + str(term))
        sections = queryPisa(term, args.get_detail)
        supabase.table("courses").upsert(sections).execute()
elif args.term:
    if args.term in term_list:
        print("scraping specific term: " + str(args.term))
        sections = queryPisa(args.term, args.get_detail)
        supabase.table("courses").upsert(sections).execute()
    else:
        print(f"{args.term} not in {term_list}")
else:
    terms = [term_list[0], term_list[1]]
    print("scraping " + str(terms))
    for term in terms:
        sections = queryPisa(term, args.get_detail)
        supabase.table("courses").upsert(sections).execute()
