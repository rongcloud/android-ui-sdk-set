package io.rong.imkit;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public abstract class BaseConversationEventListener implements ConversationEventListener {

    @Override
    public void onSaveDraft(Conversation.ConversationType type, String targetId, String content) {

    }

    @Override
    public void onClearedMessage(Conversation.ConversationType type, String targetId) {

    }

    @Override
    public void onClearedUnreadStatus(Conversation.ConversationType type, String targetId) {

    }

    @Override
    public void onConversationRemoved(Conversation.ConversationType type, String targetId) {

    }

    @Override
    public void onOperationFailed(RongIMClient.ErrorCode code) {

    }

    @Override
    public void onClearConversations(Conversation.ConversationType... conversationTypes) {

    }
}
