package io.rong.imkit.handler;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionEventData;
import io.rong.imlib.model.MessageReactionOperationType;
import io.rong.imlib.model.MessageReactionUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息回应处理器。
 *
 * <p>负责会话消息列表中的回应监听和 DataKey 分发。回应操作、摘要加载和 delta 合并分别由独立协作类承担。使用完毕后需要调用 {@link #stop()}。
 *
 * @since 5.42.0
 */
public class MessageReactionHandler extends MultiDataHandler {

    public static final DataKey<SummaryResult> KEY_MESSAGE_REACTION_SUMMARIES =
            DataKey.obtain("KEY_MESSAGE_REACTION_SUMMARIES", SummaryResult.class);

    public static final DataKey<ReactionChange> KEY_MESSAGE_REACTION_CHANGED =
            DataKey.obtain("KEY_MESSAGE_REACTION_CHANGED", ReactionChange.class);

    public static final DataKey<OperationError> KEY_MESSAGE_REACTION_OPERATION_ERROR =
            DataKey.obtain("KEY_MESSAGE_REACTION_OPERATION_ERROR", OperationError.class);

    private final MessageReactionSummaryLoader summaryLoader = new MessageReactionSummaryLoader();
    private final MessageReactionToggleHandler toggleHandler = new MessageReactionToggleHandler();
    private final MessageReactionSummaryLoader.Callback summaryCallback =
            new MessageReactionSummaryLoader.Callback() {
                @Override
                public void onSummaryLoaded(
                        List<String> requestedMessageUIds,
                        Map<String, List<MessageReaction>> summaries) {
                    notifyDataChange(
                            KEY_MESSAGE_REACTION_SUMMARIES,
                            new SummaryResult(requestedMessageUIds, summaries));
                }

                @Override
                public void onSummaryLoadError(IRongCoreEnum.CoreErrorCode errorCode) {
                    notifyDataError(KEY_MESSAGE_REACTION_SUMMARIES, errorCode);
                }
            };
    private final MessageReactionToggleHandler.Callback toggleCallback =
            new MessageReactionToggleHandler.Callback() {
                @Override
                public void onToggleSuccess(
                        MessageReactionOperationType operationType, MessageReaction reaction) {
                    notifyReactionChanged(operationType, reaction, true);
                }

                @Override
                public void onOperationError(
                        UiMessage uiMessage,
                        String reactionId,
                        IRongCoreEnum.CoreErrorCode coreErrorCode) {
                    notifyOperationError(uiMessage, reactionId, coreErrorCode);
                }
            };
    private final IRongCoreListener.MessageReactionListener reactionListener =
            reactions -> {
                if (reactions == null || reactions.isEmpty()) {
                    return;
                }
                for (MessageReactionEventData eventData : reactions) {
                    notifyReactionChanged(eventData, false);
                }
            };

    public MessageReactionHandler() {
        RongCoreClient.getInstance().addMessageReactionListener(reactionListener);
    }

    /**
     * 切换某条消息上的回应：当前用户已回应则移除，否则添加。
     *
     * <p>操作成功后按 iOS 逻辑应用本地增量：已有回应原位更新，新回应追加，不触发单条摘要刷新重排。
     */
    public void toggleReaction(
            @NonNull UiMessage uiMessage,
            @NonNull String reactionId,
            IRongCoreCallback.OperationCallback callback) {
        toggleHandler.toggleReaction(uiMessage, reactionId, callback, toggleCallback);
    }

    /** 批量拉取多条消息的回应摘要。 */
    public void loadReactionSummaries(
            ConversationIdentifier conversationIdentifier, List<Message> messages) {
        summaryLoader.loadReactionSummaries(conversationIdentifier, messages, summaryCallback);
    }

    /** 拉取单条消息的回应摘要，用于实时移除事件后的服务端状态校准。 */
    public void reloadReactionSummary(
            ConversationIdentifier conversationIdentifier, String messageUId) {
        if (conversationIdentifier == null || TextUtils.isEmpty(messageUId)) {
            return;
        }
        summaryLoader.loadReactionSummariesByUIds(
                conversationIdentifier, Collections.singletonList(messageUId), summaryCallback);
    }

    private void notifyOperationError(
            UiMessage uiMessage, String reactionId, IRongCoreEnum.CoreErrorCode coreErrorCode) {
        notifyDataChange(
                KEY_MESSAGE_REACTION_OPERATION_ERROR,
                new OperationError(uiMessage, reactionId, coreErrorCode));
    }

    private void notifyReactionChanged(
            MessageReactionOperationType operationType, MessageReaction reaction, boolean local) {
        notifyReactionChanged(null, operationType, reaction, local);
    }

    private void notifyReactionChanged(
            ConversationIdentifier conversationIdentifier,
            MessageReactionOperationType operationType,
            MessageReaction reaction,
            boolean local) {
        if (operationType == null
                || reaction == null
                || TextUtils.isEmpty(reaction.getMessageUId())) {
            return;
        }
        notifyDataChange(
                KEY_MESSAGE_REACTION_CHANGED,
                new ReactionChange(conversationIdentifier, operationType, reaction, local));
    }

    private void notifyReactionChanged(MessageReactionEventData eventData, boolean local) {
        if (eventData == null) {
            return;
        }
        notifyReactionChanged(
                eventData.getConversationIdentifier(),
                eventData.getOperationType(),
                toMessageReaction(eventData),
                local);
    }

    private MessageReaction toMessageReaction(MessageReactionEventData eventData) {
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageUId(eventData.getMessageUId());
        reaction.setReactionId(eventData.getReactionId());
        reaction.setUsers(eventData.getUsers());
        reaction.setTotalCount(eventData.getTotalCount());
        reaction.setReactionTime(getFirstReactionTime(eventData.getUsers()));
        return reaction;
    }

    private long getFirstReactionTime(List<MessageReactionUser> users) {
        if (users == null || users.isEmpty() || users.get(0) == null) {
            return 0;
        }
        return users.get(0).getReactionTime();
    }

    @Override
    public void stop() {
        RongCoreClient.getInstance().removeMessageReactionListener(reactionListener);
        super.stop();
    }

    public static class SummaryResult {
        private final List<String> requestedMessageUIds;
        private final Map<String, List<MessageReaction>> summaries;

        SummaryResult(
                List<String> requestedMessageUIds, Map<String, List<MessageReaction>> summaries) {
            this.requestedMessageUIds =
                    requestedMessageUIds == null
                            ? Collections.emptyList()
                            : new ArrayList<>(requestedMessageUIds);
            this.summaries = summaries == null ? new HashMap<>() : summaries;
        }

        public List<String> getRequestedMessageUIds() {
            return requestedMessageUIds;
        }

        public Map<String, List<MessageReaction>> getSummaries() {
            return summaries;
        }
    }

    public static class ReactionChange {
        private final ConversationIdentifier conversationIdentifier;
        private final MessageReactionOperationType operationType;
        private final MessageReaction reaction;
        private final boolean local;

        ReactionChange(
                ConversationIdentifier conversationIdentifier,
                MessageReactionOperationType operationType,
                MessageReaction reaction,
                boolean local) {
            this.conversationIdentifier = conversationIdentifier;
            this.operationType = operationType;
            this.reaction = reaction;
            this.local = local;
        }

        public ConversationIdentifier getConversationIdentifier() {
            return conversationIdentifier;
        }

        public MessageReactionOperationType getOperationType() {
            return operationType;
        }

        public MessageReaction getReaction() {
            return reaction;
        }

        public boolean isLocal() {
            return local;
        }
    }

    public static class OperationError {
        private final UiMessage uiMessage;
        private final String reactionId;
        private final IRongCoreEnum.CoreErrorCode coreErrorCode;

        OperationError(
                UiMessage uiMessage, String reactionId, IRongCoreEnum.CoreErrorCode coreErrorCode) {
            this.uiMessage = uiMessage;
            this.reactionId = reactionId;
            this.coreErrorCode = coreErrorCode;
        }

        public UiMessage getUiMessage() {
            return uiMessage;
        }

        public String getReactionId() {
            return reactionId;
        }

        public IRongCoreEnum.CoreErrorCode getCoreErrorCode() {
            return coreErrorCode;
        }
    }
}
