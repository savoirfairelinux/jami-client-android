# Sources and objects

export ANDROID_HOME=$(ANDROID_SDK)

TOP=$(shell pwd)/ring-android
SRC=$(TOP)/app/src/main

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

$(RING_APK): $(JAVA_SOURCES)
	@echo
	@echo "=== Building $@ for ${ARCH} ==="
	@echo
	date +"%Y-%m-%d" > $(SRC)/assets/builddate.txt
	echo `id -u -n`@`hostname` > $(SRC)/assets/builder.txt
	git rev-parse --short HEAD > $(SRC)/assets/revision.txt
	# many times the gradlew script is not executable by default
	$(VERBOSE)cd $(TOP) && chmod +x ./gradlew && ./gradlew $(GRADLE_OPTS) $(GRADLE_TARGET) -Parchs=$(ARCH)

apk:
	$(call build_apk)

apkclean:
	rm -f $(RING_APK)

lightclean:
	cd $(SRC) && rm -rf libs/$(ARCH) obj bin $(RING_APK)

clean: lightclean
	rm -rf $(SRC)/gen java-libs/*/gen java-libs/*/bin

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

.PHONY: lightclean clean distclean distclean-run apkclean apkclean-run install run build-and-run
