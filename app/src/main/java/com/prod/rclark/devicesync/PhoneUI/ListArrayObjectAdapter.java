package com.prod.rclark.devicesync.PhoneUI;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
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
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;

import java.util.ArrayList;

/**
 * Created by rclark on 4/26/16.
 */

public class ListArrayObjectAdapter extends RecyclerView.Adapter <ListArrayObjectAdapter.ViewHolder> {
    private ArrayList<ObjectDetail> mArray;
    private Activity mActivity;
    private int mRowPosition;

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView titleText;
        public TextView subTitleText;
        public ImageView image;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            super(itemView);

            image = (ImageView) itemView.findViewById(R.id.grid_item_image);
            titleText = (TextView) itemView.findViewById(R.id.grid_title);
            subTitleText = (TextView) itemView.findViewById(R.id.grid_subtitle);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int position = getAdapterPosition();
            ImageView clicked = (ImageView) v.findViewById(R.id.grid_item_image);
            /*
            String display = "clicky";
            Object object = clicked.getTag();
            if (object instanceof ObjectDetail) {
                display = "clickster of " + ((ObjectDetail) object).serial + " " + ((ObjectDetail) object).pkg;
            }
            Toast.makeText(v.getContext(), display, Toast.LENGTH_SHORT).show();
            */

            //Kick off detail activity
            //set up the base intent
            Intent intent = new Intent(v.getContext(), PhoneDetailActivity.class);

            //add the position here as well...
            intent.putExtra(MainPhoneActivity.EXTRA_LIST_POSITION, position);
            intent.putExtra(MainPhoneActivity.EXTRA_ROW_POSITION, mRowPosition);

            //late binding setting of transition name
            ImageView iconView = (ImageView) v.findViewById(R.id.grid_item_image);
            iconView.setTransitionName(v.getResources().getString(R.string.transition) + position);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(mActivity, iconView,
                    iconView.getTransitionName());

            //v.getContext().startActivity(intent, options.toBundle());
            mActivity.startActivityForResult(intent, MainPhoneActivity.PHONE_DETAILS_REQUEST_CODE, options.toBundle());
        }
    }

    // Provide a constructor
    public ListArrayObjectAdapter(Activity activity, ArrayList<ObjectDetail> array, int rowposition) {
        mActivity = activity;
        mArray = array;
        mRowPosition = rowposition;
    }


    @Override
    public ListArrayObjectAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View view = inflater.inflate(R.layout.phone_list_item, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(ListArrayObjectAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        ObjectDetail app = mArray.get(position);
        // Binding operations
        final ImageView iconView = (ImageView) viewHolder.image;
        TextView titleView = (TextView) viewHolder.titleText;
        TextView subtitleView = (TextView) viewHolder.subTitleText;

        //tag this view - object or position? object safest...
        iconView.setTag(app);

        //But we can tag the top level view itself with position
        viewHolder.itemView.setTag(position);

        titleView.setText(app.label);
        subtitleView.setText(app.pkg);

        Drawable banner = null;
        //is there a local app who can supply image?...
        banner = AppUtils.getLocalApkImage(iconView.getContext(), app.pkg, app.type);

        //deal with bitmap...
        if (banner != null) {
            iconView.setImageDrawable(banner);
        } else {
            ImageDetail image = DBUtils.getImageRecordFromCP(iconView.getContext(), app.pkg);
            if (image != null) {
                //glide it in
                Glide.with(iconView.getContext())
                        .load(image.download_url)
                        .centerCrop()
                        .into(new SimpleTarget<GlideDrawable>() {
                            @Override
                            public void onResourceReady(GlideDrawable resource,
                                                        GlideAnimation<? super GlideDrawable>
                                                                glideAnimation) {
                                iconView.setImageDrawable(resource);
                            }
                        });
            } else {
                iconView.setImageDrawable(iconView.getResources().getDrawable(R.drawable.noimage));
            }
        }

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mArray.size();
    }

}
