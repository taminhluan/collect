package org.gfd.collect.android.widgets;

import androidx.annotation.NonNull;

import org.gfd.collect.android.widgets.base.GeneralSelectOneWidgetTest;

/**
 * @author James Knight
 */

public class SpinnerWidgetTest extends GeneralSelectOneWidgetTest<SpinnerWidget> {
    @NonNull
    @Override
    public SpinnerWidget createWidget() {
        return new SpinnerWidget(activity, formEntryPrompt, false);
    }
}
