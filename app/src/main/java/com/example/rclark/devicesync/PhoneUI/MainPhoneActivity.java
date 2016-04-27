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

package com.example.rclark.devicesync.PhoneUI;

/*
See ATVUI/MainActivity for summary

This is the launch entry point on Phone/Tablet. Currently simple skeleton for a recyclerview.

*/

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.UIDataSetup;
import com.example.rclark.devicesync.data.AppContract;
import com.example.rclark.devicesync.sync.EndpointsTesting;
import com.example.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;

public class MainPhoneActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = "MainPhone";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    //Our data helper - only keep a single instance shared by all
    public static UIDataSetup mUIDataSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String[] headers;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_phone);

        //Set up the data helper
        //Load the UI data structure
        if (mUIDataSetup == null) {
            mUIDataSetup = new UIDataSetup(getApplicationContext());
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        //update nav drawer with the headers we want to use...
        headers = new String[mUIDataSetup.getNumberOfHeaders()];
        for (int i = 0; i < mUIDataSetup.getNumberOfHeaders(); i++) {
            headers[i] = mUIDataSetup.getRowHeader(i);
        }

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                headers);

        //Start up sync service
        //And update the local content provider...
        GCESync.startActionUpdateLocal(getApplicationContext(), null, null);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        //Set up the data helper
        //Load the UI data structure
        if (mUIDataSetup == null) {
            mUIDataSetup = new UIDataSetup(getApplicationContext());
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position))
                .commit();
    }

    public void onSectionAttached(int number) {
        mTitle = "AppSyncr - " + mUIDataSetup.getRowHeader(number);

        restoreActionBar();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private RecyclerView mRecyclerView;
        private ListObjectAdapter mAdapter;
        private ArrayList<ObjectDetail> mArray;
        private RecyclerView.LayoutManager mLayoutManager;
        private int mPosition;

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);

            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_phone, container, false);

            //Only initialize this once...
            if (mRecyclerView == null) {
                Log.d(TAG, "Init recycler view");
                mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

                // not changing the size of recyclerview
                mRecyclerView.setHasFixedSize(true);

                //how many columns can we get? Should be width of screen / width of grid element
                DisplayMetrics displaymetrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                int width = displaymetrics.widthPixels;

                // use GridLayoutManager
                mLayoutManager = new GridLayoutManager(getActivity(), (width / 300));
                mRecyclerView.setLayoutManager(mLayoutManager);

                //attach the adapter for this instance
                if (mAdapter != null) {
                    mRecyclerView.setAdapter(mAdapter);
                } else {
                    Log.d(TAG, "error creating adapter");
                }
            }

            return rootView;
        }

        @Override
        public void onAttach(Context ctx) {
            super.onAttach(ctx);

            mPosition = getArguments().getInt(ARG_SECTION_NUMBER);

            //update the title...
            ((MainPhoneActivity) ctx).onSectionAttached(mPosition);

            //create the adapter for the view...
            if (MainPhoneActivity.mUIDataSetup != null) {
                if (mUIDataSetup.useArrayAdapter(mPosition)) {
                    //okay - use an array object...
                    if (mAdapter == null) {
                        //create the array...
                        Log.d(TAG, "Create adapter/array");
                        if (mArray == null) {
                            mArray = mUIDataSetup.getArrayAdapter(mPosition);
                        }

                        //and create the adapter - TODO
                        //mAdapter = array object
                    }
                } else {
                    //use a cursor object
                    if (mAdapter == null) {
                        Uri uri = MainPhoneActivity.mUIDataSetup.getRowUri(mPosition);
                        String selection = MainPhoneActivity.mUIDataSetup.getRowSelection(mPosition);
                        String[] selection_args = MainPhoneActivity.mUIDataSetup.getRowSelectionArgs(mPosition);

                        Log.d(TAG, "Create adapter/grab cursor - uri:" + uri.toString());

                        Cursor c = getActivity().getContentResolver().query(uri, null, selection, selection_args, null);

                        // and set the adapter with a null cursor
                        mAdapter = new ListObjectAdapter(getActivity(), c, mUIDataSetup.isDeviceRow(mPosition));
                    }
                }
            } else {
                Log.d(TAG, "yikes! mUIDataSetup not initialized in onAttach!");
            }
        }

        /**
         * Call when underlying CP changes. Update the array (if it exists) and notify the adapter.
         */
        public void updateArray() {
            if (mArray != null) {
                mArray = mUIDataSetup.getArrayAdapter(mPosition);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

}
