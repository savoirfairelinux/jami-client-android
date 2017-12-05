#! /bin/bash
# Build Ring daemon and client APK for Android

if [ -z "$ANDROID_ABI" ]; then
    ANDROID_ABI="armeabi-v7a arm64-v8a x86 x86_64"
    echo "ANDROID_ABI not provided, building for ${ANDROID_ABI}"
fi

TOP=$(pwd)/ring-android

# Flags:

  # --release: build in release mode

RELEASE=0
DAEMON_ONLY=0
for i in ${@}; do
    case "$i" in
        release|--release)
        RELEASE=1
        ;;
        *)
        ;;
    esac
done
export RELEASE

    if [[ $RELEASE -eq 1 ]]; then
        cd $TOP && ./gradlew assembleRelease
    else
        cd $TOP && ./gradlew assembleDebug
    fi
