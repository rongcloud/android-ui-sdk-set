package io.rong.imkit.event.actionevent;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;

public class SendEvent {
    @IntDef({SUCCESS, ATTACH, ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
    }

    public static final int ATTACH = 0;
    public static final int SUCCESS = 1;
    public static final int ERROR = 2;


    private @Event
    int event;
    private Message message;
    private RongIMClient.ErrorCode code;

    public SendEvent(@Event int event, Message message) {
        this(event, message, null);
    }

    public SendEvent(int event, Message message, RongIMClient.ErrorCode code) {
        this.event = event;
        this.message = message;
        this.code = code;
    }

    public @Event
    int getEvent() {
        return event;
    }

    public Message getMessage() {
        return message;
    }

    public RongIMClient.ErrorCode getCode() {
        return code;
    }

}
