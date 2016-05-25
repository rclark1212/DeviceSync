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

package com.prod.rclark.devicesync.sync;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.text.format.Time;

import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

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

    /*
    Populates an object with local device info (including location)
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
