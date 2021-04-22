package io.rong.imkit;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public interface ConversationEventListener {
    /**
     * 会话里保存草稿时，回调此方法
     *
     * @param type     会话类型
     * @param targetId 会话 Id
     * @param content  草稿内容
     */
    void onSaveDraft(Conversation.ConversationType type, String targetId, String content);

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调此方法。
     *
     * @param type     会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     */
    void onClearedMessage(Conversation.ConversationType type, final String targetId);

    /**
     * 清除某会话的所有消息未读状态时，回调此方法。
     *
     * @param type     会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     */
    void onClearedUnreadStatus(Conversation.ConversationType type, String targetId);

    /**
     * 从会话列表删除某会话时的回调。
     *
     * @param type     会话类型
     * @param targetId 会话 Id
     */
    void onConversationRemoved(Conversation.ConversationType type, String targetId);

    /**
     * 操作失败的回调。
     *
     * @param code
     */
    void onOperationFailed(RongIMClient.ErrorCode code);


    /**
     * 清除某类会话。
     *
     * @param conversationTypes 会话类型。
     */
    void onClearConversations(Conversation.ConversationType... conversationTypes);
}
