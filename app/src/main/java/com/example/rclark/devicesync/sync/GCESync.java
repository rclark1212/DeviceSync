package com.example.rclark.devicesync.sync;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;

import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.AppList;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.Utils;
import com.example.rclark.devicesync.data.AppContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
public class GCESync extends IntentService  implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_UPDATE_LOCAL_DB = "com.example.rclark.devicesync.sync.action.Update";
    private static final String ACTION_PUSH_REMOTE_DB = "com.example.rclark.devicesync.sync.action.Push";
    private static final String ACTION_FETCH_REMOTE_DB = "com.example.rclark.devicesync.sync.action.Fetch";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.rclark.devicesync.sync.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.rclark.devicesync.sync.extra.PARAM2";

    private static Context mCtx;
    private static boolean mbUseLocation;
    private static GoogleApiClient mGoogleApiClient;

    public GCESync() {
        super("GCESync");
    }

    /*
        Callbacks for google services
     */
    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {};

    @Override
    public void onConnectionSuspended(int i) {
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
        //first, do we have context saved off?
        if (mCtx == null) {
            mCtx = getApplicationContext();
        }

        //next attach to GMS if not yet attached...
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            ConnectionResult connectionResult =   mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            //Just check if we are connected
            mbUseLocation = mGoogleApiClient.isConnected();
        }

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
        //Get an object with local device info...
        ObjectDetail device = getLocalDeviceInfo();

        //Get the device DB reference...
        Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;

        //create the buffer
        ContentValues contentValues = new ContentValues();

        //Now, search for the device (is it in DB yet?) - search by serial
        Uri deviceSearchUri = deviceDB.buildUpon().appendPath(device.serial).build();

        Cursor c = getApplicationContext().getContentResolver().query(deviceSearchUri, null, null, null, null);

        if (c.getCount() > 0) {
            //device exists...
            //preload the content values...
            c.moveToFirst();
            DatabaseUtils.cursorRowToContentValues(c, contentValues);
        }

        //load up contentValues with latest info...
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICES_SSN, device.serial);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_NAME, device.label);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_MODEL, device.name);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_OSVER, device.ver);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DATE, device.installDate);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_TYPE, AppContract.TYPE_ATV);
        contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_LOCATION, device.location);

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
        ArrayList<ObjectDetail> apps = AppList.loadApps(getApplicationContext());

        //Step 2 - write them to db...
        //Get the device DB reference...
        Uri appDB = AppContract.AppEntry.CONTENT_URI;

        for (int i = 0; i < apps.size(); i++) {
            //Now, search for the device (is it in DB yet?)
            ObjectDetail app = (ObjectDetail) apps.get(i);
            long flags = 0;

            //Remember, device.pkg contains the serial number... (unique)
            Uri appSearchUri = appDB.buildUpon().appendPath(device.serial).appendPath(app.label).build();

            //Build an insert URI to trigger notifications...
            Uri insertUri = appDB.buildUpon().appendPath(device.serial).build();

            //clear contentValues...
            contentValues.clear();

            c = getApplicationContext().getContentResolver().query(appSearchUri, null, null, null, null);

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
                app.ver = "?";
            }
            
            //load up contentValues with latest info...
            contentValues.put(AppContract.AppEntry.COLUMN_APP_LABEL, app.label);
            contentValues.put(AppContract.AppEntry.COLUMN_APP_PKG, app.pkg);
            contentValues.put(AppContract.AppEntry.COLUMN_APP_VER, app.ver);
            contentValues.put(AppContract.AppEntry.COLUMN_DEV_SSN, device.serial);
            contentValues.put(AppContract.AppEntry.COLUMN_DATE, app.installDate);
            contentValues.put(AppContract.AppEntry.COLUMN_APP_TYPE, AppContract.TYPE_ATV);

            //note that the app is local
            flags = flags | AppContract.AppEntry.FLAG_APP_LOCAL;
            contentValues.put(AppContract.AppEntry.COLUMN_APP_FLAGS, flags);

            //now blob... - COLUMN_APP_BANNER
            //convert drawable to bytestream
            Bitmap bitmap = drawableToBitmap(app.banner);           //convert drawable to bitmap
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] imageInByte = stream.toByteArray();
            //And now into contentValues
            contentValues.put(AppContract.AppEntry.COLUMN_APP_BANNER, imageInByte);

            if (c.getCount() > 0) {
                //replace
                getApplicationContext().getContentResolver().update(appSearchUri, contentValues, null, null);
            } else {
                //add
                getApplicationContext().getContentResolver().insert(insertUri, contentValues);
            }

            c.close();
        }

        //throw new UnsupportedOperationException("Not yet implemented");
    }

    /*
        Populates an object with local device info
     */
    private ObjectDetail getLocalDeviceInfo() {
        ObjectDetail device = new ObjectDetail();

        //Set up local device into object
        device.bIsDevice = true;
        device.serial = Build.SERIAL;
        device.label = BluetoothAdapter.getDefaultAdapter().getName();
        device.name = Build.MODEL;
        device.ver = Build.FINGERPRINT + " (" + Build.VERSION.RELEASE + ")";

        if (Utils.bIsThisATV(mCtx)) {
            device.type = AppContract.TYPE_ATV;
        } else {
            device.type = AppContract.TYPE_TABLET;
        }

        Time time = new Time();
        time.setToNow();
        device.installDate = time.toMillis(true);
        if (mbUseLocation) {
            device.location = Utils.getLocation(mCtx, mGoogleApiClient);
        } else {
            device.location = mCtx.getResources().getString(R.string.unknown);
        }

        return device;
    }

    /*
        Grabbed this nice little routine from
        http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap from Andre.
     */
    private static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
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
