package com.example.rclark.devicesync.ATVUI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.rclark.devicesync.R;

/**
 * Created by rclark on 4/21/2016.
 */
public class SettingsFragment extends LeanbackPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);

        // Load the preferences from an XML resource
        //addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onViewCreated (View view, Bundle savedInstanceState) {

    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootkey) {
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
