package org.gfd.collect.caferedd.widgets;

import androidx.annotation.NonNull;

import org.gfd.collect.caferedd.widgets.base.GeneralSelectOneWidgetTest;

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
