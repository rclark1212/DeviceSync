package com.example.rclark.devicesync.ATVUI;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.view.View;

import com.example.rclark.devicesync.DBUtils;
import com.example.rclark.devicesync.R;

/**
 * Created by rclark on 4/16/2016.
 * Simple class we use as an observer for our CP changing.
 * Most changes auto-handled by using sync adapters for the UI
 * There are some controls though (title text), which need updating when backing db changes
 */
public class AppObserver extends ContentObserver {

    private ContentObserverCallback contentObserverCallback;

    public AppObserver(ContentObserverCallback contentObserverCallback) {
        // null is fine here
        super(null);
        this.contentObserverCallback = contentObserverCallback;
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        //send a message to update the UI (or anything else that needs updating)
        contentObserverCallback.updateFromCP(uri);
    }
}
