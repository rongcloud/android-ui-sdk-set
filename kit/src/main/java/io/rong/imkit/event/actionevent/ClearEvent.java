package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Conversation;

public class ClearEvent {

    private Conversation.ConversationType mConversationType;
    private String mTargetId;

    public ClearEvent(Conversation.ConversationType conversationType, String targetId) {
        mConversationType = conversationType;
        mTargetId = targetId;
    }

    public Conversation.ConversationType getConversationType() {
        return mConversationType;
    }

    public String getTargetId() {
        return mTargetId;
    }
}
