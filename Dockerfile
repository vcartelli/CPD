FROM ubuntu:16.04 as builder

#ENV HOME /home/gdimodica/Software/CPD-github/CPD

#Configure the building environment
RUN apt-get update
RUN apt-get install -y software-properties-common
RUN add-apt-repository ppa:openjdk-r/ppa 
RUN apt-get update && apt-get install --fix-missing -y -f \
openjdk-8-jdk \
maven \
mongodb

WORKDIR /app
ADD . /app

RUN ./prepare-bundle.sh
#FROM ubuntu:latest

#Configure the production environment
#RUN apt-get update
#RUN apt-get install -y software-properties-common
#RUN add-apt-repository ppa:openjdk-r/ppa
#RUN apt-get update && apt-get install --fix-missing -y -f \
#openjdk-8-jre \
#mongodb


