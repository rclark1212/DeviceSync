package com.example.rclark.devicesync;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * Created by rclark on 3/27/2016.
 * Use as a structure (yes, inefficient)
 * Used for both devices and apps
 */
public class ObjectDetail {
    public String label;        //nickname or app
    public String name;         //model name
    public String pkg;          //pkg name
    public String ver;          //version for both
    public String serial;       //serial number of device app is on
    public long installDate;    //update date or install date
    public long flags;
    public Drawable banner;
    public boolean bIsDevice;   //true if using object to represent device, false for app
    public int type;            //indicates device type for both apps/devices - 0=atv, 1=tablet
    public String location;     //location for devices
    public ApplicationInfo ai;
    public Resources res;
}
