package com.example.rclark.devicesync.ATVUI;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by rclark on 4/21/16.
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }
}
