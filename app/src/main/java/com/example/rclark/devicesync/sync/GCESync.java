package com.example.rclark.devicesync.sync;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.text.format.Time;

import com.example.rclark.devicesync.data.AppContract;

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
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPDATE_LOCAL_DB = "com.example.rclark.devicesync.sync.action.Update";
    private static final String ACTION_PUSH_REMOTE_DB = "com.example.rclark.devicesync.sync.action.Push";
    private static final String ACTION_FETCH_REMOTE_DB = "com.example.rclark.devicesync.sync.action.Fetch";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.rclark.devicesync.sync.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.rclark.devicesync.sync.extra.PARAM2";

    private static Context mCtx;

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
            } else if (ACTION_FETCH_REMOTE_DB.equals(action)) {
                handleActionFetch();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionUpdate() {
        // TODO: Handle action Foo

        //Okay - first update the device database - serial number, nickname, date, model name, os_ver
        String serial = Build.SERIAL;
        String nickname = BluetoothAdapter.getDefaultAdapter().getName();
        String model = Build.MODEL;
        String osver = Build.FINGERPRINT;

        //Get the device DB reference...
        Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;

        //create the buffer
        ContentValues contentValues = new ContentValues();

        //Now, search for the device (is it in DB yet?)
        Uri deviceSearchUri = deviceDB.buildUpon().appendPath(serial).build();

        Cursor c = getApplicationContext().getContentResolver().query(deviceSearchUri, null, null, null, null);

        if (c.getCount() > 0) {
            //device exists...
            //preload the content values...
            DatabaseUtils.cursorRowToContentValues(c, contentValues);
        }

        //load up contentValues with latest info...
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICES_SSN, serial);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_NAME, serial);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_MODEL, serial);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_OSVER, serial);

        Time time = new Time();
        time.setToNow();
        contentValues.put(AppContract.DevicesEntry.COLUMN_DATE, time.toMillis(true));

        if (c.getCount() > 0) {
            //replace
            getApplicationContext().getContentResolver().update(deviceSearchUri, contentValues, null, null);
        } else {
            //add
            getApplicationContext().getContentResolver().insert(AppContract.DevicesEntry.CONTENT_URI, contentValues);
        }

        c.close();

        /**
         * Okay - device database should be updated. Now time to do same for apps...
         */

        //Step 1 - get a copy of the apps...

        //Step 2 - write them to db...

        //throw new UnsupportedOperationException("Not yet implemented");
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
