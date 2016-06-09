package com.prod.rclark.devicesync;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ScrollView;

/**
 * Created by rclark on 6/7/2016.
 * Class to show help screens as well as tutorial up front.
 */
public class HelpActivity extends Activity {
    private WebView mWView;
    private Button mButton;

    private final static int PAN_SCALE_FACTOR = 40;

    //our assets
    public static final String TUTORIAL_HELP_ATV = "file:///android_asset/device_sync_tutorial_atv.html";
    public static final String TUTORIAL_HELP_TABLET = "file:///android_asset/device_sync_tutorial_tablet.html";
    public static final String WELCOME_HELP = "file:///android_asset/device_sync_welcome.html";
    public static final String HELP = "file:///android_asset/device_sync_help.html";
    public static final String HELP_ORDINAL = "help";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        String help = null;

        //get passed in params...
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            help = extras.getString(HELP_ORDINAL);
        }

        mWView = (WebView) findViewById(R.id.helpview);
        mButton = (Button) findViewById(R.id.helpbutton);

        //Default to help screen if there is no file passed in...
        if (help == null) {
            mWView.loadUrl(HELP);
        } else if (help.equals(WELCOME_HELP)
                || help.equals(TUTORIAL_HELP_ATV)
                || help.equals(TUTORIAL_HELP_TABLET)) {
            mWView.loadUrl(help);
        } else {
            mWView.loadUrl(HELP);
        }

        mButton.requestFocus();
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /*
    Handle left stick controller move events for panning here
 */
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event != null) {
            //Check that this is a move action (rather than hover)
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float y = event.getAxisValue(MotionEvent.AXIS_Y);

                //scale up the events...
                int scale = PAN_SCALE_FACTOR;

                int py = (int) (y * scale);

                //if we have movement, move...
                if (py != 0) {
                    mWView.scrollBy(0, py);
                }
            }
        }

        return super.dispatchGenericMotionEvent(event);
    }

    /*
    Handle the controller shortcuts for the buttons
    Always go back on A...
 */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean bEatKey = false;    //some keys need to be eaten (voice search, back button)
        if (event != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_A) {
                    bEatKey = true;
                    setResult(RESULT_OK);
                    finish();
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN) {
                    bEatKey = true;
                    mWView.scrollBy(0, 20);
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) {
                    bEatKey = true;
                    mWView.scrollBy(0, -20);
                }
            }
        }
        //If we want to eat the key, return true here...
        if (bEatKey) {
            return true;
        }

        return super.dispatchKeyEvent(event);
    }
}
