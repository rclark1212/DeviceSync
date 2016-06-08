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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.prod.rclark.devicesync.data.AppContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rclark on 4/22/16.
 * Set of static routines to deal with local app data (really just gets the app info on the device)
 */
public class AppUtils {
    private static final String TAG = "DS_AppUtils";

    //This is a hardcoded list to reject known package names (on my devices at least) that I don't care about
    private static String[] rejectApps = {"com.google.android.gms",
            "com.google.android.leanbacklauncher",
            "com.android.settings",
            "com.google.android.launcher",
            "com.android.providers.download",
            "com.prod.rclark.devicesync"};

    /*
     * Making sure public utility methods remain static
     */
    private AppUtils() {
    }

    /**
     * Populates an object detail from the pkgname passed in.
     * Note that this routine will return null if the application matches one in the rejection list.
     * This routine will also punt on system_flag apps if that preference is set
     *
     * @param ctx
     * @param pkgName
     * @return
     */
    public static ObjectDetail getLocalAppDetails(Context ctx, String pkgName) {

        //Step 1 - figure out if this app is in the reject list...
        for (int i = 0; i < rejectApps.length; i++) {
            if (pkgName.equals(rejectApps[i])) {
                //REJECT
                return null;
            }
        }

        //Step 2 - check preferences for system apps
        //Get the pref bool flag...
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean bIgnoreSystemApps = pref.getBoolean(ctx.getResources().getString(R.string.key_pref_ignore_system_apps), true);

        ObjectDetail app = new ObjectDetail();

        //grab the package manager
        PackageManager manager = ctx.getPackageManager();

        app.bIsDevice = false;
        //set the right type...
        app.type = getAppType(ctx, pkgName);

        if (app.type == AppContract.TYPE_NONE) {
            return null;
        }

        try {
            PackageInfo info = manager.getPackageInfo(pkgName, 0);
            //Step 3 - verify the flags (if system_flag and pref set, punt)
            if (((info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                    && bIgnoreSystemApps) {
                //system app and we are supposed to ignore system apps...
                return null;
            }

            app.ver = info.versionName;
            app.installDate = info.lastUpdateTime;
            app.label = info.applicationInfo.loadLabel(manager).toString();
            app.pkg = pkgName;
            app.serial = Build.SERIAL;  //this serial number
            app.flags = AppContract.AppEntry.FLAG_NO_ACTION;

            app.name = info.applicationInfo.name;

        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Can't find package err!!!");
            return null;
        }
        return app;
    }

    /**
     * Gets the drawable for the local installed package.
     * Pass in apkname and type (tablet or ATV)
     * @param ctx
     * @param apkname
     * @return
     */
    public static Drawable getLocalApkImage(Context ctx, String apkname, long type) {
        Drawable retdraw = null;

        PackageManager manager = ctx.getPackageManager();

        try {
            if (type == AppContract.TYPE_TABLET) {
                retdraw = manager.getApplicationLogo(apkname);
            } else {    //Both ATV and "BOTH"
                retdraw = manager.getApplicationBanner(apkname);
                if (retdraw == null) {
                    Intent intent = manager.getLeanbackLaunchIntentForPackage(apkname);
                    //try banner first
                    if (intent != null) {
                        retdraw = manager.getActivityBanner(intent);
                        if (isWide(retdraw)) {
                            //looks like a good banner...
                            //early return
                            return retdraw;
                        }

                        //then logo
                        retdraw = manager.getActivityLogo(intent);
                        if (isWide(retdraw)) {
                            //looks like a good banner...
                            //early return
                            return retdraw;
                        }

                        //then icon
                        retdraw = manager.getActivityIcon(intent);
                        if (isWide(retdraw)) {
                            //looks like a good banner...
                            //early return
                            return retdraw;
                        }
                    }

                    //some apps store banner as the logo. check here
                    retdraw = manager.getApplicationLogo(apkname);
                    if (isWide(retdraw)) {
                        //looks like a good banner...
                        //early return
                        return retdraw;
                    }

                    //okay - we give up - just use icon - ugg
                    retdraw = manager.getApplicationLogo(apkname);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Can't find package err while trying to get image!!!");
            return null;
        }

        return retdraw;
    }

    /**
     * Checks for a wide aspect ratio indicating this is a banner resource (as opposed to icon)
     *
     * @param testdraw
     * @return
     */
    private static boolean isWide(Drawable testdraw) {
        if (testdraw != null) {
            //is this approx right aspect ratio? (at least 3:2)
            if (2 * testdraw.getIntrinsicWidth() > (3 * testdraw.getIntrinsicHeight())) {
                //looks like a good banner...
                return true;
            }
        }
        return false;
    }

    /**
     * Gets app type - returns either TYPE_ATV, TYPE_TABLET, TYPE_BOTH
     * @param ctx
     * @param pkg
     * @return
     */
    public static int getAppType(Context ctx, String pkg) {
        Log.d(TAG, "Checking package type - " + pkg);
        //Do the simple test
        int returntype = Utils.bIsThisATV(ctx) ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;

        //Now the only remaining work is to see if this app actually supports both ATV and Tablet...
        PackageManager manager = ctx.getPackageManager();

        //See if it has both normal and leanback launch intents...
        Intent intentlb = manager.getLeanbackLaunchIntentForPackage(pkg);
        Intent intentnorm = manager.getLaunchIntentForPackage(pkg);
        if ((intentlb == null) && (intentnorm == null)) {
            return AppContract.TYPE_NONE;
        } else if ((intentlb != null) && (intentnorm != null)) {
            //If ATV, need to do more checking for "BOTH". For tablet, if there is a leanback intent, we are good to go.
            if (Utils.bIsThisATV(ctx)) {
                if (intentnorm.hasCategory(Intent.CATEGORY_LAUNCHER)) {
                    try {
                        PackageInfo pi = manager.getPackageInfo(pkg, 0);
                        if (pi.configPreferences != null) {
                            for (int i = 0; i < pi.configPreferences.length; i++) {
                                if ((pi.configPreferences[i].reqTouchScreen & Configuration.TOUCHSCREEN_FINGER) != 0) {
                                    Log.d(TAG, "Rare unicorn app with both leanback and normal launch intents found - " + pkg);
                                    returntype = AppContract.TYPE_BOTH;
                                    return returntype;
                                }
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.d(TAG, "When trying to check for TYPE_BOTH, couldn't find package");
                    }
                }
            } else {
                Log.d(TAG, "Rare unicorn app with both leanback and normal launch intents found - " + pkg);
                returntype = AppContract.TYPE_BOTH;
            }
        }
        return returntype;
    }

    public static ArrayList<ObjectDetail> loadApps(Context ctx) {
        return loadAppsByOS(ctx, Utils.bIsThisATV(ctx));
    }

    public static ArrayList<ObjectDetail> loadAppsByOS(Context ctx, boolean bATV) {
        PackageManager manager = ctx.getPackageManager();
        ArrayList<ObjectDetail> apps = new ArrayList<ObjectDetail>();
        ArrayList<String> pkgs = new ArrayList<String>();

        //Grab the apps
        Intent intent = new Intent(Intent.ACTION_MAIN, null);

        if (bATV) {
            intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        } else {
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }

        //next, set up apps, shieldhub, games
        List<ResolveInfo> availableActivities = manager.queryIntentActivities(intent, 0);
        //loop through all apps...
        for (int j = 0; j < availableActivities.size(); j++) {
            ResolveInfo ri = availableActivities.get(j);

            ObjectDetail app = AppUtils.getLocalAppDetails(ctx, ri.activityInfo.packageName);

            if (app != null) {
                //Have we already added this?
                if (pkgs.contains(app.pkg.toString())) {
                    //punt...
                    continue;
                }

                apps.add(app);
                pkgs.add(app.pkg.toString());
            }
        }

        return apps;
    }

    /**
     * Populates an object with local device info (including location)
     * Note, this doesn't really belong in this utility class since it is loading a device, not app)
     *
    */
    public static ObjectDetail getLocalDeviceInfo(Context ctx) {
        ObjectDetail device = new ObjectDetail();

        //Set up local device into object
        device.bIsDevice = true;
        device.serial = Build.SERIAL;
        device.label = BluetoothAdapter.getDefaultAdapter().getName();
        device.name = Build.MODEL;
        device.ver = Build.FINGERPRINT + " (" + Build.VERSION.RELEASE + ")";

        device.type = Utils.bIsThisATV(ctx) ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;

        Time time = new Time();
        time.setToNow();
        device.installDate = time.toMillis(true);
        device.location = Utils.getCachedLocation(ctx);
        if (device.location == null) {
            device.location = ctx.getResources().getString(R.string.unknown);
        }

        return device;
    }
}
