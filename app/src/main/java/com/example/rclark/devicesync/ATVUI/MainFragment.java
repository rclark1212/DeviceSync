/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.rclark.devicesync.ATVUI;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.database.CursorMapper;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rclark.devicesync.DBUtils;
import com.example.rclark.devicesync.UIDataSetup;
import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.Utils;
import com.example.rclark.devicesync.data.AppContract;
import com.example.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;
import java.util.List;


public class MainFragment extends BrowseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ContentObserverCallback, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "MainFragment";
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 1;

    private static final int PREFERENCE_REQUEST_CODE = 1;
    public static final String PREF_RESULT_KEY = "prefResult";
    public static final int PREF_DO_NOTHING = 0;
    public static final int PREF_UPDATE_UI_FLAG = 1;
    public static final int PREF_UPDATE_CP_FLAG = 2;

    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayList<ListRow> mBackingListRows;

    private AppObserver mAppObserver;
    private boolean mbAllowUpdates = false;         //semaphore used to block updates from the initial CP flurry of loads...

    private UIDataSetup mUIDataSetup;

    //We use broadcast intents to message back from GCESync to the main activity.
    //Define our handler for this here.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int status = intent.getIntExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.EXTENDED_DATA_STATUS_NULL);
            if (status == GCESync.EXTENDED_DATA_STATUS_LOCALUPDATECOMPLETE) {
                //Okay - done with loading local data!

                //set up some fake data - hack - FIXME remove later
                //NOTE - causes a delay at start
                //DBUtils.loadFakeData(getActivity());

                //And finish up the initialization we started in onActivityCreated (if we punt on initial setup in onActivityCreated)
                if (mUIDataSetup == null) {
                    mUIDataSetup = new UIDataSetup(getActivity());

                    loadRows();

                    setupEventListeners();
                }

                //Enable updates once initial CP load complete
                mbAllowUpdates = true;

                //And force an update
                updateFromCP(null);
            }

            Log.d("DS_mainfrag_receiver", "Got status: " + status);
        }
    };

    /**
     * Okay - on first install, need to ask for permission for location. By that time, CP already updated.
     * Fix that here by adding location to the device's record...
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (MY_PERMISSIONS_REQUEST_LOCATION == requestCode) {
            // Is there a result array? (should be if okay chosen)
            if ((grantResults.length > 0)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Yay! We get location. Go ahead an update the device record...
                Log.d(TAG, "location priviledge granted. Updating CP");
                GCESync.startActionLocalDeviceUpdate(getActivity(), null, null);
            } else {
                Log.d(TAG, "location priviledge DENIED. Grr...");
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        //Make sure we have location permissions at start
        // Assume thisActivity is the current activity
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
                //and only need to do this for SDK23...
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, MY_PERMISSIONS_REQUEST_LOCATION);
                }
            }
        }

        setupUIElements();

        //Update the local content provider if running for first time...
        //FIXME - remember to add a force sync button to settings page
        if (Utils.isRunningForFirstTime(getActivity())) {
            GCESync.startActionUpdateLocal(getActivity(), null, null);
            //and init the preferences
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        } else {
            //Note an optimization we made. We block processing of the notify callback from CP until the sync adapter
            //has had a chance to complete on the initial run. The sync service will send a message if this is first run.
            //If it is not first run, enable updates here
            mbAllowUpdates = true;
            //and note that we always want to update the local device (pick up new location and pick up any BT name changes)
            GCESync.startActionLocalDeviceUpdate(getActivity(), null, null);
        }

        //Load the UI structure
        mUIDataSetup = new UIDataSetup(getActivity());

        loadRows();

        setupEventListeners();

        //and finally, update the title
        String titleformat = getString(R.string.browse_title);
        String title = String.format(titleformat, DBUtils.countDevices(getActivity()), DBUtils.countApp(getActivity(), null));

        setTitle(title);
    }

    @Override
    public void onResume() {
        super.onResume();

        //set up handler and register content observer
        if (mAppObserver == null) {
            mAppObserver = new AppObserver(this);
        }
        getActivity().getContentResolver().registerContentObserver(AppContract.AppEntry.CONTENT_URI,
                true,
                mAppObserver);

        //Register receiver
        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(mMessageReceiver, new IntentFilter(GCESync.BROADCAST_ACTION));

    }

    @Override
    public void onPause() {
        super.onPause();
        // always call unregisterContentObserver in onPause
        getActivity().getContentResolver().unregisterContentObserver(mAppObserver);

        //Unregister receiver
        LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(mMessageReceiver);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        int prefResult = 0;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PREFERENCE_REQUEST_CODE) {
            //So don't really care about result code - cancel or OK, user may have modified settings
            //Instead, look at the data...
            if (data != null) {
                prefResult = data.getIntExtra(PREF_RESULT_KEY, PREF_DO_NOTHING);

                //And now process...
                if ((prefResult & PREF_UPDATE_UI_FLAG) != 0) {
                    Log.d(TAG, "PreferenceRet - update UI");
                    //New strat. And a smoother UI experience.
                    //Simply save off hidden rows and restore them as necessary to the mRowAdapter object.
                    //Don't deallocate or anything (as browsefragment doesn't deal with deallocation well).
                    mUIDataSetup.loadRowEnables();
                    updateRowVisibility();
                }

                if ((prefResult & PREF_UPDATE_CP_FLAG) != 0) {
                    Log.d(TAG, "PreferenceRet - update CP");
                    //And kick off a CP update. Turn off the processing flag for UI until done...
                    mbAllowUpdates = false;
                    GCESync.startActionUpdateLocal(getActivity(), null, null);
                }
            }
        }
    }


    public void updateFromCP(Uri uri) {
        //called when CP changes underneath us
        if (mbAllowUpdates) {
            if (mUIDataSetup != null) {
                if (uri != null) {
                    Log.d("DS_mainfrag_updateFrmCP", "Starting update for " + uri.toString());
                } else {
                    Log.d("DS_mainfrag_updateFrmCP", "Starting update for (no uri)");
                }
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Run this on UI thread.
                    // But not until we have initialized the UI (don't bother before then). Look at mUIDataSetup to tell us if we are initialized.
                    if (mUIDataSetup != null) {
                        //Update the text on title view
                        String titleformat = getString(R.string.browse_title);
                        String title = String.format(titleformat, DBUtils.countDevices(getActivity()), DBUtils.countApp(getActivity(), null));
                        setTitle(title); // Badge, when set, takes precedent

                        //And now, since we are using arrays for the missing apps/unique apps, on a CP change, we have to regenerate. Grr...
                        //Get the rows first...
                        ListRow lr_missing = (ListRow) mRowsAdapter.get(mUIDataSetup.getMissingRow());
                        ListRow lr_unique = (ListRow) mRowsAdapter.get(mUIDataSetup.getUniqueRow());

                        //Now get the adapters
                        ArrayObjectAdapter missing_adapter = (ArrayObjectAdapter) lr_missing.getAdapter();
                        ArrayObjectAdapter unique_adapter = (ArrayObjectAdapter) lr_unique.getAdapter();

                        //Update the backing stores for the missing adapter
                        ArrayList<ObjectDetail> objectArray = mUIDataSetup.getArrayAdapter(mUIDataSetup.getMissingRow());

                        //and if data is valid
                        if (objectArray != null) {
                            //clear the existing adapter...
                            missing_adapter.clear();
                            //add it to the object adapter
                            for (int j = 0; j < objectArray.size(); j++) {
                                missing_adapter.add(objectArray.get(j));
                            }
                        }

                        //Update the backing stores for the unique adapter
                        objectArray = mUIDataSetup.getArrayAdapter(mUIDataSetup.getUniqueRow());

                        //and if data is valid
                        if (objectArray != null) {
                            //clear the existing adapter...
                            unique_adapter.clear();
                            //add it to the object adapter
                            for (int j = 0; j < objectArray.size(); j++) {
                                unique_adapter.add(objectArray.get(j));
                            }
                        }

                        //All done!
                    } else {
                        Log.d("DS_mainfrag_updateFrmCP", "Skip update");
                    }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     *  Loads the browse rows (headers and data).
     */
    private void loadRows() {
        int i;

        //If adapter is null, initialize
        if (mRowsAdapter == null) {
            mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        }

        //If backing list rows null, initialize
        if (mBackingListRows == null) {
            mBackingListRows = new ArrayList<ListRow>();
        }

        CardPresenter cardPresenter = new CardPresenter();

        /*
            For CursorObjectAdapter...
            Need to construct with a presenter.
            Need to provider a mapper (two of them - one for apps, one for devices)
            Need to construct the adapter, implement cursor callbacks
            Then off to races...
         */
        for (i = 0; i < mUIDataSetup.getNumberOfHeaders(); i++) {
            HeaderItem header = new HeaderItem(i, mUIDataSetup.getRowHeader(i));

            //see
            //https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/VideoDetailsFragment.java

            //Sigh - can't do all the queries we want with a cursor and CP. So we will do a mix.
            //Where we can use a CP, we will. Where we can't, we will use an arrayadapter
            if (mUIDataSetup.useArrayAdapter(i)) {
                //set up an array adapter
                ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
                //get the data
                ArrayList<ObjectDetail> objectArray = mUIDataSetup.getArrayAdapter(i);

                //and if data is valid
                if (objectArray != null) {
                    //add it to the object adapter
                    for (int j = 0; j < objectArray.size(); j++) {
                        listRowAdapter.add(objectArray.get(j));
                    }
                }
                //Create the list row and save off both to the row adapter and a backing store
                ListRow lr = new ListRow(header, listRowAdapter);
                mBackingListRows.add(lr);
                if (mUIDataSetup.isHeaderRowVisible(i)) {
                    mRowsAdapter.add(lr);
                }
            } else {
                CursorObjectAdapter listRowAdapter = new CursorObjectAdapter(cardPresenter);

                //construct mapper
                if (mUIDataSetup.isDeviceRow(i)) {
                    DeviceCursorMapper deviceMapper = new DeviceCursorMapper();
                    listRowAdapter.setMapper(deviceMapper);
                } else {
                    CursorMapper appMapper = new AppCursorMapper();
                    listRowAdapter.setMapper(appMapper);
                }
                //set up the cursorobjectadapter - note, we reference cursor loader by row and set up cursor there...
                //set it
                //Create the list row and save off both to the row adapter and a backing store
                ListRow lr = new ListRow(header, listRowAdapter);
                mBackingListRows.add(lr);
                if (mUIDataSetup.isHeaderRowVisible(i)) {
                    mRowsAdapter.add(lr);
                }
            }
        }

        HeaderItem gridHeader = new HeaderItem(i, getResources().getString(R.string.preferences));

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.sync_now));
        gridRowAdapter.add(getString(R.string.error_fragment));
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        setAdapter(mRowsAdapter);

        LoaderManager loaderManager = getLoaderManager();

        //loop through the uris and set up loaders (and yes, could use a bundle to the loader and a local variable instead of global).
        for (i = 0; i < mUIDataSetup.getNumberOfHeaders(); i++) {
            //if uri that comes back is null, don't init a cursor (blank row)
            if (mUIDataSetup.getRowUri(i) != null) {
                loaderManager.initLoader(i, null, this);
            }
        }
    }

    /**
     * Updates the visibility of the rows (removes rows that are now not visible, adds rows that are)
     */
    private void updateRowVisibility() {
        //loop through all the possible rows...
        int rowIndex = 0;
        for (int i = 0; i < mUIDataSetup.getNumberOfHeaders(); i++) {
            if (mUIDataSetup.isHeaderRowVisible(i)) {
                //this row should be visible...
                if (!isHeaderIdInRowAdapter((long)i)) {
                    //doesn't exist in the row adapter... so add it..
                    mRowsAdapter.add(rowIndex, mBackingListRows.get(i));
                }
                rowIndex++;
            } else {
                //this row should be hidden...
                if (isHeaderIdInRowAdapter((long)i)) {
                    //it does exist in row adapter... So remove it...
                    mRowsAdapter.remove( mBackingListRows.get(i));
                }
            }
        }
    }

    /**
     * Returns true if mRowsAdapter contains a header which has the passed in ID
     * @param headerid
     * @return
     */
    private boolean isHeaderIdInRowAdapter(long headerid) {
        boolean bret = false;

        for (int i=0; i < mRowsAdapter.size(); i++) {
            ListRow lr = (ListRow) mRowsAdapter.get(i);

            if (lr.getHeaderItem().getId() == headerid) {
                bret = true;
                break;
            }
        }
        return bret;
    }

    /**
     *  Cursor loader for app object provider
     *  multiple loaders -
     *      We store the loaders by row (row is the loader id)
     *      The loaders search url is stored in mLoaderUri for that row
     *      Not all rows may have cursor adapters - thus we check to make sure adapter is a cursor adapter
     *      when doing an operation (but this should never fail).
     */
    @Override
    public Loader<Cursor> onCreateLoader(int rowid, final Bundle args) {
        String sortorder = null;

        //if it is an app, sort by app label
        if (!mUIDataSetup.isDeviceRow(rowid)) {
            sortorder = AppContract.AppEntry.COLUMN_APP_LABEL + " ASC";
        }

        return new CursorLoader(getActivity(), mUIDataSetup.getRowUri(rowid), null,
                mUIDataSetup.getRowSelection(rowid), mUIDataSetup.getRowSelectionArgs(rowid), sortorder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        //ListRow row = (ListRow) getAdapter().get(loader.getId()); - use backing store
        ListRow row = (ListRow) mBackingListRows.get(loader.getId());
        if (row.getAdapter() instanceof CursorObjectAdapter) {
            CursorObjectAdapter rowAdapter = (CursorObjectAdapter) row.getAdapter();
            rowAdapter.swapCursor(cursor);
        }

        //TODO - note that background app changes are no problem. However, if we change the devices in background...
        //have to regenerate all the rows. At the moment, we ignore this case. But we should fix it. The simple way
        //is to not update devices while app is open (don't use a cursor here). The more complicated way would be to
        //allow updates.
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //ListRow row = (ListRow) getAdapter().get(loader.getId()); - use backing store instead
        ListRow row = (ListRow) mBackingListRows.get(loader.getId());
        if (row.getAdapter() instanceof CursorObjectAdapter) {
            CursorObjectAdapter rowAdapter = (CursorObjectAdapter) row.getAdapter();
            rowAdapter.swapCursor(null);
        }
    }


    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));

        //Update the title at end of initial load...
        //String titleformat = getString(R.string.browse_title);
        //String title = String.format(titleformat, DBUtils.countDevices(getActivity()), DBUtils.countApp(getActivity(), null));

        setTitle(getString(R.string.please_wait)); // Badge, when set, takes precedent
        // over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                        .show();
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        //setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }


    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof ObjectDetail) {
                ObjectDetail element = (ObjectDetail) item;
                Log.d(TAG, "Item: " + item.toString());
                Uri elementUri;

                if (element.bIsDevice) {
                    elementUri = AppContract.DevicesEntry.CONTENT_URI.buildUpon().appendPath(element.serial).build();
                } else {
                    elementUri = AppContract.AppEntry.CONTENT_URI.buildUpon()
                            .appendPath(element.serial).appendPath(element.pkg).build();
                }

                //NOTE - we send the object id to the detail view as a URI, not as a serializable object.
                //Why do we do this? Well, who wants to serialize a drawable in the case of apps that are
                //not on the market... Either is painful slow or will crash with a large bitmap.
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.OBJECTURI, elementUri);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            } else if (item instanceof String) {
                if (((String) item).indexOf(getString(R.string.error_fragment)) >= 0) {
                    Intent intent = new Intent(getActivity(), BrowseErrorActivity.class);
                    startActivity(intent);
                } else if (item.equals(getResources().getString(R.string.personal_settings))) {
                    // Display the settings fragment
                    Intent intent = new Intent();
                    intent.setClass(getActivity(), SettingsActivity.class);
                    //and start for a result so that we can take care of any UI elements that we have to do when we get back...
                    startActivityForResult(intent, PREFERENCE_REQUEST_CODE);
                } else if (item.equals(getResources().getString(R.string.sync_now))) {
                    //Kick off a sync...
                    //but disable updates until it completes
                    mbAllowUpdates = false;
                    GCESync.startActionUpdateLocal(getActivity(), null, null);
                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

}
