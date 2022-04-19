package io.rong.imkit.feature.customservice.event;

import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imlib.cs.CustomServiceConfig;

public class CSExtensionConfigEvent implements PageEvent {
    public CustomServiceConfig mConfig;

    public CSExtensionConfigEvent(CustomServiceConfig config) {
        mConfig = config;
    }
}
