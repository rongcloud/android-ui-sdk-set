package io.rong.imkit.feature.quickreply;

import java.util.List;

import io.rong.imlib.model.Conversation;

public interface IQuickReplyProvider {
    public List<String> getPhraseList(Conversation.ConversationType type);
}
