package com.prod.rclark.devicesync;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;

/**
 * Created by rclark on 6/7/2016.
 * Class to show help screens as well as tutorial up front.
 */
public class HelpActivity extends Activity {
    public WebView mWView;
    public Button mButton;
    private static final String TUTORIAL_HELP = "file:///android_asset/device_sync_tutorial.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        mWView = (WebView) findViewById(R.id.helpview);
        mButton = (Button) findViewById(R.id.helpbutton);

        //Pass in which screen to look for in savedInstanceState... For now, just hardcode
        mWView.loadUrl(TUTORIAL_HELP);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //just go back...
                onBackPressed();
            }
        });
    }

}
