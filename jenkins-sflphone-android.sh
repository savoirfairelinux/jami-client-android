#!/bin/bash -e
#
#  Copyright (C) 2004-2014 Savoir-Faire Linux Inc.
#
#  Author: Alexandre Savard <alexandre.savard@savoirfairelinux.com>
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  If you modify this program, or any covered work, by linking or
#  combining it with the OpenSSL project's OpenSSL library (or a
#  modified version of that library), containing parts covered by the
#  terms of the OpenSSL or SSLeay licenses, Savoir-Faire Linux Inc.
#  grants you additional permission to convey the resulting work.
#  Corresponding Source for a non-source form of such a combination
#  shall include the source code for the parts of OpenSSL used as well
#  as that of the covered work.
#

# Script used by Jenkins continious integration server to build and
# test sflphone-android project

#
# Make sure that:
#     Download android_ndk_
#              android_sdk_
#     Install java runtime engine, ant
#     Required dependencies
#     sudo apt-get install libglfw-dev

# Setup environment variables
export ANDROID_NDK=$HOME/android-buildtools/ndk
export ANDROID_SDK=$HOME/android-buildtools/sdk
export ANDROID_HOME=$HOME/android-buildtools/sdk
export GENYMOTION_HOME=$HOME/android-buildtools/genymotion
export ANDROID_NDK_ROOT=$ANDROID_NDK
export ANDROID_ABI=armeabi-v7a

ANDROID_SDK_TOOLS=$ANDROID_SDK/tools

export PATH=$PATH:$ANDROID_NDK
export PATH=$PATH:$ANDROID_SDK
export PATH=$PATH:$ANDROID_SDK/platform-tools
export PATH=$PATH:$ANDROID_SDK_TOOLS

VIRTUAL_DEVICE_ID=31
VIRTUAL_DEVICE_ABI=armeabi-v7a
VIRTUAL_DEVICE_NAME=sflphone-android

ANDROID_PROJECT_PATH=$HOME/sflphone/sflphone-android

ANDROID_SFLPHONE_BIN=bin/SFLPhoneHome-debug.apk
ANDROID_SFLPHONE_TEST_SUITE=tests/bin/sflphoneTest-debug.apk

ANDROID_TEST_PACKAGE=org.sflphone.tests
ANDROID_TEST_RUNNNER=android.test.InstrumentationTestRunner

print_help() {
    echo "Init sflphone-android test server, run test suite
    Options:
        -h     Print this help message
        -i     Init test server environment (should be run only once)
        -l     Launch the emulator
        -b     Build the application, do not run the test suite
        -t     Build the test suite
        -r     Run the full test suite, priorly build the application"
}

init_build_server() {
    android delete avd --name $VIRTUAL_DEVICE_NAME

    echo "Create a new android virtual device, overwrite precendent one"
    android create avd -n $VIRTUAL_DEVICE_NAME -t $VIRTUAL_DEVICE_ID -f -b $VIRTUAL_DEVICE_ABI
}

launch_emulator() {
#    echo "Terminate any currently running emulator"
#    killall emulator-arm -u $USER

    # build_sflphone_android
#    echo "List of currently available android virtual devices"
#    android list avd

#    echo "Launching the android emulator using \"$VIRTUAL_DEVICE_NAME\" avd"
#    emulator -avd $VIRTUAL_DEVICE_NAME -audio none -gpu off -partition-size 256 -no-window &

#    echo "List of devices currently running"
#    adb devices
    vboxmanage snapshot "Nexus4-API18" restore "factory"
    $GENYMOTION_HOME/player --vm-name "Nexus4-API18" &

    echo "Waiting for device ..."
    adb wait-for-device

#    adb push launch-sflphone.sh /data/data
#    adb shell sh /data/data/launch-sflphone.sh
}

retrieve_screenshots() {

    echo "----------------- Zipping screenshots"
    adb pull /sdcard/Robotium-Screenshots screens/phone
    zip -r screens.zip screens
    rm -rf screens

}

build_sflphone_android() {
    echo "Build sflphone"
    ./compile.sh
}

build_sflphone_test_suite() {
    echo "Build test suite"
    pushd Tests
    ant debug
    popd
}

run_test_suite() {
    adb shell am instrument -w org.sflphone.tests/android.test.InstrumentationTestRunner
}

if [ "$#" -eq 0 ]; then
    print_help
fi

while getopts "hilbrt" opts; do
    case $opts in
        h)
            print_help
            ;;
        i)
            init_build_server
            ;;
        l)
            launch_emulator
            ;;
        b)
            build_sflphone_android
            ;;
        t)
            build_sflphone_test_suite
            ;;
        r)
            run_test_suite
            ;;
        *)
            print_help
            ;;
    esac
done
