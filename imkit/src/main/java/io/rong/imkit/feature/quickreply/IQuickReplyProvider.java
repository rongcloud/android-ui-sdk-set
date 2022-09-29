package io.rong.imkit.feature.quickreply;

import io.rong.imlib.model.Conversation;
import java.util.List;

public interface IQuickReplyProvider {
    public List<String> getPhraseList(Conversation.ConversationType type);
}
