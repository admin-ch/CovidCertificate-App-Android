#!/bin/bash
set -e

if [[ $# -ne 1 ]]; then
  echo "Pass the path/to/store.apk as an argument!"
  exit 1
fi

echo "Which app would you like to build ('wallet' or 'verifier')?"
read appName

echo "Please enter the build timestamp:"
read buildTimestamp

echo Branch:
read branch

echo "Building apk from source..."

docker images -a | grep "covidcertificate-builder" | awk '{print $3}' | xargs -r docker rmi
docker build -t covidcertificate-builder .
currentPath=`pwd`
docker run --rm -v $currentPath:/home/covidcertificate/external -w /home/covidcertificate covidcertificate-builder /bin/bash -c "git clone https://github.com/admin-ch/CovidCertificate-App-Android.git; cd CovidCertificate-App-Android; keytool -genkeypair -storepass securePassword -keypass securePassword -alias keyAlias -keyalg RSA -keystore $appName/build.keystore -dname 'CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown'; git checkout $branch; echo -n 'Building from commit: '; git rev-parse --verify HEAD; gradle $appName:assembleProdRelease -PkeystorePassword=securePassword -PkeyAlias=keyAlias -PkeyAliasPassword=securePassword -PkeystoreFile=build.keystore -PbuildTimestamp=$buildTimestamp; cp $appName/build/outputs/apk/prod/release/$appName-prod-release.apk /home/covidcertificate/external/$appName-built.apk"

echo "Comparing the APK built from source with the reference APK..."
python3 apkdiff.py $appName-built.apk $1
