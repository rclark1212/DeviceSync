package com.example.rclark.devicesync.PhoneUI;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.Utils;
import com.example.rclark.devicesync.data.AppContract;

/**
 * Created by rclark on 4/13/2016.
 * Pulled a nice implementation of cursor adapter + recycler view from
 * http://stackoverflow.com/questions/26517855/using-the-recyclerview-with-a-database
 *
 */
public class ListObjectAdapter extends RecyclerView.Adapter <ListObjectAdapter.ViewHolder> {
    //TODO - make this into the CP recycler view adapter for phone/tablet UI
    private CursorAdapter mCursorAdapter;
    private Context mCtx;

    // Provide a constructor
    public ListObjectAdapter(Context ctx, Cursor c) {
        mCtx = ctx;
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
                ImageView iconView = (ImageView) view.findViewById(R.id.grid_item_image);
                TextView titleView = (TextView) view.findViewById(R.id.grid_title);
                TextView subtitleView = (TextView) view.findViewById(R.id.grid_subtitle);

                // FIXME - only binding app for now... (and depending on app cursor being passed in)
                titleView.setText(cursor.getString(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL)));
                subtitleView.setText(cursor.getString(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_PKG)));

                //deal with bitmap...
                byte[] blob = cursor.getBlob(cursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_BANNER));
                if (blob != null) {
                    //Leave this null if package available on play store and download... Use glide or picassa
                    iconView.setImageDrawable(new BitmapDrawable(Utils.convertByteArrayToBitmap(blob)));
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
            //int appColumnIndex = mCursor.getColumnIndex(AppContract.AppEntry.COLUMN_APP_LABEL);
            //mClickHandler.onClick(mCursor.getString(appColumnIndex), this);
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
