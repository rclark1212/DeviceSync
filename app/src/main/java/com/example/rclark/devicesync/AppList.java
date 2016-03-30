package com.example.rclark.devicesync;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rclar on 3/27/2016.
 */
public class AppList {

    public static final String APP_CATEGORY[] = {
            "Devices",
            "Local",
            "Superset",
    };

    public final static int CAT_DEVICES = 0;
    public final static int CAT_LOCAL = 1;
    public final static int CAT_SUPERSET = 2;

    private static PackageManager manager;
    public static ArrayList<ObjectDetail> apps;


    public static void loadRows(ArrayList<String> rows) {
        for (int i = 0; i < APP_CATEGORY.length; i++) {
            //add serial number for local...
            if (APP_CATEGORY[i].equals("Local")) {
                String combo = "Local (" + Build.SERIAL + ")";
                rows.add(combo);
            } else {
                rows.add(APP_CATEGORY[i]);
            }
        }
    }


    public static ArrayList<ObjectDetail> loadApps(Context ctx) {
        manager = ctx.getPackageManager();
        apps = new ArrayList<ObjectDetail>();
        ArrayList<String> pkgs = new ArrayList<String>();

        //first set up each row with an array list
        /*
        for (int i = 0; i < APP_CATEGORY.length; i++) {
            //set up base array list...
            apps.add(i, new ArrayList<ObjectDetail>());
        }*/

        //Grab the apps
        Intent intend = new Intent(Intent.ACTION_MAIN, null);

        intend.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        //next, set up apps, shieldhub, games
        List<ResolveInfo> availableActivities = manager.queryIntentActivities(intend, 0);
        //loop through all apps...
        for (int j = 0; j < availableActivities.size(); j++) {
            ResolveInfo ri = availableActivities.get(j);

            ObjectDetail app = new ObjectDetail();
            app.label = ri.loadLabel(manager).toString();
            app.pkg = ri.activityInfo.packageName;
            app.name = ri.activityInfo.name;
            app.banner = ri.activityInfo.loadBanner(manager);
            app.bIsDevice = false;

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
}
