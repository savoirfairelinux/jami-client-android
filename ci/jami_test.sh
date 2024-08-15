#!/bin/bash

# Before using this script, ensure the following:
# 1. Set the JAVA_HOME environment variable to the path where Java is installed.
# 2. Set the SPOON_RUNNER_PATH environment variable to the path of the Spoon runner JAR file.
# 3. Set the ANDROID_SDK_ROOT environment variable to the path where Android SDK is installed.
# 4. Compile Jami using the following Gradle command: ./gradlew assembleAndroidTest assembleDebug

# Check the JAVA_HOME environment variable
if [ -z "$JAVA_HOME" ]; then
    echo "Error: JAVA_HOME environment variable is not set."
    exit 1
fi

# Check the SPOON_RUNNER_PATH environment variable
if [ -z "$SPOON_RUNNER_PATH" ]; then
    echo "Error: SPOON_RUNNER_PATH environment variable is not set."
    exit 1
fi

# Check the ANDROID_SDK_ROOT environment variable
if [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "Error: ANDROID_SDK_ROOT environment variable is not set."
    exit 1
fi

SCRIPT_DIRECTORY=$(dirname "$0")
TEST_ASSETS_DIRECTORY_PATH=$SCRIPT_DIRECTORY/test_assets/
APK_PATH=$SCRIPT_DIRECTORY/../jami-android/app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
TEST_APK_PATH=$SCRIPT_DIRECTORY/../jami-android/app/build/outputs/apk/androidTest/noPush/debug/app-noPush-debug-androidTest.apk

# Check the existence of the APK_PATH file
if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK file does not exist at the specified location: $APK_PATH"
    exit 1
fi

# Check the existence of the TEST_APK_PATH file
if [ ! -f "$TEST_APK_PATH" ]; then
    echo "Error: Test APK file does not exist at the specified location: $TEST_APK_PATH"
    exit 1
fi

# Install test assets on emulator (images, videos, audios, files, ...)
sh adb push $TEST_ASSETS_DIRECTORY_PATH "/data/local/tmp/jami_test_assets"

# Launch test execution
"$JAVA_HOME"/bin/java -jar "$SPOON_RUNNER_PATH" --apk "$APK_PATH" --test-apk "$TEST_APK_PATH" --sdk "$ANDROID_SDK_ROOT" --fail-on-failure

# Capture the exit code
exit_code=$?

# Check the exit code and display appropriate logs
if [ $exit_code -eq 0 ]; then
    echo "Jami UI test success. More info on 'spoon-output' directory."
else
    echo "Jami UI test failure ($exit_code). More info on 'spoon-output' directory."
    exit $exit_code
fi
