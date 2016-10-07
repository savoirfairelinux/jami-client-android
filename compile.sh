#! /bin/bash
# Build Ring daemon and client APK for Android

if [ -z "$ANDROID_ABI" ]; then
    ANDROID_ABI="armeabi-v7a x86 x86_64"
    echo "ANDROID_ABI not provided, building for ${ANDROID_ABI}"
fi

pushd ring-android
./make-swig.sh
popd

ANDROID_ABIS=""
ANDROID_ABI_LIST="${ANDROID_ABI}"
echo "Building ABIs: ${ANDROID_ABI_LIST}"
for i in ${ANDROID_ABI_LIST}; do
    echo "$i starts building"
    ANDROID_NDK=$ANDROID_NDK ANDROID_SDK=$ANDROID_SDK ANDROID_ABI=$i \
        ./build-daemon.sh $* || { echo "$i build KO"; exit 1; }
    echo "$i build OK"
done
export ANDROID_ABIS
make -b -j1 RELEASE=$RELEASE apk
