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

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.example.rclark.devicesync.DBUtils;
import com.example.rclark.devicesync.ObjectDetail;
import com.example.rclark.devicesync.Utils;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        ObjectDetail element = (ObjectDetail) item;

        if (element != null) {
            if (element.bIsDevice) {
                viewHolder.getTitle().setText(element.label);
                viewHolder.getSubtitle().setText(element.name);
                String body = "Serial: " + element.serial + "\nLocation: " + element.location +
                        "\nOSVer: " + element.ver + "\nUpdated: " + Utils.unNormalizeDate(element.installDate);
                viewHolder.getBody().setText(body);
            } else {
                viewHolder.getTitle().setText(element.label);
                viewHolder.getSubtitle().setText(element.pkg);
                String body = "Version: " + element.ver + "\nInstallDate: " + Utils.unNormalizeDate(element.installDate) + "\nCount: " + DBUtils.countApp(viewHolder.view.getContext(),element.pkg);
                viewHolder.getBody().setText(body);
            }
        }
    }
}
