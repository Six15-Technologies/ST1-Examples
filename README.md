ST1 Examples
===
ST1 Examples is an Android Studio project which helps developers quickly integrate the Six15 ST1 Head Mounted Display (HMD) into their app.

For more documentation see https://six15.engineering, and particularly https://six15.engineering/examples

__Running these examples requires the "Six15 ST1" application to be installed on your device.__

An APK file can be found as part of our Android SDK release at https://six15.engineering/st1_sdk_overview/#latest-release.
Or directly downloaded at https://six15.engineering/latest_st1_app

The app is also available on the Google Play Store https://play.google.com/store/apps/details?id=com.six15.st1_connect.

Modules
===
This project is built from 5 modules.

hudservice-aidl
---
This is a trivial Android library module which adds hudservice-aidl.aar to your app. This AAR file defines the AIDL interface and data structures needed to talk to the "Six15 ST1" app. This AAR file defines the versioned and backward compatible section of the Six15 SDK.

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