package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Message;

public class RefreshEvent {
    private Message message;

    public RefreshEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}
