/opt/java/openjdk/bin/java -jar /spoon-runner.jar --apk ./jami-android/app/build/outputs/apk/noPush/debug/app-noPush-debug.apk --test-apk ./jami-android/app/build/outputs/apk/androidTest/noPush/debug/app-noPush-debug-androidTest.apk --sdk /opt/android/ --fail-on-failure

# Capture the exit code
exit_code=$?

# Check the exit code and display appropriate logs
if [ $exit_code -eq 0 ]; then
    echo "Jami UI test failure. More info on `spoon-output` directory."
else
    echo "Jami UI test success. More info on `spoon-output` directory."
fi