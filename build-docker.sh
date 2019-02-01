REV=$(git rev-parse --short HEAD)
NLP_IMAGE=nlp-server:$REV

docker build --tag $NLP_IMAGE .
