package io.rong.imkit.event.uievent;

import io.rong.imkit.model.UiMessage;

/**
 * 消息气泡中的组件点击事件
 *
 * @author rongcloud
 * @since 5.30.0
 */
public class ReadReceiptStateClickEvent implements PageEvent {

    private UiMessage message;

    public ReadReceiptStateClickEvent(UiMessage message) {
        this.message = message;
    }

    public UiMessage getMessage() {
        return message;
    }
}
