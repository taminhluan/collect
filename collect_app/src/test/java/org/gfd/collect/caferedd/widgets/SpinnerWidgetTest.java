package org.gfd.collect.caferedd.widgets;

import androidx.annotation.NonNull;

import org.gfd.collect.caferedd.widgets.base.GeneralSelectOneWidgetTest;

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
