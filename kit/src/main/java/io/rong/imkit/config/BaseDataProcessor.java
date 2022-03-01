package io.rong.imkit.config;

import io.rong.imlib.model.Conversation;
import java.util.List;

/** @author jenny_zhou */
public abstract class BaseDataProcessor<T> implements DataProcessor<T> {
    @Override
    public Conversation.ConversationType[] supportedTypes() {
        return new Conversation.ConversationType[] {
            Conversation.ConversationType.PRIVATE,
            Conversation.ConversationType.GROUP,
            Conversation.ConversationType.SYSTEM,
            Conversation.ConversationType.CUSTOMER_SERVICE,
            Conversation.ConversationType.CHATROOM,
            Conversation.ConversationType.APP_PUBLIC_SERVICE,
            Conversation.ConversationType.PUBLIC_SERVICE,
            Conversation.ConversationType.ENCRYPTED
        };
    }

    @Override
    public List<T> filtered(List<T> data) {
        return data;
    }

    @Override
    public boolean isGathered(Conversation.ConversationType type) {
        return false;
    }
}
