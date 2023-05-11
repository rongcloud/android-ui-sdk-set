package io.rong.imkit;

import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;

public interface ConversationEventListener {
    /**
     * 会话里保存草稿时，回调此方法
     *
     * @param type 会话类型
     * @param targetId 会话 Id
     * @param content 草稿内容
     */
    void onSaveDraft(Conversation.ConversationType type, String targetId, String content);

    default void onSaveDraft(ConversationIdentifier conversationIdentifier, String content) {
        if (conversationIdentifier != null) {
            onSaveDraft(
                    conversationIdentifier.getType(),
                    conversationIdentifier.getTargetId(),
                    content);
        } else {
            onSaveDraft(Conversation.ConversationType.NONE, "", content);
        }
    }

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调此方法。
     *
     * @param type 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     */
    void onClearedMessage(Conversation.ConversationType type, final String targetId);

    default void onClearedMessage(ConversationIdentifier conversationIdentifier) {
        if (conversationIdentifier != null) {
            onClearedMessage(
                    conversationIdentifier.getType(), conversationIdentifier.getTargetId());
        } else {
            onClearedMessage(Conversation.ConversationType.NONE, "");
        }
    }

    /**
     * 清除某会话的所有消息未读状态时，回调此方法。
     *
     * @param type 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     */
    void onClearedUnreadStatus(Conversation.ConversationType type, String targetId);

    default void onClearedUnreadStatus(ConversationIdentifier conversationIdentifier) {
        if (conversationIdentifier != null) {
            onClearedUnreadStatus(
                    conversationIdentifier.getType(), conversationIdentifier.getTargetId());
        } else {
            onClearedUnreadStatus(Conversation.ConversationType.NONE, "");
        }
    }

    /**
     * 从会话列表删除某会话时的回调。
     *
     * @param type 会话类型
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

    /**
     * @since 5.2.5 媒体消息发生改变回调
     * @param conversationType 聊天类型
     * @param targetId 会话 Id
     * @param status 状态
     */
    default void onMessageReceivedStatusChange(
            int messageId,
            Conversation.ConversationType conversationType,
            String targetId,
            Message.ReceivedStatus status) {
        // default implementation ignored
    }

    default void onChannelChange(
            String groupId, String channelId, IRongCoreEnum.UltraGroupChannelType type) {
        // default implementation ignored
    }

    default void onChannelDelete(String groupId, String channelId) {
        // default implementation ignored
    }

    default void onChannelKicked(String groupId, String channelId, String userId) {
        // default implementation ignored
    }
}
