package com.example.rclark.devicesync;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by rclark on 4/13/2016.
 * Static utility class used to install/uninstall apps through the package manager
 * (through intents)
 */
public class InstallUtil {

    private final static String INSTALL_PREFIX = "market://details?id=";
    private final static String UNINSTALL_PREFIX = "package:";
    private InstallUtil() {};

    /**
     * Installs an APK from google play store
     * @param apk
     */
    public static void installAPK(Context ctx, String apk) {
        //TODO - implement
        Utils.showToast(ctx, "Installing " + apk);

        String install = INSTALL_PREFIX + apk;
        Intent goToMarket = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse(install));
        ctx.startActivity(goToMarket);

        //FIXME - implement broadcast receiver to catch and update CP.
    }

    /**
     * Uninstall an APK from device
     * @param apk
     */
    public static void uninstallAPK(Context ctx, String apk) {
        //TODO -  implement
        Utils.showToast(ctx, "UNinstalling " + apk);
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse(UNINSTALL_PREFIX + apk));
        ctx.startActivity(intent);

        //FIXME - implement broadcast receiver to catch and update CP.
    }
}
