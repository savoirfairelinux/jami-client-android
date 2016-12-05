#!/bin/bash

# Name of the emulator to create
AVD_NAME="Testing"

android delete avd -n ${AVD_NAME}

#TODO remove this line when using cqfd
echo "y" | android update sdk -a --no-ui --filter sys-img-x86_64-android-21

echo "n" | android create avd --name ${AVD_NAME} -t "android-21" --abi "default/x86_64"

#Start the emulator
$ANDROID_SDK/tools/emulator -avd ${AVD_NAME} -wipe-data &
EMULATOR_PID=$!

# Wait for Android to finish booting
WAIT_CMD="$ANDROID_SDK/platform-tools/adb wait-for-device shell getprop init.svc.bootanim"
until $WAIT_CMD | grep -m 1 stopped; do
  echo "Waiting..."
  sleep 1
done

# Unlock the Lock Screen
$ANDROID_SDK/platform-tools/adb shell input keyevent 82

# Clear and capture logcat
$ANDROID_SDK/platform-tools/adb logcat -c
$ANDROID_SDK/platform-tools/adb logcat > build/logcat.log &
LOGCAT_PID=$!

# Run the tests
./gradlew connectedAndroidTest -i

# Stop the background processes
kill $LOGCAT_PID
kill $EMULATOR_PID
