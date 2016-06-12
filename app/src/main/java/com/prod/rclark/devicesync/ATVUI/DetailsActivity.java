/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.util.Log;

import com.prod.rclark.devicesync.R;

/*
 * Details activity class that loads LeanbackDetailsFragment class
 */
public class DetailsActivity extends Activity {
    public static final String SHARED_ELEMENT_NAME = "hero";
    public static final String OBJECTURI = "ObjectUri";
    public static String mOpenSerial;               //returns the serial number of the row to open
    public static int mReturnCode = 0;              //set an ordinal on return to indicate what to do
    private static final String TAG = "VideoDetailsActivity";

    //return codes
    //leave null at 0
    public static final int DETAIL_RETCODE_OPENROW = 1;         //open row
    public static final int DETAIL_RETCODE_INSTALLMISSING = 2;  //install missing
    public static final int DETAIL_RETCODE_REMOVEDEVICE = 3;    //change device name
    public static final int DETAIL_RETCODE_CLONEFROM = 4;       //clone from device


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
    }

    @Override
    public void finishAfterTransition() {
        //Save off what we should do...
        Intent data = new Intent();

        if (mReturnCode == DETAIL_RETCODE_OPENROW) {
            Log.d(TAG, "Setting up open serial return");
            data.putExtra(MainFragment.DETAILS_RESULT_KEY, mOpenSerial);
            data.putExtra(MainFragment.DETAILS_RESULT_ACTION, DETAIL_RETCODE_OPENROW);
        } else if (mReturnCode == DETAIL_RETCODE_INSTALLMISSING) {
            Log.d(TAG, "Setting up install missing return");
            data.putExtra(MainFragment.DETAILS_RESULT_KEY, mOpenSerial);
            data.putExtra(MainFragment.DETAILS_RESULT_ACTION, DETAIL_RETCODE_INSTALLMISSING);
        } else if (mReturnCode == DETAIL_RETCODE_REMOVEDEVICE) {
            Log.d(TAG, "Setting up remove device return");
            data.putExtra(MainFragment.DETAILS_RESULT_KEY, mOpenSerial);
            data.putExtra(MainFragment.DETAILS_RESULT_ACTION, DETAIL_RETCODE_REMOVEDEVICE);
        } else if (mReturnCode == DETAIL_RETCODE_CLONEFROM) {
            Log.d(TAG, "Setting up clone from return");
            data.putExtra(MainFragment.DETAILS_RESULT_KEY, mOpenSerial);
            data.putExtra(MainFragment.DETAILS_RESULT_ACTION, DETAIL_RETCODE_CLONEFROM);
        }

        mReturnCode = 0;
        setResult(RESULT_OK, data);

        super.finishAfterTransition();
    }
}
