# Ring Android

This repository is meant for the porting of Ring to Android.

## Environment

Make sure you have the android-ndk and android-sdk, and you'll want something
like this in your .bashrc (or equivalent):

    export ANDROID_NDK=$HOME/src/android-ndk
    export ANDROID_NDK_ROOT=$ANDROID_NDK
    export ANDROID_SDK=$HOME/src/android-sdk-linux
    export ANDROID_HOME=$ANDROID_SDK
    export PATH=$ANDROID_SDK/platform-tools:${PATH}

install swig-2.0.6 or later and python-2.7 or later on your system

## Build instructions

Supported archs are: armeabi-v7a, armeabi, arm64-v8a, x86, x86_64 or mips

Example:

    ANDROID_ABI ="armeabi-v7a, x86"

Then:

    ./compile.sh

We use a tested hash to build ring daemon, but if you want to use master:

    cd ring && git pull
    cd -
    ./compile --build

`--build` will bypass the tested hash's checkout.

**When all else fails**:

    rm -rf ring-daemon
    git clean -dfx

And start again.

## Update translations

Update translations using the Transifex client (tx) :

    ./update-translations.sh

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
