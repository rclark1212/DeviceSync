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

import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;

import com.prod.rclark.devicesync.DBUtils;
import com.prod.rclark.devicesync.ObjectDetail;
import com.prod.rclark.devicesync.R;
import com.prod.rclark.devicesync.UIUtils;
import com.prod.rclark.devicesync.Utils;

public class DetailsDescriptionPresenter extends AbstractDetailsDescriptionPresenter {

    @Override
    protected void onBindDescription(ViewHolder viewHolder, Object item) {
        ObjectDetail element = (ObjectDetail) item;

        if (element != null) {
            String title = element.label;
            if (!DBUtils.isObjectLocal(viewHolder.view.getContext(), element)) {
                title = String.format(viewHolder.view.getResources().getString(R.string.append_remote), title);
            } else {
                title = String.format(viewHolder.view.getResources().getString(R.string.append_local), title);
            }

            if (element.bIsDevice) {
                viewHolder.getTitle().setText(title);
                viewHolder.getSubtitle().setText(element.name);
                viewHolder.getBody().setText(UIUtils.getObjectDetailDescription(viewHolder.view.getContext(), element));
            } else {
                viewHolder.getTitle().setText(title);
                viewHolder.getSubtitle().setText(element.pkg);
                viewHolder.getBody().setText(UIUtils.getObjectDetailDescription(viewHolder.view.getContext(), element));
            }
        }
    }
}
