package com.example.rclark.devicesync.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

/**
 * Created by rclar on 3/27/2016.
 */
public class AppProvider extends ContentProvider {

    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private AppDbHelper mOpenHelper;

    static final int APPS = 100;
    static final int APPS_WITH_DEVICE = 101;
    static final int APPS_WITH_DEVICE_AND_APP = 102;
    static final int DEVICES = 300;
    static final int DEVICES_WITH_DEVICE = 301;

    private static final SQLiteQueryBuilder sAppQueryBuilder = new SQLiteQueryBuilder();

    //devices ssn_setting = ?
    private static final String sDevicesSelection =
            AppContract.DevicesEntry.TABLE_NAME+
                    "." + AppContract.DevicesEntry.COLUMN_DEVICES_SSN + " = ? ";

    //app db - label_setting = ?
    private static final String sAppSelection =
            AppContract.AppEntry.TABLE_NAME+
                    "." + AppContract.AppEntry.COLUMN_APP_LABEL + " = ? ";

    //app db - devices sn = ?
    private static final String sAppWithDevicesSelection =
            AppContract.AppEntry.TABLE_NAME+
                    "." + AppContract.AppEntry.COLUMN_DEV_SSN + " = ? ";

    //app db - devices snn_setting = ? AND app label = ?
    private static final String sAppsWithDevicesAndAppsSelection =
            AppContract.AppEntry.TABLE_NAME+
                    "." + AppContract.AppEntry.COLUMN_APP_LABEL + " = ? AND " +
                    AppContract.AppEntry.COLUMN_DEV_SSN + " = ? ";

    //get device by device
    private Cursor getDeviceByDevice(Uri uri, String[] projection, String sortOrder) {
        String device = AppContract.DevicesEntry.getDeviceFromUri(uri);

        String[] selectionArgs;
        String selection;

        selectionArgs = new String[]{device};
        selection = sDevicesSelection;

        return sAppQueryBuilder.query(mOpenHelper.getReadableDatabase(),
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

        return sAppQueryBuilder.query(mOpenHelper.getReadableDatabase(),
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

        selectionArgs = new String[]{device, label};
        selection = sAppsWithDevicesAndAppsSelection;

        return sAppQueryBuilder.query(mOpenHelper.getReadableDatabase(),
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
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = AppContract.CONTENT_AUTHORITY;

        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, AppContract.PATH_APPS, APPS);
        matcher.addURI(authority, AppContract.PATH_APPS + "/*", APPS_WITH_DEVICE);
        matcher.addURI(authority, AppContract.PATH_APPS + "/*/*", APPS_WITH_DEVICE_AND_APP);

        matcher.addURI(authority, AppContract.PATH_DEVICES, DEVICES);
        matcher.addURI(authority, AppContract.PATH_DEVICES + "/*", DEVICES_WITH_DEVICE);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new AppDbHelper(getContext());
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

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;

        switch (match) {
            case APPS: {
                normalizeDate(values);
                long _id = db.insert(AppContract.AppEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = AppContract.AppEntry.buildAppUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case DEVICES: {
                long _id = db.insert(AppContract.DevicesEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = AppContract.DevicesEntry.buildDeviceUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        if ( null == selection ) selection = "1";
        switch (match) {
            case APPS:
                rowsDeleted = db.delete(
                        AppContract.AppEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case DEVICES:
                rowsDeleted = db.delete(
                        AppContract.DevicesEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(AppContract.AppEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(AppContract.AppEntry.COLUMN_DATE);
            values.put(AppContract.AppEntry.COLUMN_DATE, AppContract.normalizeDate(dateValue));
        }
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
                rowsUpdated = db.update(AppContract.AppEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            case DEVICES:
                rowsUpdated = db.update(AppContract.DevicesEntry.TABLE_NAME, values, selection,
                        selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
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
                return returnCount;
            default:
                return super.bulkInsert(uri, values);
        }
    }

}
