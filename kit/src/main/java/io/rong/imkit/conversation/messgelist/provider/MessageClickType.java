package io.rong.imkit.conversation.messgelist.provider;

/**
 * 消息各个点击事件标记类，所有 sdk 的消息，点击事件从 -1 开始定义方便与用户定义的区分
 */
public class MessageClickType {
    public static final int WARNING_CLICK = -1;
    public static final int READ_RECEIPT_REQUEST_CLICK = -2;
    public static final int READ_RECEIPT_STATE_CLICK = -3;
    public static final int CONTENT_LONG_CLICK = -4;
    public static final int USER_PORTRAIT_CLICK = -5;
    public static final int USER_PORTRAIT_LONG_CLICK = -6;
    public static final int AUDIO_CLICK = -7;
    public static final int REEDIT_CLICK = -8;
    public static final int CONTENT_CLICK = -9;
    public static final int EDIT_CLICK = -10;
}
