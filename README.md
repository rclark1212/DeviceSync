# DeviceSync

A super irritating result of having multiple ATV devices scattered around your home/office is that they get out of sync on their app loads. I might install HBOGo on one device and then forget to install on the other. Which irritates the wife when she expects her "tv appliance" to offer the same functionality no matter which TV.

So I wrote this app which, if installed on your device, will sync the app load on your device to a GCE back end. So all your devices will record their app loads in the cloud. Then, when you run this app, it will show you the superset of apps on all your devices and allow you to quickly install/remove to keep devices in sync.

# Implementation

Create an intent service which runs in the background which communicates between GCE and a CP. Two intents that matter. When an app gets installed on local device, upload that info to the cloud. When the cloud updates, go ahead and store that info in the CP.

For the UI, just re-use the sample ATV app. Note that it is a bit hacky in that I didn't bother to rename the classes or cut out the code I didn't need. This is a utility app - not one whose destiny is the play store.


# Building

You must set the following variables in gradle.properties
KEYSTORE_PATH={filespec of keystore}
SIGNKEY_ALIAS={keyname}
KEYSTORE_PW=password
KEY_PW=password

TBD
