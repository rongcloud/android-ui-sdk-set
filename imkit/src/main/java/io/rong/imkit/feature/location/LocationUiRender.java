package io.rong.imkit.feature.location;

import io.rong.imkit.conversation.messgelist.processor.IConversationUIRenderer;

/** @author gusd @Date 2022/05/15 */
public abstract class LocationUiRender implements IConversationUIRenderer {
    private static final String TAG = "LocationUiRender";

    public abstract void joinLocation();
}
