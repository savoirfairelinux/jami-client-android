# Sources and objects

export ANDROID_HOME=$(ANDROID_SDK)

ARCH = $(ANDROID_ABI)

PSRC=ring-android
SRC=$(PSRC)/app/src/main
LIBRINGJNI_H=ring-daemon/src/dring/dring.h
LIBRINGJNI=$(SRC)/obj/local/${ANDROID_ABI}/libring.so

JAVA_SOURCES=$(shell find $(SRC)/java/cx/ring/ -type f -name "*.java")

ifneq ($(V),)
GRADLE_OPTS += -d
VERBOSE =
GEN =
else
VERBOSE = @
GEN = @echo "Generating" $@;
endif

ifeq ($(RELEASE),1)
GRADLE_TARGET = assembleRelease
RING_APK=$(SRC)/bin/Ring-release-unsigned.apk
NDK_DEBUG=0
else
GRADLE_TARGET = assembleDebug
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
	# many times the gradlew script is not executable by default
	$(VERBOSE)cd $(PSRC) && chmod +x ./gradlew && ./gradlew $(GRADLE_OPTS) $(GRADLE_TARGET)
endef

$(RING_APK): $(JAVA_SOURCES)
	@echo
	@echo "=== Building $@ for $(ARCH) ==="
	@echo
	date +"%Y-%m-%d" > $(SRC)/assets/builddate.txt
	echo `id -u -n`@`hostname` > $(SRC)/assets/builder.txt
	git rev-parse --short HEAD > $(SRC)/assets/revision.txt
	# many times the gradlew script is not executable by default
	$(VERBOSE)cd $(PSRC) && chmod +x ./gradlew && ./gradlew $(GRADLE_OPTS) $(GRADLE_TARGET)

apk:
	$(call build_apk)

apkclean:
	rm -f $(RING_APK)

lightclean:
	cd $(SRC) && rm -rf libs/armeabi-v7a libs/x86 libs/mips obj bin $(RING_APK)

clean: lightclean
	rm -rf $(SRC)/gen java-libs/*/gen java-libs/*/bin .sdk

distclean: clean

install: $(RING_APK)
	@echo "=== Installing Ring on device ==="
	adb wait-for-device
	adb install -r $(RING_APK)

uninstall:
	adb wait-for-device
	adb uninstall cx.ring

run:
	@echo "=== Running Ring on device ==="
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
