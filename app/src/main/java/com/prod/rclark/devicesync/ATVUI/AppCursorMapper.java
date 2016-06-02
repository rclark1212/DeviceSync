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
import android.graphics.drawable.BitmapDrawable;
import android.support.v17.leanback.database.CursorMapper;

import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

/**
 * Created by rclark on 3/30/2016.
 * Cursor mapper for application SQL database
 */
public final class AppCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int labelIndex;
    private static int pkgIndex;
    private static int verIndex;
    private static int serialIndex;
    private static int dateIndex;
    private static int flagsIndex;
    private static int bannerIndex;
    private static int typeIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(AppContract.AppEntry._ID);
        labelIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL);
        pkgIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG);
        verIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_VER);
        serialIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN);
        dateIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_DATE);
        flagsIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_FLAGS);
        typeIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_TYPE);
    }

    @Override
    protected Object bind(Cursor cursor) {

        ObjectDetail app = new ObjectDetail();

        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        app.label = cursor.getString(labelIndex);
        app.pkg = cursor.getString(pkgIndex);
        app.ver = cursor.getString(verIndex);
        app.serial = cursor.getString(serialIndex);
        app.installDate = cursor.getLong(dateIndex);
        app.flags = cursor.getLong(flagsIndex);
        app.type = cursor.getLong(typeIndex);

        app.bIsDevice = false;  //app, not device

        return app;
    }

}
