# Jami Android

The Jami client for Android

| App | CI
| :-: | :-: |
| [![Download on the Play Store](https://img.shields.io/badge/download-play%20store-blue.svg)](https://play.google.com/store/apps/details?id=cx.ring) [![Download on F-Droid](https://img.shields.io/badge/download-fdroid-blue.svg)](https://f-droid.org/repository/browse/?fdid=cx.ring) | [![Build Status](https://jenkins.jami.net/buildStatus/icon?job=client-android)](https://jenkins.jami.net/job/client-android/)

## Environment

Download the project including the daemon submodule with:
> git clone --recursive https://review.jami.net/jami-client-android

Or to download the daemon submodule from the existing project directory:
> git submodule update --init --recursive

Make sure to have autotools, autopoint, swig, yasm, autotools and cmake available on your system:
on Debian/Ubuntu:
> apt install cmake build-essential swig yasm

On macOS:
> brew install cmake automake autotools libtool pkg-config yasm swig

When using brew on macOS, the 'libtoolize' binary might be available as 'glibtoolize'.
In that case, the following command makes it avaialble to the build system:
> ln -s /opt/homebrew/bin/glibtoolize /opt/homebrew/bin/libtoolize

Make sure to have the Android SDK and NDK available.

## Build instructions

With Android Studio:
* Add 'jami-android' in Android Studio
* Click on build
* Enjoy!

With the command line:
```sh
cd client-android/jami-android
./gradlew assembleDebug
```

## Update translations

Update translations using the Transifex client (tx) :
> ./update-translations.sh

# Generate new release commit

Generate a new release commit updating the version code and version string:
> ./update_version.py --commit

# Report issues

Report issues on Gitlab:
https://git.jami.net/savoirfairelinux/jami-client-android
