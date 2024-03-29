## Inspiration

Like students at every university, the four of us have to sign up for courses at the start of each quarter. In order to determine what classes are offered or the status of classes we're interested in, we must use [Pisa](https://pisa.ucsc.edu/class_search/), UCSC's class search website. It does exactly what it says on the tin: allows you to search for classes with a variety of filters, such as whether the class is open or waitlisted, the subject, the name of the class, and the number of credits. As a tool, it has numerous issues from a mobile interface standpoint:

* The mobile website is not user-friendly at all
* It does not have a smooth and responsive design
* The design lacks an ease of use for those with accessibility issues
* It does not follow the best practices of mobile usability
* It is difficult to navigate through and search for classes
* The interface was clearly designed to be navigated with a mouse and not a touch screen


<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/pisasearch.gif?token=GHSAT0AAAAAACNEB4YKNME6MHLKXNX4ZFA6ZNNJK7Q" width=30% height=30% alt="pisa current mobile interface">


We believe that we could make an improved version of the class search website's mobile interface, making it accessible to mobile users who want to access the UCSC catalogue anytime and anywhere. 

Among other things, we also realized was that finding classes among the 1500+ courses offered at UCSC every quarter was a challenge. Pisa does not offer any easy way of searching or comparing classes with natural language. In order to search for a class, you must have prior knowledge of the class in question ranging from its name, department, catalog number, or the professor in charge. To make this easier, we envisioned a chatbot powered by a large language model (LLM) which could use a technique called [retrieval-augmented generation](https://blogs.nvidia.com/blog/what-is-retrieval-augmented-generation/) (RAG) to scan a database of UCSC courses and return the courses most relevant to a user's course. 


## What it does

The app we created allows users to search for classes intuitively in a simple and easy to navigate interface.


<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/search%20for%20cse%20100.gif?token=GHSAT0AAAAAACNEB4YKHNOCL26SAKTVI6UKZNNKKLQ" width=30% height=30% alt="searhing for a class">


The results page presents information in a similarly easy-to-read manner. Tapping on any result brings up a more detailed results page. If the user still wants even more details, they can tap on a button to take them to Pisa's page for it.


<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/result%20to%20pisa.gif?token=GHSAT0AAAAAACNEB4YKX3HN53QZYM5ZE2ROZNNKLFA" width=30% height=30% alt="result to pisa">


Advanced filters on the main search page also allow for more fine tuned results.

<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/ge%20filter%20search.gif?token=GHSAT0AAAAAACNEB4YLWR7EOFOWV7HAZ7G4ZNNKMLA" width=30% height=30% alt="result to pisa">


The integrated LLM-based chatbot can be talked to via a button on the bottom navigation bar. It can be asked questions about courses to take.


<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/llm%20response.gif?token=GHSAT0AAAAAACNEB4YKS6WCKMTIZLAO4FBMZNNKN4A" width=30% height=30% alt="llm response">


## How we built it

First, we set our sights on creating a database of all UCSC courses. To do this, we wrote a webscraper in Python that scraped the entire website for all 1,456 courses offered this quarter, as well as the 1000+ courses offered in each of the prior four quarters for a total of close to 6000 courses. We then pushed this to [Supabase](https://supabase.com/), a PostgresSQL database. In order to update the database with the most relevant information, the scraper is re-run at the top of every hour. Within the app itself, whenever a user searches for a course, this database is queried for the most relevant courses and all its information. 


<div>
<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/database.png?token=GHSAT0AAAAAACNEB4YKNYNHEGD46ICC6PECZNNJ5GQ" width=100% height=30% alt="pisa current mobile interface">

<img src="https://raw.githubusercontent.com/prapooskur/CruzHacks-2024-Project/main/images/search%20results.png?token=GHSAT0AAAAAACNEB4YLVEQNQYC23QKGYMQYZNNJ6FQ" width=30% height=30% alt="pisa current mobile interface">
</div>

Next up was the LLM chatbot. The model we chose was Google's [Gemini](https://blog.google/technology/ai/google-gemini-ai/), the same model that powers Bard, Google's ChatGPT competitor. Now we couldn't simply give it our entire database and tell it to extract the most relevant courses (though that didn't stop us from trying). We needed a way to extract the classes that closely matched the user's input query, whether that be a question ("What astronomy courses are offered?") or an imperative statement ("Recommend me classes about chemistry"). In order to do this, we decided on [Haystack](https://haystack.deepset.ai/), an open source Python framework for implementing retrieval-augmented generation. We wrote Python code that pulled our entire repository of course data into a single file. Using Haystack, we wrote a seven-stage pipeline for getting user input:

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

Once we got the pipeline running, we used the Python FastAPI and Uvicorn libraries to create an API endpoint that the app could use to send the user's query and get back Gemini's response. 


## Challenges we ran into

One of the hurdles we ran into was attempting to find ways to fine tune the LLM response. Give it too simple of a prompt and it would be saying too much to even fit on screen. Give it too restrictive of a prompt and it wouldn't give enough information. We had to engineering the prompt in such a way that it would balance just the right amount of information given.

Another challenge we ran into was turning Gemini's response into a proper API. Because Gemini returned its response in chunks at a time, we had two choices: either wait for it to return its *entire* response before handing the response to the app (which would be easier but result in longer wait times), or attempt to stream the response API to the app as the responses come in (harder, but would reduce wait times). We tried various methods of passing the responses to the app as it was returned from Gemini, such as chunking the data into separate JSON responses, but to no avail. Eventually, in the interest of time we decided to go with the former option of returning the response all at once so we could make the rest of the app function. 

## Accomplishments that we're proud of

We're really proud of the UI/UX of the app. The mobile experience is significantly improved over the website. The UI was modernized to be more aesthetically pleasing, and content is presented in a easily digestible manner. The UX is markedly enhanced with proper support for phones and touch screen devices. It up to date with modern UI principles (we follow Google's Material Design 3 guidelines) and was designed with modern UI prototyping tools such as Figma. It is also integrated with a lot of common touch screen actions such as swiping to the left of the screen to go back the previous screen. 

We are also incredibly proud of the LLM integration. It was a massive undertaking to delve into the realm of large language models, and especially something as complicated as retrieval-augmented generation. By creating our own pipeline instead of going with a cloud solution, we learned an incredible amount of information about the underlying workings of language models and retrieval pipelines.


## What we learned

We definitely learned a lot of stuff. We learned to work with a proper cloud SQL database for our backend data and how to integrate it with webscraped data. We also learned a lot about how LLMs could be used to search documents in addition to simple conversations. We also learned the complexity and depth of UX/UI design on Figma. We also learned a lot about app development and integrating it with all of our complex moving parts. 

## What's next for SlugCourses

There are numerous future features and improvements that we can make to SlugCourses. This include the following:

* Refine the current search functionality
* Improving the functionality of the chatbot
* Create an integration using Rate My Professor
* Develop iOS/iPadOS and desktop versions of the app

This experience was invaluable and taught us a lot about app development, user interface design, and natural language processing. We are proud of what we have accomplished and we hope to continue working on SlugCourses to make it even better for UCSC students. 