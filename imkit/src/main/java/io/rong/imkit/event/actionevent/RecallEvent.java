package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Conversation;
import io.rong.message.RecallNotificationMessage;

public class RecallEvent {
    private Conversation.ConversationType conversationType;
    private String targetId;
    private int messageId;
    private RecallNotificationMessage message;
    private String messageUid;

    public RecallEvent(
            Conversation.ConversationType conversationType,
            String targetId,
            int messageId,
            RecallNotificationMessage message) {
        this.conversationType = conversationType;
        this.targetId = targetId;
        this.messageId = messageId;
        this.message = message;
    }

    public RecallEvent(
            Conversation.ConversationType conversationType,
            String targetId,
            int messageId,
            RecallNotificationMessage message,
            String messageUid) {
        this.conversationType = conversationType;
        this.targetId = targetId;
        this.messageId = messageId;
        this.message = message;
        this.messageUid = messageUid;
    }

    public Conversation.ConversationType getConversationType() {
        return conversationType;
    }

    public String getTargetId() {
        return targetId;
    }

    public int getMessageId() {
        return messageId;
    }

    public RecallNotificationMessage getRecallNotificationMessage() {
        return message;
    }

    public String getMessageUid() {
        return messageUid;
    }
}
