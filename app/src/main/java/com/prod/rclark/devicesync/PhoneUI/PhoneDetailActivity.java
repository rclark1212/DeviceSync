package com.prod.rclark.devicesync.PhoneUI;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.app.SharedElementCallback;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;

import java.util.List;
import java.util.Map;

/**
 * Created by rclark on 6/9/2016.
 * Modified from the xyzreader class project
 */
public class PhoneDetailActivity extends AppCompatActivity {

    private static final String TAG = "PhoneDetailActivity";

    private int mStartPos;              //indicates starting position
    private int mSelectedPos;           //indicates selected position

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private boolean mIsReturning = false;
    private PhoneDetailFragment mCurrentDetailFragment = null;
    private boolean mIsCard = false;
    private final String CURRENT_INDEX = "currentIndex";

    private SharedElementCallback mCallback = null;
    {
        mCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (mIsReturning) {
                    ImageView sharedElementSrc = mCurrentDetailFragment.getDetailImage();  //FIXME (see xyz reader)
                    if (sharedElementSrc == null) {
                        // (1) If shared element is null, then it has been scrolled off screen and
                        // no longer visible. In this case we cancel the shared element transition by
                        // removing the shared element from the shared elements map.
                        names.clear();
                        sharedElements.clear();
                    } else if (mStartPos != mSelectedPos) {
                        //Check if user swiped to a different pager page. Need to remove old shared element
                        //and replace it with new to use for transition
                        names.clear();
                        names.add(sharedElementSrc.getTransitionName());
                        sharedElements.clear();
                        sharedElements.put(sharedElementSrc.getTransitionName(), sharedElementSrc);
                    }
                }
            }
        };
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            //set an immersive view - full screen ahead!
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.phone_detail_frame);

        //Postpone transition until ready and set up callback
        postponeEnterTransition();
        setEnterSharedElementCallback(mCallback);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        //Leave this - allows a nice little line showing page margin during flip...
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));

        //add nice transition approach for page flips - use the zoomout method in android dev docs
        mPager.setPageTransformer(true, new ZoomOutPageTransformer());

        //Check if this is cardview... (big tablet mode)
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);

        //and if so, set toolbar to support action bar and add up button
        if (mIsCard) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_cardview);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    //Default back button processing does not appear to follow same code path as back button
                    //pressed (likely because we added the view to the activity parent container instead of
                    //fragment. So just force toolbar back button to do a back event...
                    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                        }
                    });
                }
            }
        }

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }

            @Override
            public void onPageSelected(int position) {
                //set the new selected position
                mSelectedPos = position;
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                //get starting position
                mStartPos = getIntent().getIntExtra(MainPhoneActivity.EXTRA_LIST_POSITION, 0);
                mSelectedPos = mStartPos;
            }
        } else {
            //in case of rotate/activity destroy, need to recover current page pos
            mSelectedPos = savedInstanceState.getInt(CURRENT_INDEX);
            if (getIntent() != null && getIntent().getData() != null) {
                mStartPos = getIntent().getIntExtra(MainPhoneActivity.EXTRA_LIST_POSITION, 0);
            }
        }
    }

    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        //Return data to mainactivity here (i.e. return back data allowing transition to be properly
        //set up to the right element)
        Intent data = new Intent();
        data.putExtra(MainPhoneActivity.EXTRA_STARTING_POS, mStartPos);
        data.putExtra(MainPhoneActivity.EXTRA_CURRENT_POS, mSelectedPos);
        setResult(RESULT_OK, data);

        super.finishAfterTransition();
    }

    //Save the current position on activity destroy (rotate, etc)
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_INDEX, mSelectedPos);
    }

    /**
     *  Data lookup routines
     *  FIXME
     */
    public int getObjectCount() {
        return 2;
    }

    public ObjectDetail getObject(int position) {
        ObjectDetail returnObject = new ObjectDetail();
        returnObject.pkg = "com.example.dummy" + position;
        returnObject.label = "hello world";
        returnObject.serial = "12345A";
        return returnObject;
    }

    //
    // add a transition effect for page flips.
    // Code was taken from android developer documentation
    // http://developer.android.com/training/animation/screen-slide.html
    //
    public class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailFragment = (PhoneDetailFragment) object;
        }

        @Override
        public Fragment getItem(int position) {
            ObjectDetail object = getObject(position);
            return PhoneDetailFragment.newInstance(object.bIsDevice, object.serial, object.pkg, position);
        }

        @Override
        public int getCount() {
            //return count of objects
            return getObjectCount();
        }
    }

}
