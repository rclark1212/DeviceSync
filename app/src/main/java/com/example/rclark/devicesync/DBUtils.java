package com.example.rclark.devicesync;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;

import com.example.rclark.devicesync.data.AppContract;

/**
 * Created by rclark on 4/14/2016.
 * Move static database utilities into this file
 * This is a static lib utility file for DB/CP functions
 *
 */
public class DBUtils {
    private static final String TAG = "DS_DBUtils";

    /*
     * Making sure public utility methods remain static
     */
    private DBUtils() {
    }


    /**
     * Returns an ObjectDetail from the CP based on the URI.
     * Used by detail view.
     */
    public static ObjectDetail getAppFromCP(Context ctx, Uri uri) {
        ObjectDetail returnObject = null;
        Boolean bApp = false;

        //First are we loading an app or a device object?
        String type = ctx.getContentResolver().getType(uri);

        if (type == AppContract.AppEntry.CONTENT_ITEM_TYPE) {
            bApp = true;
        } else if (type == AppContract.DevicesEntry.CONTENT_ITEM_TYPE) {
            bApp = false;
        } else {
            //urp - bad uri
            return null;
        }

        //grab the cursor
        Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);

        if (c.getCount() > 0) {
            //exists!!!
            c.moveToFirst();
            returnObject = new ObjectDetail();
        } else {
            return null;
        }

        if (bApp) {
            int labelIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL);
            int pkgIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG);
            int verIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_VER);
            int serialIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN);
            int dateIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_DATE);
            int flagsIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_FLAGS);
            int bannerIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_BANNER);

            returnObject.label = c.getString(labelIndex);
            returnObject.pkg = c.getString(pkgIndex);
            returnObject.ver = c.getString(verIndex);
            returnObject.serial = c.getString(serialIndex);
            returnObject.installDate = c.getLong(dateIndex);
            returnObject.flags = c.getLong(flagsIndex);

            //deal with bitmap...
            byte[] blob = c.getBlob(bannerIndex);
            if (blob != null) {
                //FIXME - can't get a context for scaling. Should we just make everything a bitmap and not drawable?
                //OR, leave this null if package available on play store and download...
                returnObject.banner = new BitmapDrawable(Utils.convertByteArrayToBitmap(blob));
            }

            returnObject.bIsDevice = false;  //app, not device
        } else {
            //this is a device...
            int serialIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICES_SSN);
            int nameIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_NAME);
            int modelIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_MODEL);
            int osverIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_OSVER);
            int dateIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DATE);
            int typeIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_TYPE);
            int locationIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_LOCATION);

            returnObject.serial = c.getString(serialIndex);
            returnObject.label = c.getString(nameIndex);
            returnObject.name = c.getString(modelIndex);
            returnObject.ver = c.getString(osverIndex);
            returnObject.location = c.getString(locationIndex);

            returnObject.installDate = c.getLong(dateIndex);
            returnObject.type = c.getInt(typeIndex);

            //deal with some device specific items...
            returnObject.banner = null;       //will be picked up from resource and type
            returnObject.bIsDevice = true;    //this is a device
        }

        c.close();

        return returnObject;
    }
}
