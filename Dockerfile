FROM maven:3.6.0-jdk-8-alpine
MAINTAINER github@kotlinnlp.com

RUN mkdir -p /app
WORKDIR /app

COPY ./ .

ENV NLS_PORT 3000

# Build nlp server package
RUN mvn package

RUN mv ./target/nlpserver-*-jar-with-dependencies.jar ./nlpserver.jar && \
	rm -rf ./target

RUN sh ./create-docker-entrypoint.sh

EXPOSE $NLS_PORT

ENTRYPOINT ["./docker-entrypoint.sh"]
