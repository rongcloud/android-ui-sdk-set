package io.rong.imkit.feature.customservice.event;

import io.rong.imkit.event.uievent.PageEvent;

public class CSQuitEvent implements PageEvent {
    public String mContent;
    public boolean isEvaluate;

    public CSQuitEvent(String mContent, boolean isEvaluate) {
        this.mContent = mContent;
        this.isEvaluate = isEvaluate;
    }
}
