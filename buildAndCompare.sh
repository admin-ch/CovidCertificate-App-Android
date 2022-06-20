#!/bin/bash

# Script to automate the building and comparing of the CovidCertificate apps
#
# The first and only argument should be the path to the APK to be compared.

set -eu

if [[ $# -ne 1 ]]; then
  echo "Pass the path/to/store.apk as an argument!"
  exit 1
fi
referenceApk=$1

echo "Which app would you like to build ('wallet' or 'verifier')?"
read appName

echo "Do you want to provide a keystore [Yn]?"
read willProvideKeystore

case "$willProvideKeystore" in
  # Case 1: Autogenerate a dummy keystore
  [nN][oO]|[nN])
  echo "[WARNING] Auto-generating a dummy keystore with default credentials. Do NOT use the resulting APK!"
  rm -f "$appName"/insecure.keystore
  # Generate a keystore with default credentials that is only valid 1 day
  keytool -genkeypair -storepass password -keypass password -alias keyAlias -keyalg RSA -keystore "$appName"/insecure.keystore -dname 'CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown' -validity 1
  keystoreFile="$appName"/insecure.keystore
  keystorePassword=password
  keyAlias=keyAlias
  keyAliasPassword=password
  ;;

  # Case 2: Let the user choose a keystore
  *)
  echo "Please enter the keystore filename (e.g. wallet/build.keystore):"
  read keystoreFile

  echo "Please enter the keystore password:"
  read -s keystorePassword

  echo "Please enter the keyAlias:"
  read keyAlias

  echo "Please enter the keyAlias password:"
  read -s keyAliasPassword
  ;;
esac

echo "Please enter the build timestamp (e.g. 1622186583268):"
read buildTimestamp

# This is necessary because Ubique's gradle plugin will automatically set the branch.
# Here we want to override it in order to reproduce the build.
echo "Please enter the branch off which the release was build (e.g. release/version-1.0.0):"
read buildBranch

echo "Please enter the git tag (e.g. v2.7.0-2700-wallet) or branch (e.g. release/version-1.0.0) to be reproduced."
echo "This is what will be checked out and reproduced."
read tree

echo "Do you want to build the F-Droid version [yN]?"
read fdroid

# Set gradle task to be run
case "$fdroid" in
  [yY][eE][sS]|[yY])
  overrideMinSdk="-PminSdkVersion=24"
  ;;

  *)
  overrideMinSdk=""
  ;;
esac

echo "Building apk from source..."

# Clean up any existing images
docker images -a | grep "covidcertificate-builder" | awk '{print $3}' | xargs -r docker rmi

# Build a fresh container image
docker build -t covidcertificate-builder .

# Prepare the build command (for readability)
currentPath=`pwd`
buildCommand=$(cat <<EOF
git clone https://github.com/admin-ch/CovidCertificate-App-Android.git;
cd CovidCertificate-App-Android;
git checkout $tree;
gradle $appName:assembleProdRelease -PkeystorePassword='$keystorePassword' -PkeyAlias=$keyAlias -PkeyAliasPassword='$keyAliasPassword' -PkeystoreFile=/home/covidcertificate/external/$keystoreFile -PbuildTimestamp=$buildTimestamp -Pbranch=$buildBranch $overrideMinSdk;
cp $appName/build/outputs/apk/prod/release/$appName-prod-release.apk /home/covidcertificate/external/$appName-built.apk
EOF
)
buildCommand=$(echo -n "$buildCommand" | tr '\n' ' ' ) # replace newlines by spaces

# Build the app in the container
docker run --rm -v "$currentPath":/home/covidcertificate/external -w /home/covidcertificate covidcertificate-builder /bin/bash -c -eux "$buildCommand"

# Remove dummy keystore again
rm -f "$appName"/insecure.keystore

echo "Comparing the APK built from source with the reference APK..."
python3 apkdiff.py $appName-built.apk $referenceApk

