package com.example.rclark.devicesync.ATVUI;

import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.support.v17.leanback.database.CursorMapper;

import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.Utils;
import com.example.rclark.devicesync.data.AppContract;

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
        bannerIndex = cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_BANNER);
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

        //deal with bitmap...
        byte[] blob = cursor.getBlob(bannerIndex);
        if (blob != null) {
            //FIXME - can't get a context for scaling. Should we just make everything a bitmap and not drawable?
            //OR, leave this null if package available on play store and download...
            app.banner = new BitmapDrawable(Utils.convertByteArrayToBitmap(blob));
        }

        app.bIsDevice = false;  //app, not device

        return app;
    }

}
