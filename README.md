# DeviceSync

A super irritating result of having multiple ATV devices scattered around your home/office is that they get out of sync on their app loads. I might install HBOGo on one device and then forget to install on the other. Which irritates the wife when she expects her "tv appliance" to offer the same functionality no matter which TV.

So I wrote this app which, if installed on your device, will sync the app load on your device to a firebase back end. So all your devices will record their app loads in the cloud. Then, when you run this app, it will show you the superset of apps on all your devices and allow you to quickly install/remove to keep devices in sync.

Functionality includes showing apps per device. Allowing you to install/uninstall (launch play intents) from the app. The app also munges the data to provide you views of the superset of all apps installed, any apps which are installed on your other devices but missing on the local device or any apps you have installed on your local device which are unique to that device. Another feature is if you wipe your device and then start this app and there is a record of an app load in the cloud for your device, it will offer to reinstall from the cloud references. Finally, you can use the app to clone  app loads from other devices.

This app is for the udacity nanodegree final project.

# Implementation notes

Supports both ATV and tablet devices. Will show a list of devices and apps to the user using browseview (TV) or recycler view with nav drawer on phone. Phone app also includes a widget to show number of missing apps on the device (and will launch app when clicked).

When starting app, use a initialization state machine service to process all the possible initialization paths including account login, permission requests, etc. This will also show a brief tutorial. Then will dump you into the UI.

The basic structure of the app is the UI which sits in front of a content provider. The local content provider keeps all data synchronized with the cloud. All UI is done through content provider adapters.

There is also a service which is running. This service both implements broadcast receivers (for watching for app install/uninstall events) and also manages the cloud back end updates to the local content provider. 

For certain long running events (such as initialization or updating local device data to the cloud), we use an intent service. 

For help screens, we use a webview (all help stored as html pages).

If you look through the project structure...
*root of project - this contains common files/code used by both ATV and Phone UI (utils, data setup, initialization activity, help activity, etc)
*ATVUI - this contains ATV UI specific code
*PhoneUI - this contains phone/tablet specific code
*cloud - this contains both the firebase interface class as well as the background service used to keep CP updated from the cloud
*data - this is the content provider
*sync - this contains a utility intent service used for longer running operations

In terms of what we use the cloud for, we use it to replicate/share device, app and image data between instances of the app. We use firebase for this. Specifically we use firebase database for real time database updates. We use firebase authentication for authentication services. We use firebase crash reporting for crash reporting services. And we use firebase storage to store images for apks in the cloud (to be shared/used by remote instantiations of the app).

Looking at the data structure, we essentially replicate our CP to firebase - there are 3 basic databases per user node (or per device).
*Devices - contains metadata for the device (name, serial number, location, etc)
*Apps - contains metadata for the apk (name, apk name, etc)
*Images - we use this database to store apk icon images (and references into files stored in firebase storage). Why is this db seperate from app? We don't want to store more than one image per apk if an apk is installed multiple times.

What is critical to remember with both content provider and firebase is the key data fields we key off of. For devices, we use the serial number. This uniquely identifies each device. And there is an assumption that no two devices will have the same serial number. For apps, we use the apk name to uniquely identify the app. And we assume that no two apps will have same apk name (which is safe for google play apps but if you do app development on your own, you could create two different apps with same apk name).

In terms of communicating with the cloud, whenever we write our CP, we upload to the cloud at that point. Whenever the cloud changes, the listener set up by the service is called and we update the local CP.

And for a little background, cloud has gone through some transitions. Originally was going to use GCE and GCM for notifications/storage. But didn't want to set up a server to support. So then moved to GCM (and found a clever way to run notification code without needing a seperate server) + google drive w/json files. It ~worked~ but google drive has some pretty severe latency and synchronization issues when used with higher frequency data updates. Finally tried firebase which fit this project perfectly.

One final note - be aware that I stubbed Log.d so I could quickly turn off debug log messages. If you don't see debug logs, check LogD in utils.java.

# Udacity rubric

A listing of udacity rubric requirements for nanodegree and response:
Third party library - use glide for image display
Validate input - mostly done in the firebase receiver (but there are checks throughout)
Accessibility - yes
Strings - yes - although note that there are debug strings or internal tags for bundles which are obviously not localization targets and thus not in strings.h
Widget - yes - simple widget which shows number of missing apps on device

Google play services - using both GMS location as well as several firebase services (crash, database, storage, auth)
Location - used to show where devices are located (if user allows)

AppCompat - yes. Note that we use two different themes - one for tablet/phone and one for ATV.
App bar/toolbar - yes for phone/tablet UI
Material transitions - yes for both UIs

Building - clean builds working
installRelease - verified
Signing config - included but note that I am using a sample keystore. This app is uploaded to google play right now as a closed alpha and uses real keys (different keystore than sample keys). So if you pull down google play store alpha, will show up as not compatible.
Gradle app dependencies - yes

ContentProvider - yes, built on a CP.
Uses an IntentService for longer running tasks (like populating CP with local device info)
Note that we also use background threads for processing incoming data from firebase cloud (not specifically asynctask but still we spawn off threads)
Loaders - yes, we use loaders and adpters for the UI views backed by the CP

I'll add that one custom view was created for multi-apk installs/uninstalls. Shows a mosaic of the apk icons for a single dialog box.

# Building

A sample keystore is checked into the project. This sample keystore can still be used or replace with your own.


