package io.rong.imkit.event.actionevent;

import io.rong.imlib.model.Message;

public class InsertEvent {

    private Message message;


    public InsertEvent(Message message) {
        this.message = message;
    }


    public Message getMessage() {
        return message;
    }
}
