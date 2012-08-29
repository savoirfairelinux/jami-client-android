#!/bin/bash
#
#  Copyright (C) 2004-2012 Savoir-Faire Linux Inc.
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
export ANDROID_NDK=$HOME/android/android-ndk-r8b
export ANDROID_SDK=$HOME/android/android-sdk-linux
export ANDROID_SWT=$ANDROID_SDK/tools/lib/x86_64

ANDROID_SDK_TOOLS=$ANDROID_SDK/tools

export PATH=$PATH:$ANDROID_NDK
export PATH=$PATH:$ANDROID_SDK
export PATH=$PATH:$ANDROID_SDK/platform-tools
export PATH=$PATH:$ANDROID_SDK_TOOLS

VIRTUAL_DEVICE_ID=15
VIRTUAL_DEVICE_ABI=9
VIRTUAL_DEVICE_NAME=sflphone-android

ANDROID_PROJECT_PATH=$HOME/sflphone/sflphone-android

print_help() {
    echo "Init sflphone-android test server, run test suite 
    Options:
        -h     Print this help message
        -i     Init test server environment (should be run only once)
        -b     Build the application, do not run the test suite
        -r     Run the full test suite, priorly build the application"
}

init_build_server() {
    android delete avd --name $VIRTUAL_DEVICE_NAME

    echo "Create a new android virtual device, overwrite precendent one"
    android create avd -n $VIRTUAL_DEVICE_NAME -t $VIRTUAL_DEVICE_ID -f
}

build_sflphone_android() {
    # android update project --target $VIRTUAL_DEVICE_ID --path $ANDROID_PROJECT_PATH

    echo "Build JNI related libraries"
    # ndk-build clean
    ndk-build -j4

    echo "Build Java application"
    ant debug
}

run_test_suite() {
    echo "Terminate any currently running emulator"
    killall emulator-arm -u $USER

    # build_sflphone_android
    echo "List of currently available android virtual devices"
    android list avd

    echo "Launching the android emulator using \"$VIRTUAL_DEVICE_NAME\" avd"
    emulator -avd $VIRTUAL_DEVICE_NAME -audio none -gpu off -partition-size 256 -no-window &

    echo "Waiting for device ..."
    adb wait-for-device

    echo "List of devices currently running"
    adb devices

    echo "Upload sflphone on the virtual device"
#    ./adb-push-sflphone.sh

#    adb push launch-sflphone.sh /data/data
#    adb shell sh /data/data/launch-sflphone.sh
}

if [ "$#" -eq 0 ]; then
    print_help
fi

while getopts "hibr" opts; do
    case $opts in
        h)
            print_help
            ;;
        i)
            init_build_server
            ;;
        b)
            build_sflphone_android
            ;;
        r)
            run_test_suite
            ;;
        *)
            print_help
            ;;
    esac
done
