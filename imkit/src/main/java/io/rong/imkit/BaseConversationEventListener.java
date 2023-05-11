package io.rong.imkit;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public abstract class BaseConversationEventListener implements ConversationEventListener {

    @Override
    public void onSaveDraft(Conversation.ConversationType type, String targetId, String content) {
        // default implementation ignored
    }

    @Override
    public void onClearedMessage(Conversation.ConversationType type, String targetId) {
        // default implementation ignored
    }

    @Override
    public void onClearedUnreadStatus(Conversation.ConversationType type, String targetId) {
        // default implementation ignored
    }

    @Override
    public void onConversationRemoved(Conversation.ConversationType type, String targetId) {
        // default implementation ignored
    }

    @Override
    public void onOperationFailed(RongIMClient.ErrorCode code) {
        // default implementation ignored
    }

    @Override
    public void onClearConversations(Conversation.ConversationType... conversationTypes) {
        // default implementation ignored
    }
}
