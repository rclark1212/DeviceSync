package com.prod.rclark.devicesync.PhoneUI;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.prod.rclark.devicesync.R;

/**
 * Created by rclar on 7/17/2016.
 */
public class NavTrayAdapter extends ArrayAdapter<String> {

    private final Context mCtx;
    private final int mLayoutResourceId;
    private String mData[] = null;
    private int mRemoteRow = 0;

    public NavTrayAdapter(Context context, int layoutResourceId, String[] data, int remoteRow) {
        super(context, layoutResourceId, data);
        mCtx = context;
        mLayoutResourceId = layoutResourceId;
        mData = data;
        mRemoteRow = remoteRow;
    }

    @Override
    public boolean areAllItemsEnabled() {
        if (mRemoteRow < 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isEnabled(int position) {
        if (position == mRemoteRow) {
            return false;
        } else {
            return true;
        }
    }

    //And handle indenting here for remotes...
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = ((Activity) parent.getContext()).getLayoutInflater();

        View v = inflater.inflate(mLayoutResourceId, parent, false);

        TextView textView = (TextView) v.findViewById(android.R.id.text1);

        textView.setText(mData[position]);

        //Okay - fixup now. Text color and indenting
        if (position < mRemoteRow) {
            //Normal options
            textView.setTextColor(mCtx.getResources().getColor(R.color.primary_text));
        } else if (position == mRemoteRow) {
            //Header
            textView.setTextColor(mCtx.getResources().getColor(R.color.secondary_text));
        } else {
            //Remotes
            textView.setTextColor(mCtx.getResources().getColor(R.color.primary_text));
            float indent = mCtx.getResources().getDimension(R.dimen.phone_navtray_indent);
            textView.setTranslationX(indent);
        }

        return v;
    }
}
