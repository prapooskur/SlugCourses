## Inspiration

Like students at every university, the four of us have to sign up for courses at the start of each quarter. In order to determine what classes are offered or the status of classes we're interested in, we must use [Pisa](https://pisa.ucsc.edu/class_search/), UCSC's class search website. It does exactly what it says on the tin: it allows you to search for classes with a variety of filters, such as whether the class is open or waitlisted, the subject, the name of the class, the number of credits, etc. The useability on desktop is decent, however, there are numerous issues with the mobile website:


* The mobile website is not user-friendly at all
* It does not have a smooth and responsive design
* It does not follow the best practices of mobile usability
* It is difficult to navigate and search for classes with
* The interface was clearly designed to be navigated with a mouse and not a touch screen


As such, we believed that we could make an improved version of the class search website's mobile interface that would the expectations and preferences of the mobile users who want to access the UCSC catalogue anytime and anywhere. 

Another thing we realized was that finding classes to take from the 1500+ courses offered at UCSC every quarter was a challenge. Pisa does not offer any easy way of searching for classes with natural language. In order to search for a class, you must already know its name, or department, or catalogue number, or the professor who teaches it. In order to make this easier, we envisioned a chatbot powered by a large language model (LLM) which could use a technique called [retrieval-augmented generation](https://blogs.nvidia.com/blog/what-is-retrieval-augmented-generation/) to scan a database of UCSC courses and return the courses most relevant to a user's course. 


## What it does



## How we built it

First, we set our sights on creating a database of all UCSC courses. To do this, we wrote a webscraper in Python that scraped the entire website for all 1,456 courses offered this quarter, as well as the 1000+ courses offered in each of the prior four quarters for a total of close to 6000 courses. We then pushed this to [Supabase](https://supabase.com/), a PostgresSQL database. In order to update the database with the most relevant information, the scraper is re-run at the top of every hour. Within the app itself, whenever a user searches for a course, this database is queried for the most relevant courses and all its information. 

//screenshot of database and search results here

Next up was the LLM chatbot. The model we chose was Google's [Gemini](https://blog.google/technology/ai/google-gemini-ai/), the same model that powers Bard, Google's ChatGPT competitor. Now we couldn't simply give it our entire database and tell it to extract the most relevant courses (though that didn't stop us from trying). We needed a way to extract the classes that closely matched the user's input query, whether that be a question ("What astronomy courses are offered?") or a declarative statement ("Recommend me classes about chemistry"). In order to do this, we decided on [Haystack](https://haystack.deepset.ai/), an open source Python framework for implements retrieval-augmented generation. We wrote Python code that pulled our entire repository of course data into a single file. Using Haystack, we wrote a seven-stage pipeline for getting user input:

1. Load data about all classes into document store
2. Generate text embedding for user input
    * This turns the user's input into a vector to be processed 
3. Create a list of courses that match the input (dense document retrieval) 
    * Neural encoder learns the best way to encode text into vectors, taking into account context and semantic meaning of words
4. Create a second list of documents that match the input (sparse document retrieval) 
    * Uses the "bag of words" technique: simply matches keywords without taking context into account
5. Join sparse/dense documents into one list of documents, ranked by score
6. Insert merged document list and user input into prompt
    * Prompt was specifically engineered to make Gemini returns its recommendations in a specific and consistent format with only relevant information
7. Query Gemini LLM with final prompt and return response. 

Once we got the pipeline running, we used the Python FastAPI and Uvicorn libraries in order to create an API endpoint such that the app could send it the user's query and get back Gemini's response. 


## Challenges we ran into

deserializing llm data from server to phone

## Accomplishments that we're proud of

everything

## What we learned

webscraping/database stuff

ai/using llms for document retrieval

apps (love kotlin)


## What's next for Slug Courses

death

being sold to advaith to break into ~~china~~ the ios market

.

.

.

.

.

.

.

.

-------ignore this-----------------------------


We used Python to write a web scraper that grabbed the data of every single class offered at UCSC for the past six quarters and entered it into Supabase, a PostgreSQL database. We also used Python to write a backend API that used the Haystack library to query our database of class data and perform retrieval augmented generation to grab the most classes most relevant to the user's query. These were fed into Gemini, which we prompt engineered to return a bullet point list of the top classes, along with some basic information about each class (description, prerequisites, enrollment restrictions, etc). The data from the database and the Haystack/Gemini API was fed into the Android app, which was written in Kotlin, where it is displayed to the user in an aesthetically pleasing and easy to navigate UI.