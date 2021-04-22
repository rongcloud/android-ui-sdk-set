package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Conversation;

public class DeleteEvent {
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private int[] mMessageIds;

    public DeleteEvent(Conversation.ConversationType conversationType, String targetId, int[] messageIds) {
        this.mConversationType = conversationType;
        this.mTargetId = targetId;
        this.mMessageIds = messageIds;
    }

    public Conversation.ConversationType getConversationType() {
        return mConversationType;
    }

    public String getTargetId() {
        return mTargetId;
    }

    public int[] getMessageIds() {
        return mMessageIds;
    }
}
