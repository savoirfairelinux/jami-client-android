JAVA_PATH=/opt/java/openjdk/bin
APK_PATH=./jami-android/app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
TEST_APK_PATH=./jami-android/app/build/outputs/apk/androidTest/noPush/debug/app-noPush-debug-androidTest.apk
SDK_PATH=/opt/android/

$JAVA_PATH/java -jar /spoon-runner.jar --apk $APK_PATH --test-apk $TEST_APK_PATH --sdk $SDK_PATH --fail-on-failure

# Capture the exit code
exit_code=$?

# Check the exit code and display appropriate logs
if [ $exit_code -eq 0 ]; then
    echo "Jami UI test success. More info on 'spoon-output' directory."
else
    echo "Jami UI test failure. More info on 'spoon-output' directory."
    exit 1
fi
