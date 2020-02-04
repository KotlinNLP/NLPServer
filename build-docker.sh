#! /bin/bash

REV=$(git rev-parse --short HEAD)
NLP_IMAGE=nlp-server:${REV}
PORT=3000

sudo docker build --tag ${NLP_IMAGE} --build-arg PORT=${PORT} .
