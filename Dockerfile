FROM openjdk:11-slim

RUN mkdir -p /app /app/resources
WORKDIR /app
COPY target/uberjar/voila-api-uberjar.jar .
COPY public public

EXPOSE 9000

CMD java -jar voila-api-uberjar.jar
