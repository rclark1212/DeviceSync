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

package com.example.rclark.devicesync;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.example.rclark.devicesync.data.AppContract;
import com.example.rclark.devicesync.sync.GCESync;

import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Created by rclark on 4/18/2016.
 * Broadcast receiver for install/uninstall events...
 */
public class DeviceSyncReceiver extends BroadcastReceiver {

    private static final String TAG = "DS_Receive";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        boolean bInstall = false;

        //make sure we have a valid pointer
        if (intent.getData() == null) {
            return;
        }

        String packageName=intent.getData().getEncodedSchemeSpecificPart();

        //Install or remove?
        if ((intent.getAction() == Intent.ACTION_PACKAGE_ADDED) ||
                (intent.getAction() == Intent.ACTION_PACKAGE_CHANGED) ||
                (intent.getAction() == Intent.ACTION_PACKAGE_REPLACED)) {
            //install
            Log.d(TAG, "Got install/update intent for " + packageName);
            bInstall = true;
        } else if ((intent.getAction() == Intent.ACTION_PACKAGE_REMOVED) ||
                (intent.getAction() == Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            //removed
            Log.d(TAG, "Got remove intent for " + packageName);
            bInstall = false;
        }

        //Update the CP with this record...
        if (bInstall) {
            //TODO - review this as part of final review. Initially was seeing us getting called before a bitmap for app available (getting null back).
            //But that was while single stepping. When running, appears we get several calls including one which lets us successfully get bitmap.
            //Update - really should use service to update (and get out of broadcast receiver asap). Doing this below. Latency of service probably
            //will fix this issue. If not, can add a slight delay to the service on starting processing...
            //Have intent sync service do this work...
            GCESync.startActionLocalAppUpdate(ctx, packageName, "1000");    //add a 1 second delay...
        } else {
            //okay - easy one.
            //delete the app from CP
            //Construct the Uri...
            Log.d(TAG, "Deleting app from CP " + packageName);
            Uri appDB = AppContract.AppEntry.CONTENT_URI;
            //build up the local device query
            appDB = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(packageName).build();
            ctx.getContentResolver().delete(appDB, null, null);
        }
        //Note that the main activity UI will auto-refresh from the CP changing...
    }

}