package org.gfd.collect.caferedd.widgets;

import androidx.annotation.NonNull;

import org.gfd.collect.caferedd.widgets.base.GeneralSelectOneWidgetTest;

/**
 * @author James Knight
 */

public class GridWidgetTest extends GeneralSelectOneWidgetTest<GridWidget> {
    @NonNull
    @Override
    public GridWidget createWidget() {
        return new GridWidget(activity, formEntryPrompt, 1, false);
    }
}
