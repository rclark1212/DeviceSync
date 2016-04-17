package com.example.rclark.devicesync;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;

import com.example.rclark.devicesync.data.AppContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rclark on 3/27/2016.
 * A class of routines that the UI uses to setup based on the CP
 * Sets up headers as well as cursors for the UI
 */
public class UIDataSetup {
    private static final String TAG = "DS_UIDataSetup";

    private String[] mHeaders;          // base header strings
    private int[] mFunction;            //function per header
    private ArrayList<String> mRemotes; //Remotes can be variable size - this supplements above
    private boolean[] mHide;            //do we hide the row? (user config) - TODO
    private Context mCtx;
    public ArrayList<ObjectDetail> mUnique;     //array list of unique apps on this device
    public ArrayList<ObjectDetail> mMissing;    //array list of missing apps on this device

    // The defined row functions we support. Do not edit below. If you want to hide, change description, re-order, please modify the array.xml file
    // If you want to add a function, add an ordinal define below and make sure array.xml matches
    private final static int DEVICES_ROW = 0;
    private final static int LOCALAPPS_ROW = 1;
    private final static int FLAGGEDAPPS_ROW = 2;
    private final static int MISSINGAPPS_ROW = 3;
    private final static int UNIQUEAPPS_ROW = 4;
    private final static int SUPERSET_ROW = 5;
    private final static int REMOTESTART_ROW = 6;

    public UIDataSetup(Context ctx) {
        mCtx = ctx;
        loadDataFromResources();

        //Ugg - can't use cursor adapters for all. Need arrays...
        if (mUnique == null) {
            mUnique = new ArrayList<ObjectDetail>();
        }
        if (mMissing == null) {
            mMissing = new ArrayList<ObjectDetail>();
        }
    }

    /**
     * Loads the header raw data from the resource file.
     * Decorates that row data (for example appending SSN)
     * And then initializes the remote headers.
     */
    private void loadDataFromResources() {
        //read the data in the order set in xml. We will return proper order on the fetch routines
        mFunction = mCtx.getResources().getIntArray(R.array.header_function);
        mHeaders = mCtx.getResources().getStringArray(R.array.header_names);
        mHide = new boolean[mFunction.length];
        mRemotes = new ArrayList<String>();

        decorateRowHeaders();

        loadRemotes();
    }

    /**
     * Adds any text fixup to the headers that is needed
     */
    private void decorateRowHeaders() {

        // loop through the headers
        for (int i = 0; i < mHeaders.length; i++) {
            if (mFunction[i] == LOCALAPPS_ROW) {
                //Append serial number
                mHeaders[i] = mHeaders[i] + " (" + Build.SERIAL + ")";
            }
            //any more to do?
        }
    }

    /**
     * Reads the CP to load up all the unique remotes
     */
    private void loadRemotes() {
        //TODO - read CP and populate mRemotes
    }

    /**
     *      NOW THE PUBLIC METHODS
     */

    /**
     * Returns number of rows
     * @return
     */
    public int getNumberOfHeaders() {
        //TODO - fix up for REMOTES (hide ATV issue here?)
        return mHeaders.length;
    }

    /**
     *  Returns rows in the order which was set within array.xml ordering
     */
    public String getRowHeader(int row) {
        return mHeaders[row];
    }

    /**
     * Is this a device row?
     */
    public boolean isDeviceRow(int row) {
        if (mFunction[row] == DEVICES_ROW) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get "MISSING" row
     */
    public int getMissingRow() {
        int i;
        for (i = 0; i < mFunction.length; i++) {
            if (mFunction[i] == MISSINGAPPS_ROW) break;
        }
        return i;
    }

    /**
     * Get "UNIQUE" row
     */
    public int getUniqueRow() {
        int i;
        for (i = 0; i < mFunction.length; i++) {
            if (mFunction[i] == UNIQUEAPPS_ROW) break;
        }
        return i;
    }


    /**
     *  Return back an array of unique apps for the device
     */
    private ArrayList<ObjectDetail> getUnique() {
        //clear the array
        mUnique.clear();

        //Set up the query
        Uri appDB = AppContract.AppEntry.CONTENT_URI;

        //set up a local app query
        Uri localApp = appDB.buildUpon().appendPath(Build.SERIAL).build();

        //grab the cursor
        Cursor c = mCtx.getContentResolver().query(localApp, null, null, null, null);

        if (c.getCount() > 0) {
            //Okay - we have a cursor with all the local apps.

            //Loop through the app names
            for (int i = 0; i < c.getCount(); i++) {
                //move to position
                c.moveToPosition(i);

                //get the app name
                String appname = c.getString(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG));

                //Now query full database
                String selection = AppContract.AppEntry.COLUMN_APP_PKG + " = ? ";
                String[] selectionArgs = {appname};

                //grab the cursor (and sort by app label)
                Cursor c_all = mCtx.getContentResolver().query(appDB, null, selection, selectionArgs, AppContract.AppEntry.COLUMN_APP_LABEL + " ASC");

                //and is there only one?
                if (c_all.getCount() < 2) {
                    //Okay - we just found a unique app. Yay!
                    //For simplicity sake, grab the object with a utility
                    Uri getApp = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(appname).build();
                    ObjectDetail unique = DBUtils.getObjectFromCP(mCtx, getApp);
                    mUnique.add(unique);
                }
                c_all.close();
            }
        }

        c.close();

        return mUnique;
    }

