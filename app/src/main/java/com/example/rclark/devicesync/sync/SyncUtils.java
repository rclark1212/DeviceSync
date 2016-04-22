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

package com.example.rclark.devicesync.sync;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.format.Time;
import android.util.Log;

import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.Utils;
import com.example.rclark.devicesync.data.AppContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by rclark on 4/12/16.
 * Provide utility routines used by the sync intent service
 * Refactor some code out of the UI projects to here (UI should never need to access either the device info or
 * the app info. Go through CP always.
 * All static
 */

public class SyncUtils {
    private static PackageManager manager;
    public static ArrayList<ObjectDetail> apps;
    private static final String TAG = "GCESyncUtils";

    /*
    * Making sure public utility methods remain static
    */
    private SyncUtils() {
    }

    public static ArrayList<ObjectDetail> loadApps(Context ctx) {
        return loadAppsByOS(ctx, Utils.bIsThisATV(ctx));
    }

    public static ArrayList<ObjectDetail> loadAppsByOS(Context ctx, boolean bATV) {
        manager = ctx.getPackageManager();
        apps = new ArrayList<ObjectDetail>();
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

            ObjectDetail app = new ObjectDetail();
            app.label = ri.loadLabel(manager).toString();
            app.pkg = ri.activityInfo.packageName;
            app.name = ri.activityInfo.name;
            app.banner = ri.activityInfo.loadBanner(manager);

            //FIXME - if package available in play store, null out above
            app.bIsDevice = false;
            //set the right type...
            app.type = bATV ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;

            try {
                PackageInfo info = manager.getPackageInfo(app.pkg, 0);
                app.ver = info.versionName;
                app.installDate = info.lastUpdateTime;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Can't find package on initial loop");
            }

            //Have we already added this?
            if (pkgs.contains(app.pkg.toString())) {
                //punt...
                continue;
            }

            //hmm - for some apps, this not getting us data... (well, tablet apps of course)
            if (app.banner == null) {
                //use the icon...
                app.banner = ri.activityInfo.loadIcon(manager);
            }

            if (app.label.equals("LeanbackLauncher")) {
                //also punt
                //FIXME - need to filter out a lot more apps than just leanback launcher - both tablet and ATV
                continue;
            }

            try {
                app.ai = manager.getApplicationInfo(app.name.toString(), PackageManager.GET_META_DATA);
                app.res = manager.getResourcesForApplication(ri.activityInfo.packageName);

            } catch (PackageManager.NameNotFoundException e) {

            }

            apps.add(app);
            pkgs.add(app.pkg.toString());

        }

        return apps;
    }

    /*
    Populates an object with local device info
 */
    public static ObjectDetail getLocalDeviceInfo(Context ctx, boolean useLocation, GoogleApiClient mClient) {
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
        if (useLocation) {
            device.location = getLocation(ctx, mClient);
        } else {
            device.location = ctx.getResources().getString(R.string.unknown);
        }

        return device;
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

        return ret;
    }

}
