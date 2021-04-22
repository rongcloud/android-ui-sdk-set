package io.rong.imkit;

import io.rong.imlib.model.Message;

/**
 * 消息拦截器
 */
public interface MessageInterceptor {

    /**
     * 接收实时或者离线消息拦回调。
     * <p>
     * 注意: <br>
     * 1. 针对接收离线消息时，服务端会将 200 条消息打成一个包发到客户端，客户端对这包数据进行解析。<br>
     * 2. hasPackage 标识是否还有剩余的消息包，left 标识这包消息解析完逐条抛送给 App 层后，剩余多少条。<br>
     * 如何判断离线消息收完：<br>
     * 1. hasPackage 和 left 都为 0；<br>
     * 2. hasPackage 为 0 标识当前正在接收最后一包（200条）消息，left 为 0 标识最后一包的最后一条消息也已接收完毕。
     * </p>
     *
     * @param message    接收到的消息对象
     * @param left       每个数据包数据逐条上抛后，还剩余的条数
     * @param hasPackage 是否在服务端还存在未下发的消息包
     * @param offline    消息是否离线消息
     * @return true 用户拦截此次消息，sdk不再做任何分发处理，false,交由sdk处理
     */
    boolean interceptReceivedMessage(Message message, int left, boolean hasPackage, boolean offline);

    /**
     * 准备发送消息的拦截回调。
     *
     * @param message 准备发送的消息
     * @return true 用户拦截此次消息，sdk不再做后续处理，false,交由sdk处理
     */
    boolean interceptOnSendMessage(Message message);

    /**
     * 发送消息成功后的拦截回调。
     *
     * @param message 准备发送的消息
     * @return true 用户拦截此次消息，sdk不再做后续处理，false,交由sdk处理
     */
    boolean interceptOnSentMessage(Message message);
}
