#! /bin/bash

REV=$(git rev-parse --short HEAD)
NLP_IMAGE=nlp-server:$REV

sudo docker build --tag $NLP_IMAGE .
