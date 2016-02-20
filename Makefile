# Sources and objects

export ANDROID_HOME=$(ANDROID_SDK)

TOP=$(shell pwd)/ring-android
SRC=$(TOP)/app/src/main
LIBRINGJNI_H=${DAEMON_DIR}/src/dring/dring.h
LIBRINGJNI=$(SRC)/obj/local/${ARCH}/libring.so

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
	$(VERBOSE)cd $(TOP) && chmod +x ./gradlew && ./gradlew $(GRADLE_OPTS) $(GRADLE_TARGET) -Parchs=$(ARCH)
endef

$(RING_APK): $(LIBRINGJNI) $(JAVA_SOURCES)
	@echo
	@echo "=== Building $@ for ${ARCH} ==="
	@echo
	date +"%Y-%m-%d" > $(SRC)/assets/builddate.txt
	echo `id -u -n`@`hostname` > $(SRC)/assets/builder.txt
	git rev-parse --short HEAD > $(SRC)/assets/revision.txt
	# many times the gradlew script is not executable by default
	$(VERBOSE)cd $(TOP) && chmod +x ./gradlew && ./gradlew $(GRADLE_OPTS) $(GRADLE_TARGET) -Parchs=$(ARCH)

$(LIBRINGJNI): $(LIBRINGJNI_H)
	@if [ -z "$(RING_BUILD_DIR)" ]; then echo "RING_BUILD_DIR not defined" ; exit 1; fi
	@if [ -z "$(ANDROID_NDK)" ]; then echo "ANDROID_NDK not defined" ; exit 1; fi
	@echo
	@echo "=== Building libringjni ==="
	@echo
	$(VERBOSE)if [ -z "$(RING_SRC_DIR)" ] ; then RING_SRC_DIR='${DAEMON_DIR}'; fi ; \
	if [ -z "$(RING_CONTRIB)" ] ; then RING_CONTRIB="$$RING_SRC_DIR/contrib/$(TARGET_TUPLE)"; fi ; \
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
	cd $(SRC) && rm -rf libs/armeabi-v7a libs/x86 obj bin $(RING_APK)

clean: lightclean
	rm -rf $(SRC)/gen java-libs/*/gen java-libs/*/bin

jniclean: lightclean
	rm -f $(LIBRINGJNI)

distclean: clean jniclean

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

.PHONY: lightclean clean jniclean distclean distclean-run apkclean apkclean-run install run build-and-run
