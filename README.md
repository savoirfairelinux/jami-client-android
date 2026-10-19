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

## Backup and device transfer

Android cloud backup is disabled by explicit data-extraction rules. Device-to-device
transfer is allowed on Android 9 and later, but currently includes only a versioned
snapshot of reviewed display and privacy preferences. Accounts, device identities,
conversations, attachments, push tokens, biometric credentials, logs, and runtime
service configuration are not included. Continue using account export/import or
authenticated device linking to recover or add accounts; this settings transfer is
not a complete account or conversation backup.

The backup agent has no Hilt or daemon dependency. Restored settings are validated
and staged outside the live preferences, then committed for idempotent replay before
normal runtime startup. Interrupted or invalid payloads do not replace live settings,
and legacy sandbox restore paths are ignored. Android 16's restricted-backup-mode
opt-out is enabled; older restricted-mode service launches are guarded before Hilt
injection. Daemon startup is demand-driven, not a side effect of creating the
Application for backup or restore.

New cloud exclusions do not delete historical backups already held by a transport.
Full account/history migration and cross-platform transfers are not enabled yet.

Focused regression tests live in `PortableSettingsBackupTest` and run with
`./gradlew :app:testNoPushDebugUnitTest --tests cx.ring.backup.PortableSettingsBackupTest`.
Before release, also verify the policy with Android's cloud and D2D transports on
physical devices, including interrupted restore and concurrent background wakeups.

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
