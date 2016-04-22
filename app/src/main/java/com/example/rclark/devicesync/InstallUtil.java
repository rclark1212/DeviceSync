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
