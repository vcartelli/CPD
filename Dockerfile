#FROM frolvlad/alpine-oraclejdk8 as builder
#Configure the develop environment
#RUN apk update
#RUN apk add mongodb
#RUN apk add maven

#EXPOSE 22
#VOLUME /home/gdimodica/.m2
#WORKDIR /app
#ADD pom.xml /app
#RUN mvn verify --fail-never
#ADD . /app
#RUN ./prepare-bundle-docker.sh

FROM anapsix/alpine-java
#Configure the production environment
RUN apk update
RUN apk add mongodb

WORKDIR /CPD
COPY --from=gdimodica/beng-repo:cpd-develop /app/target/deploy-bundle .
RUN chmod 754 *.sh


