package com.example.rclark.devicesync;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public static ArrayList<ArrayList<AppDetail>> apps;

    public static ArrayList<ArrayList<AppDetail>> loadApps(Context ctx) {
        manager = ctx.getPackageManager();
        apps = new ArrayList<ArrayList<AppDetail>>();
        ArrayList<String> pkgs = new ArrayList<String>();

        //first set up each row with an array list
        for (int i = 0; i < APP_CATEGORY.length; i++) {
            //set up base array list...
            apps.add(i, new ArrayList<AppDetail>());
        }

        //Grab the apps
        Intent intend = new Intent(Intent.ACTION_MAIN, null);

        intend.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        //next, set up apps, shieldhub, games
        List<ResolveInfo> availableActivities = manager.queryIntentActivities(intend, 0);
        //loop through all apps...
        for (int j = 0; j < availableActivities.size(); j++) {
            ResolveInfo ri = availableActivities.get(j);

            AppDetail app = new AppDetail();
            app.label = ri.loadLabel(manager);
            app.pkg = ri.activityInfo.packageName;
            app.name = ri.activityInfo.name;
            app.banner = ri.activityInfo.loadBanner(manager);
            app.bIsDevice = false;

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

            apps.get(CAT_LOCAL).add(app);
            pkgs.add(app.pkg.toString());

        }

        return apps;
    }
}
