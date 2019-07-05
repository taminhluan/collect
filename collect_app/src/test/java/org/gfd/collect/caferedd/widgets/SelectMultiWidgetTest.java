package org.gfd.collect.caferedd.widgets;

import androidx.annotation.NonNull;

import org.gfd.collect.caferedd.widgets.base.GeneralSelectMultiWidgetTest;

/**
 * @author James Knight
 */

public class SelectMultiWidgetTest extends GeneralSelectMultiWidgetTest<SelectMultiWidget> {
    @NonNull
    @Override
    public SelectMultiWidget createWidget() {
        return new SelectMultiWidget(activity, formEntryPrompt);
    }
}
