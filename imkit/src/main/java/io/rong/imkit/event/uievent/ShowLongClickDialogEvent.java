package io.rong.imkit.event.uievent;

import io.rong.imkit.conversation.messgelist.viewmodel.MessageItemLongClickBean;

public class ShowLongClickDialogEvent implements PageEvent {
    private MessageItemLongClickBean bean;

    public ShowLongClickDialogEvent(MessageItemLongClickBean bean) {
        this.bean = bean;
    }

    public MessageItemLongClickBean getBean() {
        return bean;
    }
}
