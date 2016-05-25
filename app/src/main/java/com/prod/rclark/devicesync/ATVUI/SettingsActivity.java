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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by rclark on 4/21/16.
 */
public class SettingsActivity extends Activity {

    //Two things can come out of settings - update the UI or update the CP. Track it here...
    public static boolean mbUpdateCP = false;
    public static boolean mbUpdateUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

    @Override
    public void finishAfterTransition() {
        //Save off what we should do...
        Intent data = new Intent();
        int retflags = MainFragment.PREF_DO_NOTHING;

        if (mbUpdateCP) {
            retflags |= MainFragment.PREF_UPDATE_CP_FLAG;
        }

        if (mbUpdateUI) {
            retflags |= MainFragment.PREF_UPDATE_UI_FLAG;
        }

        //shove the flags into return intent...
        data.putExtra(MainFragment.PREF_RESULT_KEY, retflags);
        setResult(RESULT_OK, data);

        super.finishAfterTransition();
    }
}
