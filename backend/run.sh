#!/bin/bash
poetry run uvicorn recommendation:classRecommender --host 0.0.0.0 --port 8020 --reload
