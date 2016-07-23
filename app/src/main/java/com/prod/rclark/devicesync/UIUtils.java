package com.prod.rclark.devicesync;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.prod.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;

/**
 * Created by rclark on 6/11/2016.
 * Utility routines which generate UI elements
 */
public class UIUtils {
    private static final String TAG = "DS_UIUtils";

    /*
     * Making sure public utility methods remain static
     */
    private UIUtils() {
    }

    /**
     * Routine to provide back the common description text used in detail for the object
     * AND TODO - we could do a much better job here...
     * @param object
     * @return
     */
    public static String getObjectDetailDescription(Context ctx, ObjectDetail element) {
        String body = "";

        if (element.bIsDevice) {
            body = ctx.getString(R.string.od_serial) + " " + element.serial
                    + "\n" + ctx.getString(R.string.od_location) + " " + element.location
                    + "\n" + ctx.getString(R.string.od_osver) + " " + element.ver
                    + "\n" + ctx.getString(R.string.od_updated) + " " + Utils.unNormalizeDate(ctx, element.installDate);
        } else {
            body = ctx.getString(R.string.od_version) + " " + element.ver
                    + "\n" + ctx.getString(R.string.od_installdate) + " " + Utils.unNormalizeDate(ctx, element.installDate)
                    + "\n" + ctx.getString(R.string.od_count) + " " + DBUtils.countApp(ctx, element.pkg);
        }
        return body;
    }


    /**
     * Used to ask user if they want to download apps found on network for the device (in case of a device wipe)
     */
    public static void askDownloadExistingApps(final Activity activity, final ArrayList<ObjectDetail> missing) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle(activity.getString(R.string.restore_apps_title));

        String msg = String.format(activity.getString(R.string.restore_apps_msg), missing.size());
        alertDialogBuilder
                .setMessage(msg)
                .setCancelable(false)
                .setNeutralButton(activity.getResources().getString(R.string.restore_disable_syncs), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //disable syncs
                        Utils.setSyncDisabled(activity, true);
                        //TODO - note this leaves apps in a weird state. Will show apps as local to device but no option
                        //to install, etc...
                    }
                })
                .setNegativeButton(activity.getResources().getString(R.string.restore_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        GCESync.startActionUpdateLocal(activity, null, null);
                    }
                })
                .setPositiveButton(activity.getResources().getString(R.string.restore_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //build the install list...
                        ArrayList<String> apklist = new ArrayList<String>();
                        for (int i=0; i < missing.size(); i++) {
                            apklist.add(missing.get(i).pkg);
                        }
                        //let the updates go through
                        GCESync.startActionUpdateLocal(activity, null, null);
                        //and kick off the batch install
                        confirmBatchOperation(activity, apklist, true);
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    /**
     * Used to ask user to change BT name
     * Pass in an activity here so on conclusion, we can issue a backpress to update the UI.
     */
    public static void changeBTName(final Activity activity, String name) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle(activity.getString(R.string.setname_title));

        // Set up the input
        final EditText input = new EditText(activity);
        input.setText(name);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialogBuilder.setView(input);

        alertDialogBuilder
                .setPositiveButton(activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //set BT name
                        String name = input.getText().toString();
                        Utils.setLocalDeviceName(activity, name);
                        activity.onBackPressed();
                    }
                })
                .setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
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

    /**
     * Used to confirm with user the batch install/uninstalls before executing.
     * Will call either batch install or batch uninstall
     */
    public static void confirmBatchOperation(final Activity activity, final ArrayList<String> apklist, final boolean bInstall) {

        //first check list - if null, punt.
        if (apklist == null) {
            return;
        }

        if (apklist.size() == 0) {
            return;
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        if (bInstall) {
            alertDialogBuilder.setTitle(activity.getString(R.string.batch_install_title));
        } else {
            alertDialogBuilder.setTitle(activity.getString(R.string.batch_uninstall_title));
        }

        // Set up the input
        final MultiListImageView mosaic = new MultiListImageView(activity);
        //have to set a placeholder before a list...
        mosaic.setPlaceholderImage(R.drawable.batchplaceholder);
        mosaic.setImageFromAPKList(activity, apklist);
        alertDialogBuilder.setView(mosaic);

        String msg = "";
        for (int i=0; i<apklist.size(); i++) {
            msg = msg + apklist.get(i) + ", ";
        }
        if (bInstall) {
            Log.d(TAG, "confirm batch install - " + msg);
        } else {
            Log.d(TAG, "confirm batch uninstall - " + msg);
        }

        alertDialogBuilder
                .setNegativeButton(activity.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(activity.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (bInstall) {
                            InstallUtil.batchInstallAPK(activity, apklist);
                        } else {
                            InstallUtil.batchUninstallAPK(activity, apklist);
                        }
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
