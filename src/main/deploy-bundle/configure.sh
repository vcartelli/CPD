#!/usr/bin/env bash

EXAMPLEFILE=example.properties
PRODUCTIONFILE=.properties
EXAMPLEJSONCONF1=./conf/example.config.json
JSONCONF1=./conf/config.json
EXAMPLEJSONCONF2=./web/assets/conf/example.config.json
JSONCONF2=./web/assets/conf/config.json
KEYSTOREFILE=keystore.jks
OAUTH2PROVIDERSFILE=.oauth2providers.json

#checking if oauth2providers file exists
if [ -f $OAUTH2PROVIDERSFILE ]; then
	echo "found a $OAUTH2PROVIDERSFILE file. Do you want to keep it? (Y/n)"
        read keepit
        if [ "$keepit" = "n" ]; then
                echo "generate a new $OAUTH2PROVIDERSFILE file (use example.oauth2providers.json as a template) and re-run the script!"
		exit 0
        fi
else
	echo "generate a $OAUTH2PROVIDERSFILE file (use example.oauth2providers.json as a template) and re-run the script!"
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

#checking if PRODUCTIONFILE exists
if [ -f $PRODUCTIONFILE ]; then
	echo "found $PRODUCTIONFILE file. Do you want to keep it? (Y/n)"
	read keepit
	if [ "$keepit" = "" ] || [ "$keepit" = "y" ] || [ "$keepit" = "Y" ]; then 
		exit 0;
	else
		echo "Loading existing $PRODUCTIONFILE file. Creating json config files from template..."
		cp $EXAMPLEJSONCONF1 $JSONCONF1	
		cp $EXAMPLEJSONCONF2 $JSONCONF2
		#cp $EXAMPLEFILE $PRODUCTIONFILE
	fi
else 
	#creating a PRODUCTIONFILE file
	echo "$PRODUCTIONFILE file not found. Creating from template..."
	cp $EXAMPLEFILE $PRODUCTIONFILE
	echo "Creating json config files from template..."
	cp $EXAMPLEJSONCONF1 $JSONCONF1
	cp $EXAMPLEJSONCONF2 $JSONCONF2
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

foundoauth2providers=false

for input in $(grep "^[^#\!].*$" $PRODUCTIONFILE);
do 
#cat "$input" | xxd -p -r > "${input%'.hex'}"; rm "$input"; 
IFS='=' read -r -a array <<< "$input"
echo "read ${array[0]}=${array[1]}"
#if  [ ${array[0]} != "cpd.server.pub.scheme" ] || [ ${array[0]} != "cpd.oauth2.origin" ] ; then
if ! [[ ${array[0]} =~ ^cpd\.(server\.scheme|oauth2\.origin|oauth2\.providers)$ ]] ; then  
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

elif [ $foundoauth2providers = false -a ${array[0]} = "cpd.oauth2.providers" ]; then
	foundoauth2providers=true

	if [[ ${array[1]} != "[*" ]]; then
		#array[1]=${array[1]}	
	#else	
		array[1]=$(cat $OAUTH2PROVIDERSFILE | sed ':a;N;$!ba;s/\n//g' | tr -d '[:space:]')
		#buffer="$(sed ':a;N;$!ba;s/\n//g' <<<$buffer)"
		#trimmed_buffer=$(tr -d '[:space:]' $buffer)
		#$buffer="$(sed 'sE/[\n ]+//g' <<<$buffer)"
		#array[1]=$(tr -d '[:space:]' "$buffer")
		#tr '\n' ' ' "$buffer" | echo
	fi
	#echo "$OAUTH2PROVIDERSFILE json is: ${array[1]}"
fi

echo "writing ${array[0]}=${array[1]} to $PRODUCTIONFILE file"
sed -i "s|${array[0]}=.*|${array[0]}=${array[1]}|" $PRODUCTIONFILE

echo "writing ${array[1]} value to \${${array[0]}} property in $JSONCONF1 file"
sed -i "s#\${${array[0]}}#${array[1]}#" $JSONCONF1

echo "writing ${array[1]} value to \${${array[0]}} property in $JSONCONF2 file"
sed -i "s#\${${array[0]}}#${array[1]}#" $JSONCONF2

#sed -E '/(#.*)/!s/${array[0]}=.*/${array[0]}=${array[1]}/g' $PRODUCTIONFILE
done
