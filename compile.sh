#! /bin/bash
# Build Ring daemon and client APK for Android

if [ -z "$ANDROID_ABI" ]; then
    ANDROID_ABI="armeabi-v7a arm64-v8a x86_64"
    echo "ANDROID_ABI not provided, building for ${ANDROID_ABI}"
fi

TOP=$(pwd)/ring-android

# Flags:

  # --release: build in release mode
  # --daemon: Only build the daemon for the selected archs

RELEASE=0
DAEMON_ONLY=0
NO_GRADLE=0
for i in ${@}; do
    case "$i" in
        release|--release)
        RELEASE=1
        ;;
        daemon|--daemon)
        DAEMON_ONLY=1
        ;;
        no-gradle|--no-gradle)
        NO_GRADLE=1
        ;;
        *)
        ;;
    esac
done
export RELEASE
export DAEMON_ONLY

ANDROID_ABI_LIST="${ANDROID_ABI}"
echo "Building ABIs: ${ANDROID_ABI_LIST}"
for i in ${ANDROID_ABI_LIST}; do
    echo "$i starts building"
    ANDROID_NDK=$ANDROID_NDK ANDROID_SDK=$ANDROID_SDK ANDROID_ABI=$i \
       ./build-daemon.sh $* || { echo "$i build KO"; exit 1; }
    echo "$i build OK"
done

if [[ $NO_GRADLE -eq 0 ]]; then
    if [ -z "$RING_BUILD_FIREBASE" ]; then
        echo "Building without Firebase support"
    else
        GRADLE_PROPERTIES="-PbuildFirebase"
        echo "Building with Firebase support"
    fi
    if [[ $DAEMON_ONLY -eq 0 ]]; then
        if [[ $RELEASE -eq 1 ]]; then
            cd $TOP && ./gradlew $GRADLE_PROPERTIES assembleRelease
        else
            cd $TOP && ./gradlew $GRADLE_PROPERTIES assembleDebug
        fi
    fi
else
    echo "Stopping before gradle build as requested."
fi
