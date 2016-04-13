/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.rclark.devicesync.ATVUI;

/*
Quick summary of app.
Super irritating to have multiple ATVs that never stay in sync with the apps installed.
This app is intended to communicate with GCE through the google ID to upload apps on the device.
All devices which this app is installed on will do this.
The app will show a list of devices in GCE (information only)
And will show a list of apps which is the superset of all devices
Can then kick off an intent to install each app not installed (or go through a loop to install all)
What also *could* be done is to have a service watching in background and pop up a notification when something installed to another
device.

Launch entry point on ATV

http://stackoverflow.com/questions/4604239/install-application-programmatically-on-android

 */

import android.app.Activity;
import android.os.Bundle;

import com.example.rclark.devicesync.R;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
