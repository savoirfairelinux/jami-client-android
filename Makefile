# Sources and objects

export ANDROID_HOME=$(ANDROID_SDK)

ARCH = $(ANDROID_ABI)

SRC=sflphone-android
export SFLPHONE_SRC=$(PWD)/sflphone
LIBSFLPHONEJNI_H=sflphone/daemon/src/sflphone.h
LIBSFLPHONEJNI=$(SRC)/obj/local/$(ARCH)/libsflphone.so

JAVA_SOURCES=$(shell find $(SRC)/src/org/sflphone/ -type f -name "*.java")
JNI_SOURCES=$(SRC)/jni/*.cpp $(SRC)/jni/*.h

ifneq ($(V),)
ANT_OPTS += -v
VERBOSE =
GEN =
else
VERBOSE = @
GEN = @echo "Generating" $@;
endif

ifeq ($(RELEASE),1)
ANT_TARGET = release
SFLPHONE_APK=$(SRC)/bin/SFLphone-release-unsigned.apk
NDK_DEBUG=0
else
ANT_TARGET = debug
SFLPHONE_APK=$(SRC)/bin/SFLphone-debug.apk
NDK_DEBUG=1
endif

$(SFLPHONE_APK): $(LIBSFLPHONEJNI) $(JAVA_SOURCES)
	@echo
	@echo "=== Building $@ for $(ARCH) ==="
	@echo
	date +"%Y-%m-%d" > $(SRC)/assets/builddate.txt
	echo `id -u -n`@`hostname` > $(SRC)/assets/builder.txt
	git rev-parse --short HEAD > $(SRC)/assets/revision.txt
	$(VERBOSE)cd $(SRC) && ant $(ANT_OPTS) $(ANT_TARGET)

$(LIBSFLPHONEJNI): $(JNI_SOURCES) $(LIBSFLPHONEJNI_H)
	@if [ -z "$(SFLPHONE_BUILD_DIR)" ]; then echo "SFLPHONE_BUILD_DIR not defined" ; exit 1; fi
	@if [ -z "$(ANDROID_NDK)" ]; then echo "ANDROID_NDK not defined" ; exit 1; fi
	@echo
	@echo "=== Building libsflphonejni ==="
	@echo
	$(VERBOSE)if [ -z "$(SFLPHONE_SRC_DIR)" ] ; then SFLPHONE_SRC_DIR=./sflphone; fi ; \
	if [ -z "$(SFLPHONE_CONTRIB)" ] ; then SFLPHONE_CONTRIB="$$SFLPHONE_SRC_DIR/contrib/$(TARGET_TUPLE)"; fi ; \
	if [ `echo "$(SFLPHONE_BUILD_DIR)" | head -c 1` != "/" ] ; then \
        SFLPHONE_BUILD_DIR="../$(SFLPHONE_BUILD_DIR)"; \
	fi ; \
	[ `echo "$$SFLPHONE_CONTRIB" | head -c 1` != "/" ] && SFLPHONE_CONTRIB="../$$SFLPHONE_CONTRIB"; \
	[ `echo "$$SFLPHONE_SRC_DIR" | head -c 1` != "/" ] && SFLPHONE_SRC_DIR="../$$SFLPHONE_SRC_DIR"; \
	$(ANDROID_NDK)/ndk-build -C $(SRC) \
		SFLPHONE_SRC_DIR="$$SFLPHONE_SRC_DIR" \
		SFLPHONE_CONTRIB="$$SFLPHONE_CONTRIB" \
		SFLPHONE_BUILD_DIR="$$SFLPHONE_BUILD_DIR" \
		NDK_DEBUG=$(NDK_DEBUG) \
		TARGET_CFLAGS="$$SFLPHONE_EXTRA_CFLAGS"

apkclean:
	rm -f $(SFLPHONE_APK)

lightclean:
	cd $(SRC) && rm -rf libs/armeabi-v7a libs/x86 libs/mips obj bin $(SFLPHONE_APK)

clean: lightclean
	rm -rf $(SRC)/gen java-libs/*/gen java-libs/*/bin .sdk vlc-sdk/ vlc-sdk.7z

jniclean: lightclean
	rm -f $(LIBSFLPHONEJNI)

distclean: clean jniclean

install: $(SFLPHONE_APK)
	@echo "=== Installing SFLphone on device ==="
	adb wait-for-device
	adb install -r $(SFLPHONE_APK)

uninstall:
	adb wait-for-device
	adb uninstall org.sflphone

run:
	@echo "=== Running SFLphone on device ==="
	adb wait-for-device
	adb shell am start -n org.sflphone/org.sflphone.client.HomeActivity

build-and-run: install run

apkclean-run: apkclean build-and-run
	adb logcat -c

distclean-run: distclean build-and-run
	adb logcat -c

vlc-sdk.7z: .sdk
	7z a $@ vlc-sdk/

.sdk:
	mkdir -p vlc-sdk/libs
	cd vlc-android; cp -r libs/* ../vlc-sdk/libs
	mkdir -p vlc-sdk/src/org/videolan
	cp -r vlc-android/src/org/videolan/libvlc vlc-sdk/src/org/videolan
	touch $@

.PHONY: lightclean clean jniclean distclean distclean-run apkclean apkclean-run install run build-and-run
