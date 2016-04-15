package com.example.rclark.devicesync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.example.rclark.devicesync.data.AppContract;
import com.example.rclark.devicesync.sync.SyncUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

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
    public static ObjectDetail getObjectFromCP(Context ctx, Uri uri) {
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
            int typeIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_TYPE);


            returnObject.label = c.getString(labelIndex);
            returnObject.pkg = c.getString(pkgIndex);
            returnObject.ver = c.getString(verIndex);
            returnObject.serial = c.getString(serialIndex);
            returnObject.installDate = c.getLong(dateIndex);
            returnObject.flags = c.getLong(flagsIndex);
            returnObject.type = c.getLong(typeIndex);

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
            returnObject.type = c.getLong(typeIndex);

            //deal with some device specific items...
            returnObject.banner = null;       //will be picked up from resource and type
            returnObject.bIsDevice = true;    //this is a device
        }

        c.close();

        return returnObject;
    }

    /**
     * Routine for testing CP and queries only...
     */
    public static void loadFakeData(Context ctx) {

        //Okay - lets hack it up.
        //Do 4 fake devices.
        // Serial numbers existing with _0, _1, _2, _3 appended
        //Do 8 apps on each device
        // 4 should be same as apps on local device
        // 4 should be named com.fake.pkg but on incrementing values (device1 will have fake1-4, device2 will have fake2-5, etc)

        //Okay - first update the device database - serial number, nickname, date, model name, os_ver
        //Get an object with local device info...
        //Get the device DB reference...
        ArrayList<String> fakedevice = new ArrayList<String>();

        Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;
        Uri localdeviceDB = deviceDB.buildUpon().appendPath(Build.SERIAL).build();


        ObjectDetail device = getObjectFromCP(ctx, localdeviceDB);
        String serialbase = device.serial;  //save off base serial
        ContentValues contentValues = new ContentValues();

        for (int i = 0; i < 4; i++) {
            //create the buffer
            contentValues.clear();

            //create fake serial...
            String serialfake = serialbase + "_" + i;
            fakedevice.add(serialfake);

            //Now, search for the device (is it in DB yet?) - search by serial
            Uri deviceSearchUri = deviceDB.buildUpon().appendPath(serialfake).build();

            Log.v(TAG, "device query - uri:" + deviceSearchUri.toString());
            Cursor c = ctx.getContentResolver().query(deviceSearchUri, null, null, null, null);

            if (c.getCount() > 0) {
                //device exists...
                //preload the content values...
                c.moveToFirst();
                DatabaseUtils.cursorRowToContentValues(c, contentValues);
            }

            //load up contentValues with latest info...
            long type = Utils.bIsThisATV(ctx) ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;
            contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICES_SSN, serialfake);
            contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_NAME, device.label);
            contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_MODEL, device.name);
            contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_OSVER, device.ver);
            contentValues.put(AppContract.DevicesEntry.COLUMN_DATE, device.installDate);
            contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_TYPE, type);
            contentValues.put(AppContract.DevicesEntry.COLUMN_DEVICE_LOCATION, device.location);

            if (c.getCount() > 0) {
                //replace
                ctx.getContentResolver().update(deviceSearchUri, contentValues, null, null);
            } else {
                //add
                ctx.getContentResolver().insert(AppContract.DevicesEntry.CONTENT_URI, contentValues);
            }

            c.close();
        }

        /**
         * Okay - device database should be updated. Now time to do same for apps...
         */

        //loop through the fake devices...
        for (int i=0; i < fakedevice.size(); i++) {
            //Get the device DB reference...
            Uri appDB = AppContract.AppEntry.CONTENT_URI;
            //build up the local device query
            Uri localAppDB = appDB.buildUpon().appendPath(Build.SERIAL).build();
            Uri fakeappDB = appDB.buildUpon().appendPath(fakedevice.get(i)).build();
            Cursor c = ctx.getContentResolver().query(localAppDB, null, null, null, null);

            //8 apps per device...
            for (int j=0; j < 8; j++) {

                //first 4 copy real apps
                if (j < 4) {
                    //move cursor...
                    //just assume there are a dozen apps on the test system...
                    c.moveToPosition(i+j);    //start at i+j offset

                    ObjectDetail app = new ObjectDetail();

                    int labelIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL);
                    int pkgIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG);
                    int verIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_VER);
                    int serialIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN);
                    int dateIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_DATE);
                    int flagsIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_FLAGS);
                    int bannerIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_BANNER);
                    int typeIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_TYPE);


                    app.label = c.getString(labelIndex);
                    app.pkg = c.getString(pkgIndex);
                    app.ver = c.getString(verIndex);
                    app.serial = c.getString(serialIndex);
                    app.installDate = c.getLong(dateIndex);
                    app.flags = c.getLong(flagsIndex);
                    app.type = c.getLong(typeIndex);

                    //deal with bitmap...
                    byte[] blob = c.getBlob(bannerIndex);
                    if (blob != null) {
                        //FIXME - can't get a context for scaling. Should we just make everything a bitmap and not drawable?
                        //OR, leave this null if package available on play store and download...
                        app.banner = new BitmapDrawable(Utils.convertByteArrayToBitmap(blob));
                    }

                    //now write it out with fake device sn
                    Uri fakeOutUri = fakeappDB.buildUpon().appendPath(app.pkg).build();
                    Cursor cout = ctx.getContentResolver().query(fakeOutUri, null, null, null, null);
                    contentValues.clear();

                    long ostype = Utils.bIsThisATV(ctx) ? AppContract.TYPE_ATV : AppContract.TYPE_TABLET;
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_LABEL, app.label);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_PKG, app.pkg);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_VER, app.ver);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_DEVSSN, fakedevice.get(i));
                    contentValues.put(AppContract.AppEntry.COLUMN_DATE, app.installDate);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_TYPE, ostype);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_FLAGS, app.flags);

                    //now blob... - COLUMN_APP_BANNER
                    //convert drawable to bytestream
                    Bitmap bitmap = SyncUtils.drawableToBitmap(app.banner);           //convert drawable to bitmap
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] imageInByte = stream.toByteArray();
                    //And now into contentValues
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_BANNER, imageInByte);


                    if (cout.getCount() > 0) {
                        //replace
                        ctx.getContentResolver().update(fakeOutUri, contentValues, null, null);
                    } else {
                        //add
                        ctx.getContentResolver().insert(fakeappDB, contentValues);
                    }
                    cout.close();
                } else {
                    //Now insert 4 fake apps
                    ObjectDetail app = new ObjectDetail();

                    app.label = "FakeApp_" + (i+j);
                    app.pkg = "com.fake.rclark" + (i+j);
                    app.ver = "dummyver";
                    app.serial = fakedevice.get(i);
                    app.installDate = System.currentTimeMillis();   //right now!
                    app.flags = 0;
                    app.type = AppContract.TYPE_ATV;
                    //now write it out with fake device sn
                    Uri fakeOutUri = fakeappDB.buildUpon().appendPath(app.pkg).build();
                    Cursor cout = ctx.getContentResolver().query(fakeOutUri, null, null, null, null);
                    contentValues.clear();

                    contentValues.put(AppContract.AppEntry.COLUMN_APP_LABEL, app.label);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_PKG, app.pkg);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_VER, app.ver);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_DEVSSN, fakedevice.get(i));
                    contentValues.put(AppContract.AppEntry.COLUMN_DATE, app.installDate);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_TYPE, app.type);
                    contentValues.put(AppContract.AppEntry.COLUMN_APP_FLAGS, app.flags);

                    if (cout.getCount() > 0) {
                        //replace
                        ctx.getContentResolver().update(fakeOutUri, contentValues, null, null);
                    } else {
                        //add
                        ctx.getContentResolver().insert(fakeappDB, contentValues);
                    }
                    cout.close();

                }

            }
            c.close();
        }
    }

}
