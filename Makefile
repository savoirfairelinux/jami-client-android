# Sources and objects

export ANDROID_HOME=$(ANDROID_SDK)

ARCH = $(ANDROID_ABI)

SRC=ring-android
LIBRINGJNI_H=ring/src/dring/dring.h
LIBRINGJNI=$(SRC)/obj/local/$(ARCH)/libring.so

JAVA_SOURCES=$(shell find $(SRC)/src/org/sflphone/ -type f -name "*.java")

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
RING_APK=$(SRC)/bin/Ring-release-unsigned.apk
NDK_DEBUG=0
else
ANT_TARGET = debug
RING_APK=$(SRC)/bin/Ring-debug.apk
NDK_DEBUG=1
endif

define build_apk
	@echo
	@echo "=== Building $(RING_APK) for $(ARCH) ==="
	@echo
	date +"%Y-%m-%d" > $(SRC)/assets/builddate.txt
	echo `id -u -n`@`hostname` > $(SRC)/assets/builder.txt
	git rev-parse --short HEAD > $(SRC)/assets/revision.txt
	./gen-env.sh $(SRC)
	$(VERBOSE)cd $(SRC) && ant $(ANT_OPTS) $(ANT_TARGET)
endef

$(RING_APK): $(LIBRINGJNI) $(JAVA_SOURCES)
	@echo
	@echo "=== Building $@ for $(ARCH) ==="
	@echo
	date +"%Y-%m-%d" > $(SRC)/assets/builddate.txt
	echo `id -u -n`@`hostname` > $(SRC)/assets/builder.txt
	git rev-parse --short HEAD > $(SRC)/assets/revision.txt
	$(VERBOSE)cd $(SRC) && ant $(ANT_OPTS) $(ANT_TARGET)

$(LIBRINGJNI): $(LIBRINGJNI_H)
	@if [ -z "$(RING_BUILD_DIR)" ]; then echo "RING_BUILD_DIR not defined" ; exit 1; fi
	@if [ -z "$(ANDROID_NDK)" ]; then echo "ANDROID_NDK not defined" ; exit 1; fi
	@echo
	@echo "=== Building libringjni ==="
	@echo
	$(VERBOSE)if [ -z "$(RING_SRC_DIR)" ] ; then RING_SRC_DIR=./ring; fi ; \
	if [ -z "$(RING_CONTRIB)" ] ; then RING_CONTRIB="$$RING_SRC_DIR/daemon/contrib/$(TARGET_TUPLE)"; fi ; \
	if [ `echo "$(RING_BUILD_DIR)" | head -c 1` != "/" ] ; then \
        RING_BUILD_DIR="../$(RING_BUILD_DIR)"; \
	fi ; \
	[ `echo "$$RING_CONTRIB" | head -c 1` != "/" ] && RING_CONTRIB="../$$RING_CONTRIB"; \
	[ `echo "$$RING_SRC_DIR" | head -c 1` != "/" ] && RING_SRC_DIR="../$$RING_SRC_DIR"; \
	$(ANDROID_NDK)/ndk-build -C $(SRC) \
		RING_SRC_DIR="$$RING_SRC_DIR" \
		RING_CONTRIB="$$RING_CONTRIB" \
		RING_BUILD_DIR="$$RING_BUILD_DIR" \
		NDK_DEBUG=$(NDK_DEBUG) \
		TARGET_CFLAGS="$$RING_EXTRA_CFLAGS"

apk:
	$(call build_apk)

apkclean:
	rm -f $(RING_APK)

lightclean:
	cd $(SRC) && rm -rf libs/armeabi-v7a libs/x86 libs/mips obj bin $(RING_APK)

clean: lightclean
	rm -rf $(SRC)/gen java-libs/*/gen java-libs/*/bin .sdk

jniclean: lightclean
	rm -f $(LIBRINGJNI)

distclean: clean jniclean

install: $(RING_APK)
	@echo "=== Installing SFLphone on device ==="
	adb wait-for-device
	adb install -r $(RING_APK)

uninstall:
	adb wait-for-device
	adb uninstall cx.ring

run:
	@echo "=== Running SFLphone on device ==="
	adb wait-for-device
	adb shell am start -n cx.ring/cx.ring.client.HomeActivity

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
