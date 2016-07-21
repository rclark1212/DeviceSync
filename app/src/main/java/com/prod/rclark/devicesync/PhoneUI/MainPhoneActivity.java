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

package com.prod.rclark.devicesync.PhoneUI;

/*
See ATVUI/MainActivity for summary

This is the launch entry point on Phone/Tablet. Currently simple skeleton for a recyclerview.

Needs to be updating for message receiver
Needs to be updated for firebase logon
Needs to be updated for CP listener/updater
Needs to be updated for GMS location services
more?

*/

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ListRow;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.prod.rclark.devicesync.ATVUI.DetailsActivity;
import com.prod.rclark.devicesync.ATVUI.MainFragment;
import com.prod.rclark.devicesync.ATVUI.SettingsActivity;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.HelpActivity;
import com.prod.rclark.devicesync.InitActivity;
import com.prod.rclark.devicesync.InstallUtil;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.UIDataSetup;
import com.prod.rclark.devicesync.UIUtils;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.cloud.FirebaseMessengerService;
import com.prod.rclark.devicesync.sync.GCESync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    //Pending intent returns...
    private static final int PREFERENCE_REQUEST_CODE = 1967;
    private static final int REQUEST_INIT_COMPLETE = 3022;
    public static final int PHONE_DETAILS_REQUEST_CODE = 3030;
    public static final String INTENT_EXTRA_LAUNCH = "phone_launch_page";
    public static final String INTENT_EXTRA_LAUNCH_MISSING = "missing";

    //Data keys used between list/detail views
    static final String EXTRA_STARTING_POS = "extra_starting_pos";
    static final String EXTRA_CURRENT_POS = "extra_current_pos";
    static final String EXTRA_LIST_POSITION = "extra_view_position";
    static final String EXTRA_ROW_POSITION = "extra_row_position";

    //  Service
    boolean mBoundToService;
    Messenger mService = null;
    int mPendingSvsMessage = 0;
    boolean mbServiceLoggedIn = false;
    boolean mbPendingCompleteSetup = false;
    static boolean mbInitDone = false;

    // Method globals
    private Bundle mReenterStateBundle = null;
    private PlaceholderFragment mCurrentPlaceholderFrag = null;
    private int mReturnPos = 0;
    private int mScrollPos = 0;
    private boolean bShowMissing = false;

    // Shared element transition
    //Set a callback for shared element transition
    private SharedElementCallback mShareCallback = null;
    {
        mShareCallback = new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                //are we coming back from a detail view?
                if (mReenterStateBundle != null) {
                    //Check here if we are coming back to a different story than when we started. And fix up.
                    int startingPos = mReenterStateBundle.getInt(EXTRA_STARTING_POS);
                    int currentPos = mReenterStateBundle.getInt(EXTRA_CURRENT_POS);
                    if (startingPos != currentPos) {
                        // If startingPosition != currentPosition the user must have swiped to a
                        // different page in the DetailsActivity. We must update the shared element
                        // so that the correct one falls into place.
                        if (mCurrentPlaceholderFrag != null) {
                            String newTransitionName = getResources().getString(R.string.transition) + currentPos;
                            View newSharedElement = mCurrentPlaceholderFrag.mRecyclerView.findViewWithTag(currentPos);
                            if (newSharedElement != null) {
                                names.clear();
                                names.add(newTransitionName);
                                sharedElements.clear();
                                sharedElements.put(newTransitionName, newSharedElement);
                            }
                        }
                    }

                    mReenterStateBundle = null;
                } else {
                    // If mReenterStateBundle is null, then the activity is exiting.
                    View navigationBar = findViewById(android.R.id.navigationBarBackground);
                    View statusBar = findViewById(android.R.id.statusBarBackground);
                    if (navigationBar != null) {
                        names.add(navigationBar.getTransitionName());
                        sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                    }
                    if (statusBar != null) {
                        names.add(statusBar.getTransitionName());
                        sharedElements.put(statusBar.getTransitionName(), statusBar);
                    }
                }
            }
        };
    }

    //  Class for interacting with our firebase service...
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            Log.d(TAG, "got bound to service callback");
            mBoundToService = true;
            //create fragment after we bind...
            //if there is a pending message, execute it (this can happen on activity attach in fragment before onstart
            if (mPendingSvsMessage != 0) {
                sendMessageToService(mPendingSvsMessage, null);
                mPendingSvsMessage = 0;
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.d(TAG, "got unbound to service callback");
            mBoundToService = false;
        }
    };

    //We use broadcast intents to message back from GCESync to the main activity.
    //Define our handler for this here.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //check for any load complete messages
            // Get extra data included in the Intent
            int status = intent.getIntExtra(GCESync.EXTENDED_DATA_STATUS, GCESync.EXTENDED_DATA_STATUS_NULL);
            int command = intent.getIntExtra(GCESync.EXTENDED_DATA_CMD, GCESync.EXTENDED_DATA_STATUS_NULL);

            if (status == GCESync.EXTENDED_DATA_STATUS_LOCALUPDATECOMPLETE) {
                //Okay - done with loading local data!
                //if you must update CP, do it here...
            } else if (status == GCESync.FIREBASE_SERVICE_LOGGEDIN) {
                Log.d(TAG, "Service has been logged into firebase!");
                mbServiceLoggedIn = true;
                //and if we have any remaining setup work to do, do it here...
                if (mbPendingCompleteSetup) {
                    mbPendingCompleteSetup = false;
                    finishSetup();
                }
            } else if (status == GCESync.FIREBASE_SERVICE_NOTLOGGEDIN) {
                Log.d(TAG, "Service not logged into firebase - trying to log in");
            }

            Log.d("DS_mainfrag_receiver", "Got status: " + status);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_phone);

        //is there a bundle?
        Intent launchIntent = getIntent();
        if (launchIntent.hasExtra(INTENT_EXTRA_LAUNCH)) {
            Log.d(TAG, "onCreate intent has launch extra");
            if (INTENT_EXTRA_LAUNCH_MISSING.equals(launchIntent.getStringExtra(INTENT_EXTRA_LAUNCH))) {
                //launched with intent to show missing page...
                bShowMissing = true;
            }
        }

        //Do our initialization
        Intent intent = new Intent(getApplication(), InitActivity.class);
        startActivityForResult(intent, REQUEST_INIT_COMPLETE);
    }

    /**
     * Complete the setup after the init service returns...
     */
    private void finishSetup() {
        String[] headers;

        //deal with scanning system/setting up CP first...
        //Update the local content provider if running for first time...
        Log.d(TAG, "Starting phone finish setup");
        boolean bFirstTime = Utils.isRunningForFirstTime(this, true);
        if (bFirstTime) {
            //Lets add a feature addition here - IFF we are running for first time *but* the database
            //has a record of apps for this serial number, then ask the user whether they want to "restore"
            //the system. Useful for when wiping a device.
            ArrayList<ObjectDetail> missing = DBUtils.getMissingApps(this, Build.SERIAL);
            if (missing.size() > 0) {
                //Okay - put up a dialog here...
                //Give 3 options - no, copyall, or disable uploads
                UIUtils.askDownloadExistingApps(this, missing);
                //askDownloadExistingApps(missing);
            } else {
                GCESync.startActionUpdateLocal(this, null, null);
            }
            //init the preferences
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        } else {
            //and note that we always want to update the local device (pick up new location and pick up any BT name changes)
            GCESync.startActionLocalDeviceUpdate(this, null, null);
        }

        //set init done flag
        mbInitDone = true;

        //Set up the data helper
        //Load the UI data structure
        if (mUIDataSetup == null) {
            mUIDataSetup = new UIDataSetup(getApplicationContext());
        }

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        //update nav drawer with the headers we want to use...
        int remoteRow = -1;
        headers = new String[mUIDataSetup.getNumberOfHeaders()];
        for (int i = 0; i < mUIDataSetup.getNumberOfHeaders(); i++) {
            headers[i] = mUIDataSetup.getRowHeader(i);
            if (mUIDataSetup.isHeaderRow(i)) {
                remoteRow = i;
            }
        }

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                headers,
                remoteRow);

        //And now, set the initial row if bundle is passed in to do that...
        if (bShowMissing) {
            mNavigationDrawerFragment.selectItem(mUIDataSetup.getMissingRow());
            bShowMissing = false;
        }

        //Set a shared element callback for the case where we may be exiting to a different element than we start from
        setExitSharedElementCallback(mShareCallback);
    }


    @Override
    public void onStart() {
        super.onStart();
        // Bind to the service
        if (!mBoundToService) {
            Log.d(TAG, "onStart - binding to service");
            bindService(new Intent(this, FirebaseMessengerService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        // Unbind from the service
        Log.d(TAG, "onStop - unbinding from service");
        if (mBoundToService) {
            mBoundToService = false;
            unbindService(mConnection);
        }
        Log.d(TAG, "onStop - unbound from service");
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        //set up app as active
        Utils.setAppActive(this, true);

        /*
        //set up handler and register content observer
        if (mAppObserver == null) {
            mAppObserver = new AppObserver(this);
        }
        getActivity().getContentResolver().registerContentObserver(AppContract.AppEntry.CONTENT_URI,
                true,
                mAppObserver);
        */

        //Register receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(GCESync.BROADCAST_ACTION));

    }

    @Override
    public void onPause() {
        //set up app as inactive
        Utils.setAppActive(this, false);

        /*
        // always call unregisterContentObserver in onPause
        getActivity().getContentResolver().unregisterContentObserver(mAppObserver);
        */

        //Unregister receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        super.onPause();
    }


    @Override
    public void onActivityReenter(int requestCode, Intent data) {
        super.onActivityReenter(requestCode, data);

        //Get bundle from detail activity (pass back parameters on what the currently selected item might be)
        mReenterStateBundle = new Bundle(data.getExtras());
        int startingPos = mReenterStateBundle.getInt(EXTRA_STARTING_POS);
        int currentPos = mReenterStateBundle.getInt(EXTRA_CURRENT_POS);
        mScrollPos = -1;
        if (startingPos != currentPos) {
            //scroll to currentId - note, convert the ID to position...
            mScrollPos = currentPos;
            //scroll here (need to get the element visible for transitions to work)
            if (mCurrentPlaceholderFrag != null) {
                mCurrentPlaceholderFrag.mRecyclerView.scrollToPosition(mScrollPos);
            }
        }

        if ((mCurrentPlaceholderFrag != null) && (mReenterStateBundle != null)) {
            postponeEnterTransition();


            //so, always force an invalidate to force a predraw below
            if (mCurrentPlaceholderFrag.mRecyclerView != null) {
                mCurrentPlaceholderFrag.mRecyclerView.invalidate();

                mCurrentPlaceholderFrag.mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public boolean onPreDraw() {
                        mCurrentPlaceholderFrag.mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                        //
                        // And one bug here. If no adapter, no scrolling. And if view on on page, can't transition.
                        if (mCurrentPlaceholderFrag.mRecyclerView.getAdapter() != null) {
                            mCurrentPlaceholderFrag.mRecyclerView.requestLayout();
                            if (mScrollPos >= 0) {
                                //And it turns out you need to scroll again to proper position after requesting layout.
                                mCurrentPlaceholderFrag.mRecyclerView.scrollToPosition(mScrollPos);
                                mScrollPos = -1;
                            }

                            //ready to start the transition
                            startPostponedEnterTransition();
                        }
                        return true;
                    }
                });
            } else {
                //and if we run into a null recycler view, better let the transition just go forward.
                startPostponedEnterTransition();
            }
        }
    }

    /**
     * Sends a message to our service
     */
    private boolean sendMessageToService(int messageId, Bundle bundle) {
        if (!mBoundToService) {
            Log.d(TAG, "Service not bound but someone tried to send message");
            return false;
        }

        Message msg = Message.obtain(null, messageId, 0, 0);
        if (bundle != null) {
            msg.setData(bundle);
        }

        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.d(TAG, "Error accessing service!");
            e.printStackTrace();
        }

        return true;
    }

    /**
     * And wait for finish from init
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INIT_COMPLETE) {
            //init is done!
            Log.d(TAG, "InitService complete");
            //was it successful?
            if (resultCode == RESULT_OK) {
                //set up flag to complete setup
                mbPendingCompleteSetup = true;
                //make sure we are logged in by service...
                //below message will generate a broadcast message back which will then finish setup
                if (!sendMessageToService(FirebaseMessengerService.MSG_ATTEMPT_LOGON, null)) {
                    //We have a problem
                    //Toast.makeText(getApplicationContext(), "Service not bound", Toast.LENGTH_SHORT).show();
                    mPendingSvsMessage = FirebaseMessengerService.MSG_ATTEMPT_LOGON;
                }
            } else {
                UIUtils.finishIt(this);
            }
        } else if (PHONE_DETAILS_REQUEST_CODE == requestCode) {
            //came back from details screen
            //check to see if detail screen selection was to show apps for a device...
            if (data != null) {
                String serial = "";
                if (data.hasExtra(MainFragment.DETAILS_RESULT_KEY)) {
                    serial = data.getStringExtra(MainFragment.DETAILS_RESULT_KEY);
                }
                int action = data.getIntExtra(MainFragment.DETAILS_RESULT_ACTION, 0);

                if (action == DetailsActivity.DETAIL_RETCODE_OPENROW) {
                    //find the row with this serial number...
                    //and select it...
                    if (serial != null) {
                        Log.d(TAG, "DetailRet - select pos");

                        //get the id...
                        int id = mUIDataSetup.getSerialRow(serial);

                        //and select it
                        mNavigationDrawerFragment.selectItem(id);
                    }
                } else if (action == DetailsActivity.DETAIL_RETCODE_INSTALLMISSING) {
                    //get to install our missing apps...
                    //Get the missing items
                    ArrayList<ObjectDetail> missing = DBUtils.getMissingApps(this, Build.SERIAL);
                    //Now construct the install list...
                    ArrayList<String> apklist = new ArrayList<String>();
                    for (int i=0; i < missing.size(); i++) {
                        apklist.add(missing.get(i).pkg);
                    }
                    //Okay - created list - go install!
                    //InstallUtil.batchInstallAPK(this, apklist);
                    UIUtils.confirmBatchOperation(this, apklist, true);
                } else if (action == DetailsActivity.DETAIL_RETCODE_REMOVEDEVICE) {
                    //Okay, remove device
                    //To delete device, just have to delete from the CP. That deletes from firebase. Which then
                    //mirrors down to everyone else.
                    DBUtils.deleteDeviceFromCP(this, serial);
                    //And force a sync
                    GCESync.startActionUpdateLocal(this, null, null);
                } else if (action == DetailsActivity.DETAIL_RETCODE_CLONEFROM) {
                    Utils.cloneDevice(this, serial);
                } else if (action == DetailsActivity.DETAIL_RETCODE_REFRESH) {
                    //Force an update here for device screen
                    mNavigationDrawerFragment.selectItem(0);
                }
            }
        } else if (requestCode == PREFERENCE_REQUEST_CODE) {
            //Back from preference screen
            //So don't really care about result code - cancel or OK, user may have modified settings
            //Instead, look at the data...
            if (data != null) {
                int prefResult = data.getIntExtra(MainFragment.PREF_RESULT_KEY, MainFragment.PREF_DO_NOTHING);

                //And now process...
                if ((prefResult & MainFragment.PREF_UPDATE_UI_FLAG) != 0) {
                    Log.d(TAG, "PreferenceRet - update UI");
                    //New strat. And a smoother UI experience.
                    //Simply save off hidden rows and restore them as necessary to the mRowAdapter object.
                    //Don't deallocate or anything (as browsefragment doesn't deal with deallocation well).
                    mUIDataSetup.loadRowEnables();
                    //updateRowVisibility();
                    //FIXME - change adapter for navtray (mDrawerListView.setAdapter) to use a custom adapter where
                    //we can override areAllItemsEnabled() to return false and isEnabled() to return true or false as needed.
                }

                if ((prefResult & MainFragment.PREF_UPDATE_CP_FLAG) != 0) {
                    Log.d(TAG, "PreferenceRet - update CP");
                    //And kick off a CP update. Turn off the processing flag for UI until done...
                    GCESync.startActionUpdateLocal(this, null, null);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_phone, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (mNavigationDrawerFragment.mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Display the settings fragment
            Intent intent = new Intent();
            intent.setClass(this, PhoneSettingsActivity.class);
            //and start for a result so that we can take care of any UI elements that we have to do when we get back...
            startActivityForResult(intent, PREFERENCE_REQUEST_CODE);
        } else if (id == R.id.action_disablesync) {
            if (Utils.getSyncDisabled(this)) {
                Utils.setSyncDisabled(this, false);
                //flip button
                item.setTitle(getResources().getString(R.string.disable_sync_button_text));
                GCESync.startActionUpdateLocal(this, null, null);

            } else {
                //disable sync
                Utils.setSyncDisabled(this, true);
                //and flip button to enable sync
                item.setTitle(getResources().getString(R.string.enable_sync_button_text));
            }
        } else if (id == R.id.action_syncnow) {
            GCESync.startActionUpdateLocal(this, null, null);
        } else if (id == R.id.action_help) {
            //show help
            Intent intent = new Intent(this, HelpActivity.class);
            intent.putExtra(HelpActivity.HELP_ORDINAL, HelpActivity.HELP);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
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
        mCurrentPlaceholderFrag = PlaceholderFragment.newInstance(position);
        fragmentManager.beginTransaction()
                .replace(R.id.container, mCurrentPlaceholderFrag)
                .commit();
    }

    public void onSectionAttached(int number) {
        if (mUIDataSetup == null) {
            mUIDataSetup = new UIDataSetup(getApplicationContext());
        }

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
        public RecyclerView mRecyclerView;
        private ListCursorObjectAdapter mCAdapter;
        private ListArrayObjectAdapter mAAdapter;
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
                if (mCAdapter != null) {
                    mRecyclerView.setAdapter(mCAdapter);
                } else if (mAAdapter != null) {
                    mRecyclerView.setAdapter(mAAdapter);
                } else {
                    Log.d(TAG, "error creating adapter");
                }
            }

            return rootView;
        }

        @Override
        public void onAttach(Context ctx) {
            super.onAttach(ctx);

            if (mUIDataSetup == null) {
                mUIDataSetup = new UIDataSetup(ctx);
            }

            mPosition = getArguments().getInt(ARG_SECTION_NUMBER);

            //update the title...
            ((MainPhoneActivity) ctx).onSectionAttached(mPosition);

            //create the adapter for the view...
            if (MainPhoneActivity.mUIDataSetup != null) {
                if (mUIDataSetup.useArrayAdapter(mPosition)) {
                    //okay - use an array object...
                    if (mAAdapter == null) {
                        //create the array...
                        Log.d(TAG, "Create adapter/array");
                        if (mArray == null) {
                            mArray = mUIDataSetup.getArrayAdapter(mPosition);
                        }

                        //Pass in position as it is used by detail activity
                        mAAdapter = new ListArrayObjectAdapter(getActivity(), mArray, mPosition);
                    }
                } else {
                    //use a cursor object
                    if (mCAdapter == null) {
                        Uri uri = MainPhoneActivity.mUIDataSetup.getRowUri(mPosition);
                        String selection = MainPhoneActivity.mUIDataSetup.getRowSelection(mPosition);
                        String[] selection_args = MainPhoneActivity.mUIDataSetup.getRowSelectionArgs(mPosition);

                        Log.d(TAG, "Create adapter/grab cursor - uri:" + uri.toString());

                        Cursor c = getActivity().getContentResolver().query(uri, null, selection, selection_args, null);

                        // and set the adapter with a null cursor
                        //Pass in position as it is used by detail activity
                        mCAdapter = new ListCursorObjectAdapter(getActivity(), c, mUIDataSetup.isDeviceRow(mPosition), mPosition);
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
                mAAdapter.notifyDataSetChanged();
            }
        }
    }

}
