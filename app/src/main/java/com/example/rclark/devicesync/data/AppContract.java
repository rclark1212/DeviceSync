package com.example.rclark.devicesync.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by rclar on 3/27/2016.
 */
public class AppContract {

    public static final String CONTENT_AUTHORITY = "com.example.android.devicesync";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_APPS = "apps";
    public static final String PATH_DEVICES = "devices";

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

        public static Uri buildDeviceUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    /* Inner class that defines the table contents of the app table */
    public static final class AppEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_APPS).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_APPS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_APPS;

        public static final String TABLE_NAME = "apps";

        // Column with the foreign key into the location table.
        public static final String COLUMN_DEVICES_KEY = "devices_id";

        // Date, stored as long in milliseconds since the epoch
        public static final String COLUMN_DATE = "date";

        public static final String COLUMN_APP_LABEL = "app_label";
        public static final String COLUMN_APP_PKG = "app_pkg";
        public static final String COLUMN_APP_FLAGS = "app_flags";
        public static final String COLUMN_APP_BANNER = "app_banner";
        public static final String COLUMN_DEV_SSN = "devicessn";

        public static Uri buildAppUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildApp(String DeviceSSN) {
            return CONTENT_URI.buildUpon().appendPath(DeviceSSN).build();
        }

        public static Uri buildAppWithDate(String DeviceSSN, long date) {
            return CONTENT_URI.buildUpon().appendPath(DeviceSSN)
                    .appendPath(Long.toString(normalizeDate(date))).build();
        }
    }
}
