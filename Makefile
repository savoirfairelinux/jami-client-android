#!/bin/sh

APP=bin/SFLphone-debug.apk
all: local.properties
	ndk-build -C jni -j4
	ant debug

local.properties:
	android update project -p .

# You may want to specify a device with -s SERIAL NUMBER
install: $(APP)
	adb install -r $^

uninstall:
	adb uninstall org.sflphone

clean:
	ant clean
	rm -rf obj $(APP)
