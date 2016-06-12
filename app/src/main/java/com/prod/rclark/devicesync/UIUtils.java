package com.prod.rclark.devicesync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.widget.EditText;

import com.prod.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;

/**
 * Created by rclark on 6/11/2016.
 * Utility routines which generate UI elements
 */
public class UIUtils {

    /*
     * Making sure public utility methods remain static
     */
    private UIUtils() {
    }

    /**
     * Routine to provide back the common description text used in detail for the object
     * AND FIXME - we could do a much better job here...
     * @param object
     * @return
     */
    public static String getObjectDetailDescription(Context ctx, ObjectDetail element) {
        String body = "";

        if (element.bIsDevice) {
            body = "Serial: " + element.serial + "\nLocation: " + element.location +
                    "\nOSVer: " + element.ver + "\nUpdated: " + Utils.unNormalizeDate(ctx, element.installDate);
        } else {
            body = "Version: " + element.ver + "\nInstallDate: " + Utils.unNormalizeDate(ctx, element.installDate) + "\nCount: " + DBUtils.countApp(ctx, element.pkg);
        }
        return body;
    }


    /**
     * Used to ask user if they want to download apps found on network for the device (in case of a device wipe)
     */
    public static void askDownloadExistingApps(final Context ctx, final ArrayList<ObjectDetail> missing) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

        alertDialogBuilder.setTitle(ctx.getString(R.string.restore_apps_title));

        String msg = String.format(ctx.getString(R.string.restore_apps_msg), missing.size());
        alertDialogBuilder
                .setMessage(msg)
                .setCancelable(false)
                .setNeutralButton(ctx.getResources().getString(R.string.restore_disable_syncs), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //disable syncs
                        Utils.setSyncDisabled(ctx, true);
                        //FIXME - note this leaves apps in a weird state. Will show apps as local to device but no option
                        //to install, etc...
                    }
                })
                .setNegativeButton(ctx.getResources().getString(R.string.restore_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        GCESync.startActionUpdateLocal(ctx, null, null);
                    }
                })
                .setPositiveButton(ctx.getResources().getString(R.string.restore_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //build the install list...
                        ArrayList<String> apklist = new ArrayList<String>();
                        for (int i=0; i < missing.size(); i++) {
                            apklist.add(missing.get(i).pkg);
                        }
                        //let the updates go through
                        GCESync.startActionUpdateLocal(ctx, null, null);
                        //and kick off the batch install
                        InstallUtil.batchInstallAPK(ctx, apklist);
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    /**
     * Used to ask user to change BT name
     * Pass in an activity here so on conclusion, we can issue a backpress to update the UI.
     */
    public static void changeBTName(final Activity ctx, String name) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);

        alertDialogBuilder.setTitle(ctx.getString(R.string.setname_title));

        // Set up the input
        final EditText input = new EditText(ctx);
        input.setText(name);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialogBuilder.setView(input);

        alertDialogBuilder
                .setPositiveButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //set BT name
                        String name = input.getText().toString();
                        Utils.setLocalDeviceName(ctx, name);
                        ctx.onBackPressed();
                    }
                })
                .setNegativeButton(ctx.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //all done
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Used to exit app when there is an error. Really GMS services the only error that will cause controlled exit :)
     */
    public static void finishIt(final Activity activity) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle(activity.getResources().getString(R.string.app_err_title));

        alertDialogBuilder
                .setMessage(activity.getResources().getString(R.string.gms_missing_msg))
                .setCancelable(false)
                .setNeutralButton(activity.getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        activity.finish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
