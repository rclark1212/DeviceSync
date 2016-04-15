package com.example.rclark.devicesync;

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.Serializable;

/**
 * Created by rclark on 3/27/2016.
 * Used for both devices and apps
 *
 * Image rules...
 * Banner will only be saved to CP if the package not available in play store (sync side).
 */
public class ObjectDetail implements Serializable {
    static final long serialVersionUID = 727566175075960651L;
    public String label;        //nickname or app
    public String name;         //model name
    public String pkg;          //pkg name
    public String ver;          //version for both
    public String serial;       //serial number of device app is on
    public long installDate;    //update date or install date
    public long flags;
    public Drawable banner;     //this should only be used for apps not in app store FIXME
    public boolean bIsDevice;   //true if using object to represent device, false for app
    public long type;            //indicates device type for both apps/devices - 0=atv, 1=tablet
    public String location;     //location for devices
    public ApplicationInfo ai;
    public Resources res;

    public ObjectDetail() {};
}
