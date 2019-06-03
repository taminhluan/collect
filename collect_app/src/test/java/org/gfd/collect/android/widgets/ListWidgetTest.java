package org.gfd.collect.android.widgets;

import androidx.annotation.NonNull;

import org.gfd.collect.android.widgets.base.GeneralSelectOneWidgetTest;

/**
 * @author James Knight
 */

public class ListWidgetTest extends GeneralSelectOneWidgetTest<ListWidget> {
    @NonNull
    @Override
    public ListWidget createWidget() {
        return new ListWidget(activity, formEntryPrompt, false, false);
    }
}
