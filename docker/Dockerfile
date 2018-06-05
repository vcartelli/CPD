#FROM gdimodica/beng-repo:cpd-develop
FROM cpd-develop-ubuntu

EXPOSE 8901

#RUN echo 'echo "starting mongo..."' >> /root/.profile
#RUN echo 'mongod --config /etc/mongod.conf' >> /root/.profile

WORKDIR /deploy
#RUN echo "set PIPPO=$(dirname \"$0\")" > .profile 

COPY --from=cpd-develop-ubuntu /develop/target/deploy-bundle .

ENTRYPOINT /usr/bin/mongod --config /etc/mongod.conf & /bin/bash





