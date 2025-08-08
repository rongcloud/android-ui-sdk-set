package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Conversation;

public class DeleteEvent {
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private int[] mMessageIds;
    private String[] mMessageUIds;

    public DeleteEvent(
            Conversation.ConversationType conversationType, String targetId, int[] messageIds) {
        this.mConversationType = conversationType;
        this.mTargetId = targetId;
        this.mMessageIds = messageIds;
    }

    public DeleteEvent(
            Conversation.ConversationType conversationType,
            String targetId,
            int[] messageIds,
            String[] mMessageUIds) {
        this.mConversationType = conversationType;
        this.mTargetId = targetId;
        this.mMessageIds = messageIds;
        this.mMessageUIds = mMessageUIds;
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

    public String[] getMessageUIds() {
        return mMessageUIds;
    }
}
