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

import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import java.io.Serializable;

/**
 * Created by rclark on 3/27/2016.
 * Used for both devices and apps
 *
 * Image rules...
 * Banner will only be saved to CP if the package not available in play store (sync side).
 */
public class ObjectDetail implements Serializable {
    public String label;        //nickname or app
    public String name;         //model name
    public String pkg;          //pkg name
    public String ver;          //version for both
    public String serial;       //serial number of device app is on
    public String image_url;    //url of the banner image for the app
    public String location;     //location for devices
    public boolean bIsDevice;   //true if using object to represent device, false for app
    public long installDate;    //update date or install date
    public long flags;
    public long type;            //indicates device type for both apps/devices - 0=atv, 1=tablet
    public long timestamp;      //timestamp used for firebase compares

    public ObjectDetail() {};
}
