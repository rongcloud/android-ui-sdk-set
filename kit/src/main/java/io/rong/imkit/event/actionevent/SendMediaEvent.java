package io.rong.imkit.event.actionevent;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;

public class SendMediaEvent {
    @IntDef({ATTACH, SUCCESS, PROGRESS, ERROR, CANCEL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Event {
    }

    public static final int ATTACH = 0;
    public static final int SUCCESS = 1;
    public static final int PROGRESS = 2;
    public static final int ERROR = 3;
    public static final int CANCEL = 4;


    private @Event
    int event;
    private Message message;
    private RongIMClient.ErrorCode code;
    private int progress;

    public SendMediaEvent(@Event int event, Message message) {
        this(event, message, 0, null);
    }

    public SendMediaEvent(@Event int event, Message message, int progress) {
        this(event, message, progress, null);
    }

    public SendMediaEvent(@Event int event, Message message, RongIMClient.ErrorCode code) {
        this(event, message, 0, code);
    }

    public SendMediaEvent(@Event int event, Message message, int progress, RongIMClient.ErrorCode code) {
        this.event = event;
        this.message = message;
        this.progress = progress;
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

    public int getProgress() {
        return progress;
    }
}
