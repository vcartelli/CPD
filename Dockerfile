FROM maven:3.5-jdk-8 as builder

LABEL name="CPD builder" \
      version="1.0.0"

ARG PROFILE=develop

COPY . /app

WORKDIR /app

RUN mvn clean package -P ${PROFILE}

FROM openjdk:8

LABEL name="CPD runner" \
      version="1.0.0"

COPY --from=builder /app/target/** /app/

WORKDIR /app

EXPOSE 8080 5701
RUN mkdir /app/log /app/logs 
CMD java -jar /app/cpd-server-1.4-SNAPSHOT-fat.jar