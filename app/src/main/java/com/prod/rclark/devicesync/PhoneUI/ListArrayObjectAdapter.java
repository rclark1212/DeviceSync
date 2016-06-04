package com.prod.rclark.devicesync.PhoneUI;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
    private Context mCtx;

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public static class ViewHolder extends RecyclerView.ViewHolder {
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
        }
    }

    // Provide a constructor
    public ListArrayObjectAdapter(Context ctx, ArrayList<ObjectDetail> array) {
        mCtx = ctx;
        mArray = array;
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

        titleView.setText(app.label);
        subtitleView.setText(app.pkg);

        Drawable banner = null;
        if (Build.SERIAL.equals(app.serial)) {
            //local app...
            banner = AppUtils.getLocalApkImage(iconView.getContext(), app.pkg, app.type);
        }

        //deal with bitmap...
        if (banner != null) {
            iconView.setImageDrawable(banner);
        } else {
            ImageDetail image = DBUtils.getImageRecordFromCP(iconView.getContext(), app.pkg);

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
        }

        /*
        //deal with bitmap...
        if (app.banner != null) {
            //Leave this null if package available on play store and download... Use glide or picassa
            iconView.setImageDrawable(app.banner);
        } */

    }

    // Return the total count of items
    @Override
    public int getItemCount() {
        return mArray.size();
    }

}
