#!/bin/bash
set -e

if [[ $# -ne 1 ]]; then
  echo "Pass the path/to/store.apk as an argument!"
  exit 1
fi

echo "Which app would you like to build ('wallet' or 'verifier')?"
read appName

echo "Please enter the keystore filename:"
read keystoreFile

echo "Please enter the keystore password:"
read -s keystorePassword

echo "Please enter the keyAlias:"
read keyAlias

echo "Please enter the keyAlias password:"
read -s keyAliasPassword

echo "Please enter the build timestamp:"
read buildTimestamp

# Make sure we have a full clean build
rm -rf $appName/build
rm -rf .gradle

echo "Building apk from source..."
docker build -t covidcertificate-builder .
currentPath=`pwd`
docker run --rm -v $currentPath:/home/covidcertificate -w /home/covidcertificate covidcertificate-builder gradle $appName:assembleProdRelease -PkeystorePassword=$keystorePassword -PkeyAlias=$keyAlias -PkeyAliasPassword=$keyAliasPassword -PkeystoreFile=$keystoreFile -PbuildTimestamp=$buildTimestamp

cp $appName/build/outputs/apk/prod/release/$appName-prod-release.apk $appName-built.apk

echo "Comparing the APK built from source with the reference APK..."
python apkdiff.py $appName-built.apk $1
