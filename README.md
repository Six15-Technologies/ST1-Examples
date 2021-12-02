ST1 Examples
===
ST1 Examples is an Android Studio project which helps developers quickly integrate the Six15 ST1 Head Mounted Display (HMD) into their app.

For more documentation see https://six15.engineering, and particularly https://six15.engineering/examples

Running these examples require the Six15 service application (either "HMD Service" or "ST1 Connect") to be installed on your device.

The APK for "HMD Service" can be found in our Android SDK release at https://six15.engineering/st1_sdk_overview/#latest-release.

The "Six15 ST1" app is available on the Google Play Store https://play.google.com/store/apps/details?id=com.six15.st1_connect.

Modules
===
This project is built from 5 modules.

hudservice-aidl
---
This is a trivial Android library module which adds hudservice-aidl.aar to your app. This AAR file defines the AIDL interface and data structures needed to talk to one of Six15's service apps, either "HMD Service" or "Six15 ST1". This AAR file defines the versioned and backward compatible section of the Six15 SDK.

examples
---
This is an Android library module which contains Java classes that make working with the ST1 easier and less verbose. It can be included into your application as is. Feel free to modify these chasses to fit your needs since they are not part of the Six15's SDK.

examples_test
---
This is an Android application which demonstrates how to use the classes inside "examples". This module does not need to be included inside your application. If you're looking for a simple example of using the ST1, this is it.

vosk-speech-recognition
---
This module contains the class "HudSpeechRecognitionHelper" which uses the Vosk library to perform speech recognition using the ST1's microphone. This module is not part of "examples" because of it's large (~50MB) dependency "vosk-speech-recognition-model". If you're not using "HudSpeechRecognitionHelper" this module can be ignored.

vosk-speech-recognition-model
---
This modules comes from https://github.com/alphacep/vosk-android-demo/ and includes an English language voice model for speech recognition. The "vosk-speech-recognition" module class "HudSpeechRecognitionHelper" depends on this module.