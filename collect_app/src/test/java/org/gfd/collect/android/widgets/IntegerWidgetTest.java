package org.gfd.collect.android.widgets;

import androidx.annotation.NonNull;

import org.javarosa.core.model.data.IntegerData;
import org.gfd.collect.android.widgets.base.GeneralStringWidgetTest;

import java.util.Random;

/**
 * @author James Knight
 */
public class IntegerWidgetTest extends GeneralStringWidgetTest<IntegerWidget, IntegerData> {

    @NonNull
    @Override
    public IntegerWidget createWidget() {
        Random random = new Random();
        boolean useThousandSeparator = random.nextBoolean();
        return new IntegerWidget(activity, formEntryPrompt, false, useThousandSeparator);
    }

    @NonNull
    @Override
    public IntegerData getNextAnswer() {
        return new IntegerData(randomInteger());
    }

    private int randomInteger() {
        return Math.abs(random.nextInt()) % 1_000_000_000;
    }
}
