#!/usr/bin/env bash
echo "generating the development self-signed certificate..."
keytool -genkey -keyalg RSA -alias develop -keystore keystore.jks -storepass simpatico -validity 1000 -keysize 2048
echo "generating the production self-signed certificate..."
keytool -genkey -keyalg RSA -alias production -keystore keystore.jks -storepass simpatico -validity 1000 -keysize 2048