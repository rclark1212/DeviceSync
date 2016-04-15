package com.example.rclark.devicesync.ATVUI;

import android.database.Cursor;
import android.support.v17.leanback.database.CursorMapper;

import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.data.AppContract;

/**
 * Created by rclark on 4/12/16.
 */
public class DeviceCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int serialIndex;
    private static int nameIndex;
    private static int modelIndex;
    private static int osverIndex;
    private static int dateIndex;
    private static int typeIndex;
    private static int locationIndex;
    private static int flagIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(AppContract.DevicesEntry._ID);
        serialIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICES_SSN);
        nameIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_NAME);
        modelIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_MODEL);
        osverIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_OSVER);
        dateIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DATE);
        typeIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_TYPE);
        locationIndex = cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_LOCATION);
    }

    @Override
    protected Object bind(Cursor cursor) {

        ObjectDetail device = new ObjectDetail();

        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        device.serial = cursor.getString(serialIndex);
        device.label = cursor.getString(nameIndex);
        device.name = cursor.getString(modelIndex);
        device.ver = cursor.getString(osverIndex);
        device.location = cursor.getString(locationIndex);

        device.installDate = cursor.getLong(dateIndex);
        device.type = cursor.getLong(typeIndex);

        //deal with some device specific items...
        device.banner = null;       //will be picked up from resource and type
        device.bIsDevice = true;    //this is a device
        return device;
    }

}
