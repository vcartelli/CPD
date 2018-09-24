# Develop image
FROM alpine:3.6 as builder

#Configure jdk
RUN apk add --no-cache openjdk8

#Configure the develop environment 
RUN apk update
RUN apk add maven

WORKDIR /develop

ADD pom.xml /develop
RUN mvn verify --fail-never -P docker-verify
ADD . /develop
RUN ./docker/docker.prepare-bundle.sh

# Deploy image
FROM alpine:3.6

#Configure jre
RUN apk add --no-cache openjdk8-jre

#Configure the production environment
RUN apk update
RUN apk add --no-cache mongodb==3.4.4-r0 mongodb-tools
RUN apk add bash
RUN apk add vim

EXPOSE 8901

WORKDIR /deploy
ADD docker/mongodb.conf /deploy

COPY --from=builder /develop/target/deploy-bundle .

ENTRYPOINT /usr/bin/mongod --config /deploy/mongodb.conf & /bin/bash
