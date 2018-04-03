#FROM frolvlad/alpine-oraclejdk8 as builder
#Configure the develop environment
#RUN apk update
#RUN apk add mongodb
#RUN apk add maven

WORKDIR /app
ADD pom.xml /app
RUN mvn verify --fail-never
ADD . /app
RUN ./prepare-bundle-docker.sh



