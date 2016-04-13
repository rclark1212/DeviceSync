package com.example.rclark.devicesync;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.example.rclark.devicesync.data.AppContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rclark on 3/27/2016.
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
}
