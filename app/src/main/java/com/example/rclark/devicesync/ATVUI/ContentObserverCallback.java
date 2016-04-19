package com.example.rclark.devicesync.ATVUI;

import android.net.Uri;

/**
 * Created by rclark on 4/16/2016.
 */
public interface ContentObserverCallback {
    void updateFromCP(Uri uri);
}
