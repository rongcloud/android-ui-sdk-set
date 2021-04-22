package io.rong.imkit.config;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;

public interface OnSendMessageListener {
    /**
     * 消息发送前监听器处理接口（是否发送成功可以从SentStatus属性获取）
     * 可以通过这个方法，过滤，修改发送出的消息。
     *
     * @param message 发送的消息实例。
     * @return 处理后的消息实例，注意：可以通过 return 的返回值，过滤消息
     * 当 return null 时，该消息不发送，界面也无显示
     * 也可以更改 message 内的消息内容，发送出的消息，就是更改后的。
     */
    Message onSend(Message message);


    /**
     * 消息发送后回调接口。
     *
     * @param message   消息实例。
     * @param errorCode 发送消息失败的状态码，消息发送成功 errorCode 为 null。
     * @return 事件是否被消费。 true 事件被消费，SDK 不会进行 UI 显示。false, 事件未被消费， SDK 会进行 UI 显示。
     */
    boolean onSent(Message message, RongIMClient.ErrorCode errorCode);
}
