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
import com.prod.rclark.devicesync.cloud.FirebaseMessengerService;
import com.prod.rclark.devicesync.data.AppContract;
import com.prod.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;

/**
 * Created by rclark on 4/18/2016.
 * Broadcast receiver for install/uninstall events...
 * Also is called with ACTION_BOOT_COMPLETED
 */
public class DeviceSyncReceiver extends BroadcastReceiver {

    private static final String TAG = "DS_Receive";

    //IMPORTANT - IF YOU CHANGE ACTION BELOW, HAVE TO CHANGE MANIFEST FILTER!!!
    //this is a private action we use to skip a file install in the case of the apk not being available on google play
    public static final String ACTION_SKIP = "com.prod.rclark.devicesync.action.ACTION_SKIP_PACKAGE";

    public static ArrayList<String> mInstallIntents;
    public static String mCurrentlyInstalling;
    public static String mPrefix;
    public static String mAction;

    @Override
    public void onReceive(Context ctx, Intent intent) {
        boolean bInstall = false;
        boolean bSkip = false;
        boolean bRelaunchApp = false;
        boolean bNotOurInstall = false;

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            //Okay - we have finished boot. Start the service!
            Intent serviceIntent = new Intent(ctx, FirebaseMessengerService.class);
            ctx.startService(serviceIntent);
        } else {
            Log.d(TAG, "Entering install/uninstall receiver");

            //make sure we have a valid pointer
            if (intent.getData() == null) {
                return;
            }

            String packageName = intent.getData().getEncodedSchemeSpecificPart();
            //if this is us, punt...
            if ("com.prod.rclark.devicesync".equals(packageName)) {
                //punt
                return;
            }

            //there could be some other background process running which is installing apps (google play market for example).
            //while chances of occurance are low, check the currently installing package to make sure we are responding
            //to one of our install actions...
            if (mCurrentlyInstalling != null) {
                if (!mCurrentlyInstalling.equals(packageName)) {
                    //urp - something else going on - let this go through but don't process it as one of our intents...
                    Log.d(TAG, "Not our install - " + mCurrentlyInstalling + " " + packageName);
                    bNotOurInstall = true;
                }
            } else {
                Log.d(TAG, "Not our install - null");
                bNotOurInstall = true;
            }

            //Install or remove or skip?
            if ((intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_ADDED)) ||
                    (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_CHANGED)) ||
                    (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_REPLACED))) {
                //install
                Log.d(TAG, "Got install/update intent for " + packageName);
                bInstall = true;
            } else if ((intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_REMOVED)) ||
                    (intent.getAction().equalsIgnoreCase(Intent.ACTION_PACKAGE_FULLY_REMOVED))) {
                //removed
                Log.d(TAG, "Got remove intent for " + packageName);
                bInstall = false;
            } else if (intent.getAction().equalsIgnoreCase(ACTION_SKIP)) {
                //skip this one...
                bSkip = true;
            }

            //Update the CP with this record...
            if (!bSkip) {
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
                }
            }

            //Is this our install?
            if (!bNotOurInstall) {
                //And note that if we have set up a list of intents to install, process them here
                if (mInstallIntents != null) {
                    //Last package just got installed. Go ahead and kick off next package intent...
                    if (mInstallIntents.size() > 0) {
                        String install = mInstallIntents.get(0);
                        Log.d(TAG, "Installing/Uninstalling " + install);
                        mInstallIntents.remove(0);
                        mCurrentlyInstalling = install;
                        if (bInstall) {
                            //Do the install intent in service so we can check inet if package is on play store
                            GCESync.startActionAPKInstall(ctx, install, mPrefix);
                        } else {
                            Intent goToMarket = new Intent(mAction).setData(Uri.parse(mPrefix + install));
                            //make sure activity not on history stack...
                            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            ctx.startActivity(goToMarket);
                        }
                    } else if (bInstall) {
                        bRelaunchApp = true;
                    }
                } else if (bInstall) {
                    bRelaunchApp = true;
                }

                if (bRelaunchApp) {
                    Log.d(TAG, "Relaunching app");
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
                //Note that the main activity UI will auto-refresh from the CP changing...
            }
        }
    }
}