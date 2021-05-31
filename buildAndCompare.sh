#!/bin/bash
set -e

echo "Which app would you like to build (type 'wallet' or 'verifier')?"
read appName

echo Please enter KeystoreFile name:
read keystoreFile

echo Please enter Keystore Password:
read -s keystorePassword

echo Please enter KeyAlias:
read keyAlias

echo Please enter KeyAlias Password:
read -s keyAliasPassword

echo Please enter Build Timestamp:
read buildTimestamp

#make sure we have a full clean build
rm -rf $appName/build
rm -rf .gradle

docker build -t covidcertificate-builder .
currentPath=`pwd`
docker run --rm -v $currentPath:/home/covidcertificate -w /home/covidcertificate covidcertificate-builder gradle $appName:assembleProdRelease -PkeystorePassword=$keystorePassword -PkeyAlias=$keyAlias -PkeyAliasPassword=$keyAliasPassword -PkeystoreFile=$keystoreFile -PbuildTimestamp=$buildTimestamp

cp $appName/build/outputs/apk/prod/release/$appName-prod-release.apk $appName-built.apk

if [[ $# -eq 1 ]] ; then
  echo Comparing the built APK with the reference:
  python apkdiff.py $appName-built.apk $1
fi