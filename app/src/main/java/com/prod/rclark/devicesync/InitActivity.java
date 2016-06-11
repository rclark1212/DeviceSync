package com.prod.rclark.devicesync;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.prod.rclark.devicesync.ATVUI.MainFragment;
import com.prod.rclark.devicesync.cloud.FirebaseMessengerService;

/**
 * Created by rclark on 6/8/2016.
 * Move all the activity initialization to this activity so it can be shared by ATV and Phone.
 * The initialization is involved enough to make this a necessity and unspaghetti-fy event code :(
 * (note that phone/ATV still need to handle connecting to service but all the initialization stuff
 * will be handled here)
 */
public class InitActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    /**
     * Called when the activity is first created.
     */
    private static final String TAG = "MainInit";

    //Initialization state machine params/variables
    private final static int STATE_EVENT_ONCREATE = 0;
    private final static int STATE_EVENT_SRVC_BIND = 1;
    private final static int STATE_EVENT_INET_OKAY = 2;
    private final static int STATE_EVENT_WELCOME_DONE = 3;
    private final static int STATE_EVENT_GMS_AVAILABLE = 4;
    private final static int STATE_EVENT_TUTORIAL_DONE = 5;
    private final static int STATE_EVENT_FIREBASE_LOGON = 6;
    private final static int STATE_EVENT_PERMISSION_CHECKED = 7;
    private final static int STATE_EVENT_GMS_CONNECTED = 8;
    private final static int STATE_EVENT_WAITING_FOR_BINDING_LOGIN = 9;
    private final static int STATE_EVENT_WAITING_FOR_BINDING_QUERY = 10;
    private final static int STATE_EVENT_BTPERMISSION_CHECKED = 11;

    //Initialization state machine params/variables
    private final static int STATE_LAUNCH = 0;
    private final static int STATE_CHECK_GMS = 1;
    private final static int STATE_SHOWING_WELCOME = 2;
    private final static int STATE_FIXING_GMS = 3;
    private final static int STATE_LOGGING_ON = 4;
    private final static int STATE_CHECKING_PERMISSIONS = 5;
    private final static int STATE_CHECKING_BTPERMISSIONS = 6;
    private final static int STATE_SHOWING_TUTORIAL = 7;
    private final static int STATE_APPINIT_COMPLETE = 8;

    private int mInitCurrentState = STATE_LAUNCH;

    //state variables
    private boolean mbCreate = false;
    private boolean mbServiceBind = false;
    private boolean mbInetOkay = false;
    private boolean mbWelcomeDone = false;
    private boolean mbGMSAvailable = false;
    private boolean mbGMSLoggedOn = false;
    private boolean mbTutorialShown = false;
    private boolean mbFirebaseLoggedOn = false;
    private boolean mbHaveLocationPermission = false;
    private boolean mbHaveBTPermission = false;
    private boolean mbStateWaitingForBindingLogin = false;
    private boolean mbStateWaitingForBindingQuery = false;


    //Pending intent returns...
    private static final int REQUEST_SHOW_WELCOME = 3017;
    private static final int REQUEST_SHOW_TUTORIAL = 3018;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 3019;
    private static final int REQUEST_FIREBASE_SIGN_IN = 3020;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 3021;
    private static final int MY_PERMISSIONS_REQUEST_BT = 3022;

    //GMS client
    private static GoogleApiClient mActivityGoogleApiClient = null;

    //  Service
    boolean mBoundToService;
    Messenger mService = null;
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
            appInitStateMachine(STATE_EVENT_SRVC_BIND);
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            Log.d(TAG, "got unbound to service callback");
            mBoundToService = false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onInitCreate");
        appInitStateMachine(STATE_EVENT_ONCREATE);

        Log.d(TAG, "Checking internet");
        //First check internet connectivity
        if (!Utils.isOnline(this)) {
            finishIt();
        } else {
            appInitStateMachine(STATE_EVENT_INET_OKAY);
        }
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

    /*****************************************************************************************************************
     * INITIALIZATION CODE FOLLOWS
     *
     * The initialization of the lifecycle is ending up causing spaghetti event code. Try to unify all the lifecycle
     * starts into one synchronized state machine. Can then trigger on this as we debug lifecyle events. Now this is
     * non-standard type of state machine. Note that the issue is that some of the initialization can be synchronous,
     * but sometimes an init call becomes async with a callback.
     *
     * Fortunately we have a straightforward progression. So construct the routine to call status and fallthrough to
     * next state. If a call goes into a callback, routine gets re-entered.
     *
     * @param newEvent
     */
    public synchronized void appInitStateMachine(int newEvent) {

        //Step1, as new events come in set the various flags
        //Probably a better way to do this but my mind works this way...
        switch (newEvent) {
            case STATE_EVENT_ONCREATE:
                mbCreate = true;
                break;
            case STATE_EVENT_SRVC_BIND:
                mbServiceBind = true;
                break;
            case STATE_EVENT_INET_OKAY:
                mbInetOkay = true;
                break;
            case STATE_EVENT_WELCOME_DONE:
                mbWelcomeDone = true;
                break;
            case STATE_EVENT_GMS_AVAILABLE:
                mbGMSAvailable = true;
                break;
            case STATE_EVENT_GMS_CONNECTED:
                mbGMSLoggedOn = true;
                break;
            case STATE_EVENT_TUTORIAL_DONE:
                mbTutorialShown = true;
                break;
            case STATE_EVENT_FIREBASE_LOGON:
                mbFirebaseLoggedOn = true;
                break;
            case STATE_EVENT_PERMISSION_CHECKED:
                mbHaveLocationPermission = true;
                break;
            case STATE_EVENT_BTPERMISSION_CHECKED:
                mbHaveBTPermission = true;
                break;
            case STATE_EVENT_WAITING_FOR_BINDING_LOGIN:          //This is a special case - onStart called after fragment activity attaches
                mbStateWaitingForBindingLogin = true;           //need to post the query and deliver once bind occurs
                break;
            case STATE_EVENT_WAITING_FOR_BINDING_QUERY:          //This is a special case - onStart called after fragment activity attaches
                mbStateWaitingForBindingQuery = true;           //need to post the query and deliver once bind occurs
                break;
        }

        //
        //Now execute the state machine to process the newEvent (if there is any processing to do)
        // Note this is in general order of operation
        //
        if (mInitCurrentState == STATE_LAUNCH) {
            Log.d(TAG, "Init routine - STATE_LAUNCH");
            if (mbCreate && mbServiceBind && mbInetOkay) {
                //service, oncreate and inet all okay...
                //Okay to have a single state waiting for all 3 since all 3 failing will punt us out of app
                mInitCurrentState = STATE_CHECK_GMS;
                mbGMSAvailable = isGMSAvailable();
            }
        }

        if (mInitCurrentState == STATE_CHECK_GMS) {
            Log.d(TAG, "Init routine - STATE_CHECK_GMS");
            if (mbGMSAvailable) {
                mInitCurrentState = STATE_SHOWING_WELCOME;
                //kick off GMS connect
                connectGMS();
                //and show welcome
                mbWelcomeDone = showWelcome();
            }
        }

        if (mInitCurrentState == STATE_SHOWING_WELCOME) {
            Log.d(TAG, "Init routine - STATE_SHOW_WELCOME");
            //Okay, welcome taken care of - now off to logon once GMS and welcome complete...
            if (mbWelcomeDone && mbGMSLoggedOn) {
                //move to logon to firebase
                mInitCurrentState = STATE_LOGGING_ON;
                //firebaselogon
                mbFirebaseLoggedOn = firebaseSignIn();
            }
        }

        /*
        if (mInitCurrentState == STATE_FIXING_GMS) {
            if (mbGMSAvailable) {
                if (mbFirebaseLoggedOn) {
                    //move to checking permissions
                    mInitCurrentState = STATE_CHECKING_PERMISSIONS;
                    //check permissions
                } else {
                    mInitCurrentState = STATE_LOGGING_ON;
                    //firebase logon
                }
            }
        }*/

        if (mInitCurrentState == STATE_LOGGING_ON) {
            Log.d(TAG, "Init routine - STATE_LOGGING_ON");
            if (mbFirebaseLoggedOn) {
                //Okay - logged on... Check permissions
                mInitCurrentState = STATE_CHECKING_PERMISSIONS;
                //tell service to login to start populating CP...
                sendMessageToService(FirebaseMessengerService.MSG_ATTEMPT_LOGON, null);
                //check permissions
                mbHaveLocationPermission = checkForLocationPermission();
            }
        }

        if (mInitCurrentState == STATE_CHECKING_PERMISSIONS) {
            Log.d(TAG, "Init routine - STATE_CHECK_PERMISSIONS");
            if (mbHaveLocationPermission) {
                //Okay - got permission result (positive or negative) for location
                mInitCurrentState = STATE_CHECKING_BTPERMISSIONS;
                mbHaveBTPermission = checkForBTPermission();
            }
        }

        if (mInitCurrentState == STATE_CHECKING_BTPERMISSIONS) {
            Log.d(TAG, "Init routine - STATE_CHECK_BTPERMISSIONS");
            if (mbHaveBTPermission) {
                //Okay - got permission result (positive or negative) for location
                mInitCurrentState = STATE_SHOWING_TUTORIAL;
                mbTutorialShown = showTutorial();
            }
        }


        if (mInitCurrentState == STATE_SHOWING_TUTORIAL) {
            Log.d(TAG, "Init routine - STATE_SHOW_TUTORIAL");
            if (mbTutorialShown) {
                Log.d(TAG, "Init routine - all done");
                mInitCurrentState = STATE_APPINIT_COMPLETE;
                setResult(RESULT_OK);
                finish();
            }
        }

        /**
         * Special case the final init state. We can get a service request before onStart rebinds service. So handle that
         * here by posting the bind request. And fulfilling after bind comes back as successful.
         */
        if (mInitCurrentState == STATE_APPINIT_COMPLETE) {
            if (newEvent == STATE_EVENT_SRVC_BIND) {
                if (mbStateWaitingForBindingQuery) {
                    Log.d(TAG, "Got a delayed binding response in appInit - Query");
                    mbStateWaitingForBindingQuery = false;
                    if (!sendMessageToService(FirebaseMessengerService.MSG_QUERY_LOGON_STATUS, null)) {
                        //Uh oh - we have a real problem
                        Toast.makeText(getApplicationContext(), "Service not bound - really stuck on query", Toast.LENGTH_SHORT).show();
                    }
                }

                if (mbStateWaitingForBindingLogin) {
                    Log.d(TAG, "Got a delayed binding response in appInit - Login");
                    mbStateWaitingForBindingLogin = false;
                    if (!sendMessageToService(FirebaseMessengerService.MSG_ATTEMPT_LOGON, null)) {
                        //Uh oh - we have a real problem
                        Toast.makeText(getApplicationContext(), "Service not bound - really stuck on login", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    //Shows a welcome screen (and hides some processing behind it)
    private boolean showWelcome() {
        Intent intent = new Intent(getApplication(), HelpActivity.class);
        intent.putExtra(HelpActivity.HELP_ORDINAL, HelpActivity.WELCOME_HELP);
        if (Utils.isRunningForFirstTime(this, false)) {
            //Start activity from fragment so we get result back...
            startActivityForResult(intent, REQUEST_SHOW_WELCOME);
            return false;
        }
        return true;
    }

    //Shows a short tutorial screen (and hides some processing behind it)
    private boolean showTutorial() {
        Intent intent = new Intent(getApplication(), HelpActivity.class);
        intent.putExtra(HelpActivity.HELP_ORDINAL, HelpActivity.TUTORIAL_HELP_ATV);
        if (Utils.isRunningForFirstTime(this, false)) {
            //Start activity from fragment so we get result back...
            startActivityForResult(intent, REQUEST_SHOW_TUTORIAL);
            return false;
        }
        return true;
    }

    /**
     * Used to exit app when there is an error. Really GMS services the only error that will cause controlled exit :)
     */
    private void finishIt() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getResources().getString(R.string.app_err_title));

        alertDialogBuilder
                .setMessage(getResources().getString(R.string.gms_missing_msg))
                .setCancelable(false)
                .setNeutralButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Check if GMS services are available
     */
    private boolean isGMSAvailable() {
        boolean bret = true;
        //Does service exist? If not, can user fix?
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code != ConnectionResult.SUCCESS) {
            if (api.isUserResolvableError(code) &&
                    api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
                //Will get this call in activity result...
                bret = false;
            } else {
                //Just exit here...
                finishIt();
            }
        }
        return bret;
    }


    /**
     * Sets up GMS. And tries to tell user how to fix any errors
     */
    private void connectGMS() {
        //Next, connect...
        Log.d(TAG, "Setting up GMS");
        //next attach to GMS if not yet attached...
        //try to get location services here
        if (mActivityGoogleApiClient == null) {
            mActivityGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mActivityGoogleApiClient.connect();
        } else {
            //if activity not null, can assume we are connected...
            appInitStateMachine(STATE_EVENT_GMS_CONNECTED);
        }
    }

    /**
     * Sign in with firebase
     */
    private boolean firebaseSignIn() {
        //Sign in with firebase...
        boolean bret = false;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // already signed in
            //store in prefs
            Log.d(TAG, "Firebase signed in with " + auth.getCurrentUser().getEmail());
            String user = auth.getCurrentUser().getUid();
            //does the user match what has been stored?
            Utils.setUserId(this, user);
            bret = true;
        } else {
            //not signed in
            Log.d(TAG, "Firebase starting sign in activity");
            //Set firebase
            startActivityForResult(
                    AuthUI.getInstance(FirebaseApp.getInstance())
                            .createSignInIntentBuilder()
                            .setProviders(AuthUI.GOOGLE_PROVIDER)
                            .build(),
                    REQUEST_FIREBASE_SIGN_IN);
        }
        return bret;
    }

    /**
     * Checks for location permission
     * @return
     */
    private boolean checkForLocationPermission() {
        boolean bret = false;

        //Make sure we have location permissions at start
        int permissionCheck = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                String[] permissions = {android.Manifest.permission.ACCESS_COARSE_LOCATION};
                //and only need to do this for SDK23...
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //below line will cause dialog to come up and pause/resume this activity...
                    requestPermissions(permissions, MY_PERMISSIONS_REQUEST_LOCATION);
                }
            }
        } else {
            bret = true;
            //we have permission - get location...
            String location = Utils.getLocation(this, mActivityGoogleApiClient);
            //set this cached value...
            if (location != null) {
                Utils.setCachedLocation(this, location);
            }
        }
        return bret;
    }

    /**
     * Checks for location permission
     * @return
     */
    private boolean checkForBTPermission() {
        boolean bret = false;

        //Make sure we have location permissions at start
        int permissionCheck = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.BLUETOOTH_ADMIN)) {
                String[] permissions = {Manifest.permission.BLUETOOTH_ADMIN};
                //and only need to do this for SDK23...
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //below line will cause dialog to come up and pause/resume this activity...
                    requestPermissions(permissions, MY_PERMISSIONS_REQUEST_BT);
                }
            }
        } else {
            bret = true;
        }
        return bret;
    }


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
                //Now get the location
                String location = Utils.getLocation(this, mActivityGoogleApiClient);
                //set this cached value...
                Utils.setCachedLocation(this, location);
            } else {
                Log.d(TAG, "location priviledge DENIED. Grr...");
                Utils.setCachedLocation(this, null);
            }
            appInitStateMachine(STATE_EVENT_PERMISSION_CHECKED);
        } else if (MY_PERMISSIONS_REQUEST_BT == requestCode) {
            // Is there a result array? (should be if okay chosen)
            if ((grantResults.length > 0)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Yay! We get admin! Note we don't use except for name changes - so this is for later
                Log.d(TAG, "btadmin priviledge granted");
            } else {
                Log.d(TAG, "btadmin priviledge DENIED. Grr...");
            }
            appInitStateMachine(STATE_EVENT_BTPERMISSION_CHECKED);
        }
    }

    /*
    Callbacks for google services
    */
    @Override
    public void onConnected(Bundle bundle) {
        // Display the connection status
        Log.d(TAG, "Success - MainFragment google GMS services connect");

        // Note that we defer most of our initialization until after google GMS connects. Do that here...
        appInitStateMachine(STATE_EVENT_GMS_CONNECTED);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "MainFragment Failed google GMS services connect - result:" + result);
        //We are not connected - exit
        finishIt();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "MainFragment Suspended google GMS services connect - cause:" + i);
        //We are not connected
        //Try to connect again
        mActivityGoogleApiClient.connect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult - code " + requestCode);
        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            appInitStateMachine(STATE_EVENT_GMS_AVAILABLE);
        } else if (requestCode == REQUEST_SHOW_TUTORIAL) {
            appInitStateMachine(STATE_EVENT_TUTORIAL_DONE);
        } else if (requestCode == REQUEST_SHOW_WELCOME) {
            appInitStateMachine(STATE_EVENT_WELCOME_DONE);
        } else if (requestCode == REQUEST_FIREBASE_SIGN_IN) {
            Log.d(TAG, "In firebase logon...");
            if (resultCode == Activity.RESULT_OK) {
                // user is signed in!
                Log.d(TAG, "Firebase signed in");
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String user = auth.getCurrentUser().getUid();
                Utils.setUserId(this, user);
            } else {
                Log.d(TAG, "Firebase failure");
                Toast.makeText(this, "Firebase failure...", Toast.LENGTH_LONG);
                // user is not signed in. We are brutal in our requirements (but app makes no sense if user not signed in)
                finishIt();
            }
            appInitStateMachine(STATE_EVENT_FIREBASE_LOGON);
        }
    }
}
