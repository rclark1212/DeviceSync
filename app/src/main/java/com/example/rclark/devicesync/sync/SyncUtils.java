package com.example.rclark.devicesync.sync;

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
import android.location.Location;
import android.os.Build;
import android.text.format.Time;

import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.Utils;
import com.example.rclark.devicesync.data.AppContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

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
            app.bIsDevice = false;
            //set the right type...
            if (bATV) {
                app.type = AppContract.TYPE_ATV;
            } else {
                app.type = AppContract.TYPE_TABLET;
            }

            try {
                PackageInfo info = manager.getPackageInfo(app.pkg, 0);
                app.ver = info.versionName;
                app.installDate = info.lastUpdateTime;
            } catch (PackageManager.NameNotFoundException e) {

            }

            //Have we already added this?
            if (pkgs.contains(app.pkg.toString())) {
                //punt...
                continue;
            }

            //hmm - for some apps, this not getting us data...
            if (app.banner == null) {
                //use the icon...
                app.banner = ri.activityInfo.loadIcon(manager);
            }

            if (app.label.equals("LeanbackLauncher")) {
                //also punt
                //FIXME - need to filter out a lot more apps than just leanback launcher
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

        if (Utils.bIsThisATV(ctx)) {
            device.type = AppContract.TYPE_ATV;
        } else {
            device.type = AppContract.TYPE_TABLET;
        }

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
        Grabbed this nice little routine from
        http://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap from Andre.
     */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

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


    /*
    gets location of device
    */
    public static String getLocation(Context ctx, GoogleApiClient mClient) {
        //FIXME - not using callbacks here...
        String ret = ctx.getResources().getString(R.string.unknown);

        Location location = LocationServices.FusedLocationApi.getLastLocation(mClient);

        //above may fail (no cached location or user may deny privileges)
        if (location != null) {
            ret = location.toString();
        }

        return ret;
    }

}
