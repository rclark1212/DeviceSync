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

package com.prod.rclark.devicesync.ATVUI;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.InstallUtil;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.UIUtils;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class ObjectDetailsFragment extends DetailsFragment {
    private static final String TAG = "VideoDetailsFragment";

    private static final int ACTION_INSTALL = 1;
    private static final int ACTION_UNINSTALL = 2;
    private static final int ACTION_CLONEFROM = 3;
    private static final int ACTION_REMOVEDEVICE = 4;
    private static final int ACTION_SHOWAPPS = 5;
    private static final int ACTION_RUNAPP = 6;
    private static final int ACTION_ADD_MISSING = 7;
    private static final int ACTION_CHANGE_NAME = 8;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private ObjectDetail mSelectedObject;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;

    private Action mActionInstall;
    private Action mActionUninstall;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        prepareBackgroundManager();

        Uri selectedUri = (Uri) getActivity().getIntent().getParcelableExtra(DetailsActivity.OBJECTURI);
        //and now load that Uri into an object...
        mSelectedObject = DBUtils.getObjectFromCP(getActivity(), selectedUri);

        if (mSelectedObject != null) {
            setupAdapter();
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            //setupMovieListRow();
            //setupMovieListRowPresenter();
            updateBackground(mSelectedObject);
            //setOnItemViewClickedListener(new ItemViewClickedListener());
        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    protected void updateBackground(ObjectDetail item) {
        /*
        NOPE - don't do backgrounds on this app
        if (item.banner != null) {
            mBackgroundManager.setDrawable(item.banner);
        }
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(mMetrics.widthPixels, mMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable> glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                }); */
    }

    private void setupAdapter() {
        mPresenterSelector = new ClassPresenterSelector();
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    private void setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedObject.toString());
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedObject);
        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();
        row.setImageDrawable(getResources().getDrawable(R.drawable.default_background));
        int width = Utils.convertDpToPixel(getActivity()
                .getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = Utils.convertDpToPixel(getActivity()
                .getApplicationContext(), DETAIL_THUMB_HEIGHT);

        if (mSelectedObject.bIsDevice) {
            if (mSelectedObject.type == AppContract.TYPE_ATV) {
                row.setImageDrawable(getResources().getDrawable(R.drawable.shieldtv));
            } else {
                row.setImageDrawable(getResources().getDrawable(Utils.getTabletResource(getActivity())));
            }
            //and setup the actions...
            if (DBUtils.isObjectLocal(getActivity(), mSelectedObject)) {
                actionAdapter.add(new Action(ACTION_SHOWAPPS, getResources().getString(R.string.show_apps)));
                actionAdapter.add(new Action(ACTION_ADD_MISSING, getString(R.string.add_missing)));
                actionAdapter.add(new Action(ACTION_CHANGE_NAME, getString(R.string.change_name)));
            } else {
                actionAdapter.add(new Action(ACTION_SHOWAPPS, getResources().getString(R.string.show_apps)));
                actionAdapter.add(new Action(ACTION_REMOVEDEVICE, getResources().getString(R.string.remove_device)));
                if (mSelectedObject.type == AppContract.TYPE_ATV) {
                    actionAdapter.add(new Action(ACTION_CLONEFROM, getResources().getString(R.string.clonefrom)));
                }
            }
        } else {
            //Get local app if it exists...
            Drawable banner = AppUtils.getLocalApkImage(getActivity(), mSelectedObject.pkg, mSelectedObject.type);

            if (banner != null) {
                row.setImageDrawable(banner);
            } else {
                //get the download URL...
                ImageDetail image = DBUtils.getImageRecordFromCP(getActivity(), mSelectedObject.pkg);
                String url = null;
                if (image != null) {
                    url = image.download_url;
                }

                if (url == null) {
                    row.setImageDrawable(getResources().getDrawable(R.drawable.noimage));
                } else {
                    //glide it in
                    Glide.with(getActivity())
                            .load(url)
                            .error(getResources().getDrawable(R.drawable.noimage))
                            .into(new SimpleTarget<GlideDrawable>(width, height) {
                                @Override
                                public void onResourceReady(GlideDrawable resource,
                                                            GlideAnimation<? super GlideDrawable>
                                                                    glideAnimation) {
                                    Log.d(TAG, "details overview card image url ready: " + resource);
                                    row.setImageDrawable(resource);
                                    mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                                }
                            });
                }
            }

            //and setup the actions...
            if ((mSelectedObject.type == AppContract.TYPE_ATV) || (mSelectedObject.type == AppContract.TYPE_BOTH)){
                if (DBUtils.isObjectLocal(getActivity(), mSelectedObject)) {
                    //run app and uninstall
                    actionAdapter.add(new Action(ACTION_RUNAPP, getResources().getString(R.string.run_app)));
                    actionAdapter.add(new Action(ACTION_UNINSTALL, getResources().getString(R.string.uninstall)));
                } else {
                    //install if the device type is correct...
                    if (Utils.bIsThisATV(getActivity())) {
                        if ((mSelectedObject.type == AppContract.TYPE_ATV)
                                || (mSelectedObject.type == AppContract.TYPE_BOTH)) {
                            actionAdapter.add(new Action(ACTION_INSTALL, getResources().getString(R.string.install)));
                        }
                    } else {
                        if ((mSelectedObject.type == AppContract.TYPE_TABLET)
                                || (mSelectedObject.type == AppContract.TYPE_BOTH)) {
                            actionAdapter.add(new Action(ACTION_INSTALL, getResources().getString(R.string.install)));
                        }
                    }
                }
            }
        }
        if (actionAdapter.size() > 0) {
            row.setActionsAdapter(actionAdapter);
        }

        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background and style.
        final DetailsOverviewRowPresenter detailsPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(getResources().getColor(R.color.selected_background));
        detailsPresenter.setStyleLarge(true);

        // Hook up transition element.
        detailsPresenter.setSharedElementEnterTransition(getActivity(),
                DetailsActivity.SHARED_ELEMENT_NAME);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                switch ((int) action.getId()) {
                    case ACTION_SHOWAPPS:
                        //Go to app row. To do this, set some activity globals and return them on activity finish
                        DetailsActivity.mOpenSerial = mSelectedObject.serial;
                        DetailsActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_OPENROW;
                        getActivity().onBackPressed();
                        break;
                    case ACTION_INSTALL:
                        InstallUtil.installAPK(getActivity(), mSelectedObject.pkg);
                        getActivity().onBackPressed();
                        break;
                    case ACTION_UNINSTALL:
                        InstallUtil.uninstallAPK(getActivity(), mSelectedObject.pkg);
                        getActivity().onBackPressed();
                        break;
                    case ACTION_REMOVEDEVICE:
                        DetailsActivity.mOpenSerial = mSelectedObject.serial;
                        DetailsActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_REMOVEDEVICE;
                        getActivity().onBackPressed();
                        break;
                    case ACTION_CLONEFROM:
                        DetailsActivity.mOpenSerial = mSelectedObject.serial;
                        DetailsActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_CLONEFROM;
                        getActivity().onBackPressed();
                        break;
                    case ACTION_RUNAPP:
                        Utils.launchApp(getActivity(), mSelectedObject.pkg);
                        getActivity().onBackPressed();
                        break;
                    case ACTION_ADD_MISSING:
                        DetailsActivity.mOpenSerial = mSelectedObject.serial;
                        DetailsActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_INSTALLMISSING;
                        getActivity().onBackPressed();
                        break;
                    case ACTION_CHANGE_NAME:
                        //FIXME - ugly dialog...
                        UIUtils.changeBTName(getActivity(), mSelectedObject.label);
                        break;
                }
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }


    /*
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(getResources().getString(R.string.movie), mSelectedObject);
                intent.putExtra(getResources().getString(R.string.should_start), true);
                startActivity(intent);


                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    } */
}
