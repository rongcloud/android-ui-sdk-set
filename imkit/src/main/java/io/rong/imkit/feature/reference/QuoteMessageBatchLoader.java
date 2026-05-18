package io.rong.imkit.feature.reference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageResult;
import io.rong.imlib.model.QuoteInfo;
import io.rong.imlib.params.GetMessagesByUIdsParams;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Batches and caches V2 quote-card original-message lookups for the current message page. */
public final class QuoteMessageBatchLoader {
    private static final String TAG = "QuoteMessageBatchLoader";
    private static final int MAX_CACHE_SIZE = 50;
    private static final QuoteMessageBatchLoader INSTANCE =
            new QuoteMessageBatchLoader(new RongCoreQueryClient(), new IMCenterRefreshDispatcher());

    public enum State {
        LOADING,
        LOADED,
        DELETED,
        RECALLED,
        FAILED
    }

    public static final class Result {
        private final State state;
        private final Message message;

        private Result(@NonNull State state, @Nullable Message message) {
            this.state = state;
            this.message = message;
        }

        @NonNull
        public State getState() {
            return state;
        }

        @Nullable
        public Message getMessage() {
            return message;
        }
    }

    interface QueryClient {
        void getMessagesByUIds(
                GetMessagesByUIdsParams params,
                IRongCoreCallback.ResultCallback<List<MessageResult>> callback);
    }

    interface RefreshDispatcher {
        void refresh(Message message);
    }

