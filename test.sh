#/jami-client-android/start_emu_headless.sh

java -jar /spoon-runner.jar --apk ./app/build/outputs/apk/noPush/debug/app-noPush-debug.apk --test-apk ./app/build/outputs/apk/androidTest/noPush/debug/app-noPush-debug-androidTest.apk --sdk /opt/android/
