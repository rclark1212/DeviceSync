package com.prod.rclark.devicesync.PhoneUI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prod.rclark.devicesync.ATVUI.SettingsActivity;
import com.prod.rclark.devicesync.R;

/**
 * Created by rclark on 4/26/2016.
 */
public class PhoneSettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsActivity.mbUpdateCP = false;
        SettingsActivity.mbUpdateUI = false;
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        //register listener
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //unregister
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Okay, this routine called when shared preferences change. Check to see what changed to see if we need to rebuild UI
     * (for example, a row gets hidden/unhidden)
     *
     * @param sharedPreferences
     * @param key
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //If we update the system apps, reset CP
        if (key.equals(getResources().getString(R.string.key_pref_ignore_system_apps))) {
            PhoneSettingsActivity.mbUpdateCP = true;
        }

        //If we update the hide rows, update the UI
        if (key.equals(getResources().getString(R.string.key_pref_disable_rows))) {
            PhoneSettingsActivity.mbUpdateUI = true;
        }
    }
}
