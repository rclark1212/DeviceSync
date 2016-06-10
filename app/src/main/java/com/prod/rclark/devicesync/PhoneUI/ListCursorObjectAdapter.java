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

package com.prod.rclark.devicesync.PhoneUI;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.HelpActivity;
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

/**
 * Created by rclark on 4/13/2016.
 * Pulled a nice implementation of cursor adapter + recycler view from
 * http://stackoverflow.com/questions/26517855/using-the-recyclerview-with-a-database
 *
 */
public class ListCursorObjectAdapter extends RecyclerView.Adapter <ListCursorObjectAdapter.ViewHolder> {
    private CursorAdapter mCursorAdapter;
    private Context mCtx;
    private boolean mbIsDevice;
    private int mRowPosition;

    // Provide a constructor
    public ListCursorObjectAdapter(Context ctx, Cursor c, boolean isDevice, int rowposition) {
        mCtx = ctx;
        mbIsDevice = isDevice;
        mRowPosition = rowposition;
        mCursorAdapter = new CursorAdapter(mCtx, c, 0) {
            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                if ( parent instanceof RecyclerView ) {
                    // Inflate the view here
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.phone_list_item, parent, false);
                    view.setFocusable(true);
                    return view;
                } else {
                    throw new RuntimeException("Not bound to RecyclerView");
                }
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {

                // Binding operations
                final ImageView iconView = (ImageView) view.findViewById(R.id.grid_item_image);
                TextView titleView = (TextView) view.findViewById(R.id.grid_title);
                TextView subtitleView = (TextView) view.findViewById(R.id.grid_subtitle);
                String serial;
                long type;

                if (!mbIsDevice) {
                    serial = cursor.getString(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_DEVSSN));
                    type = cursor.getLong(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_TYPE));
                } else {
                    serial = cursor.getString(cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICES_SSN));
                    type = cursor.getLong(cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_TYPE));
                }

                //Set tags... Set title tag to serial
                titleView.setTag(serial);

                // FIXME - only binding app for now... (and depending on app cursor being passed in)
                Drawable banner = null;
                if (!mbIsDevice) {
                    String apk = cursor.getString(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG));
                    titleView.setText(cursor.getString(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL)));
                    subtitleView.setText(apk);

                    //tag view with apk
                    subtitleView.setTag(apk);

                    //See if there is a local banner...
                    banner = AppUtils.getLocalApkImage(view.getContext(), apk, type);

                    if (banner != null) {
                        //local app has image...
                        iconView.setImageDrawable(banner);
                    } else {
                        //get the download URL...
                        ImageDetail image = DBUtils.getImageRecordFromCP(view.getContext(), apk);
                        //glide it in
                        if (image != null) {
                            //glide it in
                            Glide.with(view.getContext())
                                    .load(image.download_url)
                                    .centerCrop()
                                    .error(view.getResources().getDrawable(R.drawable.noimage))
                                    .into(new SimpleTarget<GlideDrawable>() {
                                        @Override
                                        public void onResourceReady(GlideDrawable resource,
                                                                    GlideAnimation<? super GlideDrawable>
                                                                            glideAnimation) {
                                            iconView.setImageDrawable(resource);
                                        }
                                    });
                        } else {
                            iconView.setImageDrawable(view.getResources().getDrawable(R.drawable.noimage));
                        }
                    }
                } else {
                    titleView.setText(cursor.getString(cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICE_NAME)));
                    subtitleView.setText(cursor.getString(cursor.getColumnIndex(AppContract.DevicesEntry.COLUMN_DEVICES_SSN)));

                    //set subtitle tag to null for devices
                    subtitleView.setTag(null);

                    if (type == AppContract.TYPE_ATV) {
                        banner = view.getResources().getDrawable(R.drawable.shieldtv);
                    } else {
                        banner = view.getResources().getDrawable(R.drawable.shieldtablet);
                    }
                    iconView.setImageDrawable(banner);
                }
            }
        };
    }


    /**
     * Cache of child views for a listobject list item
     */
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ViewHolder(View view) {
            super(view);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            //process the click - get what we need and then launch detail...
            TextView title = (TextView) v.findViewById(R.id.grid_title);
            TextView subtitle = (TextView) v.findViewById(R.id.grid_subtitle);

            String display = "clicky";
            Object serial = title.getTag();
            Object apk = subtitle.getTag();
            if (serial instanceof String) {
                display = display + " " + (String)serial;
            }

            if (apk instanceof String) {
                display = display + " " + (String)apk;
            }

            Toast.makeText(v.getContext(), display, Toast.LENGTH_SHORT).show();

            //Kick off detail activity
            //set up the base intent
            Intent intent = new Intent(v.getContext(), PhoneDetailActivity.class);

            //add the position here as well...
            intent.putExtra(MainPhoneActivity.EXTRA_LIST_POSITION, adapterPosition);
            intent.putExtra(MainPhoneActivity.EXTRA_ROW_POSITION, mRowPosition);

            //late binding setting of transition name
            ImageView iconView = (ImageView) v.findViewById(R.id.grid_item_image);
            iconView.setTransitionName(v.getResources().getString(R.string.transition) + adapterPosition);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation((Activity)mCtx, iconView,
                    iconView.getTransitionName());

            v.getContext().startActivity(intent, options.toBundle());
        }

    }

    @Override
    public int getItemCount() {
        return mCursorAdapter.getCount();
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Passing the binding operation to cursor loader
        mCursorAdapter.getCursor().moveToPosition(position);
        mCursorAdapter.bindView(holder.itemView, mCtx, mCursorAdapter.getCursor());

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Passing the inflater job to the cursor-adapter
        View v = mCursorAdapter.newView(mCtx, mCursorAdapter.getCursor(), parent);
        return new ViewHolder(v);
    }
}
