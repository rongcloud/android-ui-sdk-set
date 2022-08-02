package io.rong.imkit.conversation.messgelist.viewmodel;

import java.util.List;

import io.rong.imkit.MessageItemLongClickAction;
import io.rong.imkit.model.UiMessage;

public class MessageItemLongClickBean {
    private List<MessageItemLongClickAction> messageItemLongClickActions;
    private UiMessage uiMessage;

    public MessageItemLongClickBean(List<MessageItemLongClickAction> messageItemLongClickActions, UiMessage uiMessage) {
        this.messageItemLongClickActions = messageItemLongClickActions;
        this.uiMessage = uiMessage;
    }

    public List<MessageItemLongClickAction> getMessageItemLongClickActions() {
        return messageItemLongClickActions;
    }

    public UiMessage getUiMessage() {
        return uiMessage;
    }
}
