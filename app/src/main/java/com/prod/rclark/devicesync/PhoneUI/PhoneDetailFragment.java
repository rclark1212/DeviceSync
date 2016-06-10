package com.prod.rclark.devicesync.PhoneUI;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ShareCompat;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.graphics.Palette;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.Utils;

/**
 * Created by rclark on 6/9/2016.
 * Modified from the xyzreader class project
 */
public class PhoneDetailFragment extends Fragment {
    private static final String TAG = "PhoneDetailFragment";

    public static final String ARG_ITEM_DEVICE = "item_device";
    public static final String ARG_ITEM_SERIAL = "item_serial";
    public static final String ARG_ITEM_APK = "item_apk";
    public static final String ARG_ITEM_POSITION = "item_position";

    private int mPosition;
    private boolean mbIsDevice;
    private String mSerial;
    private String mAPK;
    private View mRootView;
    private TextView mTitleView;
    private ImageView mPhotoView;
    private int mNoImageMuted = 0;
    private boolean mIsTransitioning = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PhoneDetailFragment() {
    }

    public static PhoneDetailFragment newInstance(boolean bIsDevice, String serial, String apk, int position) {
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_ITEM_DEVICE, bIsDevice);
        arguments.putString(ARG_ITEM_SERIAL, serial);
        arguments.putString(ARG_ITEM_APK, apk);
        arguments.putInt(ARG_ITEM_POSITION, position);
        PhoneDetailFragment fragment = new PhoneDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get the params
        if (getArguments().containsKey(ARG_ITEM_DEVICE)) {
            mbIsDevice = getArguments().getBoolean(ARG_ITEM_DEVICE);
        }

        if (getArguments().containsKey(ARG_ITEM_SERIAL)) {
            mSerial = getArguments().getString(ARG_ITEM_SERIAL);
        }

        if (getArguments().containsKey(ARG_ITEM_APK)) {
            mAPK = getArguments().getString(ARG_ITEM_APK);
        }

        if (getArguments().containsKey(ARG_ITEM_POSITION)) {
            mPosition = getArguments().getInt(ARG_ITEM_POSITION);
        }

        mIsTransitioning = (savedInstanceState == null);

        setHasOptionsMenu(true);
    }

    public PhoneDetailActivity getActivityCast() {
        return (PhoneDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.phone_detail_fragment, container, false);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);

        //set transition name
        mPhotoView.setTransitionName(getString(R.string.transition) + mPosition);

        //Grab the title view
        mTitleView = (TextView) mRootView.findViewById(R.id.article_title);

        bindViews();

        if (mIsTransitioning) {
            //TODO do any work here for transitions not done in xml...
            //None for now but leave this here in case we need it...
        }

        return mRootView;
    }

    //Do transition here
    private void startPostponedEnterTransition() {
        //Make sure we are attached to an activity (can flip through pages fast and be detached)
        if (isAdded()) {
            mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
                    getActivity().startPostponedEnterTransition();
                    return true;
                }
            });
        }
    }

    @Nullable
    ImageView getDetailImage() {
        //urp - need to check that image is visible...
        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
            return mPhotoView;
        }
        return null;
    }

    //Check that view is in bounds of the provided container...
    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
        Rect containerBounds = new Rect();
        container.getHitRect(containerBounds);
        return view.getLocalVisibleRect(containerBounds);
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        //FIXME but for now, set serial/apk
        //get object.

        if (true) { //(object != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            titleView.setText(mSerial);
            //fix string for localization
            bylineView.setText(mAPK);
            bodyView.setText("Hello World");

            //and glide it in...
            //See if there is a local banner...
            //FIXME FOR DEVICE
            Drawable banner = null; //AppUtils.getLocalApkImage(getActivity(), mAPK, type);
            if (banner == null) {
                //local app has image...
                mPhotoView.setImageDrawable(banner);
                setMetabarColor(R.drawable.noimage);
                startPostponedEnterTransition();
            } else {
                //get the download URL...
                ImageDetail image = DBUtils.getImageRecordFromCP(getActivity(), mAPK);
                //glide it in
                if (image != null) {
                    //glide it in
                    Glide.with(getActivity())
                            .load(image.download_url)
                            .centerCrop()
                            .error(getActivity().getResources().getDrawable(R.drawable.noimage))
                            .into(new SimpleTarget<GlideDrawable>() {
                                @Override
                                public void onResourceReady(GlideDrawable resource,
                                                            GlideAnimation<? super GlideDrawable>
                                                                    glideAnimation) {
                                    mPhotoView.setImageDrawable(resource);
                                    setMetabarColor(resource);
                                    startPostponedEnterTransition();
                                }
                            });
                } else {
                    mPhotoView.setImageDrawable(getActivity().getResources().getDrawable(R.drawable.noimage));
                    setMetabarColor(R.drawable.noimage);
                    startPostponedEnterTransition();
                }
            }
        } else {
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    private void setMetabarColor(int resid) {
        if (resid == 0) {
            return;
        }

        //optimize here since called so often...
        if (resid == R.drawable.noimage)  {
            if (mNoImageMuted != 0) {
                mRootView.findViewById(R.id.meta_bar).setBackgroundColor(mNoImageMuted);
            }
        }

        Bitmap bm = BitmapFactory.decodeResource(getActivity().getResources(), resid);

        Palette p = Palette.from(bm).generate();
        int MutedColor = p.getDarkVibrantColor(getResources().getColor(R.color.theme_primary_dark));

        mRootView.findViewById(R.id.meta_bar).setBackgroundColor(MutedColor);

        if (resid == R.drawable.noimage)  {
            mNoImageMuted = MutedColor;
        }
    }

    private void setMetabarColor(Drawable image) {
        if (image == null) {
            return;
        }

        Bitmap bm = Utils.drawableToBitmap(image);

        Palette p = Palette.from(bm).generate();
        int mMutedColor = p.getDarkVibrantColor(getResources().getColor(R.color.theme_primary_dark));

        mRootView.findViewById(R.id.meta_bar).setBackgroundColor(mMutedColor);
    }
}
