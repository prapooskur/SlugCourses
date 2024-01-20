from fastapi import FastAPI
from pydantic import BaseModel
from recommendation import GetRecommendations
import asyncio

app = FastAPI()


@app.get("/")
async def read_items():
    thing = asyncio.run(GetRecommendations("Recommend me classes about machine learning"))
    return thing