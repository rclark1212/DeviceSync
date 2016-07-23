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

package com.prod.rclark.devicesync.sync;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.prod.rclark.devicesync.ATVUI.MainFragment;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.DeviceSyncReceiver;
import com.prod.rclark.devicesync.InstallUtil;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 *
 * Do a couple things with this service class...
 * (1) Update(merge) local database with current apps/devices
 * (2) Push(merge) local database to GCE
 * (3) Fetch(merge) remote database from GCE
 * (4) Do the local scan of apps/devices
 *
 */
public class GCESync extends IntentService {

    private static final String TAG = "GCESync";

    private static final String ACTION_UPDATE_LOCAL_DB = "com.prod.rclark.devicesync.sync.action.Update";
    private static final String ACTION_UPDATE_LOCAL_DEVICE = "com.prod.rclark.devicesync.sync.action.DeviceUpdate";
    private static final String ACTION_UPDATE_LOCAL_APP = "com.prod.rclark.devicesync.sync.action.AppUpdate";
    private static final String ACTION_INSTALL_APK = "com.prod.rclark.devicesync.sync.action.APKInstall";

    private static final String EXTRA_PARAM1 = "com.prod.rclark.devicesync.sync.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.prod.rclark.devicesync.sync.extra.PARAM2";

    // Message defines for communicating back to calling activity
    // Used by settings panel as well...
    public static final String BROADCAST_ACTION = "com.prod.rclark.devicesync.BROADCAST";

    public static final String EXTENDED_DATA_STATUS = "com.prod.rclark.devicesync.gcesync.STATUS";
    public static final String EXTENDED_DATA_CMD = "com.prod.rclark.devicesync.gcesync.CMD";

    public static final String EXTENDED_DATA_PARAM1 = "com.prod.rclark.devicesync.gcesync.PARAM1";
    public static final String EXTENDED_DATA_PARAM2 = "com.prod.rclark.devicesync.gcesync.PARAM2";

    public static final int EXTENDED_DATA_STATUS_NULL = 0;

    //Status messages for broadcasts above
    public static final int EXTENDED_DATA_STATUS_LOCALUPDATECOMPLETE = 1;
    public static final int EXTENDED_DATA_STATUS_PUSHCOMPLETE = 2;
    public static final int MAINACTIVITY_SHOW_NETWORKBUSY = 8;
    public static final int MAINACTIVITY_SHOW_NETWORKFREE = 9;
    public static final int EXTENDED_DATA_STATUS_PHOTO_COMPLETE = 10;
    public static final int FIREBASE_SERVICE_LOGGEDIN = 20;
    public static final int FIREBASE_SERVICE_NOTLOGGEDIN = 21;

    //Command messages for broadcasts above

    private static Context mCtx;
    private static boolean mbIsATV;

    public GCESync() {
        super("GCESync");
    }

    /**
     * Full update of the local device in the content provider. Updates both the device as well as the apps
     *
     * @see IntentService
     */
    public static void startActionUpdateLocal(Context context, String param1, String param2) {
        if (!Utils.isSyncDisabled(context)) {
            Intent intent = new Intent(context, GCESync.class);
            intent.setAction(ACTION_UPDATE_LOCAL_DB);
            //intent.putExtra(EXTRA_PARAM1, param1);
            //intent.putExtra(EXTRA_PARAM2, param2);
            context.startService(intent);
        }
    }

    /**
     * Updates the local device in the content provider. Updates the device only (not the apps)
     *
     * @see IntentService
     */
    public static void startActionLocalDeviceUpdate(Context context, String param1, String param2) {
        if (!Utils.isSyncDisabled(context)) {
            Intent intent = new Intent(context, GCESync.class);
            intent.setAction(ACTION_UPDATE_LOCAL_DEVICE);
            context.startService(intent);
        }
    }

    /**
     * Updates a local app in the CP
     *
     * Note that this called by broadcast receiver. Make sure to process delay once we are inside the intentservice...
     *
     * @see IntentService
     */
    public static void startActionLocalAppUpdate(Context context, String pkgname, String delayExecutionInMS) {
        if (!Utils.isSyncDisabled(context)) {
            Intent intent = new Intent(context, GCESync.class);
            intent.setAction(ACTION_UPDATE_LOCAL_APP);
            intent.putExtra(EXTRA_PARAM1, pkgname);
            intent.putExtra(EXTRA_PARAM2, delayExecutionInMS);
            context.startService(intent);
        }
    }

