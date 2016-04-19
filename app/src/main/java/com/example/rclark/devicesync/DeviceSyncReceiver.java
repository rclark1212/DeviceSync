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
        //TODO - do stuff

        //Install or remove?
        if ((intent.getAction() == Intent.ACTION_PACKAGE_ADDED) ||
                (intent.getAction() == Intent.ACTION_PACKAGE_CHANGED) ||
                (intent.getAction() == Intent.ACTION_PACKAGE_REPLACED)) {
            //install
            Log.v(TAG, "Got install/update intent");
            bInstall = true;
        } else if ((intent.getAction() == Intent.ACTION_PACKAGE_REMOVED) ||
                (intent.getAction() == Intent.ACTION_PACKAGE_FULLY_REMOVED)) {
            //removed
            Log.v(TAG, "Got remove intent");
            bInstall = false;
        }

        //Update the CP with this record...
        if (bInstall) {
            //grr - not getting the icon banner on all installs. Suspect it is due to executing code right at broadcast method.
            //delay by 2 seconds to let the icon/banner settle. (else getting null)
            DBUtils.processInstallApp(ctx, packageName);
        } else {
            //okay - easy one.
            //delete the app from CP
            //Construct the Uri...
            Uri appDB = AppContract.AppEntry.CONTENT_URI;
            //build up the local device query
            appDB = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(packageName).build();
            ctx.getContentResolver().delete(appDB, null, null);
        }
        //Note that the main activity UI will auto-refresh from the CP changing...
    }

    private void installApp(Context ctx, String packageName, Uri appDB) {
        //Create an object
        ObjectDetail app = new ObjectDetail();

        //install the app. Get the app info.
        PackageManager manager = ctx.getPackageManager();

        //FIXME - if package available in play store, null out above
        app.bIsDevice = false;
        //set the right type...
        app.type = Utils.bIsThisATV(ctx) ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;

        try {
            PackageInfo info = manager.getPackageInfo(packageName, 0);
            app.ver = info.versionName;
            app.installDate = info.lastUpdateTime;
            app.label = info.applicationInfo.loadLabel(manager).toString();
            app.pkg = packageName;
            app.serial = Build.SERIAL;  //this serial number
            app.flags = AppContract.AppEntry.FLAG_NO_ACTION;


            app.name = info.applicationInfo.name;
            app.banner = info.applicationInfo.loadBanner(manager);
            if (app.banner == null) {
                info.applicationInfo.loadIcon(manager);
            }
            //FIXME - if package available in play store, null out above

        } catch (PackageManager.NameNotFoundException e) {
            Log.v(TAG, "Can't find package err!!!");
        }

        //Okay - we have an app object... Put it into CP
        DBUtils.saveAppToCP(ctx, appDB, app);
    }
}