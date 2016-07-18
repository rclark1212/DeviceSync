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

package com.prod.rclark.devicesync;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.prod.rclark.devicesync.sync.GCESync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "DS_Utils";
    private static final String PREFS_HAS_RUN_ALREADY = "prefs_has_run_already";
    private static final String SYNCS_ARE_DISABLED = "prefs_syncs_disabled";
    private static final String CACHED_LOCATION = "prefs_last_location";
    private static final String APP_IS_RUNNING = "prefs_app_is_active";
    private static final String PREFS_USER_ID = "prefs_user_id";

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
        boolean bRet = false;
        UiModeManager uiModeManager = (UiModeManager) ctx.getSystemService(Context.UI_MODE_SERVICE);
        bRet = (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION);
        return bRet;
    }

    /*
        Turns a normalized date ino a human readable one
     */
    public static String unNormalizeDate(Context ctx, long normalizedDateInMillis) {
        Time time = new Time();
        time.setToNow();
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(ctx.getResources().getString(R.string.date_format), Locale.getDefault());
        String yearMonthDayString = dbDateFormat.format(normalizedDateInMillis);
        return yearMonthDayString;
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

    /*
    Grabbed this nice little routine from
    http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap from Andre.
 */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                if (bitmapDrawable.getBitmap().isRecycled()) {
                    return null;
                } else {
                    return bitmapDrawable.getBitmap();
                }
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
     *  Routine to check if this is a first time run
     */
    public static boolean isRunningForFirstTime(Context ctx, boolean bUpdate) {
        boolean bret = true;

        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bhasrun = pref.getBoolean(PREFS_HAS_RUN_ALREADY, false);

        if (bhasrun) {
            bret = false;
        }

        if (bUpdate) {
            //And, if we are here, we have run for first time so mark preferences as such
            SharedPreferences.Editor edit = pref.edit();
            edit.putBoolean(PREFS_HAS_RUN_ALREADY, true);
            edit.commit();
        }

        return bret;
    }

    /**
     *  Routine to set a flag if app is active (used by service for whether to deliver messages)
     */
    public static void setAppActive(Context ctx, boolean bAppActive) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(APP_IS_RUNNING, true);
        edit.commit();
    }

    /**
     *  Routine to set a flag if app is active (used by service for whether to deliver messages)
     */
    public static boolean isAppActive(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bReturn = pref.getBoolean(APP_IS_RUNNING, false);
        return bReturn;
    }


    //
    //  Utility routine to check if we have internet connection. Check on start
    //
    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo == null)
            return false;

        return netInfo.isConnected();
    }

    /**
     *  Routine to check to see if an image for a package is available on network (don't save it to CP if available on net)
     *  Return null if not available.
     *  Return a uri if available.
     */
    public static Uri getAppImageUriOnNetwork(String pkgname, Context ctx) {
        return null;
        //FIXME - fix this stub
    }

    /**
     * Launches an app
     * @param ctx
     * @param pkgName
     */
    public static void launchApp(Context ctx, String pkgName) {
        PackageManager manager = ctx.getPackageManager();

        Intent i = manager.getLaunchIntentForPackage(pkgName);
        if (i == null) {
            return;
        }

        if (bIsThisATV(ctx)) {
            i.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        } else {
            i.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        ctx.startActivity(i);
    }

    /**
     * Tells us if syncs are disabled or not
     * @param ctx
     * @return
     */
    public static boolean isSyncDisabled(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bret = pref.getBoolean(SYNCS_ARE_DISABLED, false);
        return bret;
    }

    /**
     * Set whether syncs are disabled or not. NOTE - this is sync's to the CP database! (local->CP).
     * It does NOT impact CP<->cloud
     * @param ctx
     * @return
     */
    public static void setSyncDisabled(Context ctx, boolean bDisabled) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = pref.edit();
        edit.putBoolean(SYNCS_ARE_DISABLED, bDisabled);
        edit.commit();
    }

    /**
     * Gets whether syncs are disabled or not.
     * @param ctx
     * @return
     */
    public static boolean getSyncDisabled(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getBoolean(SYNCS_ARE_DISABLED, false);
    }


    /**
     *  Routine to store off location discovered in onCreate to preferences. Used by service/other routines
     *  later...
     */
    public static void setCachedLocation(Context ctx, String location) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = pref.edit();
        if (location != null) {
            edit.putString(CACHED_LOCATION, location);
            edit.commit();
        } else {
            edit.remove(CACHED_LOCATION);
            edit.commit();
        }
    }

    /**
     *  Routine to store off location discovered in onCreate to preferences. Used by service/other routines
     *  later...
     */
    public static String getCachedLocation(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String location = pref.getString(CACHED_LOCATION, null);
        return location;
    }

    /*
    gets location of device
    */
    public static String getLocation(Context ctx, GoogleApiClient mClient) {
        String ret = ctx.getResources().getString(R.string.unknown);
        Location location = null;

        //Double check permissions (should have been asked for at startup)
        int permissionCheck = ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            //its okay to get last location here - don't need accuracy of realtime ping for location
            location = LocationServices.FusedLocationApi.getLastLocation(mClient);
            Log.d(TAG, "Grabbing location...");
        }

        //above may fail (no cached location or user may deny privileges)
        if (location != null) {
            Geocoder geo = new Geocoder(ctx);

            List<Address> addresses = null;

            if (geo != null) {
                try {
                    addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException e) {
                    Log.e(TAG, "Error getting address on geolocation");
                } catch (IllegalArgumentException illegalArgumentException) {
                    Log.e(TAG, "Bad lat and long");
                }
            }

            if ((addresses != null) && (addresses.size() > 0)) {
                //We got an address!!!
                if (addresses.get(0).getAdminArea() != null) {
                    if (Utils.bIsThisATV(ctx)) {
                        ret = String.format(ctx.getResources().getString(R.string.atv_location), addresses.get(0).getLocality(), addresses.get(0).getAdminArea());
                    } else {
                        ret = String.format(ctx.getResources().getString(R.string.phone_location),
                                addresses.get(0).getAddressLine(0), addresses.get(0).getLocality(), addresses.get(0).getAdminArea());
                    }
                } else {
                    ret = String.format(ctx.getResources().getString(R.string.latlong_location), addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                }
            }
        }

        return ret;
    }

    //Returns user ID we got from accounts (is a token)
    public static String getUserId(Context ctx) {
        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String user = pref.getString(PREFS_USER_ID, null);

        return user;
    }

    //Sets user ID we got from accounts
    public static void setUserId(Context ctx, String user) {
        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(PREFS_USER_ID, user);
        edit.commit();
    }

    /**
     *  stripForFirebase
     *  Silly but true - no periods, #, $, [ or ] in firebase strings
     */
    public static String stripForFirebase(String input) {

        String output = input.replace('.', '_');

        try {
            return URLEncoder.encode(output, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }


    /**
     * Routine to deliver back appropriate tablet image - logic is if >6", tablet. Else phone.
     */
    public static int getTabletResource(Context ctx) {
        //Are we a large or xlarge device?
        if ((ctx.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            return R.drawable.shieldtablet;
        } else {
            return R.drawable.nexus5;
        }
    }


    /**
     * Sets the local device name.
     * BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
     * String deviceName = myDevice.getName();
     */
    public static void setLocalDeviceName(Context ctx, String name) {
        BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
        //Make sure we permission to do this
        int permissionCheck = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_ADMIN);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            myDevice.setName(name);
            //And need to update the local device CP...
            ObjectDetail local = DBUtils.getDeviceFromCP(ctx, Build.SERIAL);
            local.label = name;
            local.timestamp = System.currentTimeMillis();
            DBUtils.saveDeviceToCP(ctx, local);
        } else {
            Toast.makeText(ctx, ctx.getResources().getString(R.string.no_bt_admin), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * As this action a little more in-depth, put in utility class so both phone/ATVUI can leverage.
     * Sets up install/uninstall lists to bring local device to same state as remote serial.
     * @param ctx
     * @param serial
     */
    public static void cloneDevice(Activity activity, String serial) {
        //This is the fun and useful one... Generate both an install and uninstall list.
        //Get the list of apps on the remote device.
        ArrayList<ObjectDetail> remote = DBUtils.getAppsOnDevice(activity, serial);
        //Convert these object details to a simple string list. We don't want to be comparing objects, only apks
        ArrayList<String> remote_apk = new ArrayList<String>();
        for (int i=0; i < remote.size(); i++) {
            remote_apk.add(remote.get(i).pkg);
        }
        remote.clear();
        remote = null;  //release the list

        //Get the list of apps on our device
        ArrayList<ObjectDetail> local = DBUtils.getAppsOnDevice(activity, Build.SERIAL);
        ArrayList<String> local_apk = new ArrayList<String>();
        for (int i=0; i < local.size(); i++) {
            local_apk.add(local.get(i).pkg);
        }
        local.clear();
        local = null;  //release the list

        //Create an install apk list...
        ArrayList<String> apk_install = new ArrayList<String>();
        for (int i=0; i < remote_apk.size(); i++) {
            //loop through - if app not already on local
            if (!local_apk.contains(remote_apk.get(i))) {
                //add it to the install list
                apk_install.add(remote_apk.get(i));
            }
        }

        //Create the uninstall list (in reverse)
        ArrayList<String> apk_uninstall = new ArrayList<String>();
        for (int i=0; i < local_apk.size(); i++) {
            //loop through - if app not already on local
            if (!remote_apk.contains(local_apk.get(i))) {
                //add it to the uninstall list
                apk_uninstall.add(local_apk.get(i));
            }
        }

        //Okay - we have the 2 lists... Call our APK routines
        //and now uninstall. Do uninstall first for space reasons... Ha, put uninstall second for it to come up last (i.e. first)
        //InstallUtil.batchInstallAPK(ctx, apk_install);
        UIUtils.confirmBatchOperation(activity, apk_install, true);
        //InstallUtil.batchUninstallAPK(ctx, apk_uninstall);
        UIUtils.confirmBatchOperation(activity, apk_uninstall, false);
    }
}
