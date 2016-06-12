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

package com.prod.rclark.devicesync.data;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.prod.rclark.devicesync.cloud.FirebaseMessengerService;

import java.util.ArrayList;

/**
 * Created by rclark on 3/27/2016.
 */
public class AppProvider extends ContentProvider {

    private static final String TAG = "DS_ContentProvider";

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private AppDbHelper mOpenHelper;
    private Context mCtx;

    static final int APPS = 100;
    static final int APPS_WITH_DEVICE = 101;
    static final int APPS_WITH_DEVICE_AND_APP = 102;
    static final int APPS_WITH_GROUPBY = 200;
    static final int APPS_WITH_GROUPBY_AND_HAVING = 201;
    static final int DEVICES = 300;
    static final int DEVICES_WITH_DEVICE = 301;
    static final int IMAGES = 400;
    static final int IMAGES_WITH_APK = 401;

    //  Service
    private boolean mBoundToService;
    private Messenger mService = null;
    //  Class for interacting with our firebase service...
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.d(TAG, "Bound to service in CP");
            mService = new Messenger(service);
            mBoundToService = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.d(TAG, "Unbind service in CP");
            mService = null;
            mBoundToService = false;
        }
    };

    //devices ssn_setting = ?
    private static final String sDevicesSelection =
            AppContract.DevicesEntry.TABLE_NAME+
                    "." + AppContract.DevicesEntry.COLUMN_DEVICES_SSN + " = ? ";

    //app db - label_setting = ?
    private static final String sAppSelection =
            AppContract.AppEntry.TABLE_NAME+
                    "." + AppContract.AppEntry.COLUMN_APP_PKG + " = ? ";

    //app db - devices sn = ?
    private static final String sAppWithDevicesSelection =
            AppContract.AppEntry.TABLE_NAME+
                    "." + AppContract.AppEntry.COLUMN_APP_DEVSSN + " = ? ";

    //app db - devices snn_setting = ? AND app label = ?
    private static final String sAppsWithDevicesAndAppsSelection =
            AppContract.AppEntry.COLUMN_APP_PKG + " = ? AND " + AppContract.AppEntry.COLUMN_APP_DEVSSN + " = ? ";

    //image db - apkname = ?
    private static final String sImageSelection =
            AppContract.ImageEntry.TABLE_NAME+
                    "." + AppContract.ImageEntry.COLUMN_IMG_APKNAME + " = ? ";

    //get device by device
    private Cursor getDeviceByDevice(Uri uri, String[] projection, String sortOrder) {
        String device = AppContract.DevicesEntry.getDeviceFromUri(uri);

        String[] selectionArgs;
        String selection;

        selectionArgs = new String[]{device};
        selection = sDevicesSelection;

        return mOpenHelper.getReadableDatabase().query(
                AppContract.DevicesEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    //get apps by device
    private Cursor getAppsByDevice(Uri uri, String[] projection, String sortOrder) {
        String device = AppContract.AppEntry.getDeviceFromUri(uri);

        String[] selectionArgs;
        String selection;

        selectionArgs = new String[]{device};
        selection = sAppWithDevicesSelection;

        return mOpenHelper.getReadableDatabase().query(
                AppContract.AppEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    //get apps by device and app label
    private Cursor getAppsByDeviceAndLabel(Uri uri, String[] projection, String sortOrder) {
        String device = AppContract.AppEntry.getDeviceFromUri(uri);
        String label = AppContract.AppEntry.getAppFromUri(uri);

        String[] selectionArgs;
        String selection;

        selectionArgs = new String[]{label, device};
        selection = sAppsWithDevicesAndAppsSelection;

        return mOpenHelper.getReadableDatabase().query(
                AppContract.AppEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    //get apps by device
    private Cursor getImageByAPK(Uri uri, String[] projection, String sortOrder) {
        String apk = AppContract.ImageEntry.getApkFromUri(uri);

        String[] selectionArgs;
        String selection;

        selectionArgs = new String[]{apk};
        selection = sImageSelection;

        return mOpenHelper.getReadableDatabase().query(
                AppContract.ImageEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    static UriMatcher buildUriMatcher() {

        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.

        /**
         * For apps db:
         * / - all apps
         * /devicesn - apps for this serial number
         * /devicesn/app_pkg - specific app for specific device
         *
         * For device db:
         * / - all devices
         * /devicesn - that specific device
         *
         */
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AppContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, AppContract.PATH_APPS, APPS);
        matcher.addURI(authority, AppContract.PATH_APPS + "/*", APPS_WITH_DEVICE);
        matcher.addURI(authority, AppContract.PATH_APPS + "/*/*", APPS_WITH_DEVICE_AND_APP);

        matcher.addURI(authority, AppContract.PATH_DEVICES, DEVICES);
        matcher.addURI(authority, AppContract.PATH_DEVICES + "/*", DEVICES_WITH_DEVICE);

        //These are effectively fake (aliased) Uris. Android does not support groupby/having in
        //its CP. But we need to use syncadapters with CP. Get around this with a fake Uri
        //that lets us pass groupby/having info to the query behind the CP adapter.
        //db = apps, * = group by this column (pass this param into groupby)
        matcher.addURI(authority, AppContract.PATH_GROUPBY + "/*", APPS_WITH_GROUPBY);
        //db = apps, /* (get(1)) = group by this column (pass this param into groupby)
        // /*/* (get(2)) = pass this into having param
        matcher.addURI(authority, AppContract.PATH_GROUPBY + "/*/*", APPS_WITH_GROUPBY_AND_HAVING);

        matcher.addURI(authority, AppContract.PATH_IMAGES, IMAGES);
        matcher.addURI(authority, AppContract.PATH_IMAGES + "/*", IMAGES_WITH_APK);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new AppDbHelper(getContext());
        mCtx = getContext();

        //okay - we are using firebase service to mirror our CP data up to cloud - bind here
        // Bind to the service
        // NOTE - content providers never destroyed so no real opportunity/place to unbind. But that should be okay.
        if (!mBoundToService) {
            mCtx.bindService(new Intent(mCtx, FirebaseMessengerService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }

        return true;
    }

    /*
        Students: Here's where you'll code the getType function that uses the UriMatcher.  You can
        test this by uncommenting testGetType in TestProvider.

     */
    @Override
    public String getType(Uri uri) {

        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case APPS_WITH_DEVICE_AND_APP:
                return AppContract.AppEntry.CONTENT_ITEM_TYPE;
            case APPS_WITH_DEVICE:
                return AppContract.AppEntry.CONTENT_TYPE;
            case APPS:
                return AppContract.AppEntry.CONTENT_TYPE;
            case DEVICES_WITH_DEVICE:
                return AppContract.DevicesEntry.CONTENT_ITEM_TYPE;
            case DEVICES:
                return AppContract.DevicesEntry.CONTENT_TYPE;
            case IMAGES:
                return AppContract.ImageEntry.CONTENT_TYPE;
            case IMAGES_WITH_APK:
                return AppContract.ImageEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            // "apps/*/*"
            case APPS_WITH_DEVICE_AND_APP:
            {
                retCursor = getAppsByDeviceAndLabel(uri, projection, sortOrder);
                break;
            }
            // "apps/*"
            case APPS_WITH_DEVICE: {
                retCursor = getAppsByDevice(uri, projection, sortOrder);
                break;
            }
            // apps
            case APPS: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AppContract.AppEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            //fugly hack to get around android limitation of no groupBy for CP. Alias in a special Uri.
            case APPS_WITH_GROUPBY: {
                String groupBy = uri.getPathSegments().get(1);          //grab the encoded groupBy...
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AppContract.AppEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        groupBy,
                        null,
                        sortOrder
                );
                break;
            }
            //fugly hack2 to get around android limitation of no groupBy/having for CP. Alias in a special Uri.
            case APPS_WITH_GROUPBY_AND_HAVING: {
                String groupBy = uri.getPathSegments().get(1);          //grab the encoded groupBy...
                String having = uri.getPathSegments().get(2);
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AppContract.AppEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        groupBy,
                        having,
                        sortOrder
                );
                break;
            }
            // devices/*
            case DEVICES_WITH_DEVICE: {
                retCursor = getDeviceByDevice(uri, projection, sortOrder);
                break;
            }
            // devices
            case DEVICES: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AppContract.DevicesEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // images/*
            case IMAGES_WITH_APK: {
                retCursor = getImageByAPK(uri, projection, sortOrder);
                break;
            }
            // images
            case IMAGES: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        AppContract.ImageEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        //Log.d(TAG, "query - uri:" + uri.toString());
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case APPS_WITH_DEVICE:
            case APPS: {
                normalizeDate(values);
                updateTimeStamp(values, true);
                long _id = db.insert(AppContract.AppEntry.TABLE_NAME, null, values);
                if ( _id > 0 ) {
                    returnUri = AppContract.AppEntry.buildAppUri(_id);
                    updateFirebaseAppFromCV(values);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case DEVICES: {
                updateTimeStamp(values, false);
                long _id = db.insert(AppContract.DevicesEntry.TABLE_NAME, null, values);
                if ( _id > 0 ) {
                    returnUri = AppContract.DevicesEntry.buildDeviceUri(_id);
                    updateFirebaseDeviceFromCV(values);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            }
            case IMAGES_WITH_APK:
            case IMAGES: {
                updateTimeStamp(values, false);
                long _id = db.insert(AppContract.ImageEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = AppContract.ImageEntry.buildImgUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        //Log.d(TAG, "insert - uri:" + uri.toString());
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        //Deleting with firebase is tricky... Have to first capture the apps/devices that
        //are being deleted with the uri/selection being passed in
        //Then delete from database
        //Then replay the list to firebase service to delete the nodes
        ArrayList<DeleteList> deleteList;

        if ( null == selection ) selection = "1";
        switch (match) {
            case APPS: {
                deleteList = getAppDeleteList(uri, selection, selectionArgs);
                rowsDeleted = db.delete(
                        AppContract.AppEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted > 0) {
                    deleteAppDeleteList(deleteList);
                }
                break;
            }
            case APPS_WITH_DEVICE: {
                deleteList = getAppDeleteList(uri, selection, selectionArgs);
                String device = AppContract.AppEntry.getDeviceFromUri(uri);
                String[] parse_selectionArgs = new String[]{device};
                String parse_selection = sAppWithDevicesSelection;
                rowsDeleted = db.delete(
                        AppContract.AppEntry.TABLE_NAME, parse_selection, parse_selectionArgs);
                if (rowsDeleted > 0) {
                    deleteAppDeleteList(deleteList);
                }
                break;
            }
            case APPS_WITH_DEVICE_AND_APP: {
                deleteList = getAppDeleteList(uri, selection, selectionArgs);
                String device = AppContract.AppEntry.getDeviceFromUri(uri);
                String label = AppContract.AppEntry.getAppFromUri(uri);
                String[] parse_selectionArgs = new String[]{label, device};
                String parse_selection = sAppsWithDevicesAndAppsSelection;
                rowsDeleted = db.delete(
                        AppContract.AppEntry.TABLE_NAME, parse_selection, parse_selectionArgs);
                if (rowsDeleted > 0) {
                    deleteAppDeleteList(deleteList);
                }
                break;
            }
            case DEVICES_WITH_DEVICE: {
                String device = AppContract.DevicesEntry.getDeviceFromUri(uri);
                String[] parse_selectionArgs = new String[]{device};
                String parse_selection = sDevicesSelection;
                deleteList = getDeviceDeleteList(uri, parse_selection, parse_selectionArgs);
                rowsDeleted = db.delete(
                        AppContract.DevicesEntry.TABLE_NAME, parse_selection, parse_selectionArgs);
                if (rowsDeleted > 0) {
                    deleteDeviceDeleteList(deleteList);
                }
                break;
            }
            case DEVICES:
                deleteList = getDeviceDeleteList(uri, selection, selectionArgs);
                rowsDeleted = db.delete(
                        AppContract.DevicesEntry.TABLE_NAME, selection, selectionArgs);
                if (rowsDeleted > 0) {
                    deleteDeviceDeleteList(deleteList);
                }
                break;
            case IMAGES: {
                rowsDeleted = db.delete(
                        AppContract.ImageEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case IMAGES_WITH_APK: {
                String apk = AppContract.ImageEntry.getApkFromUri(uri);
                String[] parse_selectionArgs = new String[]{apk};
                String parse_selection = sImageSelection;
                rowsDeleted = db.delete(
                        AppContract.ImageEntry.TABLE_NAME, parse_selection, parse_selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            //Log.d(TAG, "delete - uri:" + uri.toString());
        }
        return rowsDeleted;
    }

    /*
        Normalizes date values to GMT
     */
    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(AppContract.AppEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(AppContract.AppEntry.COLUMN_DATE);
            values.put(AppContract.AppEntry.COLUMN_DATE, AppContract.normalizeDate(dateValue));
        }
    }

    /**
     *  Updates timestamp field in the content values. We always timestamp to current system time in millis
     */
    private void updateTimeStamp(ContentValues values, boolean bApp) {
        //Nope - do this in the structure itself so the app has access without having to do a query...
        /*
        long currentTime = System.currentTimeMillis();
        if (bApp) {
            values.put(AppContract.AppEntry.COLUMN_APP_TIMEUPDATED, currentTime);
        } else {
            values.put(AppContract.DevicesEntry.COLUMN_DEVICE_TIMEUPDATED, currentTime);
        } */
    }


    @Override
    public int update(
            Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsUpdated;

        switch (match) {
            case APPS:
                normalizeDate(values);
                updateTimeStamp(values, true);
                rowsUpdated = db.update(AppContract.AppEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                if (rowsUpdated > 0) {
                    updateFirebaseAppFromCV(values);
                }
                break;
            case APPS_WITH_DEVICE_AND_APP: {
                normalizeDate(values);
                updateTimeStamp(values, true);
                String device = AppContract.AppEntry.getDeviceFromUri(uri);
                String label = AppContract.AppEntry.getAppFromUri(uri);
                String[] parse_selectionArgs = new String[]{label, device};
                String parse_selection = sAppsWithDevicesAndAppsSelection;

                rowsUpdated = db.update(AppContract.AppEntry.TABLE_NAME, values, parse_selection,
                        parse_selectionArgs);
                if (rowsUpdated > 0) {
                    updateFirebaseAppFromCV(values);
                }
                break;
            }
            case DEVICES:
                updateTimeStamp(values, false);
                rowsUpdated = db.update(AppContract.DevicesEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                if (rowsUpdated > 0) {
                    updateFirebaseDeviceFromCV(values);
                }
                break;
            case DEVICES_WITH_DEVICE: {
                updateTimeStamp(values, false);
                String device = AppContract.DevicesEntry.getDeviceFromUri(uri);
                String[] parse_selectionArgs = new String[]{device};
                String parse_selection = sDevicesSelection;

                rowsUpdated = db.update(AppContract.DevicesEntry.TABLE_NAME, values, parse_selection,
                        parse_selectionArgs);
                if (rowsUpdated > 0) {
                    updateFirebaseDeviceFromCV(values);
                }
                break;
            }
            case IMAGES:
                updateTimeStamp(values, false);
                rowsUpdated = db.update(AppContract.ImageEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case IMAGES_WITH_APK: {
                updateTimeStamp(values, false);
                String apk = AppContract.ImageEntry.getApkFromUri(uri);
                String[] parse_selectionArgs = new String[]{apk};
                String parse_selection = sImageSelection;

                rowsUpdated = db.update(AppContract.ImageEntry.TABLE_NAME, values, parse_selection,
                        parse_selectionArgs);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            //Log.d(TAG, "update - uri:" + uri.toString());
        }
        return rowsUpdated;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case APPS:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues value : values) {
                        normalizeDate(value);
                        updateTimeStamp(value, true);
                        long _id = db.insert(AppContract.AppEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri, null);
                if (returnCount > 0) {
                    bulkUpdateFirebaseApp(values);
                }
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    /*************************************************************************************
     * Firebase routines follow
     * ***********************************************************************************
     */

    //add a bulk update routine for firebase so as to not clutter code above
    private void bulkUpdateFirebaseApp(ContentValues[] values) {
        for (ContentValues value : values) {
            updateFirebaseAppFromCV(value);
        }
    }

    //add a routine that pulls serial/apk out of content value
    private void updateFirebaseAppFromCV(ContentValues value) {
        String serial = value.getAsString(AppContract.AppEntry.COLUMN_APP_DEVSSN);
        String apk = value.getAsString(AppContract.AppEntry.COLUMN_APP_PKG);
        updateFirebaseApp(serial, apk);
    }

    //add a routine that pulls serial/apk out of content value
    private void updateFirebaseDeviceFromCV(ContentValues value) {
        String serial = value.getAsString(AppContract.DevicesEntry.COLUMN_DEVICES_SSN);
        updateFirebaseDevice(serial);
    }

    //Do a query of what is passed into delete routine to get a list of apps to delete
    private ArrayList<DeleteList> getAppDeleteList(Uri uri, String selection, String[] selectionArgs) {
        ArrayList<DeleteList> returnList = new ArrayList<DeleteList>();
        Cursor c = query(uri, null, selection, selectionArgs, null);
        for (int i = 0; i < c.getCount(); i++) {
            DeleteList item = new DeleteList();
            c.moveToPosition(i);
            item.serial = c.getString(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN));
            item.apk = c.getString(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG));
        }
        c.close();
        return returnList;
    }


    //Do a query of what is passed into delete routine to get a list of devices to delete
    private ArrayList<DeleteList> getDeviceDeleteList(Uri uri, String selection, String[] selectionArgs) {
        ArrayList<DeleteList> returnList = new ArrayList<DeleteList>();
        Cursor c = query(uri, null, selection, selectionArgs, null);
        for (int i = 0; i < c.getCount(); i++) {
            DeleteList item = new DeleteList();
            c.moveToPosition(i);
            item.serial = c.getString(c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICES_SSN));
        }
        c.close();
        return returnList;
    }


    //Processes delete list set up by query for apps
    private void deleteAppDeleteList(ArrayList<DeleteList> deleteList) {
        for (int i = 0; i < deleteList.size(); i++) {
            deleteFirebaseApp(deleteList.get(i).serial, deleteList.get(i).apk);
        }
    }

    //Processes delete list set up by query for devices
    private void deleteDeviceDeleteList(ArrayList<DeleteList> deleteList) {
        for (int i = 0; i < deleteList.size(); i++) {
            deleteFirebaseDevice(deleteList.get(i).serial);
        }
    }

    /**
     * Routine to trigger firebase service to update app from CP
     */
    private void updateFirebaseApp(String serial, String apk) {
        Log.d(TAG, "CP: sendMessageToService - write app " + serial + " " + apk);
        Bundle params = new Bundle();
        params.putString(FirebaseMessengerService.SERIAL_PARAM, serial);
        params.putString(FirebaseMessengerService.APK_PARAM, apk);
        sendMessageToService(FirebaseMessengerService.MSG_WRITE_APP_TO_FIREBASE, params);
    }

    /**
     * Routine to trigger firebase service to delete app from CP
     */
    private void deleteFirebaseApp(String serial, String apk) {
        Log.d(TAG, "CP: sendMessageToService - delete app " + serial + " " + apk);
        Bundle params = new Bundle();
        params.putString(FirebaseMessengerService.SERIAL_PARAM, serial);
        params.putString(FirebaseMessengerService.APK_PARAM, apk);
        sendMessageToService(FirebaseMessengerService.MSG_DELETE_APP_FROM_FIREBASE, params);
    }

    /**
     * Routine to trigger firebase service to update device from CP
     */
    private void updateFirebaseDevice(String serial) {
        Log.d(TAG, "CP: sendMessageToService - write device " + serial);
        Bundle params = new Bundle();
        params.putString(FirebaseMessengerService.SERIAL_PARAM, serial);
        sendMessageToService(FirebaseMessengerService.MSG_WRITE_DEVICE_TO_FIREBASE, params);
    }

    /**
     * Routine to trigger firebase service to delete device from CP
     */
    private void deleteFirebaseDevice(String serial) {
        Log.d(TAG, "CP: sendMessageToService - delete device " + serial);
        Bundle params = new Bundle();
        params.putString(FirebaseMessengerService.SERIAL_PARAM, serial);
        sendMessageToService(FirebaseMessengerService.MSG_DELETE_DEVICE_FROM_FIREBASE, params);
    }

    /**
     * Sends a message to our firebase service
     */
    private void sendMessageToService(int messageId, Bundle bundle) {
        if (!mBoundToService) {
            Log.d(TAG, "CP - Service not bound but someone tried to send message");
            return;
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
    }

    /**
     * Used in routine to create a delete list
     */
    private class DeleteList {
        public String serial;
        public String apk;

        public DeleteList() {}
    }

}
