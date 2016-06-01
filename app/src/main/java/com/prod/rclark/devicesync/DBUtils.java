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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.prod.rclark.devicesync.data.AppContract;

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
        boolean bApp = false;

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
            c.close();
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
            int timestampIndex = c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_TIMEUPDATED);


            returnObject.label = c.getString(labelIndex);
            returnObject.pkg = c.getString(pkgIndex);
            returnObject.ver = c.getString(verIndex);
            returnObject.serial = c.getString(serialIndex);
            returnObject.installDate = c.getLong(dateIndex);
            returnObject.flags = c.getLong(flagsIndex);
            returnObject.type = c.getLong(typeIndex);
            returnObject.timestamp = c.getLong(timestampIndex);

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
            int timestampIndex = c.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_TIMEUPDATED);

            returnObject.serial = c.getString(serialIndex);
            returnObject.label = c.getString(nameIndex);
            returnObject.name = c.getString(modelIndex);
            returnObject.ver = c.getString(osverIndex);
            returnObject.location = c.getString(locationIndex);

            returnObject.timestamp = c.getLong(timestampIndex);
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
     * Saves an ObjectDetail App to the CP based on the URI.
     * Used by broadcast receiver for apps
     */
    public static void saveAppToCP(Context ctx, Uri uri, ObjectDetail app) {
        boolean bExists = false;

        //First are we loading an app or a device object?
        String type = ctx.getContentResolver().getType(uri);

        if (type != AppContract.AppEntry.CONTENT_ITEM_TYPE) {
            //bad uri - return
            return;
        }

        //create the buffer
        ContentValues contentValues = new ContentValues();

        //grab the cursor
        Cursor c = ctx.getContentResolver().query(uri, null, null, null, null);

        if (c.getCount() > 0) {
            //exists!!!
            c.moveToFirst();
            bExists = true;
        }

        app.timestamp = System.currentTimeMillis();

        bindAppToContentValues(app, contentValues, ctx);

        if (bExists) {
            //update
            ctx.getContentResolver().update(uri, contentValues, null, null);
        } else {
            //add
            //create insert uri here
            Uri insertUri = AppContract.AppEntry.CONTENT_URI;
            insertUri = insertUri.buildUpon().appendPath(app.serial).build();
            ctx.getContentResolver().insert(insertUri, contentValues);
        }

        c.close();
    }

    /**
     *  Binds an app object detail to a content values object
     */
    public static void bindAppToContentValues(ObjectDetail app, ContentValues contentValues, Context ctx) {

        contentValues.put(AppContract.AppEntry.COLUMN_APP_LABEL, app.label);
        contentValues.put(AppContract.AppEntry.COLUMN_APP_PKG, app.pkg);
        contentValues.put(AppContract.AppEntry.COLUMN_APP_VER, app.ver);
        contentValues.put(AppContract.AppEntry.COLUMN_APP_DEVSSN, app.serial);
        contentValues.put(AppContract.AppEntry.COLUMN_DATE, app.installDate);
        contentValues.put(AppContract.AppEntry.COLUMN_APP_TYPE, app.type);
        contentValues.put(AppContract.AppEntry.COLUMN_APP_FLAGS, app.flags);
        contentValues.put(AppContract.AppEntry.COLUMN_APP_TIMEUPDATED, app.timestamp);

        //now blob... - COLUMN_APP_BANNER
        //First, is this image available on network?
        if (Utils.getAppImageUriOnNetwork(app.pkg, ctx) == null) {
            //nope - not available. So save off

            //convert drawable to bytestream
            Bitmap bitmap = Utils.drawableToBitmap(app.banner);           //convert drawable to bitmap
            if (bitmap != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageInByte = stream.toByteArray();
                //And now into contentValues
                contentValues.put(AppContract.AppEntry.COLUMN_APP_BANNER, imageInByte);
            }
        }
    }


    /**
     * Check to see if the passed in object is local or remote
     * @param ctx
     * @param object
     * @return
     */
    public static boolean isObjectLocal(Context ctx, ObjectDetail object) {
        Boolean bLocal = false;

        //check serial number first - a lot cheaper than CP.
        if (object.serial.equals(Build.SERIAL)) {
            bLocal = true;
        } else if (!object.bIsDevice) {
            if (DBUtils.isAppLocal(ctx, object.pkg)) {
                bLocal = true;
            }
        }
        return bLocal;
    }

    /**
     *  Routine to indicate if device, app combo is local
     *  (groupby won't always tell you)
     */
    public static boolean isAppLocal(Context ctx, String app) {

        boolean bret = false;
        Uri appDB = AppContract.AppEntry.CONTENT_URI;
        //build up the local device query
        appDB = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(app).build();

        //grab the cursor
        Cursor c = ctx.getContentResolver().query(appDB, null, null, null, null);

        if (c.getCount() > 0) {
            bret = true;
        }

        c.close();
        return bret;
    }

    /**
     *  Routine to count number of apps in database
     *  if app = null, count all unique apps
     */
    public static int countApp(Context ctx, String app) {

        int count = 0;
        if (app != null) {
            Uri appDB = AppContract.AppEntry.CONTENT_URI;

            //Build up the selection string
            String selection = AppContract.AppEntry.COLUMN_APP_PKG + " = ? ";
            String selectionArgs[] = {app};

            //grab the cursor
            Cursor c = ctx.getContentResolver().query(appDB, null, selection, selectionArgs, null);

            count = c.getCount();

            c.close();
        } else {
            Uri appDB = AppContract.AppEntry.GROUPBY_URI;
            //embedd the group by column here... (using our special groupby uri query)
            appDB = appDB.buildUpon().appendPath(AppContract.AppEntry.COLUMN_APP_PKG).build();

            //grab the cursor
            Cursor c = ctx.getContentResolver().query(appDB, null, null, null, null);

            count = c.getCount();

            c.close();
        }
        return count;
    }

    /**
     *  Routine to count number of devices in database
     */
    public static int countDevices(Context ctx) {

        int count = 0;
        Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;

        //grab the cursor
        Cursor c = ctx.getContentResolver().query(deviceDB, null, null, null, null);

        count = c.getCount();

        c.close();

        return count;
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
        Log.d(TAG, "Starting fake update population");

        ArrayList<String> fakedevice = new ArrayList<String>();

        Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;
        Uri localdeviceDB = deviceDB.buildUpon().appendPath(Build.SERIAL).build();


        ObjectDetail device = getObjectFromCP(ctx, localdeviceDB);
        if (device == null) return;     //so on fresh installs, might have to rnu twice to get fake data.
                                        //populating CP is done on a intent service. This call done on UI thread.
                                        //thus a potential sync issue.
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

            //Log.d(TAG, "device query - uri:" + deviceSearchUri.toString());
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
                    //First, is this image available on network?
                    if (Utils.getAppImageUriOnNetwork(app.pkg, ctx) == null) {
                        //nope - not available. So save off

                        //convert drawable to bytestream
                        Bitmap bitmap = Utils.drawableToBitmap(app.banner);           //convert drawable to bitmap
                        if (bitmap != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            byte[] imageInByte = stream.toByteArray();
                            //And now into contentValues
                            contentValues.put(AppContract.AppEntry.COLUMN_APP_BANNER, imageInByte);
                        }
                    }

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
        Log.d(TAG, "Complete fake update population");

    }

}
