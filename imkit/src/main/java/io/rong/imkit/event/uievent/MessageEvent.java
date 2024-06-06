package io.rong.imkit.event.uievent;

/**
 * 页面级别相关事件，消息事件
 *
 * @author rongcloud
 */
public class MessageEvent implements PageEvent {

    private final boolean isHasMoreMsg;

    public MessageEvent(boolean isHasMoreMsg) {
        this.isHasMoreMsg = isHasMoreMsg;
    }

    public boolean isHasMoreMsg() {
        return isHasMoreMsg;
    }
}
