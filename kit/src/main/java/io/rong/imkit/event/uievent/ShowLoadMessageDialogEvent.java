package io.rong.imkit.event.uievent;


import java.util.List;

import io.rong.imkit.conversation.messgelist.status.MessageProcessor;
import io.rong.imlib.model.Message;

public class ShowLoadMessageDialogEvent implements PageEvent{
    private final MessageProcessor.GetMessageCallback callback;
    private final List<Message> list;
    public ShowLoadMessageDialogEvent(MessageProcessor.GetMessageCallback callback, List<Message> list) {
       this.callback = callback;
       this.list = list;
    }

    public MessageProcessor.GetMessageCallback getCallback() {
        return callback;
    }


    public List<Message> getList() {
        return list;
    }
}
