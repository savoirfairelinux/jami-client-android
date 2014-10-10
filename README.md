# Ring Android

## Environnement

Make sure you have the android-ndk and android-sdk, and you'll want something
like this in your .bashrc (or equivalent):

export ANDROID_NDK=$HOME/src/android-ndk
export ANDROID_NDK_ROOT=$ANDROID_NDK
export ANDROID_SDK=$HOME/src/android-sdk-linux
export ANDROID_HOME=$ANDROID_SDK
export PATH=$ANDROID_SDK/platform-tools:${PATH}


## Build instructions

When all else fails:

rm -rf sflphone
git clean -dfx
export ANDROID_ABI=armeabi-v7a
./compile.sh


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
