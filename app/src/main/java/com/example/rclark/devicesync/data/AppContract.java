package com.example.rclark.devicesync.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by rclark on 3/27/2016.
 */
public class AppContract {

    public static final String CONTENT_AUTHORITY = "com.example.rclark.devicesync";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_APPS = "apps";
    public static final String PATH_DEVICES = "devices";
    public static final String PATH_GROUPBY = "appsgroupby";    //aliases to apps db

    public static final int TYPE_ATV = 0;       //indicates this is an ATV app/device
    public static final int TYPE_TABLET = 1;    //indicates this is a phone/tablet app/device

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long normalizeDate(long startDate) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    /* Inner class that defines the table contents of the devices table */
    // URI format
    // /deviceSN
    public static final class DevicesEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_DEVICES).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DEVICES;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DEVICES;

        // Table name
        public static final String TABLE_NAME = "devices";

        public static final String COLUMN_DEVICES_SSN = "serial_num";

        public static final String COLUMN_DATE = "date";

        //Device identifiers
        public static final String COLUMN_DEVICE_NAME = "nick_name";
        public static final String COLUMN_DEVICE_MODEL = "model_name";
        public static final String COLUMN_DEVICE_OSVER = "os_ver";
        public static final String COLUMN_DEVICE_TYPE = "type";
        public static final String COLUMN_DEVICE_LOCATION = "location";
        public static final String COLUMN_DEVICE_TIMEUPDATED = "lasttouched";       //use this field to timestamp entries to CP

        public static Uri buildDeviceUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildDeviceUri(String deviceSN) {
            return CONTENT_URI.buildUpon().appendPath(deviceSN).build();
        }

        public static String getDeviceFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    /* Inner class that defines the table contents of the app table */
    // URI Format
    // /* - all or...
    // /deviceSN/app_pkg
    public static final class AppEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_APPS).build();

        //This is fugly but is due to an android failure. No groupBy ability in a content provider.
        //So make a fake URI matcher that will alias to global /APPS but with a groupBy parameter.
        //So /APPS/groupBy.
        //USED ONLY FOR QUERIES! USED ONLY FOR GLOBAL APP SEARCHES!!!!
        public static final Uri GROUPBY_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GROUPBY).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_APPS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_APPS;

        public static final String TABLE_NAME = "apps";

        // Flags for DB...
        // Keep it simple for now. If a file is marked to be deleted, flag it for the other devices...
        public static final int FLAG_FOR_DELETE = 2;
        public static final int FLAG_FOR_ADD = 1;
        public static final int FLAG_NO_ACTION = 0;

        // Column with the foreign key into the location table.
        public static final String COLUMN_DEVICES_KEY = "devices_id";

        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";

        public static final String COLUMN_APP_LABEL = "app_label";
        public static final String COLUMN_APP_PKG = "app_pkg";
        public static final String COLUMN_APP_FLAGS = "app_flags";
        public static final String COLUMN_APP_BANNER = "app_banner";
        public static final String COLUMN_APP_VER = "app_version";
        public static final String COLUMN_APP_TYPE = "type";
        public static final String COLUMN_APP_DEVSSN = "devicessn";
        public static final String COLUMN_APP_TIMEUPDATED = "lasttouched";       //use this field to timestamp entries to CP

        public static Uri buildAppUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildApp(String DeviceSSN) {
            return CONTENT_URI.buildUpon().appendPath(DeviceSSN).build();
        }

        public static Uri buildAppWithApp(String DeviceSSN, String appPkg) {
            return CONTENT_URI.buildUpon().appendPath(DeviceSSN)
                    .appendPath(appPkg).build();
        }

        public static String getDeviceFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        public static String getAppFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }

    }
}
