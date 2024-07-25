#!/bin/bash

# File origin : https://github.com/amrsa1/Android-Emulator-image/blob/main/start_emu_headless.sh

BL='\033[0;34m'
G='\033[0;32m'
RED='\033[0;31m'
YE='\033[1;33m'
NC='\033[0m' # No Color

emulator_name="${EMULATOR_NAME}"

set -x

function check_hardware_acceleration () {
    if [[ "$HW_ACCEL_OVERRIDE" != "" ]]; then
        hw_accel_flag="$HW_ACCEL_OVERRIDE"
    else
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS-specific hardware acceleration check
            HW_ACCEL_SUPPORT=$(sysctl -a | grep -E -c '(vmx|svm)')
        else
            # generic Linux hardware acceleration check
            HW_ACCEL_SUPPORT=$(grep -E -c '(vmx|svm)' /proc/cpuinfo)
        fi

        if [[ $HW_ACCEL_SUPPORT == 0 ]]; then
            hw_accel_flag="-accel off"
        else
            hw_accel_flag="-accel on"
        fi
    fi

    echo "$hw_accel_flag"
}


hw_accel_flag=$(check_hardware_acceleration)

function launch_emulator () {
    adb devices | grep emulator | cut -f1 | xargs -I {} adb -s "{}" emu kill
    options="@${emulator_name} -no-window -no-snapshot -noaudio -no-boot-anim -memory 2048 ${hw_accel_flag} -camera-back none"
    if [[ "$OSTYPE" == *linux* ]]; then
        echo "${OSTYPE}: emulator ${options} -gpu off"
        nohup emulator $options -gpu off &
    fi
    if [[ "$OSTYPE" == *darwin* ]] || [[ "$OSTYPE" == *macos* ]]; then
        echo "${OSTYPE}: emulator ${options} -gpu swiftshader_indirect"
        nohup emulator $options -gpu swiftshader_indirect &
    fi

    if [ $? -ne 0 ]; then
        echo "Error launching emulator"
        return 1
    fi
}


function check_emulator_status () {
    printf "${G}==> ${BL}Checking emulator booting up status 🧐${NC}\n"
    start_time=$(date +%s)
    spinner=( "⠹" "⠺" "⠼" "⠶" "⠦" "⠧" "⠇" "⠏" )
    i=0
    # Get the timeout value from the environment variable or use the default value of 300 seconds (5 minutes)
    timeout=${EMULATOR_TIMEOUT:-300}

    while true; do
        result=$(adb shell getprop sys.boot_completed 2>&1)

        if [ "$result" == "1" ]; then
            printf "\e[K${G}==> \u2713 Emulator is ready : '$result'           ${NC}\n"
            adb devices -l
            adb shell input keyevent 82
            break
        elif [ "$result" == "" ]; then
            printf "${YE}==> Emulator is partially Booted! 😕 ${spinner[$i]} ${NC}\r"
        else
            printf "${RED}==> $result, please wait ${spinner[$i]} ${NC}\r"
            i=$(( (i+1) % 8 ))
        fi

        current_time=$(date +%s)
        elapsed_time=$((current_time - start_time))
        if [ $elapsed_time -gt $timeout ]; then
            printf "${RED}==> Timeout after ${timeout} seconds elapsed 🕛.. ${NC}\n"
            break
        fi
        sleep 4
    done
};


function disable_animation() {
    adb shell "settings put global window_animation_scale 0.0"
    adb shell "settings put global transition_animation_scale 0.0"
    adb shell "settings put global animator_duration_scale 0.0"
};

function hidden_policy() {
    adb shell "settings put global hidden_api_policy_pre_p_apps 1;settings put global hidden_api_policy_p_apps 1;settings put global hidden_api_policy 1"
};

function enable_android_test_orchestrator() {
    DEVICE_API_LEVEL=$(adb shell getprop ro.build.version.sdk)

    FORCE_QUERYABLE_OPTION=""
    if [[ $DEVICE_API_LEVEL -ge 30 ]]; then
       FORCE_QUERYABLE_OPTION="--force-queryable"
    fi

    # uninstall old versions
    adb uninstall androidx.test.services
    adb uninstall androidx.test.orchestrator

    # Install the test orchestrator.
    adb install $FORCE_QUERYABLE_OPTION -r path/to/m2repository/androidx/test/orchestrator/1.4.2/orchestrator-1.4.2.apk

    # Install test services.
    adb install $FORCE_QUERYABLE_OPTION -r path/to/m2repository/androidx/test/services/test-services/1.4.2/test-services-1.4.2.apk

    # Add "-e clearPackageData true" to clear your app's data in between runs.
    adb shell 'CLASSPATH=$(pm path androidx.test.services) app_process / \
     androidx.test.services.shellexecutor.ShellMain am instrument -w -e clearPackageData true -e \
     targetInstrumentation cx.ring/androidx.test.runner.AndroidJUnitRunner \
     androidx.test.orchestrator/.AndroidTestOrchestrator'
};

launch_emulator
check_emulator_status
disable_animation
hidden_policy
