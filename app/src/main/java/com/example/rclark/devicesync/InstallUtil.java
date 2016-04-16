package com.example.rclark.devicesync;

import android.content.Context;

/**
 * Created by rclark on 4/13/2016.
 * Static utility class used to install/uninstall apps through the package manager
 * (through intents)
 */
public class InstallUtil {

    private InstallUtil() {};

    /**
     * Installs an APK from google play store
     * @param apk
     */
    public static void installAPK(Context ctx, String apk) {
        //TODO - implement
        Utils.showToast(ctx, "Installing " + apk);
    }

    /**
     * Uninstall an APK from device
     * @param apk
     */
    public static void uninstallAPK(Context ctx, String apk) {
        //TODO -  implement
        Utils.showToast(ctx, "UNinstalling " + apk);
    }
}
