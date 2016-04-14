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

package com.example.rclark.devicesync;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.telecom.ConnectionRequest;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.rclark.devicesync.data.AppContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.text.SimpleDateFormat;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    public static final String DATE_FORMAT = "MM-dd-yyyy HH:mm";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "DS_Utils";

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    /**
     * Returns the screen/display size
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * Shows a (long) toast
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a (long) toast.
     */
    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    /**
     * Formats time in milliseconds to hh:mm:ss string format.
     */
    public static String formatMillis(int millis) {
        String result = "";
        int hr = millis / 3600000;
        millis %= 3600000;
        int min = millis / 60000;
        millis %= 60000;
        int sec = millis / 1000;
        if (hr > 0) {
            result += hr + ":";
        }
        if (min >= 0) {
            if (min > 9) {
                result += min + ":";
            } else {
                result += "0" + min + ":";
            }
        }
        if (sec > 9) {
            result += sec;
        } else {
            result += "0" + sec;
        }
        return result;
    }

    /*
        indicates if we are on an ATV or tablet
     */
    public static boolean bIsThisATV(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature("com.google.android.tv");
    }

    /*
        Turns a normalized date ino a human readable one
     */
    public static String unNormalizeDate(long normalizedDateInMillis) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(Utils.DATE_FORMAT);
        String yearMonthDayString = dbDateFormat.format(normalizedDateInMillis);
        return yearMonthDayString;
    }

    /*
        checkPlayServices
     */
    public static boolean checkPlayServices(AppCompatActivity activity) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(activity.getApplicationContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device does not support GMS services");
            }
            return false;
        }
        return true;
    }

    /**
     * Routine that does what it says...
     * @param byteArrayToBeCOnvertedIntoBitMap
     * @return
     */
    public static Bitmap convertByteArrayToBitmap(byte[] byteArrayToBeCOnvertedIntoBitMap) {
        Bitmap bitMapImage = BitmapFactory.decodeByteArray(
                byteArrayToBeCOnvertedIntoBitMap, 0,
                byteArrayToBeCOnvertedIntoBitMap.length);

        return bitMapImage;
    }

    /**
     * Returns an ObjectDetail from the CP based on the URI.
     * Used by detail view.
     */
    public static ObjectDetail getAppFromCP(Context ctx, Uri uri) {
        ObjectDetail returnObject = null;
        Boolean bApp = false;

        //First are we loading an app or a device object?
        String type = ctx.getContentResolver().getType(uri);

        if (type == AppContract.AppEntry.CONTENT_ITEM_TYPE) {
            bApp = true;
        } else if (type == AppContract.DevicesEntry.CONTENT_ITEM_TYPE) {
            bApp = false;
        } else {
            //urp - bad uri
            return null;
        }

        //grab the cursor
        Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);

        if (c.getCount() > 0) {
            //exists!!!
            c.moveToFirst();
            returnObject = new ObjectDetail();
        } else {
            return null;
        }

        if (bApp) {
            int labelIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL);
            int pkgIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG);
            int verIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_VER);
            int serialIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN);
            int dateIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_DATE);
            int flagsIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_FLAGS);
            int bannerIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_BANNER);

            returnObject.label = c.getString(labelIndex);
            returnObject.pkg = c.getString(pkgIndex);
            returnObject.ver = c.getString(verIndex);
            returnObject.serial = c.getString(serialIndex);
            returnObject.installDate = c.getLong(dateIndex);
            returnObject.flags = c.getLong(flagsIndex);

            //deal with bitmap...
            byte[] blob = c.getBlob(bannerIndex);
            if (blob != null) {
                //FIXME - can't get a context for scaling. Should we just make everything a bitmap and not drawable?
                //OR, leave this null if package available on play store and download...
                returnObject.banner = new BitmapDrawable(convertByteArrayToBitmap(blob));
            }

            returnObject.bIsDevice = false;  //app, not device
        } else {
            //this is a device...
            int serialIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICES_SSN);
            int nameIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_NAME);
            int modelIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_MODEL);
            int osverIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_OSVER);
            int dateIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DATE);
            int typeIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_TYPE);
            int locationIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_LOCATION);

            returnObject.serial = c.getString(serialIndex);
            returnObject.label = c.getString(nameIndex);
            returnObject.name = c.getString(modelIndex);
            returnObject.ver = c.getString(osverIndex);
            returnObject.location = c.getString(locationIndex);

            returnObject.installDate = c.getLong(dateIndex);
            returnObject.type = c.getInt(typeIndex);

            //deal with some device specific items...
            returnObject.banner = null;       //will be picked up from resource and type
            returnObject.bIsDevice = true;    //this is a device
        }

        c.close();

        return returnObject;
    }
}
