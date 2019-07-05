/*
 * Copyright 2018 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gfd.collect.caferedd.utilities;

import android.database.Cursor;

import org.gfd.collect.caferedd.R;
import org.gfd.collect.caferedd.application.Collect;
import org.gfd.collect.caferedd.provider.InstanceProviderAPI;

import java.util.Map;

public class InstanceUploaderUtils {

    public static final String DEFAULT_SUCCESSFUL_TEXT = "full submission upload was successful!";

    private InstanceUploaderUtils() {
    }


    /**
     * Returns a formatted message including submission results for all the filled forms accessible
     * through instancesProcessed in the following structure:
     *
     * Form name 1 - result
     *
     * Form name 2 - result
     */
    public static String getUploadResultMessage(Cursor instancesProcessed,
                                                Map<String, String> resultMessagesByInstanceId) {
        StringBuilder queryMessage = new StringBuilder();
        try {
            if (instancesProcessed != null && instancesProcessed.getCount() > 0) {
                instancesProcessed.moveToPosition(-1);
                while (instancesProcessed.moveToNext()) {
                    String name =
                            instancesProcessed.getString(
                                    instancesProcessed.getColumnIndex(InstanceProviderAPI.InstanceColumns.DISPLAY_NAME));
                    String id = instancesProcessed.getString(instancesProcessed.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));
                    String text = localizeDefaultAggregateSuccessfulText(resultMessagesByInstanceId.get(id));
                    queryMessage
                            .append(name)
                            .append(" - ")
                            .append(text)
                            .append("\n\n");
                }
            }
        } finally {
            if (instancesProcessed != null) {
                instancesProcessed.close();
            }
        }
        return String.valueOf(queryMessage);
    }

    private static String localizeDefaultAggregateSuccessfulText(String text) {
        if (text != null && text.equals(DEFAULT_SUCCESSFUL_TEXT)) {
            text = Collect.getInstance().getString(R.string.success);
        }
        return text;
    }
}