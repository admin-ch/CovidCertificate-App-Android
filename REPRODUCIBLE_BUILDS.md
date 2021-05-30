
# Reproducible Builds

## Install Docker

Download and install [Docker](https://www.docker.com/).

## Download the App open-source code

1. Make sure you have `git` installed
2. Clone the Github repository
3. Checkout the Tag that corresponds to the version of your app (e.g., 1.0.0)

```shell
git clone https://github.com/admin-ch/CovidCertificate-App-Android.git ~/CovidCertificate-App-Android
cd ~/CovidCertificate-App-Android
git checkout 1.0.0
```

## Wallet App

### Check your app version and build timestamp

1. Open the app
2. Click on the `i` button in the top-right corner
3. Check the app version in the top right corner
4. Check the build timestamp in the bottom right corner, which is the number before the slash (e.g., 1622186583268), and record its value to be used later

### Build the Wallet app using Docker

1. Build a Docker Image with the required Android Tools
2. Build the App in the Docker Container while specifying the build timestamp that was recorded earlier (e.g., 1595936711208)
3. Copy the freshly-built APK

```shell
cd ~/CovidCertificate-App-Android
docker build -t covidcertificate-builder .
docker run --rm -v ~/CovidCertificate-App-Android:/home/covidcertificate -w /home/covidcertificate covidcertificate-builder gradle wallet:assembleProdRelease -PkeystorePassword=securePassword -PkeyAlias=keyAlias -PkeyAliasPassword=securePassword -PkeystoreFile=build.keystore -PbuildTimestamp=1622186583268
cp wallet/build/outputs/apk/prod/release/wallet-prod-release.apk wallet-built.apk
```

### Extract the Play Store APK from your phone

1. Make sure you have `adb` installed
2. Connect your phone to your computer
3. Extract the APK from the phone

```shell
cd ~/CovidCertificate-App-Android/wallet
adb pull `adb shell pm path ch.admin.bag.covidcertificate.wallet | cut -d':' -f2` wallet-store.apk
```

If you want to check the version of the APK you are pulling from your device:

```shell
adb shell dumpsys package ch.admin.bag.covidcertificate.wallet | grep versionName=| cut -d '=' -f 2
```

### Compare the two files

1. Make sure you have `python` installed
2. Use the `apkdiff` script to compare the APKs

```shell
cd ~/CovidCertificate-App-Android/wallet
python ../apkdiff.py wallet-built.apk wallet-store.apk
```


## Verifier App

### Check your app version and build timestamp

1. Open the app
2. Click on the `i` button in the top-right corner
3. Check the app version in the top right corner
4. Check the build timestamp in the bottom right corner, which is the number before the slash (e.g., 1622186583268), and record its value to be used later

### Build the Verifier app using Docker

1. Build a Docker Image with the required Android Tools
2. Build the App in the Docker Container while specifying the build timestamp that was recorded earlier (e.g., 1595936711208)
3. Copy the freshly-built APK

```shell
cd ~/CovidCertificate-App-Android
docker build -t covidcertificate-builder .
docker run --rm -v ~/CovidCertificate-App-Android:/home/covidcertificate -w /home/covidcertificate covidcertificate-builder gradle verifier:assembleProdRelease -PkeystorePassword=securePassword -PkeyAlias=keyAlias -PkeyAliasPassword=securePassword -PkeystoreFile=build.keystore -PbuildTimestamp=1622186583268
cp verifier/build/outputs/apk/prod/release/verifier-prod-release.apk verifier-built.apk
```

### Extract the Play Store APK from your phone

1. Make sure you have `adb` installed
2. Connect your phone to your computer
3. Extract the APK from the phone

```shell
cd ~/CovidCertificate-App-Android/verifier
adb pull `adb shell pm path ch.admin.bag.covidcertificate.verifier | cut -d':' -f2` wallet-store.apk
```

If you want to check the version of the APK you are pulling from your device:

```shell
adb shell dumpsys package ch.admin.bag.covidcertificate.verifier | grep versionName=| cut -d '=' -f 2
```

### Compare the two files

1. Make sure you have `python` installed
2. Use the `apkdiff` script to compare the APKs

```shell
cd ~/CovidCertificate-App-Android/verifier
python ../apkdiff.py verifier-built.apk verifier-store.apk
```

## Building and checking with script

As an alternative you can also start the build and comparison process with the buildAndCompare.sh script. The script will ask you for the relevant input:
```shell
./buildAndCompare.sh path/to/store/apk
```