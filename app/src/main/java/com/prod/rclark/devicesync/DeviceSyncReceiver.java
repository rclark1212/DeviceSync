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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.prod.rclark.devicesync.ATVUI.MainActivity;
import com.prod.rclark.devicesync.ATVUI.MainFragment;
import com.prod.rclark.devicesync.PhoneUI.MainPhoneActivity;
import com.prod.rclark.devicesync.data.AppContract;
import com.prod.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;

/**
 * Created by rclark on 4/18/2016.
 * Broadcast receiver for install/uninstall events...
 */
public class DeviceSyncReceiver extends BroadcastReceiver {

    private static final String TAG = "DS_Receive";
    public static ArrayList<String> mInstallIntents;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        boolean bInstall = false;

        //make sure we have a valid pointer
        if (intent.getData() == null) {
            return;
        }

        String packageName=intent.getData().getEncodedSchemeSpecificPart();

        //Install or remove?
        if ((intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) ||
                (intent.getAction().equals(Intent.ACTION_PACKAGE_CHANGED)) ||
                (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED))) {
            //install
            Log.d(TAG, "Got install/update intent for " + packageName);
            bInstall = true;
        } else if ((intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) ||
                (intent.getAction().equals(Intent.ACTION_PACKAGE_FULLY_REMOVED))) {
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
            Log.d(TAG, "Adding app to CP " + packageName);
            GCESync.startActionLocalAppUpdate(ctx, packageName, "1000");    //add a 1 second delay...
        } else {
            //okay - easy one.
            //delete the app from CP
            //Construct the Uri...
            Log.d(TAG, "Deleting app from CP " + packageName);
            DBUtils.deleteAppFromCP(ctx, Build.SERIAL, packageName);
            //FIXME - verify done? update firebase here - delete record...
            if (MainFragment.mFirebase != null) {
                MainFragment.mFirebase.deleteAppFromFirebase(Build.SERIAL, packageName);
            }

            //And note that if we have set up a list of intents to uninstall, process them here
        }

        //And note that if we have set up a list of intents to install, process them here
        if (mInstallIntents != null) {
            //Last package just got installed. Go ahead and kick off next package intent...
            if (mInstallIntents.size() > 0) {
                String install = mInstallIntents.get(0);
                Log.d(TAG, "Installing/Uninstalling " + install);
                mInstallIntents.remove(0);
                Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(install));
                //make sure activity not on history stack...
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                ctx.startActivity(goToMarket);
            } else {
                Intent i = new Intent();
                i.setAction(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (Utils.bIsThisATV(ctx)) {
                    i.setComponent(new ComponentName(ctx.getPackageName(), MainActivity.class.getName()));
                } else {
                    i.setComponent(new ComponentName(ctx.getPackageName(), MainPhoneActivity.class.getName()));
                }
                ctx.startActivity(i);
            }
        }

        //Note that the main activity UI will auto-refresh from the CP changing...
    }

}