    /**
     * Kicks off an APK install intent. Why are we doing this in an intent service? Well, for devs, they may
     * have apps on their system not available on google play. So before launching intent, see if it is available
     * on play store. And if not, issue a ACTION_SKIP to move on...
     *
     * once we are inside the intentservice...
     *
     * @see IntentService
     */
    public static void startActionAPKInstall(Context context, String pkgname, String intentPrefix) {
        Intent intent = new Intent(context, GCESync.class);
        intent.setAction(ACTION_INSTALL_APK);
        intent.putExtra(EXTRA_PARAM1, pkgname);
        intent.putExtra(EXTRA_PARAM2, intentPrefix);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //first, do we have context saved off?
        if (mCtx == null) {
            mCtx = getApplicationContext();
            //also cache OS status here...
            mbIsATV = Utils.bIsThisATV(mCtx);
        }

        //next attach to GMS if not yet attached...
        //NO - MOVED all GMS processing to the primary activity

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_UPDATE_LOCAL_DB.equals(action)) {
                //final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                //final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionUpdate();
            } else if (ACTION_UPDATE_LOCAL_DEVICE.equals(action)) {
                //update the device...
                handleActionLocalDeviceUpdate();
            } else if (ACTION_UPDATE_LOCAL_APP.equals(action)) {
                //update the app...
                final String pkgname = intent.getStringExtra(EXTRA_PARAM1);
                final String msDelayStr = intent.getStringExtra(EXTRA_PARAM2);
                int msDelay = 0;
                //has a delay been sent to us? Grab it first...
                if (msDelayStr != null) {
                    try {
                        msDelay = Integer.parseInt(msDelayStr);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Bad delay string passed in to ACTION_UPDATE_LOCAL_APP");
                    }
                }
                if (msDelay > 0) {
                    //run the action on a timer thread
                    Timer timer = new Timer();
                    Log.d(TAG, "Scheduling app (" + pkgname + ") CP update in " + msDelay + "ms");
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            //run your service
                            handleActionLocalAppUpdate(pkgname);
                        }
                    }, msDelay);
                } else {
                    //call immediately
                    handleActionLocalAppUpdate(pkgname);
                }
            } else if (ACTION_INSTALL_APK.equals(action)) {
                final String pkgname = intent.getStringExtra(EXTRA_PARAM1);
                final String installIntent = intent.getStringExtra(EXTRA_PARAM2);
                handleActionInstallAPK(pkgname, installIntent);
            }
        }
    }


    /**
     * Updates the local device...
     * Specifically, captures device info and updates the CP with it.
     */
    private void handleActionLocalDeviceUpdate() {
        //Okay - first update the device database - serial number, nickname, date, model name, os_ver
        //Get an object with local device info...
        ObjectDetail device = AppUtils.getLocalDeviceInfo(mCtx);

        device.timestamp = System.currentTimeMillis();

        //save the device to CP...
        DBUtils.saveDeviceToCP(mCtx, device);
    }

    /**
     * Routine below will update local CP with local device data.
     */
    private void handleActionUpdate() {
        //grab current time first
        long currentTime = System.currentTimeMillis();

        //Update the local device...
        handleActionLocalDeviceUpdate();

        /**
         * Okay - device database should be updated. Now time to do same for apps...
         */
        //create the buffer
        ContentValues contentValues = new ContentValues();

        //Get the device DB reference...
        Uri appDB = AppContract.AppEntry.CONTENT_URI;

        //Step 1 - get a copy of the apps...
        ArrayList<ObjectDetail> apps = AppUtils.loadApps(mCtx);

        //Step 2 - use a ContentProviderOperation for better perf...
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        //Step 3 - write them to db...
        for (int i = 0; i < apps.size(); i++) {
            //Now, search for the device (is it in DB yet?)
            ObjectDetail app = (ObjectDetail) apps.get(i);
            long flags = 0;

            //Remember, device.pkg contains the serial number... (unique)
            Uri appSearchUri = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(app.pkg).build();

            //Build an insert URI to trigger notifications...
            Uri insertUri = appDB.buildUpon().appendPath(Build.SERIAL).build();

            //clear contentValues...
            contentValues.clear();

            //Log.d(TAG, "app query - uri:" + appSearchUri.toString());
            Cursor c = mCtx.getContentResolver().query(appSearchUri, null, null, null, null);

            if (c.getCount() > 0) {
                //device exists...
                //preload the content values...
                c.moveToFirst();
                //ugg - cursorRowToContentValues does not work for blobs...
                //DatabaseUtils.cursorRowToContentValues(c, contentValues);

                //But we overwrite everything below so we don't really need to load anything except the flags
                //grab the flags out of the database (preserve flags)
                flags = c.getLong(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_FLAGS));
            }

            //Do a bit of error checking...
            if (app.ver == null) {
                app.ver = "?";      //just in case
            }
            
            //load up contentValues with latest info...
            //Force the OS type by definition...
            //note that the app is local
            //flags = flags | AppContract.AppEntry.FLAG_APP_LOCAL; - nope - SSN matching = local
            app.type = AppUtils.getAppType(mCtx, app.pkg);
            app.flags = flags;
            app.serial = Build.SERIAL;
            //shove a timestamp in there.
            app.timestamp = System.currentTimeMillis();

            //bind app to the contentValues
            DBUtils.bindAppToContentValues(app, contentValues, mCtx);

            //only write app if it has a launch intent...
            if (app.type != AppContract.TYPE_NONE) {
                if (c.getCount() > 0) {
                    //replace
                    //getApplicationContext().getContentResolver().update(appSearchUri, contentValues, null, null);
                    ops.add(ContentProviderOperation.newUpdate(appSearchUri)
                            .withValues(contentValues)
                            .withYieldAllowed(true)
                            .build());
                } else {
                    //add
                    //getApplicationContext().getContentResolver().insert(insertUri, contentValues);
                    ops.add(ContentProviderOperation.newInsert(insertUri)
                            .withValues(contentValues)
                            .withYieldAllowed(true)
                            .build());
                }
            }

            c.close();
        }

        //Step 4 - apply the content operation batch
        try {
            //Log.d(TAG, "Starting batch CP application");
            mCtx.getContentResolver().applyBatch(AppContract.CONTENT_AUTHORITY, ops);
            //Log.d(TAG, "Complete batch CP application");
            Log.d(TAG, "Pushing records to firebase for serial " + Build.SERIAL);
            //TAGCPSAFE - NO - SHOULD NOT NEED THIS ROUTINE - individual apps will get written to DB entries
            //MainFragment.mFirebase.pushRecordsToFirebase(Build.SERIAL);
        } catch (RemoteException e) {
            Log.e(TAG, "Batch - remoteException err");
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Batch - OperationApplicationException err");
        }

        /**
         *  Step last - delete apps for this device which already existed in database but are no longer on the device.
         *  Technically, if our broadcast receiver was 100% reliable, should not need to do this. However, during debugging can
         *  get out of sync. And not sure if sideloading/etc will trigger broadcast receiver. So sync up here.
         *  Initially we did this a heavy handed way by deleting all the apps for this device at the start of this routine.
         *  But this caused a nasty hitch when starting app up (apps would disappear and be repopulated).
         *  So do a more advanced/elegant approach by deleting any app entry for this device which has a timestamp of
         *  older than the time when we entered this routine.
         */

        //manually create the selection/search string - delete apps with matching devices but a timestamp less than entry time
        String selection = AppContract.AppEntry.COLUMN_APP_DEVSSN + " = ? AND " + AppContract.AppEntry.COLUMN_APP_TIMEUPDATED + " < ?";
        String[] selectionArgs = new String[]{Build.SERIAL, Long.toString(currentTime)};

        //Delete only those elements with the old timestamp
        mCtx.getContentResolver().delete(AppContract.AppEntry.CONTENT_URI, selection, selectionArgs);

        //And finally, send a message back to indicate that we are all done with the local work
        Intent localIntent = new Intent(BROADCAST_ACTION).putExtra(EXTENDED_DATA_STATUS, EXTENDED_DATA_STATUS_LOCALUPDATECOMPLETE);
        //And broadcast the message
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

    }


    /**
     * Update the app package which is passed in (get the app info and update CP). Triggered by new installs of apps (amongst others)
     * from broadcast receiver.
     * @param packageName
     */
    private void handleActionLocalAppUpdate(String packageName) {
        Log.d(TAG, "Starting local app update for " + packageName);
        //Construct the Uri...
        Uri appDB = AppContract.AppEntry.CONTENT_URI;
        //build up the local device query
        appDB = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(packageName).build();

        //Create an object
        ObjectDetail app = AppUtils.getLocalAppDetails(mCtx, packageName);

        if (app != null) {
            //Okay - we have an app object... Put it into CP
            DBUtils.saveAppToCP(mCtx, appDB, app, true);
            //Note that firebase update handled in the saveAppToCP routine
        }
    }


    /**
     * Handle APK install in background thread. To be specific, check if the APK exists on the market store first
     * and then, if it does, launch market install intent else issue a broadcast ACTION_SKIP intent.
     *
     * @param pkg
     * @param installIntent
     */

    private void handleActionInstallAPK(String pkg, String intentPrefix) {

        //Step 1 - check if this app exists on play store...
        //InstallUtil.CHECK_URL
        //ping the play store address to see if it is valid
        boolean bExists = true;
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String line = null;
        try {
            String urlbuild = InstallUtil.CHECK_URL + pkg;
            //make the URL
            URL url = new URL(urlbuild);

            // Create the request to TMDB, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            //FIXME - this could be area of failure... - read a line or 2 and see what comes back... failures all will be same
            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            if (inputStream == null) {
                // Nothing to do. Seems this is also a sign of an app not on store...
                bExists = false;
                Log.d(TAG, "Check play store, got null input");
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            line = reader.readLine();

        } catch (IOException e) {
            Log.d(TAG, "URL Error - couldn't find " + pkg);
            // mark this as not existing on play store...
            bExists = false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        if (bExists) {
            //Step 2 - go ahead with install
            Log.d(TAG, "Launching install intent for " + pkg);
            Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(intentPrefix + pkg));
            //make sure activity not on history stack...
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mCtx.startActivity(goToMarket);
        } else {
            //send skip intent
            Intent intent = new Intent();
            intent.setAction(DeviceSyncReceiver.ACTION_SKIP);
            sendBroadcast(intent);
        }
    }
}
