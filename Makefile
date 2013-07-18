APP=bin/SFLPhoneHome-debug.apk
all: $(APP)

$(APP):
	ndk-build -C jni -j4
	ant debug

# You may want to specify a device with -s SERIAL NUMBER
install: $(APP)
	adb install -r $^

clean:
	rm -rf libs obj