    /**
     *  Return back an array of unique apps for the device
     */
    private ArrayList<ObjectDetail> getMissing() {
        mMissing.clear();

        //Set up the query
        Uri appDB = AppContract.AppEntry.CONTENT_URI;
        //Use a groupby for outer loop to prevent dup apps
        Uri groupByApp = AppContract.AppEntry.GROUPBY_URI;
        groupByApp = groupByApp.buildUpon().appendPath(AppContract.AppEntry.COLUMN_APP_PKG).build();

        //set up a query for apps that don't exist locally
        String selection = AppContract.AppEntry.COLUMN_APP_DEVSSN + " != ? ";
        String selectionArgs[] = {Build.SERIAL};

        //grab the cursor
        Cursor c = mCtx.getContentResolver().query(groupByApp, null, selection, selectionArgs, AppContract.AppEntry.COLUMN_APP_LABEL + " ASC");

        if (c.getCount() > 0) {
            //Okay - we have a cursor with all the remote apps.

            //Loop through the app names
            for (int i = 0; i < c.getCount(); i++) {
                //move to position
                c.moveToPosition(i);

                //get the app name
                String appname = c.getString(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG));

                //Now query local device database
                Uri localApp = appDB.buildUpon().appendPath(Build.SERIAL).appendPath(appname).build();

                //grab the local cursor (and sort by app label)
                Cursor c_local = mCtx.getContentResolver().query(localApp, null, null, null, null);

                //and is there only one?
                if (c_local.getCount() < 1) {
                    //Okay - we just found a missing app. Yay!
                    //For simplicity sake, grab the object with a utility
                    String serial = c.getString(c.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN));
                    Uri getApp = appDB.buildUpon().appendPath(serial).appendPath(appname).build();
                    ObjectDetail unique = DBUtils.getObjectFromCP(mCtx, getApp);
                    mMissing.add(unique);
                }
                c_local.close();
            }
        }

        c.close();

        return mMissing;
    }



    /**
     *  Some of the queries are too advanced for a simple SQL query.
     *  So our UI actually uses a mix of cursor loaders and array adapters (original plan was to do
     *  everything with cursor adapters but oh well - perhaps an SQL genius can create the ultimate
     *  query strings here).
     */
    /**
     * This routine indicates if the row should be satisfied with a cursor or a backing array
     * @param row
     * @return
     */
    public boolean useArrayAdapter(int row) {
        boolean bret = false;

        //Okay - two rows we will use array objects for. Missing apps and unique apps
        if (mFunction[row] == UNIQUEAPPS_ROW) {
            bret = true;
        } else if (mFunction[row] == MISSINGAPPS_ROW) {
            bret = true;
        }

        return bret;
    }

    /**
     * This routine returns back a backing array for a browse row when not using a cursor.
     */
    public ArrayList<ObjectDetail> getArrayAdapter(int row) {

        //Okay - two rows we will use array objects for. Missing apps and unique apps
        if (mFunction[row] == UNIQUEAPPS_ROW) {
            return getUnique();
        } else if (mFunction[row] == MISSINGAPPS_ROW) {
            return getMissing();
        }

        return null;
    }

    /**
     *  URI Query Logic for cursor...
     *  If getRowUri returns non-null, then also call getSelection and getSelectionArgs to return
     *  back supplemental information for the standard query.
     *  Note that we use some fake alias Uris here to get access to group by, having and count functions
     */

    /**
     * Critical routine #1 - return the Uri for the row
     * This controls what the row shows from CP (along with selectionString, selectionArgs)
     * Returns Uri to use for this row
     */
    public Uri getRowUri(int row) {
        //Okay - this routine a little less trivial.
        //Have to match up the function of this row to a cursor.
        Uri retUri = null;

        if (mFunction[row] == DEVICES_ROW) {
            Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;
            retUri = deviceDB.buildUpon().build();
        } else if (mFunction[row] == FLAGGEDAPPS_ROW)  {
            //Need a groupbyquery. Do the fuglyness in the uri here...
            Uri appDB = AppContract.AppEntry.GROUPBY_URI;
            //embedd the group by column here...
            retUri = appDB.buildUpon().appendPath(AppContract.AppEntry.COLUMN_APP_PKG).build();
        } else if (mFunction[row] == MISSINGAPPS_ROW)  {
            //Need a groupbyquery. Do the fuglyness in the uri here...
            Uri appDB = AppContract.AppEntry.GROUPBY_URI;
            //embedd the group by column here...
            retUri = appDB.buildUpon().appendPath(AppContract.AppEntry.COLUMN_APP_PKG).build();
        } else if (mFunction[row] == UNIQUEAPPS_ROW) {
            //Need a groupbyquery. Do the fuglyness in the uri here...
            Uri appDB = AppContract.AppEntry.GROUPBY_URI;
            //embedd the group by column here...
            retUri = appDB.buildUpon().appendPath(AppContract.AppEntry.COLUMN_APP_PKG).build();
        } else if (mFunction[row] == SUPERSET_ROW)  {
            //Need a groupbyquery. Do the fuglyness in the uri here...
            Uri appDB = AppContract.AppEntry.GROUPBY_URI;
            //embedd the group by column here...
            retUri = appDB.buildUpon().appendPath(AppContract.AppEntry.COLUMN_APP_PKG).build();
        } else if (mFunction[row] == REMOTESTART_ROW)  {
            retUri = null;  //nothing for now
        } else {
            //everything else is app database
            Uri appDB = AppContract.AppEntry.CONTENT_URI;
            retUri = appDB.buildUpon().appendPath(Build.SERIAL).build();
        }
        return retUri;
    }

    /**
     * Critical routine #2 - return the selectionString for the row
     * This controls what the row shows from CP (along with Uri, selectionArgs)
     * And the ugly language reference is here: https://www.sqlite.org/lang_select.html
     * Or see http://www.w3schools.com/sql/sql_having.asp
     */
    public String getRowSelection(int row) {
        String selection = null;

        if (mFunction[row] == FLAGGEDAPPS_ROW) {
            //Selection should be for unique app names, across all devices that have a flag setting != FLAG_NO_ACTION
            //I think this should be written as:
            //SELECT flag != ? {FLAG_NO_ACTION} FROM app_table GROUP BY pkgname
            selection = AppContract.AppEntry.COLUMN_APP_FLAGS + " != ? ";
        } else if (mFunction[row] == MISSINGAPPS_ROW) {
            //Selection should be for unique missing apps from this device
            //I think this should be written as:
            //SELECT device != ? {serial} FROM app_table GROUP BY pkgname
            //Nope - if you have duplicate apps, the app from non-local serial number will be picked up.
            //Think we have to do a fugly compound SELECT statement of all serial numbers *except* local
            //OR, per docs, if having clause is false, a group is discarded. So can do having localserial number, should discard a group
            //see http://www.w3schools.com/sql/sql_having.asp
            selection = AppContract.AppEntry.COLUMN_APP_DEVSSN + " != ? ";
        } else if (mFunction[row] == UNIQUEAPPS_ROW) {
            //Selection should be for apps that only exist on this device (and not others)
            //I think this should be written as:
            //tough one - need to think about it...
            //Really needs to be a device\app search that only has one entry...
            //Can be done with the having command - look for all apps, group them and then do a having just for this device...
            //Also can use COUNT function (COUNT of 1 w/local device)
            //And FIXME - fix the ordering. Put in a standard ordering for ordering by label
        } else if (mFunction[row] == SUPERSET_ROW) {
            //Selection should be for all unique apps
            //I think this should be written as:
            //SELECT * FROM app_table GROUP BY pkgname
            //see http://stackoverflow.com/questions/6127338/sql-mysql-select-distinct-unique-but-return-all-columns
            //null = SELECT *
        }

        return selection;
    }


    /**
     * Critical routine #3 - return the selectionArgs for the row
     * This controls what the row shows from CP (along with Uri, selectionString)
     */
    public String[] getRowSelectionArgs(int row) {
        String[] selectionArgs = null;

        if (mFunction[row] == FLAGGEDAPPS_ROW) {
            selectionArgs = new String[1];
            selectionArgs[0] = String.valueOf(AppContract.AppEntry.FLAG_NO_ACTION);
        } else if (mFunction[row] == MISSINGAPPS_ROW) {
            selectionArgs = new String[1];
            selectionArgs[0] = Build.SERIAL;
        } else if (mFunction[row] == UNIQUEAPPS_ROW) {
        } else if (mFunction[row] == SUPERSET_ROW) {
        }

        return selectionArgs;
    }

}
