#! /bin/bash
# Build Jami daemon and client APK for Android
TOP=$(pwd)/ring-android

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

if [ -z "$DAEMON_DIR" ]; then
    DAEMON_DIR="$(pwd)/../daemon"
    echo "DAEMON_DIR not provided trying to find it in $DAEMON_DIR"
fi
if [ ! -d "$DAEMON_DIR" ]; then
    echo 'Daemon not found.'
    echo 'If you cloned the daemon in a custom location override DAEMON_DIR to point to it'
    echo "You can also use our meta repo which contains both:
          https://review.jami.net/admin/repos/jami-project"
    exit 1
fi
export DAEMON_DIR

JNIDIR=$DAEMON_DIR/bin/jni
ANDROID_TOPLEVEL_DIR="`pwd`"
ANDROID_APP_DIR="${ANDROID_TOPLEVEL_DIR}/ring-android"

# Generate JNI interface
cd $JNIDIR
PACKAGEDIR=$ANDROID_APP_DIR/libjamiclient/src/main/java ./make-swig.sh

if [[ $DAEMON_ONLY -eq 0 ]]; then
    if [ -z "$RING_BUILD_FIREBASE" ]; then
        echo "Building without Firebase support"
    else
        GRADLE_PROPERTIES="-PbuildFirebase"
        echo "Building with Firebase support"
    fi
    if [[ $RELEASE -eq 1 ]]; then
        cd $TOP && ./gradlew $GRADLE_PROPERTIES assembleRelease
    else
        cd $TOP && ./gradlew $GRADLE_PROPERTIES assembleDebug
    fi
fi
