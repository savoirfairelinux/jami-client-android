# Jami Android

The Jami client for Android

| App | CI
| :-: | :-: |
| [![Download on the Play Store](https://img.shields.io/badge/download-play%20store-blue.svg)](https://play.google.com/store/apps/details?id=cx.ring) [![Download on F-Droid](https://img.shields.io/badge/download-fdroid-blue.svg)](https://f-droid.org/repository/browse/?fdid=cx.ring) | [![Build Status](https://jenkins.jami.net/buildStatus/icon?job=client-android)](https://jenkins.jami.net/job/client-android/)

## Environment

### Submodule

Download the project including the daemon submodule with:

```sh
git clone --recursive https://review.jami.net/jami-client-android
```

Or to download the daemon submodule from the existing project directory:

```sh
git submodule update --init --recursive
```

### Dependencies

Make sure to have autotools, autopoint, swig, yasm, m4, ninja-build and cmake available on your system.

> [!WARNING]
>
> Jami needs at least swig 4.2 to work. Else it will raise errors at compilation.
> See if the package is available with this version from your package manager, else you will need to install it [from sources](https://github.com/swig/swig).

##### On Debian/Ubuntu

```sh
apt install cmake build-essential swig yasm ninja-build m4 autotools-dev autopoint libtool
```

##### On Arch

```sh
pacman -S cmake ninja automake swig yasm m4 patch autoconf pkgconf
```

##### On macOS:

```sh
brew install cmake automake autotools libtool pkg-config rustup yasm swig
"$(brew --prefix rustup)/bin/rustup" default 1.97.1
```

The Rust toolchain is required by the yffi collaborative-editing dependency.
Its Android targets are installed automatically when the dependency is built.

When using brew on macOS, the 'libtoolize' binary might be available as 'glibtoolize'.
In that case, the following command makes it avaialble to the build system:

```sh
ln -s /opt/homebrew/bin/glibtoolize /opt/homebrew/bin/libtoolize
```

### Android SDK & NDK

Make sure to have the Android SDK and NDK available.

## Build instructions

### With Android Studio:

* Add 'jami-android' in Android Studio
* Click on build
* Enjoy!

### With the command line:

```sh
cd jami-client-android/jami-android
./gradlew assembleDebug
```

### Troubleshoot

Jami Android doesn't use the system's `pkg-config`; it builds its own version with custom parameters to support cross-compilation. However, after cleaning the project, `pkg-config` may not be rebuilt, which could result in falling back to the system's version, leading to errors when attempting to locate shared libraries.

```sh
cd jami-client-android/daemon/extras/tools
./bootstrap && make .pkg-config
```

## Android UI tests

Deterministic UI checks run locally with Robolectric on Android 9 and Android 14:

- `AccountCreationTest`: real wizard fragments and registration dialog, with a
	mocked account service and a controlled RxJava debounce clock. Covers username
	availability, password validation, optional password controls, and registration
	submission. The previous valid-username test only checked the loading state;
	its replacement also checks that a successful lookup enables the next button.
- `ChangePasswordDialogTest`: short and mismatched passwords, successful submission,
	password removal, cancellation, keyboard submission, and error correction.
- `ConversationChooserTest`: color and emoji selection, callback values, dismissal,
	and cancellation.

These tests use real Android views and lifecycle callbacks, do not initialize the
native daemon, and need no emulator, network, downloaded images, or ordered test
state. The Hilt service replacements are confined to the JVM test source set.
Twelve independent Espresso validation cases have moved to these local tests.

Run the local app suite from the Android project directory. The native-build task
exclusions avoid the existing Kotlin build dependency on the Android JNI build:

```sh
cd jami-android
./gradlew :app:testNoPushDebugUnitTest -Parchs=arm64-v8a \
	-x ':app:buildCMakeDebug[arm64-v8a]' \
	-x ':app:configureCMakeDebug[arm64-v8a]'
```

The equivalent flavor tasks are `:app:testWithFirebaseDebugUnitTest` and
`:app:testWithUnifiedPushDebugUnitTest`. Add `--tests cx.ring.account.AccountCreationTest`
to select a single test class. The unit-test tasks also run the collaborative
editor's JavaScript tests; `-x :app:testCollabEditor` limits the run to JVM tests.
Use the Android Studio JDK as `JAVA_HOME` when the shell has no suitable JDK.

Espresso remains under `app/src/androidTest` for integration coverage that
Robolectric cannot replace faithfully: real account creation and archive recovery,
nameserver registration, contact invitations over DHT, peer message delivery and
editing, native conversation-setting persistence, camera/gallery integration, and
biometric enrollment. Run these on a device or emulator with the Android native
library. The chooser integration scenarios remain because they also verify that
the selected values reach the conversation, not just that the picker returns them.

## Update translations

Update translations using the Transifex client (tx) :
```sh
./update-translations.sh
```

## Generate new release commit

Generate a new release commit updating the version code and version string:
```sh
./update_version.py --commit
```

## Report issues

Report issues on Gitlab:
https://git.jami.net/savoirfairelinux/jami-client-android
