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

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.prod.rclark.devicesync.AppUtils;
import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ImageDetail;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.Utils;
import com.prod.rclark.devicesync.data.AppContract;

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
        //Log.d(TAG, "onCreateViewHolder");

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

        //Log.d(TAG, "onBindViewHolder");
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
                //Use glide for all image sets...
                Glide.with(cardView.getContext())
                        .load("")
                        .placeholder(drawable)
                        .centerCrop()
                        .error(drawable)
                        .into(cardView.getMainImageView());
            } else {
                cardView.setTitleText(element.label);
                cardView.setContentText(element.pkg);
                //is image available locally?
                Drawable banner = AppUtils.getLocalApkImage(cardView.getContext(), element.pkg, element.type);

                if (banner != null) {
                    Glide.with(cardView.getContext())
                            .load("")
                            .placeholder(banner)
                            .centerCrop()
                            .error(banner)
                            .into(cardView.getMainImageView());
                } else {
                    //get the download URL...
                    ImageDetail image = DBUtils.getImageRecordFromCP(cardView.getContext(), element.pkg);
                    //glide it in
                    if (image != null) {
                        Glide.with(cardView.getContext())
                                .load(image.download_url)
                                .centerCrop()
                                .error(mDefaultCardImage)
                                .into(cardView.getMainImageView());
                    } else {
                        Glide.with(cardView.getContext())
                                .load("")
                                .placeholder(mDefaultCardImage)
                                .centerCrop()
                                .error(mDefaultCardImage)
                                .into(cardView.getMainImageView());
                    }
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

        //We have a tiered system of badging...
        //first, if app is of the wrong type, we will show the type badge (so for ATV, show tablet and vice versa)
        //if of an okay type, we next will show flag if the app has been flagged
        //if no flag, we will then show either home (for local apps) or cloud (for remote apps)
        Drawable badge = null;
        if (element.type != AppContract.TYPE_BOTH) {
            if (Utils.bIsThisATV(cardView.getContext())) {
                if (element.type == AppContract.TYPE_TABLET) {
                    //load tv
                    badge = cardView.getContext().getDrawable(R.drawable.ic_tv);
                }
            } else {
                if (element.type == AppContract.TYPE_ATV) {
                    //load tablet
                    badge = cardView.getContext().getDrawable(R.drawable.ic_tablet);
                }
            }
        }

        //tier2 - flags
        if (badge == null) {
            if (element.flags != 0) {
                badge = cardView.getContext().getDrawable(R.drawable.ic_flag);
            }
        }

        //tier3 - local or remote
        //Is this object local?
        bLocal = DBUtils.isObjectLocal(cardView.getContext(), element);
        if (badge == null) {
            if (bLocal) {
                badge = cardView.getContext().getDrawable(R.drawable.ic_home);
            } else {
                badge = cardView.getContext().getDrawable(R.drawable.ic_cloud);
            }
        }

        cardView.setBadgeImage(badge);
        //would be nice to have a number badge as well

        //Is this a local or remote object?
        if (!bLocal) {
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
        //Log.d(TAG, "onUnbindViewHolder");
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setTitleText("debugUnbind");   //for testing
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
