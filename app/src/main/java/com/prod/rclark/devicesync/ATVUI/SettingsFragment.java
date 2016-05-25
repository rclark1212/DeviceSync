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

package com.prod.rclark.devicesync.ATVUI;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.prod.rclark.devicesync.R;

/**
 * Created by rclark on 4/21/2016.
 */
public class SettingsFragment extends LeanbackPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        SettingsActivity.mbUpdateCP = false;
        SettingsActivity.mbUpdateUI = false;
        // Load the preferences from an XML resource
        //addPreferencesFromResource(R.xml.preferences);
        return v;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootkey) {
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        //register listener
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //unregister
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    //TODO - complete... See http://developer.android.com/intl/es/guide/topics/ui/settings.html

    /**
     * Okay, this routine called when shared preferences change. Check to see what changed to see if we need to rebuild UI
     * (for example, a row gets hidden/unhidden)
     * @param sharedPreferences
     * @param key
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //If we update the system apps, reset CP
        if (key.equals(getResources().getString(R.string.key_pref_ignore_system_apps))) {
            SettingsActivity.mbUpdateCP = true;
        }

        //If we update the hide rows, update the UI
        if (key.equals(getResources().getString(R.string.key_pref_disable_rows))) {
            SettingsActivity.mbUpdateUI = true;
        }
    }

}
