from dotenv import load_dotenv
from supabase import create_client, Client
from tqdm import tqdm
import os, json, re

def format_name(full_name: str):
    """
    Convert full name (e.g. 'Caroline Brett Casey') to format 'Casey,C.B.'
    
    Args:
        full_name (str): Full name with optional middle name(s)
        
    Returns:
        str: Formatted name with last name, first and middle initials
    """
    name_parts = full_name.split()
    last_name = name_parts[-1]
    initials = '.'.join(part[0] for part in name_parts[:-1])
    return f"{last_name},{initials}."

if __name__ == "__main__":
    load_dotenv()
    url: str = os.environ.get("SUPABASE_URL")
    key: str = os.environ.get("SUPABASE_KEY")
    supabase: Client = create_client(url, key)
    # print(supabase)

    with open('./grades.json', 'r') as grades_file:
        grades = json.load(grades_file)
    
    # print(grades)

    processed_grades = []
    grade_map = {}
    for grade in tqdm(grades, desc="Processing Grades", unit="grade"):
        # print(grade["gradeCounts"])
        dept_number = grade["class"].split(" ")
        # last_names = [name.split()[-1] for name in grade["instructors"]]

        names = [format_name(name) for name in grade["instructors"]]


        full_course_number = dept_number[1]
        course_number = full_course_number

        course_match = re.match(r'(\d+)(\D*)', full_course_number)
        course_letter = ""

        if course_match:
            course_number = course_match.group(1)
            course_letter = course_match.group(2)
        
        processed_grade = {
            "term": str(grade["termCode"]),
            "department": dept_number[0],
            "course_number": course_number,
            "course_letter": course_letter,
            "short_name": grade["title"],
            # join names in reverse order, since that's how pisa does it
            "instructor": ", ".join(names[::-1]),
        }
        for key in grade["gradeCounts"].keys():
            if key != "-":
                if key not in grade_map:
                    grade_map[key] = key.lower().replace("+", "_plus").replace("-", "_minus")
                processed_grade[grade_map[key]] = grade["gradeCounts"][key]
        # print(processed_grade)
        processed_grades.append(processed_grade)
        # print(processed_grade)
    supabase.table("grades").upsert(processed_grades).execute()
