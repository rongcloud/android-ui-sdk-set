package io.rong.imkit.handler;

import android.text.TextUtils;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionSummaryQueryParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads message reaction summaries and owns batching constraints. */
class MessageReactionSummaryLoader {

    private static final int MAX_SUMMARY_QUERY_SIZE = 100;

    interface Callback {
        void onSummaryLoaded(
                List<String> requestedMessageUIds, Map<String, List<MessageReaction>> summaries);

        void onSummaryLoadError(IRongCoreEnum.CoreErrorCode errorCode);
    }

    void loadReactionSummaries(
            ConversationIdentifier conversationIdentifier,
            List<Message> messages,
            Callback callback) {
        if (!RongConfigCenter.featureConfig().isMessageReactionEnable()
                || conversationIdentifier == null
                || messages == null
                || messages.isEmpty()) {
            return;
        }
        List<String> messageUIds = new ArrayList<>();
        for (Message message : messages) {
            if (message != null
                    && message.isHasReactions()
                    && !TextUtils.isEmpty(message.getUId())) {
                messageUIds.add(message.getUId());
            }
        }
        loadReactionSummariesByUIds(conversationIdentifier, messageUIds, callback);
    }

    void loadReactionSummariesByUIds(
            ConversationIdentifier conversationIdentifier,
            List<String> messageUIds,
            Callback callback) {
        if (!RongConfigCenter.featureConfig().isMessageReactionEnable()
                || conversationIdentifier == null
                || messageUIds == null
                || messageUIds.isEmpty()) {
            return;
        }
        for (int start = 0; start < messageUIds.size(); start += MAX_SUMMARY_QUERY_SIZE) {
            int end = Math.min(start + MAX_SUMMARY_QUERY_SIZE, messageUIds.size());
            List<String> batch = new ArrayList<>(messageUIds.subList(start, end));
            MessageReactionSummaryQueryParam param =
                    new MessageReactionSummaryQueryParam(conversationIdentifier, batch);
            RongCoreClient.getInstance()
                    .batchGetMessageReactionSummaries(
                            param,
                            new IRongCoreCallback.ResultCallback<
                                    Map<String, List<MessageReaction>>>() {
                                @Override
                                public void onSuccess(Map<String, List<MessageReaction>> result) {
                                    if (callback != null) {
                                        callback.onSummaryLoaded(
                                                batch, result == null ? new HashMap<>() : result);
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    if (callback != null) {
                                        callback.onSummaryLoadError(e);
                                    }
                                }
                            });
        }
    }
}
