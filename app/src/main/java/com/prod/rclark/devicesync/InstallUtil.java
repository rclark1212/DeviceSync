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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.prod.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;

/**
 * Created by rclark on 4/13/2016.
 * Static utility class used to install/uninstall apps through the package manager
 * (through intents)
 */
public class InstallUtil {

    private final static String TAG = "InstallUtil";
    public final static String INSTALL_PREFIX = "market://details?id=";
    public final static String UNINSTALL_PREFIX = "package:";
    public final static String CHECK_URL = "https://play.google.com/store/apps/details?id=";

    private InstallUtil() {};

    /**
     * Installs an APK from google play store
     * @param apk
     */
    public static void installAPK(Context ctx, String apk) {
        //Utils.showToast(ctx, "Installing " + apk);
        Log.d(TAG, "Installing " + apk);

        //For single install, make sure our batch install list is nulled out...
        if (DeviceSyncReceiver.mInstallIntents != null) {
            DeviceSyncReceiver.mInstallIntents.clear();
        }

        //DeviceSyncReceiver.mCurrentlyInstalling = apk;
        //Intent goToMarket = new Intent(Intent.ACTION_VIEW)
        //        .setData(Uri.parse(INSTALL_PREFIX+apk));
        //ctx.startActivity(goToMarket);
        GCESync.startActionAPKInstall(ctx, apk, INSTALL_PREFIX);
    }

    /**
     * Uninstall an APK from device
     * @param apk
     */
    public static void uninstallAPK(Context ctx, String apk) {
        //Utils.showToast(ctx, "UNinstalling " + apk);
        Log.d(TAG, "UnInstalling " + apk);

        //For single uninstall, make sure our batch install list is nulled out...
        if (DeviceSyncReceiver.mInstallIntents != null) {
            DeviceSyncReceiver.mInstallIntents.clear();
        }

        DeviceSyncReceiver.mCurrentlyInstalling = apk;
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse(UNINSTALL_PREFIX + apk));
        ctx.startActivity(intent);
    }

    /**
     * Batch install APKs
     * @param ctx
     * @param apklist
     */
    public static void batchInstallAPK(Context ctx, ArrayList<String> apklist) {
        //basically, set up an array string, kick off first one and let the DeviceSyncReceiver do the rest
        //create array
        //FIXME!!!! Have to do this in intent service and check for the availability of the app on google play (i.e. tvnote not available)
        if (DeviceSyncReceiver.mInstallIntents == null) {
            DeviceSyncReceiver.mInstallIntents = new ArrayList<String>();
        } else {
            DeviceSyncReceiver.mInstallIntents.clear();
        }

        //build list
        for (int i = 0; i < apklist.size(); i++) {
            DeviceSyncReceiver.mInstallIntents.add(apklist.get(i));
        }

        //set the prefix and action...
        DeviceSyncReceiver.mPrefix = INSTALL_PREFIX;
        DeviceSyncReceiver.mAction = Intent.ACTION_VIEW;

        //and now kick off first one...
        if (DeviceSyncReceiver.mInstallIntents.size() > 0) {
            String install = DeviceSyncReceiver.mInstallIntents.get(0);
            DeviceSyncReceiver.mInstallIntents.remove(0);
            DeviceSyncReceiver.mCurrentlyInstalling = install;
            //Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(INSTALL_PREFIX + install));
            //goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //ctx.startActivity(goToMarket);
            GCESync.startActionAPKInstall(ctx, install, INSTALL_PREFIX);
        }
    }

    /**
     * Batch uninstall APKs
     * @param ctx
     * @param apklist
     */
    public static void batchUninstallAPK(Context ctx, ArrayList<String> apklist) {
        //basically, set up an array string, kick off first one and let the DeviceSyncReceiver do the rest
        //create array
        if (DeviceSyncReceiver.mInstallIntents == null) {
            DeviceSyncReceiver.mInstallIntents = new ArrayList<String>();
        } else {
            DeviceSyncReceiver.mInstallIntents.clear();
        }

        //build list
        for (int i = 0; i < apklist.size(); i++) {
            DeviceSyncReceiver.mInstallIntents.add(apklist.get(i));
        }

        //set the prefix and action...
        DeviceSyncReceiver.mPrefix = UNINSTALL_PREFIX;
        DeviceSyncReceiver.mAction = Intent.ACTION_DELETE;

        //and now kick off first one...
        if (DeviceSyncReceiver.mInstallIntents.size() > 0) {
            String install = DeviceSyncReceiver.mInstallIntents.get(0);
            DeviceSyncReceiver.mInstallIntents.remove(0);
            DeviceSyncReceiver.mCurrentlyInstalling = install;
            Intent goToMarket = new Intent(Intent.ACTION_DELETE).setData(Uri.parse(UNINSTALL_PREFIX + install));
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            ctx.startActivity(goToMarket);
        }
    }

}
