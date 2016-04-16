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

package com.example.rclark.devicesync.ATVUI;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rclark.devicesync.DBUtils;
import com.example.rclark.devicesync.UIDataSetup;
import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.data.AppContract;
import com.example.rclark.devicesync.sync.GCESync;


public class MainFragment extends BrowseFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ContentObserverCallback {
    private static final String TAG = "MainFragment";

    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private AppObserver mAppObserver;

    private UIDataSetup mUIDataSetup;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        setupUIElements();

        //Update the local content provider...
        GCESync.startActionUpdateLocal(getActivity(), null, null);

        mUIDataSetup = new UIDataSetup(getActivity());

        loadRows();

        setupEventListeners();

        //and set up fake data - hack - FIXME remove later
        DBUtils.loadFakeData(getActivity());
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

    }

    @Override
    public void onPause() {
        super.onPause();
        // always call unregisterContentObserver in onPause
        getActivity().getContentResolver().unregisterContentObserver(mAppObserver);
    }


    @Override
    public void updateFromCP() {
        //called when CP changes underneath us
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Run this on UI thread.
                //Update the text on title view
                String titleformat = getString(R.string.browse_title);
                String title = String.format(titleformat, DBUtils.countDevices(getActivity()), DBUtils.countApp(getActivity(), null));

                setTitle(title); // Badge, when set, takes precedent
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void loadRows() {
        int i;

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
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
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        HeaderItem gridHeader = new HeaderItem(i, "PREFERENCES");

        GridItemPresenter mGridPresenter = new GridItemPresenter();
        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.grid_view));
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
        ListRow row = (ListRow) getAdapter().get(loader.getId());
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
        ListRow row = (ListRow) getAdapter().get(loader.getId());
        if (row.getAdapter() instanceof CursorObjectAdapter) {
            CursorObjectAdapter rowAdapter = (CursorObjectAdapter) row.getAdapter();
            rowAdapter.swapCursor(null);
        }
    }


    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        String titleformat = getString(R.string.browse_title);
        String title = String.format(titleformat, DBUtils.countDevices(getActivity()), DBUtils.countApp(getActivity(), null));

        setTitle(title); // Badge, when set, takes precedent
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
