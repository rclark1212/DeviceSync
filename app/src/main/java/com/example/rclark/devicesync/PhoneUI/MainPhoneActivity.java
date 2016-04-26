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
import com.example.rclark.devicesync.sync.GCESync;

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

    //Our data helper
    private UIDataSetup mUIDataSetup;

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
                .replace(R.id.container, PlaceholderFragment.newInstance(position,
                        mUIDataSetup.getRowUri(position),
                        mUIDataSetup.getRowSelection(position),
                        mUIDataSetup.getRowSelectionArgs(position)))
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
        private RecyclerView.LayoutManager mLayoutManager;

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_URI = "section_uri";
        private static final String ARG_SELECTION = "section_selection";
        private static final String ARG_SELECTION_ARGS = "section_selection_args";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber, Uri uri, String selection, String[] selectionArgs) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putString(ARG_URI, uri.toString());
            args.putString(ARG_SELECTION, selection);
            args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);

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

                // get a cursor for this view...
                //Get the device DB reference...
                //Log.d(TAG, "Grab cursor");
                //Uri appDB = AppContract.AppEntry.CONTENT_URI;

                //grab the cursor
                //Cursor c = getActivity().getContentResolver().query(appDB, null, null, null, null);

                mRecyclerView.setAdapter(mAdapter);
            }

            return rootView;
        }

        @Override
        public void onAttach(Context ctx) {
            super.onAttach(ctx);
            int position = getArguments().getInt(ARG_SECTION_NUMBER);
            String uri_string = getArguments().getString(ARG_URI);
            Uri uri = Uri.parse(uri_string);
            String selection = getArguments().getString(ARG_SELECTION);
            String[] selection_args = getArguments().getStringArray(ARG_SELECTION_ARGS);

            //update the title...
            ((MainPhoneActivity) ctx).onSectionAttached(position);

            //create a cursor from all this...
            Log.d(TAG, "Grab cursor");
            Cursor c = getActivity().getContentResolver().query(uri, null, selection, selection_args, null);

            int type = (position == 0) ? 1 : 0;

            if (mAdapter == null) {
                // and set the adapter with a null cursor
                mAdapter = new ListObjectAdapter(getActivity(), c, type);
            } else {
                mAdapter.changeCursorAndType(c, type);
            }
        }
    }

}
