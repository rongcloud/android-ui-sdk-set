package io.rong.imkit.event.uievent;

public class ShowWarningDialogEvent implements PageEvent {
    private String msg;

    public ShowWarningDialogEvent(String msg) {
        this.msg = msg;
    }

    public String getMessage() {
        return msg;
    }
}
