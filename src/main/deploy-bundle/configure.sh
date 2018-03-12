#!/usr/bin/env bash

EXAMPLEFILE=example.properties
PRODUCTIONFILE=.properties
JSONCONF1=config/config.json
JSONCONF2=web/assets/conf/config.json
KEYSTOREFILE=keystore.jks
OAUTH2PROVIDERSFILE=.oauth2providers.json

#checking if oauth2providers file exists
if [ -f $OAUTH2PROVIDERSFILE ]; then
	echo "found an oauth2providerse file. Do you want to keep it? (Y/n)"
        read keepit
        if [ "$keepit" = "n" ]; then
                echo "generate a new oauth2providers file (use example.oauth2providers.json as a template) and re-run the script!"
		exit 0
        fi
else
	echo "generate an oauth2providers file (use example.oauth2providers.json as a template) and re-run the script!"
	exit 0
fi

#checking if keystore exists
if [ -f $KEYSTOREFILE ]; then
	echo "found a keystore file. Do you want to keep it? (Y/n)"
        read keepit
        if [ "$keepit" = "n" ]; then
                echo "generate a new keystore file and re-run the script!"
		exit 0;
        fi
else
	echo "generate a keystore file and re-run the script!"
	exit 0
fi

#checking if .properties exists
if [ -f $PRODUCTIONFILE ]; then
	echo "found $PRODUCTIONFILE file. Do you want to keep it? (Y/n)"
	read keepit
	if [ "$keepit" = "" -o "$keepit" = "y" -o "$keepit" = "Y" ]; then 
		exit 0;
	fi
else 
	#creating a .properties file
	echo "$PRODUCTIONFILE file not found. Creating from template..."
	cp $EXAMPLEFILE $PRODUCTIONFILE
fi


#grep "^[^#\!].*$" example.properties | while read input; 

foundsslenabled=false
sslenabled=false

foundserverscheme=false
serverscheme=""

foundserverhost=false
serverhost=""

foundserverport=false
serverport=""

foundoauth2origin=false


for input in $(grep "^[^#\!].*$" $PRODUCTIONFILE);
do 
#cat "$input" | xxd -p -r > "${input%'.hex'}"; rm "$input"; 
IFS='=' read -r -a array <<< "$input"
echo "read ${array[0]}=${array[1]}"
#if  [ ${array[0]} != "cpd.server.pub.scheme" ] || [ ${array[0]} != "cpd.oauth2.origin" ] ; then
if ! [[ ${array[0]} =~ ^cpd\.(server\.scheme|oauth2\.origin)$ ]] ; then  
	echo "Insert value for property \"${array[0]}\" [${array[1]}]"
	read userinput
	echo "user input=$userinput"
	if [ "$userinput" != "" ]; then
		array[1]=$userinput
	fi
fi

if [ $foundsslenabled = false -a ${array[0]} = "cpd.ssl.enabled" ]; then
		
	foundsslenabled=true	
	sslenabled=${array[1]}

elif [ $foundserverscheme = false -a ${array[0]} = "cpd.server.scheme" ]; then
	foundserverscheme=true
	if [ $sslenabled = true ]; then
		array[1]="https"
		 
	else
		array[1]="http"
	fi
	serverscheme=${array[1]}

elif [ $foundserverhost = false -a ${array[0]} = "cpd.server.host" ]; then
	foundserverhost=true
	serverhost=${array[1]}

elif [ $foundserverport = false -a ${array[0]} = "cpd.server.port" ]; then
	foundserverport=true
	serverport=${array[1]}

elif [ $foundoauth2origin = false -a ${array[0]} = "cpd.oauth2.origin" ]; then
	foundoauth2origin=true
	array[1]=$serverscheme"://"$serverhost":"$serverport
fi

echo "writing ${array[0]}=${array[1]} to $PRODUCTIONFILE file"
#sed -i "s/${array[0]}=.*/${array[0]}=${array[1]}/" $PRODUCTIONFILE

done
