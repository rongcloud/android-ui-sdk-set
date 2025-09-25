package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Message;
import java.util.List;

public class RefreshEvent {
    private Message message;

    /** 批量更新的消息体 */
    private List<Message> messages;

    /** 是否修改过消息体 */
    private boolean isModifyMessageContent = false;

    public RefreshEvent(Message message) {
        this.message = message;
    }

    public RefreshEvent(List<Message> messages, boolean isModifyMessageContent) {
        if (messages != null && !messages.isEmpty()) {
            this.message = messages.get(0);
        }
        this.messages = messages;
        this.isModifyMessageContent = isModifyMessageContent;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public boolean isModifyMessageContent() {
        return isModifyMessageContent;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
