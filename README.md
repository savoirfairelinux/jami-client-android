# Jami Android

The Jami client for Android

| App | CI
| :-: | :-: |
| [![Download on the Play Store](https://img.shields.io/badge/download-play%20store-blue.svg)](https://play.google.com/store/apps/details?id=cx.ring) [![Download on F-Droid](https://img.shields.io/badge/download-fdroid-blue.svg)](https://f-droid.org/repository/browse/?fdid=cx.ring) | [![Build Status](https://jenkins.jami.net/buildStatus/icon?job=client-android)](https://jenkins.jami.net/job/client-android/)

## Environment

Install swig-4.1.0 or later and python-3.7 or later on your system.

## Build instructions

With Android Studio:
* Add the project in Android Studio
* Click on build
* Enjoy!

With the command line:
* ./gradlew assembleDebug

## Update translations

Update translations using the Transifex client (tx) :
    ./update-translations.sh

## Generate new release commit

Generate a new release commit updating the version code and version string:
    ./update_version.py --commit

# Report issues

Report issues on Gitlab:
https://git.jami.net/savoirfairelinux/jami-client-android/-/issues
