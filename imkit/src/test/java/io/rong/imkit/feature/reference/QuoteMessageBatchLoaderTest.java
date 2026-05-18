package io.rong.imkit.feature.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageResult;
import io.rong.imlib.model.QuoteInfo;
import io.rong.imlib.params.GetMessagesByUIdsParams;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.TextMessage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class QuoteMessageBatchLoaderTest {

    @Test
    public void loadForMessagesBatchesUniqueNormalQuoteUidsByConversation() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message first = sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message second =
                sourceMessage("source-2", "quoted-2", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message duplicate =
                sourceMessage("source-3", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);

        loader.loadForMessages(Arrays.asList(first, second, duplicate), null);

        assertEquals(1, queryClient.calls.size());
        assertEquals(Arrays.asList("quoted-1", "quoted-2"), queryClient.calls.get(0).messageUIds);
        assertEquals(QuoteMessageBatchLoader.State.LOADING, loader.getResult(first).getState());
        assertEquals(QuoteMessageBatchLoader.State.LOADING, loader.getResult(second).getState());
    }

    @Test
    public void loadForMessagesSkipsQuoteInfoTerminalStates() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message deleted =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DELETED);
        Message recalled =
                sourceMessage("source-2", "quoted-2", QuoteInfo.QuoteMessageStatus.RECALLED);

        loader.loadForMessages(Arrays.asList(deleted, recalled), null);

        assertEquals(0, queryClient.calls.size());
        assertEquals(QuoteMessageBatchLoader.State.DELETED, loader.getResult(deleted).getState());
        assertEquals(QuoteMessageBatchLoader.State.RECALLED, loader.getResult(recalled).getState());
    }

    @Test
    public void successMapsLoadedMissingEmptyAndRecallResultsToExplicitStates() {
        FakeQueryClient queryClient = new FakeQueryClient();
        RecordingRefreshDispatcher refreshDispatcher = new RecordingRefreshDispatcher();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, refreshDispatcher);
        Message loaded =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message missing =
                sourceMessage("source-2", "quoted-2", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message emptyContent =
                sourceMessage("source-3", "quoted-3", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message recalled =
                sourceMessage("source-4", "quoted-4", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message loadedQuoted = quotedMessage("quoted-1", TextMessage.obtain("hello"));
        Message emptyQuoted = quotedMessage("quoted-3", null);
        Message recalledQuoted =
                quotedMessage(
                        "quoted-4",
                        new RecallNotificationMessage("operator", 1L, "RC:TxtMsg", false, false));

        loader.loadForMessages(Arrays.asList(loaded, missing, emptyContent, recalled), null);
        assertEquals(1, queryClient.calls.size());
        assertEquals(
                Arrays.asList("quoted-1", "quoted-2", "quoted-3", "quoted-4"),
                queryClient.calls.get(0).messageUIds);
        queryClient.lastCallback.onSuccess(
                Arrays.asList(
                        result("quoted-1", loadedQuoted),
                        result("quoted-3", emptyQuoted),
                        result("quoted-4", recalledQuoted)));

        assertEquals(QuoteMessageBatchLoader.State.LOADED, loader.getResult(loaded).getState());
        assertSame(loadedQuoted, loader.getResult(loaded).getMessage());
        assertEquals(QuoteMessageBatchLoader.State.DELETED, loader.getResult(missing).getState());
        assertEquals(
                QuoteMessageBatchLoader.State.DELETED, loader.getResult(emptyContent).getState());
        assertEquals(QuoteMessageBatchLoader.State.RECALLED, loader.getResult(recalled).getState());
        assertEquals(4, refreshDispatcher.messages.size());
    }

    @Test
    public void queryChannelInvalidMarksFailedAndRefreshesSources() {
        FakeQueryClient queryClient = new FakeQueryClient();
        RecordingRefreshDispatcher refreshDispatcher = new RecordingRefreshDispatcher();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, refreshDispatcher);
        Message source =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);

        loader.loadForMessages(Collections.singletonList(source), null);
        queryClient.lastCallback.onError(IRongCoreEnum.CoreErrorCode.RC_NET_CHANNEL_INVALID);

        assertEquals(QuoteMessageBatchLoader.State.FAILED, loader.getResult(source).getState());
        assertEquals(1, refreshDispatcher.messages.size());
        assertSame(source, refreshDispatcher.messages.get(0));
    }

    @Test
    public void successWithPerUidChannelInvalidMarksFailed() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message source =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);

        loader.loadForMessages(Collections.singletonList(source), null);
        queryClient.lastCallback.onSuccess(
                Collections.singletonList(
                        new MessageResult(
                                IRongCoreEnum.CoreErrorCode.RC_NET_CHANNEL_INVALID,
                                "quoted-1",
                                null)));

        assertEquals(QuoteMessageBatchLoader.State.FAILED, loader.getResult(source).getState());
    }

    @Test
    public void successWithPerUidNotFoundMarksDeleted() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message source =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);

        loader.loadForMessages(Collections.singletonList(source), null);
        queryClient.lastCallback.onSuccess(
                Collections.singletonList(
                        new MessageResult(
                                IRongCoreEnum.CoreErrorCode.RC_DB_DATA_NOT_FOUND,
                                "quoted-1",
                                null)));

        assertEquals(QuoteMessageBatchLoader.State.DELETED, loader.getResult(source).getState());
    }

    @Test
    public void loadForUiMessagesUsesWholeVisibleListInOneBatch() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message first = sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message second =
                sourceMessage("source-2", "quoted-2", QuoteInfo.QuoteMessageStatus.DEFAULT);

        loader.loadForUiMessages(Arrays.asList(uiMessage(first), uiMessage(second)), null);

        assertEquals(1, queryClient.calls.size());
        assertEquals(Arrays.asList("quoted-1", "quoted-2"), queryClient.calls.get(0).messageUIds);
    }

    @Test
    public void forceMessageRequeriesLoadedQuote() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message source =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);

        loader.loadForMessages(Collections.singletonList(source), null);
        queryClient.lastCallback.onSuccess(
                Collections.singletonList(
                        result("quoted-1", quotedMessage("quoted-1", TextMessage.obtain("old")))));
        loader.loadForMessages(Collections.singletonList(source), null);
        loader.loadForMessages(Collections.singletonList(source), source);

        assertEquals(2, queryClient.calls.size());
        assertEquals(Collections.singletonList("quoted-1"), queryClient.calls.get(1).messageUIds);
        assertEquals(QuoteMessageBatchLoader.State.LOADING, loader.getResult(source).getState());
    }

    @Test
    public void markMessagesModifiedReplacesLoadedQuoteCache() {
        FakeQueryClient queryClient = new FakeQueryClient();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, new RecordingRefreshDispatcher());
        Message source =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message oldQuoted = quotedMessage("quoted-1", TextMessage.obtain("old"));
        Message modifiedQuoted = quotedMessage("quoted-1", TextMessage.obtain("modified"));

        loader.loadForMessages(Collections.singletonList(source), null);
        queryClient.lastCallback.onSuccess(
                Collections.singletonList(result("quoted-1", oldQuoted)));
        loader.markMessagesModified(Collections.singletonList(modifiedQuoted));

        assertEquals(QuoteMessageBatchLoader.State.LOADED, loader.getResult(source).getState());
        assertSame(modifiedQuoted, loader.getResult(source).getMessage());
    }

    @Test
    public void markMessagesRecalledRefreshesAlreadyLoadedQuoteSources() {
        FakeQueryClient queryClient = new FakeQueryClient();
        RecordingRefreshDispatcher refreshDispatcher = new RecordingRefreshDispatcher();
        QuoteMessageBatchLoader loader =
                new QuoteMessageBatchLoader(queryClient, refreshDispatcher);
        Message source =
                sourceMessage("source-1", "quoted-1", QuoteInfo.QuoteMessageStatus.DEFAULT);
        Message quoted = quotedMessage("quoted-1", TextMessage.obtain("old"));
        source.setConversationType(Conversation.ConversationType.ULTRA_GROUP);
        source.setChannelId("channel-1");
        quoted.setConversationType(Conversation.ConversationType.ULTRA_GROUP);
        quoted.setChannelId("channel-1");

        loader.loadForMessages(Collections.singletonList(source), null);
        queryClient.lastCallback.onSuccess(Collections.singletonList(result("quoted-1", quoted)));
        refreshDispatcher.messages.clear();
        loader.markMessagesRecalled(quoted);

        assertEquals(QuoteMessageBatchLoader.State.RECALLED, loader.getResult(source).getState());
        assertEquals(1, refreshDispatcher.messages.size());
        assertSame(source, refreshDispatcher.messages.get(0));
    }

    private static Message sourceMessage(
            String sourceUId, String quotedUId, QuoteInfo.QuoteMessageStatus status) {
        Message message =
                Message.obtain(
                        "target",
                        Conversation.ConversationType.PRIVATE,
                        "",
                        TextMessage.obtain("source"));
        message.setUId(sourceUId);
        message.setMessageId(sourceUId.hashCode());
        message.setSenderUserId("sender");
        message.setQuoteInfo(new QuoteInfo(quotedUId, "quoted-sender", "RC:TxtMsg", status));
        return message;
    }

    private static Message quotedMessage(String uid, io.rong.imlib.model.MessageContent content) {
        Message message =
                Message.obtain("target", Conversation.ConversationType.PRIVATE, "", content);
        message.setUId(uid);
        message.setMessageId(uid.hashCode());
        message.setSenderUserId("quoted-sender");
        return message;
    }

    private static UiMessage uiMessage(Message message) {
        try {
            Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Method getReflectionFactory =
                    reflectionFactoryClass.getDeclaredMethod("getReflectionFactory");
            Object reflectionFactory = getReflectionFactory.invoke(null);
            Method newConstructorForSerialization =
                    reflectionFactoryClass.getDeclaredMethod(
                            "newConstructorForSerialization", Class.class, Constructor.class);
            Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
            Constructor<?> silentConstructor =
                    (Constructor<?>)
                            newConstructorForSerialization.invoke(
                                    reflectionFactory, UiMessage.class, objectConstructor);
            silentConstructor.setAccessible(true);
            UiMessage uiMessage = (UiMessage) silentConstructor.newInstance();
            Field messageField = UiMessage.class.getDeclaredField("message");
            messageField.setAccessible(true);
            messageField.set(uiMessage, message);
            return uiMessage;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static MessageResult result(String uid, Message message) {
        return new MessageResult(IRongCoreEnum.CoreErrorCode.SUCCESS, uid, message);
    }

    private static final class FakeQueryClient implements QuoteMessageBatchLoader.QueryClient {
        private final List<QueryCall> calls = new ArrayList<>();
        private IRongCoreCallback.ResultCallback<List<MessageResult>> lastCallback;

        @Override
        public void getMessagesByUIds(
                GetMessagesByUIdsParams params,
                IRongCoreCallback.ResultCallback<List<MessageResult>> callback) {
            calls.add(new QueryCall(params.getMessageUIds()));
            lastCallback = callback;
        }
    }

    private static final class QueryCall {
        private final List<String> messageUIds;

        private QueryCall(List<String> messageUIds) {
            this.messageUIds = new ArrayList<>(messageUIds);
        }
    }

    private static final class RecordingRefreshDispatcher
            implements QuoteMessageBatchLoader.RefreshDispatcher {
        private final List<Message> messages = new ArrayList<>();

        @Override
        public void refresh(Message message) {
            messages.add(message);
        }
    }
}
