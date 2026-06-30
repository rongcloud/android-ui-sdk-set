package io.rong.imkit.handler;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.reaction.OnMessageReactionClickListener;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionOperationType;
import io.rong.imlib.model.UpdateMessageReactionParam;
import java.util.List;

/** Handles add/remove decisions and business interception. */
class MessageReactionToggleHandler {

    interface Callback {
        void onToggleSuccess(MessageReactionOperationType operationType, MessageReaction reaction);

        void onOperationError(
                UiMessage uiMessage, String reactionId, IRongCoreEnum.CoreErrorCode coreErrorCode);
    }

    void toggleReaction(
            @NonNull UiMessage uiMessage,
            @NonNull String reactionId,
            IRongCoreCallback.OperationCallback callback,
            Callback handlerCallback) {
        if (!RongConfigCenter.featureConfig().isMessageReactionEnable()) {
            notifyOperationError(
                    uiMessage,
                    reactionId,
                    IRongCoreEnum.CoreErrorCode.RC_OPERATION_NOT_SUPPORT,
                    callback,
                    handlerCallback);
            return;
        }
        Message message = uiMessage.getMessage();
        String messageUId = message == null ? null : message.getUId();
        if (TextUtils.isEmpty(messageUId) || TextUtils.isEmpty(reactionId)) {
            notifyOperationError(
                    uiMessage,
                    reactionId,
                    IRongCoreEnum.CoreErrorCode.PARAMETER_ERROR,
                    callback,
                    handlerCallback);
            return;
        }
        if (isReactionUnavailableForSentStatus(message.getSentStatus())) {
            notifyOperationError(
                    uiMessage,
                    reactionId,
                    IRongCoreEnum.CoreErrorCode.PARAMETER_ERROR,
                    callback,
                    handlerCallback);
            return;
        }

        MessageReaction existing = findReaction(uiMessage, reactionId);
        OnMessageReactionClickListener listener =
                RongConfigCenter.featureConfig().getOnMessageReactionClickListener();
        if (listener != null
                && existing != null
                && listener.onMessageReactionClicked(existing, uiMessage)) {
            return;
        }

        boolean currentlyReacted = existing != null && existing.isHasCurrentUserReacted();
        MessageReactionOperationType operationType =
                currentlyReacted
                        ? MessageReactionOperationType.REMOVED
                        : MessageReactionOperationType.ADDED;
        IRongCoreCallback.OperationCallback wrappedCallback =
                new IRongCoreCallback.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (handlerCallback != null) {
                            handlerCallback.onToggleSuccess(
                                    operationType, createLocalReaction(messageUId, reactionId));
                        }
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                        notifyOperationError(
                                uiMessage, reactionId, coreErrorCode, callback, handlerCallback);
                    }
                };
        UpdateMessageReactionParam param = new UpdateMessageReactionParam(messageUId, reactionId);
        if (currentlyReacted) {
            RongCoreClient.getInstance().removeMessageReaction(param, wrappedCallback);
        } else {
            RongCoreClient.getInstance().addMessageReaction(param, wrappedCallback);
        }
    }

    private MessageReaction createLocalReaction(String messageUId, String reactionId) {
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageUId(messageUId);
        reaction.setReactionId(reactionId);
        return reaction;
    }

    private void notifyOperationError(
            UiMessage uiMessage,
            String reactionId,
            IRongCoreEnum.CoreErrorCode coreErrorCode,
            IRongCoreCallback.OperationCallback callback,
            Callback handlerCallback) {
        if (handlerCallback != null) {
            handlerCallback.onOperationError(uiMessage, reactionId, coreErrorCode);
        }
        if (callback != null) {
            callback.onError(coreErrorCode);
        }
    }

    private MessageReaction findReaction(UiMessage uiMessage, String reactionId) {
        List<MessageReaction> reactions = uiMessage.getReactions();
        if (reactions == null || TextUtils.isEmpty(reactionId)) {
            return null;
        }
        for (MessageReaction reaction : reactions) {
            if (reaction != null && reactionId.equals(reaction.getReactionId())) {
                return reaction;
            }
        }
        return null;
    }

    static boolean isReactionUnavailableForSentStatus(Message.SentStatus sentStatus) {
        return sentStatus == Message.SentStatus.SENDING
                || sentStatus == Message.SentStatus.FAILED
                || sentStatus == Message.SentStatus.CANCELED;
    }
}
