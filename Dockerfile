FROM openjdk:11-slim

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get -o Acquire::Check-Valid-Until=false update
RUN apt-get install -y locales
RUN echo "en_GB.UTF-8 UTF-8" > /etc/locale.gen
RUN dpkg-reconfigure locales
RUN locale-gen

ENV LANG en_GB.UTF-8
ENV LANGUAGE en_GB:en
ENV LC_ALL en_GB.UTF-8

RUN mkdir -p /app /app/resources
WORKDIR /app
COPY target/uberjar/voila-1.0.1-SNAPSHOT-standalone.jar .
CMD java -jar voila-1.0.1-SNAPSHOT-standalone.jar

EXPOSE 9000

CMD java -jar voila-1.0.1-SNAPSHOT-standalone.jar
