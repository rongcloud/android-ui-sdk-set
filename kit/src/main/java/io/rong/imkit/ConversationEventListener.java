package io.rong.imkit;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public interface ConversationEventListener {
    /**
     * /~chinese 会话里保存草稿时，回调此方法
     *
     * @param type 会话类型
     * @param targetId 会话 Id
     * @param content 草稿内容
     */

    /**
     * /~english Call back this method when you save a draft in a conversation
     *
     * @param type Conversation type
     * @param targetId Conversation Id
     * @param content Draft content
     */
    void onSaveDraft(Conversation.ConversationType type, String targetId, String content);

    /**
     * /~chinese 根据会话类型，清空某一会话的所有聊天消息记录,回调此方法。
     *
     * @param type 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     */

    /**
     * /~english Depending on the conversation type, clear all chat message records for a
     * conversation and call back this method.
     *
     * @param type Conversation type Incoming ConversationType.CHATROOM is not supported
     * @param targetId Target Id Depending on the conversationType, it may be user Id, discussion
     *     group Id and group Id
     */
    void onClearedMessage(Conversation.ConversationType type, final String targetId);

    /**
     * /~chinese 清除某会话的所有消息未读状态时，回调此方法。
     *
     * @param type 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     */

    /**
     * /~english This method is called back when all messages in a conversation are cleared from the
     * unread state.
     *
     * @param type Conversation type Incoming ConversationType.CHATROOM is not supported
     * @param targetId Target Id Depending on the conversationType, it may be user Id, discussion
     *     group Id and group Id
     */
    void onClearedUnreadStatus(Conversation.ConversationType type, String targetId);

    /**
     * /~chinese 从会话列表删除某会话时的回调。
     *
     * @param type 会话类型
     * @param targetId 会话 Id
     */

    /**
     * /~english The callback when a conversation is deleted from the conversation list.
     *
     * @param type Conversation type Incoming ConversationType.CHATROOM is not supported
     * @param targetId Target Id Depending on the conversationType, it may be user Id, discussion
     *     group Id and group Id
     */
    void onConversationRemoved(Conversation.ConversationType type, String targetId);

    /**
     * /~chinese 操作失败的回调。
     *
     * @param code
     */

    /**
     * /~english Callback for failed operation.
     *
     * @param code
     */
    void onOperationFailed(RongIMClient.ErrorCode code);

    /**
     * /~chinese 清除某类会话。
     *
     * @param conversationTypes 会话类型。
     */

    /**
     * /~english Clear a certain type of conversation.
     *
     * @param conversationTypes Conversation type
     */
    void onClearConversations(Conversation.ConversationType... conversationTypes);
}
