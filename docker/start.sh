#!/bin/bash
cd "$(dirname "$0")"
if [ ! -d log ]; then
    mkdir log
fi
#check if config files already exist

#PRODUCTION DIRS AND FILES
CONFDIR=./conf
WEBASSETSCONFDIR=./web/assets/conf/
WEBSWAGGERUIDIR=./web/swagger-ui/
WEBASSETSSWAGGERDIR=./web/assets/swagger/
WEBROOTEN=./web/ROOT/en/
WEBROOTIT=./web/ROOT/it/
WEBROOTES=./web/ROOT/es/
WEBROOTGL=./web/ROOT/gl/

PRODUCTIONFILE=./.properties
DOCKERFILE=./docker.properties
JSONCONF1=$CONFDIR/config.json
DOCKERJSONCONF1=$CONFDIR/docker.config.json
JSONCONF2=$WEBASSETSCONFDIR/config.json
DOCKERJSONCONF2=$WEBASSETSCONFDIR/docker.config.json
JSONCONF3=$WEBSWAGGERUIDIR/swagger.json
DOCKERJSONCONF3=$WEBSWAGGERUIDIR/docker.swagger.json
JSONCONF4=$WEBASSETSSWAGGERDIR/private-API.json
DOCKERJSONCONF4=$WEBASSETSSWAGGERDIR/docker.private-API.json
CLUSTERCONF=$CONFDIR/cluster.xml
DOCKERCLUSTERCONF=$CONFDIR/docker.cluster.xml
INDEXHTMLEN=$WEBROOTEN/index.html
DOCKERINDEXHTMLEN=$WEBROOTEN/docker.index.html
INDEXHTMLES=$WEBROOTES/index.html
DOCKERINDEXHTMLES=$WEBROOTES/docker.index.html
INDEXHTMLIT=$WEBROOTIT/index.html
DOCKERINDEXHTMLIT=$WEBROOTIT/docker.index.html
INDEXHTMLGL=$WEBROOTGL/index.html
DOCKERINDEXHTMLGL=$WEBROOTGL/docker.index.html
KEYSTOREFILE=./keystore.jks
OAUTH2PROVIDERSFILE=./oauth2providers.json

#BACKUP DIRS AND FILES
PERSISTENTCONFIGDIR=./cpd-configuration

PRODUCTIONFILE_BK=$PERSISTENTCONFIGDIR/.properties
JSONCONF1_BK=$PERSISTENTCONFIGDIR/config_1.json
JSONCONF2_BK=$PERSISTENTCONFIGDIR/config_2.json
JSONCONF3_BK=$PERSISTENTCONFIGDIR/swagger.json
JSONCONF4_BK=$PERSISTENTCONFIGDIR/private-API.json
CLUSTERCONF_BK=$PERSISTENTCONFIGDIR/cluster.xml
INDEXHTMLEN_BK=$PERSISTENTCONFIGDIR/index_en.html
INDEXHTMLES_BK=$PERSISTENTCONFIGDIR/index_es.html
INDEXHTMLIT_BK=$PERSISTENTCONFIGDIR/index_it.html
INDEXHTMLGL_BK=$PERSISTENTCONFIGDIR/index_gl.html
KEYSTOREFILE_BK=$PERSISTENTCONFIGDIR/keystore.jks
OAUTH2PROVIDERSFILE_BK=$PERSISTENTCONFIGDIR/oauth2providers.json

if [ ! -f $PRODUCTIONFILE_BK ]; then
    #echo "configuration files from a previous setup have been found. Do you want to keep them?[Y/n]"
    #if [ "$keepit" = "n" ] ; then
        echo "running a new configuration procedure ......." 
        ./configure.sh
    #fi
    else
	echo "restoring configuration files from a previous setup ..........."
	cp $PRODUCTIONFILE_BK $PRODUCTIONFILE
	cp $JSONCONF1_BK $JSONCONF1	
	cp $JSONCONF2_BK $JSONCONF2
	cp $JSONCONF3_BK $JSONCONF3
	cp $JSONCONF4_BK $JSONCONF4
	cp $CLUSTERCONF_BK $CLUSTERCONF
	cp $INDEXHTMLEN_BK $INDEXHTMLEN
	cp $INDEXHTMLES_BK $INDEXHTMLES
	cp $INDEXHTMLIT_BK $INDEXHTMLIT
	cp $INDEXHTMLGL_BK $INDEXHTMLGL
	cp $KEYSTOREFILE_BK $KEYSTOREFILE
	cp $OAUTH2PROVIDERSFILE_BK $OAUTH2PROVIDERSFILE

fi	

APP_ID="cpd-server"
if (java -jar "$APP_ID".jar list | grep -q "$APP_ID")
then
  echo "$APP_ID is already running: use stop.sh and then start.sh to re-run"
else
    echo "starting $APP_ID..."
    java -jar "$APP_ID".jar start --vertx-id="$APP_ID" -Dvertx.hazelcast.config="./conf/cluster.xml" -cluster
fi
