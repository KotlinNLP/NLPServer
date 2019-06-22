FROM maven:3.6.0-jdk-8-alpine
MAINTAINER github@kotlinnlp.com

# Prepare building dir
RUN mkdir /app
WORKDIR /app

# Copy project content
COPY . .

# Build project
RUN mvn package

# Build entrypoint
RUN sh ./create-docker-entrypoint.sh 3000

# Copy executable files
WORKDIR /
RUN mv app/models .
RUN mv app/target/nlpserver-*-jar-with-dependencies.jar nlpserver.jar
RUN mv app/docker-entrypoint.sh .

# Remove project sources
RUN rm -rf /app

# Setup entrypoint
EXPOSE 3000
ENTRYPOINT ["./docker-entrypoint.sh"]
