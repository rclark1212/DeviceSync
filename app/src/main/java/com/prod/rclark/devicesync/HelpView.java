package com.prod.rclark.devicesync;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Button;

/**
 * Created by rclark on 6/7/2016.
 * Class to show help screens as well as tutorial up front.
 */
public class HelpView extends Activity {
    public WebView mWView;
    public Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWView = (WebView) findViewById(R.id.helpview);
        mButton = (Button) findViewById(R.id.helpbutton);
    }

}
