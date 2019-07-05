package org.gfd.collect.caferedd.widgets.interfaces;

import org.javarosa.core.model.data.IAnswerData;

/**
 * @author James Knight
 */
public interface Widget {

    IAnswerData getAnswer();

    void clearAnswer();

    void waitForData();

    void cancelWaitingForData();

    boolean isWaitingForData();
}
