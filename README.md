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
transfer is allowed on Android 9 and later and includes the daemon configuration,
account directories, local archives, conversations, locally stored attachments,
history databases (including SQLite sidecars), profiles, and a versioned snapshot
of reviewed display and privacy preferences. Within account data, only the device
identity files (`ring_device.key` and `ring_device.crt`) are excluded. Symbolic links
are not followed. Push tokens, Android Keystore-bound biometric preferences, logs,
and other application-local state outside the account directories are not copied.

The daemon must be stopped throughout export, restore, and replay. Backup-agent
activation and native startup share a lock, and a queued startup from before backup
is invalidated. If Android binds the agent into a process with a running daemon,
the transfer fails rather than copying live data or stopping an active session.
Android 16's restricted-backup-mode opt-out remains enabled; older restricted-mode
service launches are guarded before Hilt injection. Daemon startup is demand-driven,
not a side effect of creating the Application for backup or restore.

The backup agent has no Hilt dependency and does not initialize the daemon. Account
data is streamed directly into Android's backup transport, without first creating
a second local archive. Restores are staged outside the live data and require a
versioned completion marker with a matching file count. Only after restore finishes
are they published for replay before normal runtime startup. Interrupted restores
and legacy account payloads without the marker are discarded. Replay replaces each
transferred account directory, so an existing destination device identity cannot
survive the restore; interrupted replay resumes before the daemon can start.

On first normal startup, the daemon's existing migration flow regenerates the device
identity from the transferred local account archive. Unprotected archives migrate
automatically; protected archives require the account password through the existing
migration UI. The source device's identity remains untouched. Attachments stored
only outside Jami's private account directories are not included.

New cloud exclusions do not delete historical backups already held by a transport.
Cross-platform transfers are not enabled.

Focused regression tests live in `PortableSettingsBackupTest` and run with
`./gradlew :app:testNoPushDebugUnitTest --tests cx.ring.backup.PortableSettingsBackupTest`.
Before release, also verify the policy with Android's cloud and D2D transports on
physical devices, including password-protected and unprotected account migration,
new device IDs, conversation and attachment integrity, interrupted restore/replay,
and concurrent background wakeups. A process with a running daemon must reject
account transfer; a cold backup process must complete it without starting the daemon.

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
