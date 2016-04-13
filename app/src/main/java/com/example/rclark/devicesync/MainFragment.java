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

package com.example.rclark.devicesync;

import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.database.CursorMapper;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.rclark.devicesync.data.AppContract;
import com.example.rclark.devicesync.sync.GCESync;

public class MainFragment extends BrowseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "MainFragment";

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;
    private static final int NUM_ROWS = 6;
    private static final int NUM_COLS = 15;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private URI mBackgroundURI;
    private BackgroundManager mBackgroundManager;

    private ArrayList<String> mRows;        //row headers
    private ArrayList<Uri> mLoaderUris;     //uris to work with per row...


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Log.d(TAG, "onDestroy: " + mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    //Note that we only add/remove device rows. Which are given by serial numbers
    //always append to end...
    private void addRow(String row) {
        //Is this row already present?
        if (!mRows.contains(row)) {
            mRows.add(row);
            CardPresenter cardPresenter = new CardPresenter();
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            int index = mRowsAdapter.size();
            HeaderItem header = new HeaderItem(index, row);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
            mRowsAdapter.notifyArrayItemRangeChanged(index, 1);
        }
    }

    private void removeRow(String row) {
        if (mRows.contains(row)) {
            mRows.remove(row);
            int i;
            for (i = 0; i < mRowsAdapter.size(); i++) {
                ListRow lr = (ListRow) mRowsAdapter.get(i);
                if (lr.getHeaderItem().getName().equals(row)) {
                    //found it - remove
                    mRowsAdapter.removeItems(i, 1);
                    break;
                }
            }
            mRowsAdapter.notifyArrayItemRangeChanged(i, 1);
        }
    }

    private void loadRows() {
        mRows = new ArrayList<String>();
        mLoaderUris = new ArrayList<Uri>();
        Uri appDB = AppContract.AppEntry.CONTENT_URI;
        Uri deviceDB = AppContract.DevicesEntry.CONTENT_URI;
        Uri searchUri;

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();

        //init the rows list...
        AppList.loadRows(mRows);

        /*
            For CursorObjectAdapter...
            Need to construct with a presenter.
            Need to provider a mapper (two of them - one for apps, one for devices)
            Need to construct the adapter, implement cursor callbacks
            Then off to races...
         */
        int i;
        for (i = 0; i < mRows.size(); i++) {
            HeaderItem header = new HeaderItem(i, mRows.get(i));

            //see
            //https://github.com/googlesamples/androidtv-Leanback/blob/master/app/src/main/java/com/example/android/tvleanback/ui/VideoDetailsFragment.java

            CursorObjectAdapter listRowAdapter = new CursorObjectAdapter(cardPresenter);

            //construct mapper
            if (i == AppList.CAT_DEVICES) {
                DeviceCursorMapper deviceMapper = new DeviceCursorMapper();
                listRowAdapter.setMapper(deviceMapper);
            } else {
                CursorMapper appMapper = new AppCursorMapper();
                listRowAdapter.setMapper(appMapper);
            }

            //set up the cursorobjectadapter
            //open a query
            if (i == AppList.CAT_DEVICES) {
                searchUri = deviceDB.buildUpon().build();
            } else if (i == AppList.CAT_LOCAL) {
                searchUri = appDB.buildUpon().appendPath(Build.SERIAL).build();
            } else {
                searchUri = appDB.buildUpon().appendPath(Build.SERIAL).build();
            }

            //save off this uri
            mLoaderUris.add(i, searchUri);

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
        for (i = 0; i < mRows.size(); i++) {
            if (mLoaderUris.get(i) != null) {
                loaderManager.initLoader(i, null, this);
            }
        }

        //And update the local content provider...
        GCESync.startActionUpdateLocal(getActivity(), null, null);

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
    public Loader<Cursor> onCreateLoader(int id, final Bundle args) {
        return new CursorLoader(getActivity(), mLoaderUris.get(id), null, null, null, null);
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

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        // setBadgeDrawable(getActivity().getResources().getDrawable(
        // R.drawable.videos_by_google_banner));
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent
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
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    protected void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);

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

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                   RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Movie) {
                mBackgroundURI = ((Movie) item).getBackgroundImageURI();
                startBackgroundTimer();
            }

        }
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI.toString());
                    }
                }
            });

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
