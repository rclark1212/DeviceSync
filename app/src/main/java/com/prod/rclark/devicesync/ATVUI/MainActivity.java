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
/*
 * Copyright (C) 2016 Richard Clark
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

package com.prod.rclark.devicesync.ATVUI;

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

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.cloud.FirebaseMessengerService;

/*
 * MainActivity class that loads MainFragment
 */
public class MainActivity extends Activity implements
        MainFragment.OnMainActivityCallbackListener {
    /**
     * Called when the activity is first created.
     */
    private static final String TAG = "MainActivity";

    //Callback ordinals
    protected static final int CALLBACK_SEND_TO_SERVICE = 2017;


    //  Service
    boolean mBoundToService;
    Messenger mService = null;
    //  Class for interacting with our firebase service...
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBoundToService = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBoundToService = false;
        }
    };

    //Routine called by other fragments/receivers to request activity actions
    public void onMainActivityCallback(int callbackcode, String data, String extra) {
        switch (callbackcode) {
            case CALLBACK_SEND_TO_SERVICE: {
                Log.d(TAG, "Callback: sendMessageToService");
                sendMessageToService();
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown command to main callback: " + callbackcode);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    @Override
    public void onStart() {
        super.onStart();
        // Bind to the service
        bindService(new Intent(this, FirebaseMessengerService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBoundToService) {
            unbindService(mConnection);
            mBoundToService = false;
        }
    }

    /**
     * Sends a message to our service
     */
    public void sendMessageToService() {
        if (!mBoundToService) {
            Log.d(TAG, "Service not bound but someone tried to send message");
            return;
        }

        Message msg = Message.obtain(null, FirebaseMessengerService.MSG_SAY_HELLO, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d(TAG, "Error accessing service!");
            e.printStackTrace();
        }
    }

}
