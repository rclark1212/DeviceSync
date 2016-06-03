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
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
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

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPDATE_LOCAL_DB = "com.prod.rclark.devicesync.sync.action.Update";
    private static final String ACTION_PUSH_REMOTE_DB = "com.prod.rclark.devicesync.sync.action.Push";
    private static final String ACTION_FETCH_REMOTE_DB = "com.prod.rclark.devicesync.sync.action.Fetch";
    private static final String ACTION_UPDATE_LOCAL_DEVICE = "com.prod.rclark.devicesync.sync.action.DeviceUpdate";
    private static final String ACTION_UPDATE_LOCAL_APP = "com.prod.rclark.devicesync.sync.action.AppUpdate";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.prod.rclark.devicesync.sync.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.prod.rclark.devicesync.sync.extra.PARAM2";

    // Message defines for communicating back to calling activity
    // Used by settings panel as well...
    public static final String BROADCAST_ACTION = "com.prod.rclark.devicesync.BROADCAST";

    public static final String EXTENDED_DATA_STATUS = "com.prod.rclark.devicesync.gcesync.STATUS";
    public static final String EXTENDED_DATA_MSG = "com.prod.rclark.devicesync.gcesync.MSG";

    public static final int EXTENDED_DATA_STATUS_NULL = 0;
    public static final int EXTENDED_DATA_STATUS_LOCALUPDATECOMPLETE = 1;
    public static final int EXTENDED_DATA_STATUS_PUSHCOMPLETE = 2;
    public static final int MAINACTIVITY_SHOW_NETWORKBUSY = 8;
    public static final int MAINACTIVITY_SHOW_NETWORKFREE = 9;
    public static final int EXTENDED_DATA_STATUS_PHOTO_COMPLETE = 10;


    private static Context mCtx;
    private static boolean mbIsATV;

    public GCESync() {
        super("GCESync");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionUpdateLocal(Context context, String param1, String param2) {
        Intent intent = new Intent(context, GCESync.class);
        intent.setAction(ACTION_UPDATE_LOCAL_DB);
        //intent.putExtra(EXTRA_PARAM1, param1);
        //intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionLocalDeviceUpdate(Context context, String param1, String param2) {
        Intent intent = new Intent(context, GCESync.class);
        intent.setAction(ACTION_UPDATE_LOCAL_DEVICE);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     * Note that the second param here is very special. Indicates a delay to insert on processing
     * the action (in ms). Used for broadcast intent actions where app package manager not quite
     * ready for us to capture data at time we are called.
     * Note that this routine here runs in context of broadcast receiver - so we really want to delay
     * once we are inside the intentservice...
     *
     * @see IntentService
     */
    public static void startActionLocalAppUpdate(Context context, String pkgname, String delayExecutionInMS) {
        Intent intent = new Intent(context, GCESync.class);
        intent.setAction(ACTION_UPDATE_LOCAL_APP);
        intent.putExtra(EXTRA_PARAM1, pkgname);
        intent.putExtra(EXTRA_PARAM2, delayExecutionInMS);
        context.startService(intent);
    }


    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionPushRemote(Context context, String param1, String param2) {
        Intent intent = new Intent(context, GCESync.class);
        intent.setAction(ACTION_PUSH_REMOTE_DB);
        //intent.putExtra(EXTRA_PARAM1, param1);
        //intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public static void startActionFetchRemote(Context context, String param1, String param2) {
        Intent intent = new Intent(context, GCESync.class);
        intent.setAction(ACTION_FETCH_REMOTE_DB);
        //intent.putExtra(EXTRA_PARAM1, param1);
        //intent.putExtra(EXTRA_PARAM2, param2);
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
            } else if (ACTION_PUSH_REMOTE_DB.equals(action)) {
                //final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                //final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionPush();
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
            } else if (ACTION_FETCH_REMOTE_DB.equals(action)) {
                handleActionFetch();
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
        ObjectDetail device = SyncUtils.getLocalDeviceInfo(mCtx);

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
        ArrayList<ObjectDetail> apps = SyncUtils.loadApps(mCtx);

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
            app.type = mbIsATV ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;
            app.flags = flags;
            app.serial = Build.SERIAL;
            //shove a timestamp in there.
            app.timestamp = System.currentTimeMillis();

            //bind app to the contentValues
            DBUtils.bindAppToContentValues(app, contentValues, mCtx);

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

            c.close();
        }

        //Step 4 - apply the content operation batch
        try {
            //Log.d(TAG, "Starting batch CP application");
            mCtx.getContentResolver().applyBatch(AppContract.CONTENT_AUTHORITY, ops);
            //Log.d(TAG, "Complete batch CP application");
            //FIXME - verify done? - apply app changes to firebase. Note, have to loop through apps list
            Log.d(TAG, "Pushing records to firebase for serial " + Build.SERIAL);
            MainFragment.mFirebase.pushRecordsToFirebase(Build.SERIAL);
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
            DBUtils.saveAppToCP(mCtx, appDB, app);
            //Note that firebase update handled in the saveAppToCP routine
        }
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionPush() {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFetch() {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
