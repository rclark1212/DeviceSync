package com.example.rclark.devicesync.ATVUI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.example.rclark.devicesync.R;

/**
 * Created by rclarlk on 4/21/2016.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    //TODO - complete... See http://developer.android.com/intl/es/guide/topics/ui/settings.html

    /**
     * Okay, this routine called when shared preferences change. Check to see what changed to see if we need to rebuild UI
     * (for example, a row gets hidden/unhidden)
     * @param sharedPreferences
     * @param key
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //TODO - implement
    }
}
