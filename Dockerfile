FROM openjdk:11-slim

RUN mkdir -p /app /app/resources
WORKDIR /app
COPY target/uberjar/generator-api-uberjar.jar .
COPY public public

EXPOSE 9000

CMD java -jar generator-api-uberjar.jar
