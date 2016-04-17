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

package com.example.rclark.devicesync.ATVUI;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.rclark.devicesync.DBUtils;
import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.R;
import com.example.rclark.devicesync.data.AppContract;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand. 
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;
    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;
    private Drawable mDefaultCardImage;

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Log.d(TAG, "onCreateViewHolder");

        sDefaultBackgroundColor = parent.getResources().getColor(R.color.default_background);
        sSelectedBackgroundColor = parent.getResources().getColor(R.color.selected_background);
        mDefaultCardImage = parent.getResources().getDrawable(R.drawable.noimage);

        ImageCardView cardView = new ImageCardView(parent.getContext()) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
            }
        };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        ObjectDetail element = (ObjectDetail) item;
        ImageCardView cardView = (ImageCardView) viewHolder.view;

        Log.d(TAG, "onBindViewHolder");
        if (element.label.length() > 0) {
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);

            //decorate the card
            decorateCardViewImage(cardView, element);

            if (element.bIsDevice) {
                cardView.setTitleText(element.label + " (" + element.serial + ")");
                cardView.setContentText(element.name);
                Drawable drawable;
                if (element.type == AppContract.TYPE_ATV) {
                    drawable = cardView.getResources().getDrawable(R.drawable.shieldtv);
                } else {
                    drawable = cardView.getResources().getDrawable(R.drawable.shieldtablet);
                }
                cardView.setMainImage(drawable);
            } else {
                cardView.setTitleText(element.label);
                cardView.setContentText(element.pkg);
                if (element.banner != null) {
                    cardView.setMainImage(element.banner);
                } else {
                    cardView.setMainImage(mDefaultCardImage);
                }
            }
        }
    }

    /**
     * Updates the view item per the element
     * For example, gray out remote. And add a badge
     * FIXME - not done yet.
     * Suggest badge = number of installs total
     * Can also be a flag (to show flagged)
     * Can also be local/remote
     */
    private void decorateCardViewImage(ImageCardView cardView, ObjectDetail element) {
        boolean bLocal = false;

        //Is this object local?
        bLocal = DBUtils.isObjectLocal(cardView.getContext(), element);

        //Is this a local or remote object?
        if (!bLocal) {
            cardView.setBadgeImage(ContextCompat.getDrawable(cardView.getContext(), R.drawable.star_icon));
            //fade it out a bit (I know, expensive)
            cardView.setAlpha(0.6f);
            //get the text views
            TextView title = (TextView) cardView.findViewById(R.id.title_text);
            TextView subtitle = (TextView) cardView.findViewById(R.id.content_text);
            if (title != null) {
                title.setTextColor(ContextCompat.getColor(cardView.getContext(), R.color.remote_text));
            }
            if (subtitle != null) {
                title.setTextColor(ContextCompat.getColor(cardView.getContext(), R.color.remote_text));
            }
        } else {
            //view may have been recycled... restore it
            cardView.setBadgeImage(null);
            //fade it out a bit (I know, expensive)
            cardView.setAlpha(1.0f);
            //get the text views
            TextView title = (TextView) cardView.findViewById(R.id.title_text);
            TextView subtitle = (TextView) cardView.findViewById(R.id.content_text);
            if (title != null) {
                title.setTextColor(Color.WHITE);
            }
            if (subtitle != null) {
                title.setTextColor(Color.WHITE);
            }
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        Log.d(TAG, "onUnbindViewHolder");
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
