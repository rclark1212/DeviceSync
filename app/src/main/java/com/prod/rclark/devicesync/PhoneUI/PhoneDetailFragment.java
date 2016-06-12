package com.prod.rclark.devicesync.PhoneUI;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.support.v7.graphics.Palette;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.prod.rclark.devicesync.ATVUI.DetailsActivity;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.InstallUtil;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.UIUtils;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

import java.util.ArrayList;

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

    //Set up dynamic button ID codes
    public static final int BUTTON_BASE = 20300;
    public static final int BUTTON_LISTSIZE = 8;
    public static final int BUTTON_SHOWAPPS = 0;
    public static final int BUTTON_ADD_MISSING = 1;
    public static final int BUTTON_CHANGENAME = 2;
    public static final int BUTTON_REMOVE = 3;
    public static final int BUTTON_CLONEFROM = 4;
    public static final int BUTTON_RUN = 5;
    public static final int BUTTON_UNINSTALL = 6;
    public static final int BUTTON_INSTALL = 7;

    private int mPosition;
    private boolean mbIsDevice;
    private String mSerial;
    private String mAPK;
    private View mRootView;
    private GridLayout mButtonView;
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

        //Yes - could do this with a gridview
        mButtonView = (GridLayout) mRootView.findViewById(R.id.button_container);

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
        TextView subTitleView1 = (TextView) mRootView.findViewById(R.id.article_subtitle1);
        TextView subTitleView2 = (TextView) mRootView.findViewById(R.id.article_subtitle2);
        //subTitleView.setMovementMethod(new LinkMovementMethod());
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        //Could do a callback but also could just query database for the bind..
        ObjectDetail object = null;
        if (mbIsDevice) {
            object = DBUtils.getDeviceFromCP(getActivity(), mSerial);
        } else {
            object = DBUtils.getAppFromCP(getActivity(), mSerial, mAPK);
        }

        //Set up buttons here
        setUpButtons(object);

        if (object != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);

            if (object.bIsDevice) {
                titleView.setText(object.label);
                subTitleView1.setText(object.name);
                subTitleView2.setText(object.serial);
            } else {
                titleView.setText(object.label);
                subTitleView1.setText(object.pkg);
                subTitleView2.setText(object.serial);
            }

            bodyView.setText(UIUtils.getObjectDetailDescription(getActivity(), object));

            //and glide it in...
            //See if there is a local banner...
            Drawable banner = null;
            if (object.bIsDevice) {
                if (object.type == AppContract.TYPE_TABLET) {
                    banner = getResources().getDrawable(Utils.getTabletResource(getActivity()));
                } else {
                    banner = getResources().getDrawable(R.drawable.shieldtv);
                }
            } else {
                banner = AppUtils.getLocalApkImage(getActivity(), mAPK, object.type);
            }

            if (banner != null) {
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
            subTitleView1.setText("N/A" );
            bodyView.setText("N/A");
        }
    }

    /**
     * Sets up the detail buttons - button setup will depend upon whether a device or app and whether
     * it is local or remote... Note, should keep this in sync with ATV functionality
     * Device:Local = ACTION_SHOWAPPS, ACTION_ADD_MISSING, ACTION_CHANGE_NAME
     * Device:Remote = ACTION_SHOWAPPS, ACTION_REMOVEDEVICE, ACTION_CLONEFROM
     * App:Local = ACTION_RUNAPP, ACTION_UNINSTALL
     * App:Remote = ACTION_INSTALL
     */
    private void setUpButtons(final ObjectDetail object) {
        //Step0 - see if this is right type... By definition, this code only runs on phones/tablets...
        if (!mbIsDevice) {
            if (object.type == AppContract.TYPE_ATV) {
                return;
            }
        }

        //Okay - step 1, figure out what buttons to show
        ArrayList<Integer> showButtons = new ArrayList<Integer>();
        boolean isLocal = DBUtils.isObjectLocal(getActivity(), object);

        if (mbIsDevice) {
            if (isLocal) {
                showButtons.add(BUTTON_SHOWAPPS);
                showButtons.add(BUTTON_ADD_MISSING);
                showButtons.add(BUTTON_CHANGENAME);
            } else {
                showButtons.add(BUTTON_SHOWAPPS);
                showButtons.add(BUTTON_REMOVE);
                if (object.type == AppContract.TYPE_TABLET) {
                    showButtons.add(BUTTON_CLONEFROM);
                }
            }
        } else {
            if (isLocal) {
                showButtons.add(BUTTON_RUN);
                showButtons.add(BUTTON_UNINSTALL);
            } else {
                showButtons.add(BUTTON_INSTALL);
            }
        }

        //Step2 - optimize by figuring out which views already there
        for (int i=0; i < BUTTON_LISTSIZE; i++) {
            int buttonId = BUTTON_BASE+i;
            if (mButtonView.findViewById(buttonId) != null) {
                //found a button
                //if button in showButtons list, remove it (it already exists)
                if (showButtons.contains((Integer)i)) {
                    showButtons.remove((Integer)i);
                }
            }
        }

        //Step3 - build if we need to
        if (showButtons.size() != 0) {
            //oh - some changes...
            //just remove all views and then rebuild
            mButtonView.removeAllViews();

            int cols = mButtonView.getColumnCount();
            if (cols == 0) {
                //have to fix up... Get the width per button...
                float width = (getResources().getDimension(R.dimen.btn_flat_min_width) +
                        2*getResources().getDimension(R.dimen.btn_flat_margin) +
                        2*getResources().getDimension(R.dimen.btn_flat_padding))/getResources().getDisplayMetrics().density;
                cols = mButtonView.getWidth() / (int)width;
                if (cols == 0) {
                    int screenwidth = getActivity().getResources().getConfiguration().screenWidthDp;
                    cols = screenwidth / (int)width;
                }
            }
            int rowcount = showButtons.size() / cols;
            if ((showButtons.size() % cols) != 0) rowcount++;

            mButtonView.setRowCount(rowcount);
            mButtonView.setColumnCount(cols);

            //Step4 - we are now at point where button list and view cleaned up. Only remaining
            //task is to add the buttons still remaining in our list...
            for (int i = 0; i < showButtons.size(); i++) {
                Button action = new Button(new ContextThemeWrapper(getActivity(), R.style.PhoneDetailButton), null, R.style.PhoneDetailButton);
                int buttonId = BUTTON_BASE + showButtons.get(i);
                //now get the name...
                action.setText(getButtonName(showButtons.get(i)));
                action.setId(buttonId);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                action.setLayoutParams(params);
                action.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Toast.makeText(getActivity(), "Hi there " + (v.getId() - BUTTON_BASE), Toast.LENGTH_SHORT).show();
                        processButtonClick(v, object);
                    }
                });
                mButtonView.addView(action);
            }
        }
    }

    /**
     * Implements the actions for the various buttons
     * @param v
     */
    private void processButtonClick(View v, ObjectDetail object) {
        //Which button was pressed?
        switch ((v.getId()-BUTTON_BASE)) {
            case BUTTON_SHOWAPPS:
                PhoneDetailActivity.mOpenSerial = object.serial;
                PhoneDetailActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_OPENROW;
                getActivity().onBackPressed();
                break;
            case BUTTON_ADD_MISSING:
                PhoneDetailActivity.mOpenSerial = object.serial;
                PhoneDetailActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_INSTALLMISSING;
                getActivity().onBackPressed();
                break;
            case BUTTON_CHANGENAME:
                UIUtils.changeBTName(getActivity(), object.label);
                break;
            case BUTTON_REMOVE:
                PhoneDetailActivity.mOpenSerial = object.serial;
                PhoneDetailActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_REMOVEDEVICE;
                getActivity().onBackPressed();
                break;
            case BUTTON_CLONEFROM:
                PhoneDetailActivity.mOpenSerial = object.serial;
                PhoneDetailActivity.mReturnCode = DetailsActivity.DETAIL_RETCODE_CLONEFROM;
                getActivity().onBackPressed();
                break;
            case BUTTON_RUN:
                Utils.launchApp(getActivity(), object.pkg);
                getActivity().onBackPressed();
                break;
            case BUTTON_UNINSTALL:
                InstallUtil.uninstallAPK(getActivity(), object.pkg);
                getActivity().onBackPressed();
                break;
            case BUTTON_INSTALL:
                InstallUtil.installAPK(getActivity(), object.pkg);
                getActivity().onBackPressed();
                break;
        }
    }

    /**
     * Returns string resource for the button action
     * @param buttonId
     * @return
     */
    private String getButtonName(int buttonId) {
        String description = "unknown";
        switch (buttonId) {
            case BUTTON_SHOWAPPS:
                description = getActivity().getResources().getString(R.string.show_apps);
                break;
            case BUTTON_ADD_MISSING:
                description = getActivity().getResources().getString(R.string.add_missing);
                break;
            case BUTTON_CHANGENAME:
                description = getActivity().getResources().getString(R.string.change_name);
                break;
            case BUTTON_REMOVE:
                description = getActivity().getResources().getString(R.string.remove_device);
                break;
            case BUTTON_CLONEFROM:
                description = getActivity().getResources().getString(R.string.clonefrom);
                break;
            case BUTTON_RUN:
                description = getActivity().getResources().getString(R.string.run_app);
                break;
            case BUTTON_UNINSTALL:
                description = getActivity().getResources().getString(R.string.uninstall);
                break;
            case BUTTON_INSTALL:
                description = getActivity().getResources().getString(R.string.install);
                break;
        }
        return description;
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

        if (bm != null) {
            bm.recycle();
        }
    }
}
