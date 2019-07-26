FROM maven:3.6.0-jdk-8-alpine
MAINTAINER github@kotlinnlp.com

ARG PORT

#####
## Copy project sources
#####

RUN mkdir /app_src
WORKDIR /app_src
COPY . .

#####
## Build package
#####

RUN mvn clean package

#####
## Copy executable scripts
#####

RUN mkdir /app
RUN mv target/nlpserver-*-jar-with-dependencies.jar /app/run-server.jar

#####
## Remove project sources
#####

WORKDIR /app
RUN rm -r /app_src

#####
## Setup entrypoint
#####

ENTRYPOINT ["java", "-Xmx16g", "-jar", "run-server.jar"]
CMD ["-h"]
EXPOSE $PORT
