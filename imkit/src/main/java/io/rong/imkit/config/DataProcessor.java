package io.rong.imkit.config;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import java.util.List;

public interface DataProcessor<T> {
    /**
     * 设置会话列表页支持的会话类型
     *
     * @return 所支持的会话类型
     */
    Conversation.ConversationType[] supportedTypes();

    /**
     * 对会话数据进行过滤。
     *
     * <p>从数据库批量拉取到会话列表时和在线收到消息产生新会话时都会回调此方法
     *
     * @param data 待过滤的数据
     * @return 过滤完的数据。
     */
    List<T> filtered(List<T> data);

    /**
     * 某一会话类型是否聚合状态显示。
     *
     * @param type 会话类型
     */
    boolean isGathered(Conversation.ConversationType type);

    /**
     * 某一会话类型是否聚合状态显示。
     *
     * @param identifier 会话标识符
     */
    default boolean isGathered(ConversationIdentifier identifier) {
        if (identifier == null || identifier.getType() == null) {
            return false;
        }
        return isGathered(identifier.getType());
    }
}
