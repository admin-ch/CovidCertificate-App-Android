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

echo Branch:
read branch

echo "Building apk from source..."

docker images -a | grep "covidcertificate-builder" | awk '{print $3}' | xargs docker rmi
docker build -t covidcertificate-builder .
currentPath=`pwd`
docker run --rm -v $currentPath:/home/covidcertificate/external -w /home/covidcertificate covidcertificate-builder /bin/bash -c "git clone https://github.com/admin-ch/CovidCertificate-App-Android.git; cd CovidCertificate-App-Android; cp /home/covidcertificate/external/$appName/$keystoreFile $appName/$keystoreFile; git checkout $branch; gradle $appName:assembleProdRelease -PkeystorePassword='$keystorePassword' -PkeyAlias=$keyAlias -PkeyAliasPassword='$keyAliasPassword' -PkeystoreFile=$keystoreFile -PbuildTimestamp=$buildTimestamp; cp $appName/build/outputs/apk/prod/release/$appName-prod-release.apk /home/covidcertificate/external/$appName-built.apk"

echo "Comparing the APK built from source with the reference APK..."
python apkdiff.py $appName-built.apk $1 >> diffoutput.txt
