##!/usr/bin/env bash
#!/usr/bin/env sh
cd "$(dirname "$0")"/..
echo "starting docker compilation..."
#mvn -DskipTests -Drelax -gs /usr/share/maven/conf/settings-docker.xml package -P $PROFILE
mvn clean package -P docker
echo "copying target jar to deploy-bundle..."
cp target/cpd-server-1.5-SNAPSHOT-fat.jar target/deploy-bundle/cpd-server.jar
chmod 754 target/deploy-bundle/*.sh
