#!/bin/sh

APP=bin/SFLPhoneHome-debug.apk
all: $(APP)

$(APP):
	./make_swig.sh
	ndk-build -C jni -j4
	ant debug

# You may want to specify a device with -s SERIAL NUMBER
install: $(APP)
	adb install -r $^

clean:
	rm -rf libs obj
