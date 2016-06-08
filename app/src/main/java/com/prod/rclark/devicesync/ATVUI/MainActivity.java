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
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.prod.rclark.devicesync.HelpActivity;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.cloud.FirebaseMessengerService;

/*
 * MainActivity class that loads MainFragment
 *
 * We do our service initialization now in MainActivity:
 * 1) First, check for internet
 * 2) Next, if first time install, show tutorial
 * 3) Next,
 */
public class MainActivity extends Activity implements
        MainFragment.OnMainActivityCallbackListener {
    /**
     * Called when the activity is first created.
     */
    private static final String TAG = "MainActivity";

    //Callback ordinals
    protected static final int CALLBACK_SERVICE_HELLO = 2017;
    protected static final int CALLBACK_SERVICE_QUERY_LOGIN = 2018;
    protected static final int CALLBACK_SERVICE_REQUEST_LOGIN = 2019;

    //main fragment
    private MainFragment mMainFragment=null;

    //Initialization state machine params/variables
    private final static int STATE_EVENT_ONCREATE = 0;
    private final static int STATE_EVENT_SRVC_BIND = 1;
    private final static int STATE_EVENT_INET_OKAY = 2;
    private final static int STATE_EVENT_WELCOME_DONE = 3;
    private final static int STATE_EVENT_GMS_AVAILABLE = 4;
    private final static int STATE_EVENT_TUTORIAL_DONE = 5;
    private final static int STATE_EVENT_FIREBASE_LOGON = 6;
    private final static int STATE_EVENT_PERMISSION_CHECKED = 7;


    //Initialization state machine params/variables
    private final static int STATE_LAUNCH = 0;
    private final static int STATE_SHOWING_WELCOME = 1;
    private final static int STATE_FIXING_GMS = 2;
    private final static int STATE_LOGGING_ON = 3;
    private final static int STATE_CHECKING_PERMISSIONS = 5;
    private final static int STATE_SHOWING_TUTORIAL = 6;
    private final static int STATE_APPINIT_COMPLETE = 7;

    private int mInitCurrentState = STATE_LAUNCH;

    //state variables
    private boolean mbCreate = false;
    private boolean mbServiceBind = false;
    private boolean mbInetOkay = false;
    private boolean mbWelcomeDone = false;
    private boolean mbGMSAvailable = false;
    private boolean mbTutorialShown = false;
    private boolean mbFirebaseLoggedOn = false;
    private boolean mbHaveLocationPermission = false;

    //Pending intent returns...
    private int WELCOME_INTENT_DONE = 3017;
    private int TUTORIAL_INTENT_DONE = 3018;

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
            Log.d(TAG, "got bound to service callback");
            mBoundToService = true;
            //create fragment after we bind...
            appInitStateMachine(STATE_EVENT_SRVC_BIND);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.d(TAG, "got unbound to service callback");
            mBoundToService = false;
        }
    };

    //Routine called by other fragments/receivers to request activity actions
    public void onMainActivityCallback(int callbackcode, String data, String extra) {
        switch (callbackcode) {
            case CALLBACK_SERVICE_HELLO: {
                Log.d(TAG, "Callback: sendMessageToService - hello");
                sendMessageToService(FirebaseMessengerService.MSG_SAY_HELLO, null);
                break;
            }
            case CALLBACK_SERVICE_QUERY_LOGIN: {
                Log.d(TAG, "Callback: sendMessageToService - query login");
                if (!sendMessageToService(FirebaseMessengerService.MSG_QUERY_LOGON_STATUS, null)) {
                    //We have a problem...
                    Toast.makeText(getApplicationContext(), "Service not bound", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case CALLBACK_SERVICE_REQUEST_LOGIN: {
                Log.d(TAG, "Callback: sendMessageToService - request login");
                if (!sendMessageToService(FirebaseMessengerService.MSG_ATTEMPT_LOGON, null)) {
                    //We have a problem
                    Toast.makeText(getApplicationContext(), "Service not bound", Toast.LENGTH_SHORT).show();
                }
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

        Log.d(TAG, "onCreate");
        appInitStateMachine(STATE_EVENT_ONCREATE);

        Log.d(TAG, "Checking internet");
        //First check internet connectivity
        if (!Utils.isOnline(this)) {
            finishIt();
        } else {
            appInitStateMachine(STATE_EVENT_INET_OKAY);
        }
    }


    //Finish setup by creating browse fragment
    private void finishSetup() {
        if (mMainFragment == null) {
            Log.d(TAG, "finishing setup and creating browsefragment");
            mMainFragment = new MainFragment();
            //otherwise and put it in the container
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mMainFragment);
            transaction.commit();
            getFragmentManager().executePendingTransactions();  //force the commit to take place
        }
    }

    //Shows a welcome screen (and hides some processing behind it)
    private void showWelcome() {
        Intent intent = new Intent(getApplication(), HelpActivity.class);

        if (Utils.isRunningForFirstTime(this, false)) {
            //Start activity from fragment so we get result back...
            startActivityForResult(intent, WELCOME_INTENT_DONE);
        }
        appInitStateMachine(STATE_EVENT_WELCOME_DONE);
    }

    //Shows a short tutorial screen (and hides some processing behind it)
    private void showTutorial() {
        Intent intent = new Intent(getApplication(), HelpActivity.class);

        if (Utils.isRunningForFirstTime(this, false)) {
            //Start activity from fragment so we get result back...
            startActivityForResult(intent, TUTORIAL_INTENT_DONE);
        }
        appInitStateMachine(STATE_EVENT_WELCOME_DONE);
    }


    @Override
    public void onStart() {
        super.onStart();
        // Bind to the service
        if (!mBoundToService) {
            Log.d(TAG, "onStart - binding to service");
            bindService(new Intent(this, FirebaseMessengerService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        // Unbind from the service
        Log.d(TAG, "onStop - unbinding from service");
        if (mBoundToService) {
            mBoundToService = false;
            unbindService(mConnection);
        }
        Log.d(TAG, "onStop - unbound from service");
        super.onStop();
    }

    /**
     * Sends a message to our service
     */
    private boolean sendMessageToService(int messageId, Bundle bundle) {
        if (!mBoundToService) {
            Log.d(TAG, "Service not bound but someone tried to send message");
            return false;
        }

        Message msg = Message.obtain(null, messageId, 0, 0);
        if (bundle != null) {
            msg.setData(bundle);
        }

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d(TAG, "Error accessing service!");
            e.printStackTrace();
        }

        return true;
    }

    /**
     * The initialization of the lifecycle is ending up causing spaghetti event code. Try to unify all the lifecycle
     * starts into one synchronized state machine. Can then trigger on this as we debug lifecyle events.
     *
     * @param newEvent
     */
    public synchronized void appInitStateMachine(int newEvent) {

        //Step1, as new events come in set the various flags
        //Probably a better way to do this but my mind works this way...
        switch (newEvent) {
            case STATE_EVENT_ONCREATE:
                mbCreate = true;
                break;
            case STATE_EVENT_SRVC_BIND:
                mbServiceBind = true;
                break;
            case STATE_EVENT_INET_OKAY:
                mbInetOkay = true;
                break;
            case STATE_EVENT_WELCOME_DONE:
                mbWelcomeDone = true;
                break;
            case STATE_EVENT_GMS_AVAILABLE:
                mbGMSAvailable = true;
                break;
            case STATE_EVENT_TUTORIAL_DONE:
                mbTutorialShown = true;
                break;
            case STATE_EVENT_FIREBASE_LOGON:
                mbFirebaseLoggedOn = true;
                break;
            case STATE_EVENT_PERMISSION_CHECKED:
                mbHaveLocationPermission = true;
                break;
        }

        //
        //Now execute the state machine to process the newEvent (if there is any processing to do)
        // Note this is in general order of operation
        //
        switch (mInitCurrentState) {
            case STATE_LAUNCH:
                if (mbCreate && mbServiceBind && mbInetOkay) {
                    //service, oncreate and inet all okay...
                    //Okay to have a single state waiting for all 3 since all 3 failing will punt us out of app
                    //Kick off GMS enable, logon and tutorial
                    mInitCurrentState = STATE_SHOWING_WELCOME;
                    showWelcome();
                }
                break;
            case STATE_SHOWING_WELCOME:
                //Okay, welcome taken care of - now we branch...
                /*
                if (mbWelcomeDone) {
                    if (mbGMSLoggedOn && mbFirebaseLoggedOn) {
                        //move all the way to permission
                        mInitCurrentState = STATE_CHECKING_PERMISSIONS;
                        //checkPermissions
                    } else if (!mbGMSLoggedOn) {
                        //move to fix GMS
                        mInitCurrentState = STATE_FIXING_GMS;
                        //fixGMS
                    } else if (!mbFirebaseLoggedOn) {
                        //move to logon to firebase
                        mInitCurrentState = STATE_LOGGING_ON;
                        //firebaselogon
                    }
                }
                 */
                if (mbWelcomeDone) {
                    //welcome has been completed
                    finishSetup();
                }
                break;
            case STATE_FIXING_GMS:
                if (mbGMSAvailable) {
                    if (mbFirebaseLoggedOn) {
                        //move to checking permissions
                        mInitCurrentState = STATE_CHECKING_PERMISSIONS;
                        //check permissions
                    } else {
                        mInitCurrentState = STATE_LOGGING_ON;
                        //firebase logon
                    }
                }
                break;
            case STATE_LOGGING_ON:
                if (mbFirebaseLoggedOn) {
                    //Okay - logged on... Check permissions
                    mInitCurrentState = STATE_CHECKING_PERMISSIONS;
                    //check permissions
                }
                break;
            case STATE_CHECKING_PERMISSIONS:
                if (mbHaveLocationPermission) {
                    //Okay - got permission result (positive or negative) for location
                    mInitCurrentState = STATE_SHOWING_TUTORIAL;
                    showTutorial();
                }
                break;
            case STATE_SHOWING_TUTORIAL:
                if (mbTutorialShown) {
                    mInitCurrentState = STATE_APPINIT_COMPLETE;
                }

        }

    }


    /**
     * Used to exit app when there is an error. Really GMS services the only error that will cause controlled exit :)
     */
    private void finishIt() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getResources().getString(R.string.app_err_title));

        alertDialogBuilder
                .setMessage(getResources().getString(R.string.gms_missing_msg))
                .setCancelable(false)
                .setNeutralButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
