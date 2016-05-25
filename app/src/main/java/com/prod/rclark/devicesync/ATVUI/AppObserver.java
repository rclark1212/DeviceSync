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

import android.database.ContentObserver;
import android.net.Uri;

/**
 * Created by rclark on 4/16/2016.
 * Simple class we use as an observer for our CP changing.
 * Most changes auto-handled by using sync adapters for the UI
 * There are some controls though (title text), which need updating when backing db changes
 */
public class AppObserver extends ContentObserver {

    private ContentObserverCallback contentObserverCallback;

    public AppObserver(ContentObserverCallback contentObserverCallback) {
        // null is fine here
        super(null);
        this.contentObserverCallback = contentObserverCallback;
    }

    @Override
    public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        //send a message to update the UI (or anything else that needs updating)
        contentObserverCallback.updateFromCP(uri);
    }
}
