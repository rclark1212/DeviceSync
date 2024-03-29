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

package com.prod.rclark.devicesync.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.data.AppContract.DevicesEntry;
import com.prod.rclark.devicesync.data.AppContract.AppEntry;
import com.prod.rclark.devicesync.data.AppContract.ImageEntry;

/**
 * Created by rclar on 3/27/2016.
 */
public class AppDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "devices_apps.db";

    public AppDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold devices.  A device contains a SSN, model type, model nickname
        final String SQL_CREATE_DEVICES_TABLE = "CREATE TABLE " + DevicesEntry.TABLE_NAME + " (" +
                DevicesEntry._ID + " INTEGER PRIMARY KEY, " +
                DevicesEntry.COLUMN_DEVICES_SSN + " TEXT UNIQUE NOT NULL, " +
                DevicesEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                DevicesEntry.COLUMN_DEVICE_MODEL + " TEXT NOT NULL, " +
                DevicesEntry.COLUMN_DEVICE_NAME + " TEXT NOT NULL, " +
                DevicesEntry.COLUMN_DEVICE_OSVER + " TEXT NOT NULL, " +
                DevicesEntry.COLUMN_DEVICE_TYPE + " INTEGER NOT NULL, " +
                DevicesEntry.COLUMN_DEVICE_TIMEUPDATED + " INTEGER NOT NULL, " +
                DevicesEntry.COLUMN_DEVICE_LOCATION + " TEXT NOT NULL " +
                " );";

        // Create a table to hold apps.
        // Note - these are not relational. There can be many to many mappings between so just store as seperate
        // (note that we store apps as app label *and* device ssn)
        final String SQL_CREATE_APPS_TABLE = "CREATE TABLE " + AppEntry.TABLE_NAME + " (" +
                AppEntry._ID + " INTEGER PRIMARY KEY, " +
                AppEntry.COLUMN_APP_LABEL + " TEXT NOT NULL, " +
                AppEntry.COLUMN_APP_PKG + " TEXT NOT NULL, " +
                AppEntry.COLUMN_APP_FLAGS + " INTEGER NOT NULL, " +
                AppEntry.COLUMN_DATE + " INTEGER NOT NULL, " +
                AppEntry.COLUMN_APP_VER + " TEXT NOT NULL, " +
                AppEntry.COLUMN_APP_TYPE + " INTEGER NOT NULL, " +
                AppEntry.COLUMN_APP_TIMEUPDATED + " INTEGER NOT NULL, " +
                AppEntry.COLUMN_APP_DEVSSN + " TEXT NOT NULL " +
                " );";

        // Create a table to hold images.
        // Note - these are not relational. There can be many to many mappings between so just store as seperate
        // (note that we store apps as app label *and* device ssn)
        final String SQL_CREATE_IMG_TABLE = "CREATE TABLE " + ImageEntry.TABLE_NAME + " (" +
                ImageEntry._ID + " INTEGER PRIMARY KEY, " +
                ImageEntry.COLUMN_IMG_APKNAME + " TEXT NOT NULL, " +
                ImageEntry.COLUMN_IMG_STRIPNAME + " TEXT NOT NULL, " +
                ImageEntry.COLUMN_IMG_FILENAME + " TEXT NOT NULL, " +
                ImageEntry.COLUMN_IMG_URL + " TEXT " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_DEVICES_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_APPS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_IMG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // Note that this only fires if you change the version number for your database.
        // It does NOT depend on the version number for your application.
        // If you want to update the schema without wiping data, commenting out the next 2 lines
        // should be your top priority before modifying this method.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + DevicesEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AppEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + ImageEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
