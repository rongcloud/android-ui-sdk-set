package io.rong.imkit;

import io.rong.imlib.model.Message;

/** /~chinese 接收消息监听 */

/** /~english Receiving message listening */
public interface OnReceiveMessageListener {

    /**
     * /~chinese 接收实时或者离线消息。
     *
     * <p>注意: <br>
     * 1. 针对接收离线消息时，服务端会将 200 条消息打成一个包发到客户端，客户端对这包数据进行解析。<br>
     * 2. hasPackage 标识是否还有剩余的消息包，left 标识这包消息解析完逐条抛送给 App 层后，剩余多少条。<br>
     * 如何判断离线消息收完：<br>
     * 1. hasPackage 和 left 都为 0；<br>
     * 2. hasPackage 为 0 标识当前正在接收最后一包（200条）消息，left 为 0 标识最后一包的最后一条消息也已接收完毕。
     *
     * @param message 接收到的消息对象
     * @param left 每个数据包数据逐条上抛后，还剩余的条数
     * @param hasPackage 是否在服务端还存在未下发的消息包
     * @param offline 消息是否离线消息
     */

    /**
     * /~english Receive real-time or offline messages. Note: 1. When receiving offline messages,
     * the server sends 200 messages as a packet to the client, and the client parses the packet
     * data. 2. HasPackage identifies whether there are any remaining message packets, and left
     * identifies how many messages are left after the packet is parsed and thrown to the App layer
     * one by one. How to judge that offline messages have been received: 1. HasPackage and left are
     * both 0; 2. A hasPackage of 0 indicates that the last packet of messages (200) is currently
     * being received, and a left of 0 indicates that the last message of the last packet has been
     * received.
     *
     * @param message Received message object
     * @param left The number of messages left after each packet is thrown up one by one
     * @param hasPackage Whether there are any undistributed message packets on the server
     * @param offline Whether the message is offline
     */
    void onReceived(Message message, int left, boolean hasPackage, boolean offline);
}
