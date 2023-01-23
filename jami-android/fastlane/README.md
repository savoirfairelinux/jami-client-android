fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android beta
```
fastlane android beta
```
Submit a new Beta Build to the Play Store Beta channel
### android production
```
fastlane android production
```
Submit a new Build to the Play Store
### android nopush
```
fastlane android nopush
```
So far, we just sign and align the APK

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
