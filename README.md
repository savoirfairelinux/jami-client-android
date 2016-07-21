# Ring Android

This repository is meant for the porting of Ring to Android.

| App | CI | Packaging
| :-: | :-: | :-:
| [![Download on the Play Store](https://img.shields.io/badge/download-play%20store-blue.svg)](https://play.google.com/store/apps/details?id=cx.ring) [![Download on F-Droid](https://img.shields.io/badge/download-fdroid-blue.svg)](https://f-droid.org/repository/browse/?fdid=cx.ring) | [![Build Status](http://test.savoirfairelinux.com/buildStatus/icon?job=ring-client-android)](http://test.savoirfairelinux.com/job/ring-client-android) | [![Build Status](http://test.savoirfairelinux.com/buildStatus/icon?job=ring-client-android-packaging)](http://test.savoirfairelinux.com/view/Ring/job/ring-client-android-packaging/)

## Environment

Clone this as a submodule of:
<https://gerrit-ring.savoirfairelinux.com/#/admin/projects/ring-project>
to obtain the required Ring daemon source.

You can also manually clone the daemon and override the DAEMON_DIR
during compilation

Make sure you have the android-ndk and android-sdk, and you'll want something
like this in your .bashrc (or equivalent):

    export ANDROID_NDK=$HOME/src/android-ndk
    export ANDROID_NDK_ROOT=$ANDROID_NDK
    export ANDROID_SDK=$HOME/src/android-sdk-linux
    export ANDROID_HOME=$ANDROID_SDK
    export PATH=$ANDROID_SDK/platform-tools:${PATH}

install swig-2.0.6 or later and python-2.7 or later on your system

## Build instructions

Supported archs are: armeabi-v7a and x86

Example:

    ANDROID_ABI="armeabi-v7a x86"

Then:

    ./compile.sh

If you cloned the daemon in a custom directory (other than ../daemon),
you can specify it using an **absolute path**:

    DAEMON_DIR=custom_path ./compile.sh

**When all else fails**:

    git clean -dfx
    cd ../daemon (or custom_path)
    git clean -dfx

And start again.

## Update translations

Update translations using the Transifex client (tx) :

    ./update-translations.sh

## Debugging

Retrieve client log from device (client must be running before executing this)

    adb logcat *:D | grep `adb shell ps | egrep 'cx.ring' | cut -c10-15` > logring.txt

## Common issues

* Makeinfo issue
    makeinfo: command not found
    WARNING: 'makeinfo' is missing on your system.
    **Solution**:   Install texinfo package containing makeinfo dep.

* Unable to locate tools.jar
    **Solution**:   Your java installation is not pointing to a JDK.
                    Install one, or make JAVA_HOME point to it.

* When building the apk error in build-tools
    error while loading shared libraries: libstdc++.so.6
    **Solution**:   Install lib32stdc++6 lib32z1-dev
