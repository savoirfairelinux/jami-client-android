#!/bin/bash

# Define the source directory on the Android device (emulator)
screenshotDir="/sdcard/Download/screenshots"

# Define the destination directory on the local system
localDir="/jami-client-android/ci/spoon-output/screenshots"

# Create the local directory if it doesn't exist
mkdir -p "$localDir"

# Download the content of the directory from the device to the local directory
adb pull "$screenshotDir" "$localDir"

# Check if the download was successful
if [ $? -eq 0 ]; then
    echo "The content of directory '$screenshotDir' has been downloaded to '$localDir'."
else
    echo "Error downloading the directory."
fi

# Set ownership of the local directory and its contents to jenkins
chown -R jenkins:jenkins "$localDir"
