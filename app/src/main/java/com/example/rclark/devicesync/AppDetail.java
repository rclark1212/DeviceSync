package com.example.rclark.devicesync;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * Created by rclar on 3/27/2016.
 */
public class AppDetail {
    public String label;
    public String name;
    public String pkg;
    public String ver;
    public long installDate;
    public Drawable banner;
    public boolean bIsDevice;
    public ApplicationInfo ai;
    public Resources res;
}
