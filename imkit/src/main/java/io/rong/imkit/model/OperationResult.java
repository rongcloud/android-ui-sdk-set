package io.rong.imkit.model;

public class OperationResult {
    public static final int SUCCESS = 0;
    public Action mAction;
    public int mResultCode;

    public OperationResult(Action action, int code) {
        mAction = action;
        mResultCode = code;
    }

    public enum Action {
        SET_TOP,
        SET_NOTIFICATION_STATUS,
        SET_NOTIFICATION_QUIET_HOURS,
        CLEAR_CONVERSATION_MESSAGES,
    }
}
