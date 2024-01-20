import requests, json
from bs4 import BeautifulSoup

URL = "https://pisa.ucsc.edu/class_search/index.php"
PISA_API = "https://my.ucsc.edu/PSIGW/RESTListeningConnector/PSFT_CSPRD/SCX_CLASS_DETAIL.v1/"
MAX_RESULTS = "1"


def queryPisa(term : str) -> list[dict]:
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
    for panel in doc.select(".panel.panel-default.row"):
        locations = len(panel.select(".fa-location-arrow"))
        summer = len(panel.select(".fa-calendar")) != 0

        section = {
            "name": panel.select("a")[0].text.replace("\xa0\xa0\xa0", ' ').strip(),
            "id": int(panel.select("div > a")[0].text) if panel.select("div > a")[0].text.isdigit() else 0,
            "instructor": panel.select(".col-xs-6:nth-child(2)")[0].text.split(": ")[1].replace(",", ", ").strip(),
            "location": panel.select(".col-xs-6:nth-child(1)")[1].text.split(": ", 1)[1].strip(),
            "time": panel.select(".col-xs-6:nth-child(2)")[1].text.split(": ")[1].strip() if len(panel.select(".col-xs-6:nth-child(2)")[1].text.split(": ")) > 1 else "None",
            "location2": panel.select(".col-xs-6:nth-child(3)")[0].text.split(": ", 1)[1].strip() if locations > 1 else "None",
            "time2": panel.select(".col-xs-6:nth-child(4)")[0].text.split(": ")[1].strip() if locations > 1 else "None",
            "count": panel.select(".col-xs-6:nth-child({})".format(5 if summer else 4))[locations - 1].text.strip(),
            "mode": panel.select("b")[0].text.strip(),
            "summerSession": panel.select(".col-xs-6:nth-child(4)")[0].text.split(": ")[1].strip() if summer else "None",
            "url": panel.select("a")[0]['href'].strip(),
            "status": panel.select("h2 .sr-only")[0].text.strip()
        }

        pisaApiResponse = json.loads(requests.get(PISA_API + f'{term}/{section["id"]}').text)
        section["gened"] = pisaApiResponse["primary_section"]["gened"]

        sections.append(section)


    return sections