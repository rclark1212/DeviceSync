package com.prod.rclark.devicesync.cloud;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.sync.GCESync;

/**
 * FirebaseMessengerService encapsulates all the firebase activities we use in the app.
 * We put this in a service for a few reasons:
 *  1) Firebase operations (except for logon) are fire and forget
 *  2) We need firebase access even when the app not running (to support updates for our broadcast receiver on app install/uninstall)
 *  3) And firebase uses callbacks which can be long running meaning an intent service can die before callback comes back
 *
 *  Communication:
 *  This service will take bundles as its input for what functions to perform.
 *  Service will use messages back to calling activity to return information as needed.
 *
 *  A few important considerations for the service:
 *  Logon - we do not do any UI in the service. The service will attempt to silently logon when it starts. If it fails, service
 *  will stay quiescent. So this service will not work until app is authenticated for the first time.
 *  The first step for any app to use this service will be to query if service is logged in. If not, the app needs to do the
 *  login process including any UI. And then request the service to try logging in again.
 *
 */
public class FirebaseMessengerService extends Service {
    private final static String TAG = "FirebaseService";
    private FirebaseApp mFirebaseApp = null;
    private Firebase mFirebase = null;
    private boolean mbLoggedOn = false;
    private FirebaseAuth.AuthStateListener mAuthListener = null;

    /** Command to the service to display a message */
    public static final int MSG_SAY_HELLO = 1;
    public static final int MSG_QUERY_LOGON_STATUS = 2;
    public static final int MSG_ATTEMPT_LOGON = 3;
    public static final int MSG_WRITE_APP_TO_FIREBASE = 4;
    public static final int MSG_WRITE_DEVICE_TO_FIREBASE = 5;
    public static final int MSG_DELETE_APP_FROM_FIREBASE = 6;
    public static final int MSG_DELETE_DEVICE_FROM_FIREBASE = 7;

    /** Params used in bundles */
    public static final String SERIAL_PARAM = "serial";
    public static final String APK_PARAM = "apk";

    public FirebaseMessengerService() {}

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            //FIXME - for debugging purposes...
            if (mFirebaseApp == null) {
                mFirebaseApp = FirebaseApp.getInstance();
            }

            switch (msg.what) {
                case MSG_SAY_HELLO: {
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    break;
                }
                case MSG_QUERY_LOGON_STATUS: {
                    Intent localIntent;
                    if (mbLoggedOn) {
                        localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.FIREBASE_SERVICE_LOGGEDIN);
                    } else {
                        localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.FIREBASE_SERVICE_NOTLOGGEDIN);
                    }
                    //And broadcast the message
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
                }
                case MSG_ATTEMPT_LOGON: {
                    if (!mbLoggedOn) {
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null) {
                            // already signed in - w00t w00t
                            //store in prefs
                            Log.d(TAG, "Firebase signed in with " + auth.getCurrentUser().getEmail());
                            String user = auth.getCurrentUser().getUid();
                            //does the user match what has been stored?
                            if (!user.equals(Utils.getUserId(getApplicationContext()))) {
                                mFirebase = null;
                            }
                            if (mFirebase == null) {
                                //init the instance
                                mFirebase = new Firebase(getApplicationContext(), user);
                                setupFirebase();
                            }
                            Utils.setUserId(getApplicationContext(), user);
                            mbLoggedOn = true;
                        }
                    }
                    Intent localIntent;
                    if (mbLoggedOn) {
                        localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.FIREBASE_SERVICE_LOGGEDIN);
                    } else {
                        localIntent = new Intent(GCESync.BROADCAST_ACTION).putExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.FIREBASE_SERVICE_NOTLOGGEDIN);
                    }
                    //And broadcast the message
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localIntent);
                }
                case MSG_WRITE_APP_TO_FIREBASE: {
                    Bundle params = msg.getData();
                    if ((params != null) && (mFirebase != null)) {
                        String serial = params.getString(SERIAL_PARAM);
                        String apk = params.getString(APK_PARAM);
                        Log.d(TAG, "Writing app to firebase - " + serial + " " + apk);
                        mFirebase.writeAppToFirebase(serial, apk);
                    }
                    break;
                }
                case MSG_WRITE_DEVICE_TO_FIREBASE: {
                    Bundle params = msg.getData();
                    if ((params != null) && (mFirebase != null)) {
                        String serial = params.getString(SERIAL_PARAM);
                        Log.d(TAG, "Writing device to firebase - " + serial);
                        mFirebase.writeDeviceToFirebase(serial);
                    }
                    break;
                }
                case MSG_DELETE_DEVICE_FROM_FIREBASE: {
                    Bundle params = msg.getData();
                    if ((params != null) && (mFirebase != null)) {
                        String serial = params.getString(SERIAL_PARAM);
                        Log.d(TAG, "Deleting device from firebase - " + serial);
                        //Note that the below will delete all records for the serial number. So when called...
                        //Will delete devices/apps remotely. That triggers listeners to delete locally.
                        mFirebase.deleteFirebaseRecord(serial);
                    }
                    break;
                }
                case MSG_DELETE_APP_FROM_FIREBASE: {
                    Bundle params = msg.getData();
                    if ((params != null) && (mFirebase != null)) {
                        String serial = params.getString(SERIAL_PARAM);
                        String apk = params.getString(APK_PARAM);
                        Log.d(TAG, "Deleting app from firebase - " + serial + " " + apk);
                        mFirebase.deleteAppFromFirebase(serial, apk);
                    }
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Initialize here...
        //Set firebase
        mFirebaseApp = FirebaseApp.getInstance();
        //Sign in with firebase...
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // already signed in - w00t w00t
            //store in prefs
            Log.d(TAG, "Firebase signed in with " + auth.getCurrentUser().getEmail());
            String user = auth.getCurrentUser().getUid();
            //does the user match what has been stored?
            if (!user.equals(Utils.getUserId(getApplicationContext()))) {
                mFirebase = null;
            }
            if (mFirebase == null) {
                //init the instance
                mFirebase = new Firebase(getApplicationContext(), user);
                setupFirebase();
            }
            Utils.setUserId(getApplicationContext(), user);
            mbLoggedOn = true;
        }

        //set up a firebase authentication listener for any changes...
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    if (!user.getUid().equals(Utils.getUserId(getApplicationContext()))) {
                        //reset firebase instance
                        mFirebase = null;
                        mFirebase = new Firebase(getApplicationContext(), user.getUid());
                        setupFirebase();
                        Utils.setUserId(getApplicationContext(), user.getUid());
                    }
                    mbLoggedOn = true;
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    mbLoggedOn = false;
                }
            }
        };

    }

    private void setupFirebase() {
        //Move setup out of onConnected and to here...
        if (mFirebase != null) {
            mFirebase.registerFirebaseDataListeners();
        }
    }
}
