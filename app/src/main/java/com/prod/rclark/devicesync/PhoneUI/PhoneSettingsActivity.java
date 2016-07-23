package com.prod.rclark.devicesync.PhoneUI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.prod.rclark.devicesync.ATVUI.MainFragment;
import com.prod.rclark.devicesync.ATVUI.SettingsFragment;

/**
 * Created by rclark on 4/26/2016.
 */
public class PhoneSettingsActivity extends Activity {

    //Two things can come out of settings - update the UI or update the CP. Track it here...
    public static boolean mbUpdateCP = false;
    public static boolean mbUpdateUI = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PhoneSettingsFragment()).commit();
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
