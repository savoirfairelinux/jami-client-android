#! /bin/bash
# Build Ring daemon and client APK for Android

if [ -z "$ANDROID_ABI" ]; then
    ANDROID_ABI="armeabi-v7a arm64-v8a x86 x86_64"
    echo "ANDROID_ABI not provided, building for ${ANDROID_ABI}"
fi

pushd ring-android
./make-swig.sh
popd

# Flags:

  # --release: build in release mode
  # --daemon: Only build the daemon for the selected archs

RELEASE=0
DAEMON_ONLY=0
for i in ${@}; do
    case "$i" in
        release|--release)
        RELEASE=1
        ;;
        daemon|--daemon)
        DAEMON_ONLY=1
        ;;
        *)
        ;;
    esac
done
export RELEASE

ANDROID_ABIS=""
ANDROID_ABI_LIST="${ANDROID_ABI}"
echo "Building ABIs: ${ANDROID_ABI_LIST}"
for i in ${ANDROID_ABI_LIST}; do
    echo "$i starts building"
    ANDROID_NDK=$ANDROID_NDK ANDROID_SDK=$ANDROID_SDK ANDROID_ABI=$i DAEMON_ONLY=$DAEMON_ONLY \
       ./build-daemon.sh $* || { echo "$i build KO"; exit 1; }
    echo "$i build OK"
done

if [[ $DAEMON_ONLY -eq 0 ]]; then
    export ANDROID_ABIS
    make -b -j1 RELEASE=$RELEASE apk
fi
