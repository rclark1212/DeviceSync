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
import android.app.AlertDialog;
import android.app.UiModeManager;
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
import android.preference.PreferenceManager;
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
        SimpleDateFormat dbDateFormat = new SimpleDateFormat(ctx.getResources().getString(R.string.date_format));
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
     *  Routine to check if this is a first time run
     */
    public static boolean isRunningForFirstTime(Context ctx, boolean bUpdate) {
        boolean bret = true;

        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bhasrun = pref.getBoolean(PREFS_HAS_RUN_ALREADY, false);

        if (bhasrun == true) {
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
     * Used to ask user if they want to download apps found on network for the device (in case of a device wipe)
     */
    public static void askDownloadExistingApps(final Context ctx, final ArrayList<ObjectDetail> missing) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

        alertDialogBuilder.setTitle(ctx.getString(R.string.restore_apps_title));

        String msg = String.format(ctx.getString(R.string.restore_apps_msg), missing.size());
        alertDialogBuilder
                .setMessage(msg)
                .setCancelable(false)
                .setNeutralButton(ctx.getResources().getString(R.string.restore_disable_syncs), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //disable syncs
                        Utils.setSyncDisabled(ctx, true);
                        //FIXME - note this leaves apps in a weird state. Will show apps as local to device but no option
                        //to install, etc...
                    }
                })
                .setNegativeButton(ctx.getResources().getString(R.string.restore_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        GCESync.startActionUpdateLocal(ctx, null, null);
                    }
                })
                .setPositiveButton(ctx.getResources().getString(R.string.restore_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //build the install list...
                        ArrayList<String> apklist = new ArrayList<String>();
                        for (int i=0; i < missing.size(); i++) {
                            apklist.add(missing.get(i).pkg);
                        }
                        //let the updates go through
                        GCESync.startActionUpdateLocal(ctx, null, null);
                        //and kick off the batch install
                        InstallUtil.batchInstallAPK(ctx, apklist);
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
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
     * Routine to provide back the common description text used in detail for the object
     * AND FIXME - we could do a much better job here...
     * @param object
     * @return
     */
    public static String getObjectDetailDescription(Context ctx, ObjectDetail element) {
        String body = "";

        if (element.bIsDevice) {
            body = "Serial: " + element.serial + "\nLocation: " + element.location +
                    "\nOSVer: " + element.ver + "\nUpdated: " + unNormalizeDate(ctx, element.installDate);
        } else {
            body = "Version: " + element.ver + "\nInstallDate: " + unNormalizeDate(ctx, element.installDate) + "\nCount: " + DBUtils.countApp(ctx, element.pkg);
        }
        return body;
    }

}
