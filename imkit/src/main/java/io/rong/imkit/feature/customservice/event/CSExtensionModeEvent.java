package io.rong.imkit.feature.customservice.event;

import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imlib.cs.model.CustomServiceMode;

public class CSExtensionModeEvent implements PageEvent {
    public CustomServiceMode mCustomServiceMode;

    public CSExtensionModeEvent(CustomServiceMode mode) {
        mCustomServiceMode = mode;
    }
}
