package io.rong.imkit.config;

import io.rong.imlib.model.Conversation;
import java.util.List;

public interface DataProcessor<T> {
    /**
     * /~chinese 设置会话列表页支持的会话类型
     *
     * @return 所支持的会话类型
     */

    /**
     * /~english Set the conversation types supported by the conversation list page
     *
     * @return Supported conversation types
     */
    Conversation.ConversationType[] supportedTypes();
    /**
     * /~chinese 对会话数据进行过滤。
     *
     * <p>从数据库批量拉取到会话列表时和在线收到消息产生新会话时都会回调此方法
     *
     * @param data 待过滤的数据
     * @return 过滤完的数据。
     */

    /**
     * /~english Filter the conversation data.
     *
     * <p>This method is called back when a batch is pulled from the database to the conversation
     * list and when a message is received online to generate a new conversation
     *
     * @param data Data to be filtered
     * @return Filtered data
     */
    List<T> filtered(List<T> data);

    /**
     * /~chinese 某一会话类型是否聚合状态显示。
     *
     * @param type 会话类型
     */

    /**
     * /~english Whether the aggregation status of a conversation type is displayed.
     *
     * @param type Conversation type
     * @return
     */
    boolean isGathered(Conversation.ConversationType type);
}
