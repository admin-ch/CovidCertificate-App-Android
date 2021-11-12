# Reproducible Builds

This document outlines how you can reproduce the Android app.

The instructions below are for the wallet app.
To reproduce the verifier app, change **all** occurences of `wallet` to `verifier`.

## Prerequisites

1. Make sure you have both [Docker](https://www.docker.com/) and `git` installed.
2. Clone the repository
3. Checkout the tag (or branch or commit) that corresponds to the version of your app (e.g., 1.0.0)

```shell
git clone https://github.com/admin-ch/CovidCertificate-App-Android.git ~/CovidCertificate-App-Android
cd ~/CovidCertificate-App-Android
git tag       # List all available tags
git checkout v2.7.0-2700-wallet
```

## Verifying the app

### Step 1: Check your app version and build timestamp

1. Open the app
2. Click on the `i` button in the top-right corner
3. Check the app version in the top right corner
4. Check the build timestamp in the bottom right corner, which is the number before the slash (e.g., 1622186583268), and record its value to be used later

### Step 2: Extract the APK from your device

1. Make sure you have `adb` installed
2. Connect your phone to your computer
3. Extract the APK from the phone:

```shell
adb pull `adb shell pm path ch.admin.bag.covidcertificate.wallet | cut -d':' -f2` wallet-store.apk
```

If you want to check the version of the APK you are pulling from your device:

```shell
adb shell dumpsys package ch.admin.bag.covidcertificate.wallet | grep versionName=| cut -d '=' -f 2
```

### Step 3: Reproduce it

TLDR: Run the script and follow its instructions:

```shell
./buildAndCompare.sh wallet-store.apk
```

The script will do the following:

1. Build a Docker image with the required Android tools
2. (Optionally) Generate a dummy key store for signing
3. Build the app from source in the Docker container
4. Compare the APK pulled from your phone with the APK built from source

To manually compare to files you can run:

```shell
python3 apkdiff.py wallet-built.apk wallet-store.apk
```

