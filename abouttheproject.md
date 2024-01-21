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

The app we created allows users to search for classes intuitively in a simple and easy to navigate interface.

//gif of searching for a class

The results page presents information in a similarly easy-to-read manner. Tapping on any result brings up a more detailed results page. If the user still wants even more details, they can tap on a button to take them to Pisa's page for it.

//gif of tapping on result, then tapping on pisa button

Advanced filters on the main search page also allow for more fine tuned results.

//gif of using filters to get a class result

The integrated LLM-based chatbot can be talked to via a button on the bottom navigation bar. It can be asked questions about courses to take.

//gif of switching to chat and asking it a question

//maybe another gif of asking it a second question








## How we built it

First, we set our sights on creating a database of all UCSC courses. To do this, we wrote a webscraper in Python that scraped the entire website for all 1,456 courses offered this quarter, as well as the 1000+ courses offered in each of the prior four quarters for a total of close to 6000 courses. We then pushed this to [Supabase](https://supabase.com/), a PostgresSQL database. In order to update the database with the most relevant information, the scraper is re-run at the top of every hour. Within the app itself, whenever a user searches for a course, this database is queried for the most relevant courses and all its information. 

//screenshot of database and search results here

Next up was the LLM chatbot. The model we chose was Google's [Gemini](https://blog.google/technology/ai/google-gemini-ai/), the same model that powers Bard, Google's ChatGPT competitor. Now we couldn't simply give it our entire database and tell it to extract the most relevant courses (though that didn't stop us from trying). We needed a way to extract the classes that closely matched the user's input query, whether that be a question ("What astronomy courses are offered?") or a declarative statement ("Recommend me classes about chemistry"). In order to do this, we decided on [Haystack](https://haystack.deepset.ai/), an open source Python framework for implementing retrieval-augmented generation. We wrote Python code that pulled our entire repository of course data into a single file. Using Haystack, we wrote a seven-stage pipeline for getting user input:

1. Load data about all classes into document storage 
2. Generate text embedding for user input
    * This turns the user's input into a vector that can be processed 
3. Create a list of documents that match the input (sparse document retrieval) 
    * Uses the "bag of words" technique: simply matches keywords without taking context into account
4. Create a second list of courses that match the input (dense document retrieval) 
    * Neural encoder learns the best way to encode text into vectors, taking into account context and semantic meaning of words
5. Join sparse/dense documents into one list of documents, ranked by score
6. Insert merged document list and user input into prompt
    * Prompt was specifically engineered to make Gemini returns its recommendations in a specific and consistent format with only relevant information
7. Query Gemini LLM with final prompt and return response. 

Once we got the pipeline running, we used the Python FastAPI and Uvicorn libraries in order to create an API endpoint such that the app could send it the user's query and get back Gemini's response. 


## Challenges we ran into

One of the hurdles we ran into was attempting to find ways to fine tune the LLM response. Give it too simple of a prompt and it would be saying too much to even fit on screen. Give it too restrictive of a prompt and it wouldn't give enough information. We had to engineering the prompt in such a way that it would balance just the right amount of information given.

Another challenge we ran into was turning Gemini's response into a proper API. Because Gemini returned its response in chunks at a time, we had two choices: either wait for it to return its *entire* response before handing the response to the app (which would be easier but result in longer wait times), or attempt to stream the response API to the app as the responses come in (harder, but would reduce wait times). We tried various methods of passing the responses to the app as it was returned from Gemini, such as chunking the data into separate JSON responses, but to no avail. Eventually, in the interest of time we decided to go with the former option of returning the response all at once so we could make the rest of the app function. 

## Accomplishments that we're proud of

We're really proud of the UI/UX of the app. The mobile experience is significantly improved over the website. The UI was modernized to be more aesthetically pleasing, and content is presented in a easily digestible manner. The UX is markedly enhanced with proper support for phones and touch screen devices. It is also integrated with a lot of common touch screen actions such as swiping to the left of the screen to go back the previous screen. 

We are also incredibly proud of the LLM integration. It was a massive undertaking to delve into the realm of large language models, and especially something as complicated as retrieval-augmented generation. Understanding document generation a


## What we learned

We definitely learned a lot of stuff. We learned to work with a proper cloud SQL database for our backend data and how to integrate it with webscraped data. We also learned a lot about how LLMs could be used to search documents in addition to simple conversations. We also learned a lot about app development and integrating it with all of our complex moving parts. 

webscraping/database stuff

ai/using llms for document retrieval

apps (love kotlin)


## What's next for SlugCourses

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