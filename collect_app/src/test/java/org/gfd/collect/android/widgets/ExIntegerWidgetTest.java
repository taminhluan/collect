package org.gfd.collect.android.widgets;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.IntegerData;
import org.gfd.collect.android.widgets.base.GeneralExStringWidgetTest;

import static org.mockito.Mockito.when;

/**
 * @author James Knight
 */

public class ExIntegerWidgetTest extends GeneralExStringWidgetTest<ExIntegerWidget, IntegerData> {

    @NonNull
    @Override
    public ExIntegerWidget createWidget() {
        return new ExIntegerWidget(activity, formEntryPrompt);
    }

    @NonNull
    @Override
    public IntegerData getNextAnswer() {
        return new IntegerData(randomInteger());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(formEntryPrompt.getAppearanceHint()).thenReturn("");
    }

    private int randomInteger() {
        return Math.abs(random.nextInt()) % 1_000_000_000;
    }
}
