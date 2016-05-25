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

package com.prod.rclark.devicesync.ATVUI;

import android.database.Cursor;
import android.support.v17.leanback.database.CursorMapper;

import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.data.AppContract;

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