    private final QueryClient queryClient;
    private final RefreshDispatcher refreshDispatcher;
    private final Map<CacheKey, CacheEntry> cache =
            new LinkedHashMap<CacheKey, CacheEntry>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };
    private final Map<CacheKey, List<Message>> sourceMessagesByCacheKey =
            new LinkedHashMap<CacheKey, List<Message>>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, List<Message>> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            };
    private final Map<CacheKey, List<Message>> loadingSourceMessages = new HashMap<>();

    public static QuoteMessageBatchLoader getInstance() {
        return INSTANCE;
    }

    QuoteMessageBatchLoader(
            @NonNull QueryClient queryClient, @NonNull RefreshDispatcher refreshDispatcher) {
        this.queryClient = queryClient;
        this.refreshDispatcher = refreshDispatcher;
    }

    public void markMessagesDeleted(@Nullable List<Message> messages) {
        markMessagesTerminal(messages, CacheEntry.deleted());
    }

    public void markMessagesRecalled(@Nullable Message message) {
        if (message == null) {
            return;
        }
        List<Message> messages = new ArrayList<>(1);
        messages.add(message);
        markMessagesTerminal(messages, CacheEntry.recalled());
    }

    public void markMessagesModified(@Nullable List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<Message> messagesToRefresh = new ArrayList<>();
        synchronized (this) {
            for (Message message : messages) {
                CacheKey cacheKey = createCacheKeyForMessageUId(message);
                if (cacheKey == null || message.getContent() == null) {
                    continue;
                }
                cache.put(cacheKey, CacheEntry.loaded(message));
                addAllDistinct(messagesToRefresh, loadingSourceMessages.remove(cacheKey));
            }
        }
        refreshMessages(messagesToRefresh);
    }

    public void loadForUiMessages(
            @Nullable List<UiMessage> uiMessages, @Nullable Message forceMessage) {
        List<Message> messages = new ArrayList<>();
        if (uiMessages != null) {
            for (UiMessage uiMessage : uiMessages) {
                if (uiMessage != null && uiMessage.getMessage() != null) {
                    messages.add(uiMessage.getMessage());
                }
            }
        }
        loadForMessages(messages, forceMessage);
    }

    void loadForMessages(@Nullable List<Message> messages, @Nullable Message forceMessage) {
        if ((messages == null || messages.isEmpty()) && forceMessage == null) {
            return;
        }

        List<Message> sourceMessages = new ArrayList<>();
        if (messages != null) {
            sourceMessages.addAll(messages);
        }
        if (forceMessage != null && !sourceMessages.contains(forceMessage)) {
            sourceMessages.add(forceMessage);
        }

        Set<CacheKey> forceKeys = new LinkedHashSet<>();
        CacheKey forceKey = createCacheKey(forceMessage);
        if (forceKey != null) {
            forceKeys.add(forceKey);
        }

        List<PendingRequest> requests;
        synchronized (this) {
            requests = buildPendingRequestsLocked(sourceMessages, forceKeys);
        }

        for (PendingRequest request : requests) {
            queryClient.getMessagesByUIds(
                    new GetMessagesByUIdsParams(
                            request.conversationKey.toIdentifier(), request.messageUIds),
                    new IRongCoreCallback.ResultCallback<List<MessageResult>>() {
                        @Override
                        public void onSuccess(List<MessageResult> messageResults) {
                            handleSuccess(request, messageResults);
                        }

                        @Override
                        public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                            logWarning("getMessagesByUIds error: " + errorCode);
                            handleError(request, errorCode);
                        }
                    });
        }
    }

    private void markMessagesTerminal(
            @Nullable List<Message> quotedMessages, @NonNull CacheEntry terminalEntry) {
        if (quotedMessages == null || quotedMessages.isEmpty()) {
            return;
        }
        List<Message> messagesToRefresh = new ArrayList<>();
        synchronized (this) {
            for (Message quotedMessage : quotedMessages) {
                CacheKey cacheKey = createCacheKeyForMessageUId(quotedMessage);
                if (cacheKey == null) {
                    continue;
                }
                cache.put(cacheKey, terminalEntry);
                addAllDistinct(messagesToRefresh, sourceMessagesByCacheKey.get(cacheKey));
                addAllDistinct(messagesToRefresh, loadingSourceMessages.remove(cacheKey));
            }
        }
        refreshMessages(messagesToRefresh);
    }

    @NonNull
    public Result getResult(@Nullable Message sourceMessage) {
        QuoteInfo quoteInfo = sourceMessage != null ? sourceMessage.getQuoteInfo() : null;
        CacheEntry terminal = terminalEntryForQuoteInfo(quoteInfo);
        CacheKey cacheKey = createCacheKey(sourceMessage);
        if (terminal != null) {
            if (cacheKey != null) {
                synchronized (this) {
                    cache.put(cacheKey, terminal);
                }
            }
            return terminal.toResult();
        }
        if (cacheKey == null) {
            return CacheEntry.failed().toResult();
        }
        synchronized (this) {
            CacheEntry entry = cache.get(cacheKey);
            return (entry != null ? entry : CacheEntry.loading()).toResult();
        }
    }

    private List<PendingRequest> buildPendingRequestsLocked(
            @NonNull List<Message> sourceMessages, @NonNull Set<CacheKey> forceKeys) {
        LinkedHashMap<ConversationKey, PendingRequestBuilder> builders = new LinkedHashMap<>();
        for (Message sourceMessage : sourceMessages) {
            if (!shouldCollect(sourceMessage)) {
                continue;
            }
            QuoteInfo quoteInfo = sourceMessage.getQuoteInfo();
            CacheKey cacheKey = createCacheKey(sourceMessage);
            if (cacheKey != null) {
                addSourceMessageLocked(cacheKey, sourceMessage);
            }
            CacheEntry terminal = terminalEntryForQuoteInfo(quoteInfo);
            if (cacheKey != null && terminal != null) {
                cache.put(cacheKey, terminal);
                continue;
            }
            if (!QuoteCardView.shouldQueryQuotedMessage(quoteInfo)) {
                continue;
            }
            if (cacheKey == null) {
                continue;
            }
            boolean forceReload = forceKeys.contains(cacheKey);
            CacheEntry cached = cache.get(cacheKey);
            if (cached != null && cached.state == State.LOADING) {
                addLoadingSourceMessageLocked(cacheKey, sourceMessage);
                continue;
            }
            if (cached != null && !forceReload) {
                continue;
            }

            cache.put(cacheKey, CacheEntry.loading());
            loadingSourceMessages.remove(cacheKey);
            PendingRequestBuilder builder = builders.get(cacheKey.conversationKey);
            if (builder == null) {
                builder = new PendingRequestBuilder(cacheKey.conversationKey);
                builders.put(cacheKey.conversationKey, builder);
            }
            builder.add(cacheKey, sourceMessage);
        }

        List<PendingRequest> requests = new ArrayList<>(builders.size());
        for (PendingRequestBuilder builder : builders.values()) {
            requests.add(builder.build());
        }
        return requests;
    }

    private void addLoadingSourceMessageLocked(
            @NonNull CacheKey cacheKey, @NonNull Message sourceMessage) {
        List<Message> sourceMessages = loadingSourceMessages.get(cacheKey);
        if (sourceMessages == null) {
            sourceMessages = new ArrayList<>();
            loadingSourceMessages.put(cacheKey, sourceMessages);
        }
        if (!sourceMessages.contains(sourceMessage)) {
            sourceMessages.add(sourceMessage);
        }
    }

    private void addSourceMessageLocked(
            @NonNull CacheKey cacheKey, @NonNull Message sourceMessage) {
        List<Message> sourceMessages = sourceMessagesByCacheKey.get(cacheKey);
        if (sourceMessages == null) {
            sourceMessages = new ArrayList<>();
            sourceMessagesByCacheKey.put(cacheKey, sourceMessages);
        }
        if (!sourceMessages.contains(sourceMessage)) {
            sourceMessages.add(sourceMessage);
        }
    }

    private void handleSuccess(
            @NonNull PendingRequest request, @Nullable List<MessageResult> messageResults) {
        Map<String, MessageResult> resultByUid = new HashMap<>();
        if (messageResults != null) {
            for (MessageResult messageResult : messageResults) {
                if (messageResult != null && !isEmpty(messageResult.getMessageUId())) {
                    resultByUid.put(messageResult.getMessageUId(), messageResult);
                }
            }
        }

        List<Message> messagesToRefresh = new ArrayList<>();
        synchronized (this) {
            for (String messageUId : request.messageUIds) {
                CacheKey cacheKey = request.cacheKeyByUid.get(messageUId);
                if (cacheKey == null) {
                    continue;
                }
                cache.put(cacheKey, cacheEntryForResult(resultByUid.get(messageUId)));
                addAllDistinct(messagesToRefresh, request.sourceMessagesByUid.get(messageUId));
                addAllDistinct(messagesToRefresh, loadingSourceMessages.remove(cacheKey));
            }
        }
        refreshMessages(messagesToRefresh);
    }

    private void handleError(
            @NonNull PendingRequest request, @Nullable IRongCoreEnum.CoreErrorCode errorCode) {
        List<Message> messagesToRefresh = new ArrayList<>();
        CacheEntry errorEntry =
                isQuoteFetchFailedError(errorCode) ? CacheEntry.failed() : CacheEntry.deleted();
        synchronized (this) {
            for (String messageUId : request.messageUIds) {
                CacheKey cacheKey = request.cacheKeyByUid.get(messageUId);
                if (cacheKey != null) {
                    cache.put(cacheKey, errorEntry);
                    addAllDistinct(messagesToRefresh, loadingSourceMessages.remove(cacheKey));
                }
                addAllDistinct(messagesToRefresh, request.sourceMessagesByUid.get(messageUId));
            }
        }
        refreshMessages(messagesToRefresh);
    }

    private void refreshMessages(@NonNull List<Message> messagesToRefresh) {
        for (Message message : messagesToRefresh) {
            try {
                refreshDispatcher.refresh(message);
            } catch (Exception e) {
                logError("refresh quote source message", e);
            }
        }
    }

    private static void logWarning(String message) {
        try {
            RLog.w(TAG, message);
        } catch (Throwable ignored) {
            // Android Log is unavailable in local JVM unit tests.
        }
    }

    private static void logError(String message, Throwable throwable) {
        try {
            RLog.e(TAG, message, throwable);
        } catch (Throwable ignored) {
            // Android Log is unavailable in local JVM unit tests.
        }
    }

    private static void addAllDistinct(
            @NonNull List<Message> target, @Nullable List<Message> source) {
        if (source == null) {
            return;
        }
        for (Message message : source) {
            if (message != null && !target.contains(message)) {
                target.add(message);
            }
        }
    }

    @Nullable
    private static CacheEntry terminalEntryForQuoteInfo(@Nullable QuoteInfo quoteInfo) {
        if (quoteInfo == null) {
            return null;
        }
        QuoteInfo.QuoteMessageStatus status = quoteInfo.getQuoteMessageStatus();
        if (status == QuoteInfo.QuoteMessageStatus.DELETED) {
            return CacheEntry.deleted();
        }
        if (status == QuoteInfo.QuoteMessageStatus.RECALLED) {
            return CacheEntry.recalled();
        }
        return null;
    }

    @NonNull
    private static CacheEntry cacheEntryForResult(@Nullable MessageResult messageResult) {
        if (messageResult != null
                && messageResult.getCode() != null
                && messageResult.getCode() != IRongCoreEnum.CoreErrorCode.SUCCESS) {
            return isQuoteFetchFailedError(messageResult.getCode())
                    ? CacheEntry.failed()
                    : CacheEntry.deleted();
        }
        Message message = messageResult != null ? messageResult.getMessage() : null;
        if (message == null || message.getContent() == null) {
            return CacheEntry.deleted();
        }
        if (message.getContent() instanceof RecallNotificationMessage) {
            return CacheEntry.recalled();
        }
        return CacheEntry.loaded(message);
    }

    private static boolean isQuoteFetchFailedError(
            @Nullable IRongCoreEnum.CoreErrorCode errorCode) {
        return errorCode != null && errorCode.getValue() == 30001;
    }

    private static boolean shouldCollect(@Nullable Message message) {
        if (message == null) {
            return false;
        }
        QuoteInfo quoteInfo = message.getQuoteInfo();
        return quoteInfo != null
                && !isEmpty(quoteInfo.getMessageUId())
                && !(message.getContent() instanceof ReferenceMessage);
    }

    @Nullable
    private static CacheKey createCacheKey(@Nullable Message message) {
        if (message == null || message.getQuoteInfo() == null) {
            return null;
        }
        String quotedUId = message.getQuoteInfo().getMessageUId();
        if (isEmpty(quotedUId)
                || message.getConversationType() == null
                || isEmpty(message.getTargetId())) {
            return null;
        }
        return new CacheKey(
                new ConversationKey(
                        message.getConversationType(),
                        message.getTargetId(),
                        safe(message.getChannelId())),
                quotedUId);
    }

    @Nullable
    private static CacheKey createCacheKeyForMessageUId(@Nullable Message message) {
        if (message == null
                || isEmpty(message.getUId())
                || message.getConversationType() == null
                || isEmpty(message.getTargetId())) {
            return null;
        }
        return new CacheKey(
                new ConversationKey(
                        message.getConversationType(),
                        message.getTargetId(),
                        safe(message.getChannelId())),
                message.getUId());
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static boolean isEmpty(@Nullable String value) {
        return value == null || value.length() == 0;
    }

    private static final class CacheEntry {
        private final State state;
        private final Message message;

        private CacheEntry(@NonNull State state, @Nullable Message message) {
            this.state = state;
            this.message = message;
        }

        private static CacheEntry loading() {
            return new CacheEntry(State.LOADING, null);
        }

        private static CacheEntry loaded(@NonNull Message message) {
            return new CacheEntry(State.LOADED, message);
        }

        private static CacheEntry deleted() {
            return new CacheEntry(State.DELETED, null);
        }

        private static CacheEntry recalled() {
            return new CacheEntry(State.RECALLED, null);
        }

        private static CacheEntry failed() {
            return new CacheEntry(State.FAILED, null);
        }

        private Result toResult() {
            return new Result(state, message);
        }
    }

    private static final class PendingRequestBuilder {
        private final ConversationKey conversationKey;
        private final LinkedHashSet<String> messageUIds = new LinkedHashSet<>();
        private final Map<String, CacheKey> cacheKeyByUid = new LinkedHashMap<>();
        private final Map<String, List<Message>> sourceMessagesByUid = new LinkedHashMap<>();

        private PendingRequestBuilder(@NonNull ConversationKey conversationKey) {
            this.conversationKey = conversationKey;
        }

        private void add(@NonNull CacheKey cacheKey, @NonNull Message sourceMessage) {
            messageUIds.add(cacheKey.messageUId);
            cacheKeyByUid.put(cacheKey.messageUId, cacheKey);
            List<Message> sourceMessages = sourceMessagesByUid.get(cacheKey.messageUId);
            if (sourceMessages == null) {
                sourceMessages = new ArrayList<>();
                sourceMessagesByUid.put(cacheKey.messageUId, sourceMessages);
            }
            if (!sourceMessages.contains(sourceMessage)) {
                sourceMessages.add(sourceMessage);
            }
        }

        private PendingRequest build() {
            return new PendingRequest(
                    conversationKey,
                    new ArrayList<>(messageUIds),
                    new LinkedHashMap<>(cacheKeyByUid),
                    new LinkedHashMap<>(sourceMessagesByUid));
        }
    }

    private static final class PendingRequest {
        private final ConversationKey conversationKey;
        private final List<String> messageUIds;
        private final Map<String, CacheKey> cacheKeyByUid;
        private final Map<String, List<Message>> sourceMessagesByUid;

        private PendingRequest(
                @NonNull ConversationKey conversationKey,
                @NonNull List<String> messageUIds,
                @NonNull Map<String, CacheKey> cacheKeyByUid,
                @NonNull Map<String, List<Message>> sourceMessagesByUid) {
            this.conversationKey = conversationKey;
            this.messageUIds = messageUIds;
            this.cacheKeyByUid = cacheKeyByUid;
            this.sourceMessagesByUid = sourceMessagesByUid;
        }
    }

    private static final class CacheKey {
        private final ConversationKey conversationKey;
        private final String messageUId;

        private CacheKey(@NonNull ConversationKey conversationKey, @NonNull String messageUId) {
            this.conversationKey = conversationKey;
            this.messageUId = messageUId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            return conversationKey.equals(other.conversationKey)
                    && messageUId.equals(other.messageUId);
        }

        @Override
        public int hashCode() {
            return 31 * conversationKey.hashCode() + messageUId.hashCode();
        }
    }

    private static final class ConversationKey {
        private final Conversation.ConversationType conversationType;
        private final String targetId;
        private final String channelId;

        private ConversationKey(
                @NonNull Conversation.ConversationType conversationType,
                @NonNull String targetId,
                @NonNull String channelId) {
            this.conversationType = conversationType;
            this.targetId = targetId;
            this.channelId = channelId;
        }

        private ConversationIdentifier toIdentifier() {
            return ConversationIdentifier.obtain(conversationType, targetId, channelId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConversationKey)) {
                return false;
            }
            ConversationKey other = (ConversationKey) obj;
            return conversationType == other.conversationType
                    && targetId.equals(other.targetId)
                    && channelId.equals(other.channelId);
        }

        @Override
        public int hashCode() {
            int result = conversationType.hashCode();
            result = 31 * result + targetId.hashCode();
            result = 31 * result + channelId.hashCode();
            return result;
        }
    }

    private static final class RongCoreQueryClient implements QueryClient {
        @Override
        public void getMessagesByUIds(
                GetMessagesByUIdsParams params,
                IRongCoreCallback.ResultCallback<List<MessageResult>> callback) {
            RongCoreClient.getInstance().getMessagesByUIds(params, callback);
        }
    }

    private static final class IMCenterRefreshDispatcher implements RefreshDispatcher {
        @Override
        public void refresh(Message message) {
            IMCenter.getInstance().refreshMessage(message);
        }
    }
}
