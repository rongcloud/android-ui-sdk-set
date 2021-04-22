package io.rong.imkit.event.uievent;

public class ToastEvent implements PageEvent {
    private String message;

    public ToastEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
