package io.rong.imkit.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.QuoteInfo;
import io.rong.message.ReferenceMessage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import org.junit.Test;

public class EditMessageHandlerTest {

    @Test
    public void mergeQuoteInfoForRefreshPreservesExistingQuoteWhenRefreshMessageHasNoQuote() {
        Message cachedMessage = new Message();
        QuoteInfo cachedQuote =
                new QuoteInfo(
                        "quoted-message",
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DEFAULT);
        cachedMessage.setQuoteInfo(cachedQuote);
        Message refreshedMessage = new Message();

        EditMessageHandler.mergeQuoteInfoForRefresh(refreshedMessage, cachedMessage);

        assertSame(cachedQuote, refreshedMessage.getQuoteInfo());
    }

    @Test
    public void mergeQuoteInfoForRefreshKeepsIncomingQuoteWhenRefreshMessageHasValidQuote() {
        Message cachedMessage = new Message();
        cachedMessage.setQuoteInfo(
                new QuoteInfo(
                        "cached-quoted-message",
                        "cached-quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DEFAULT));
        Message refreshedMessage = new Message();
        QuoteInfo refreshedQuote =
                new QuoteInfo(
                        "refreshed-quoted-message",
                        "refreshed-quoted-sender",
                        "RC:ImgMsg",
                        QuoteInfo.QuoteMessageStatus.DEFAULT);
        refreshedMessage.setQuoteInfo(refreshedQuote);

        EditMessageHandler.mergeQuoteInfoForRefresh(refreshedMessage, cachedMessage);

        assertSame(refreshedQuote, refreshedMessage.getQuoteInfo());
    }

    @Test
    public void mergeQuoteInfoForRefreshPreservesTerminalQuoteStatusOverNormalRefreshStatus() {
        Message cachedMessage = new Message();
        cachedMessage.setQuoteInfo(
                new QuoteInfo(
                        "quoted-message",
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DELETED));
        Message refreshedMessage = new Message();
        refreshedMessage.setQuoteInfo(
                new QuoteInfo(
                        "quoted-message",
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DEFAULT));

        EditMessageHandler.mergeQuoteInfoForRefresh(refreshedMessage, cachedMessage);

        assertEquals(
                QuoteInfo.QuoteMessageStatus.DELETED,
                refreshedMessage.getQuoteInfo().getQuoteMessageStatus());
    }

    @Test
    public void shouldRefreshQuoteCardForEditedMessagesReturnsTrueWhenQuoteTargetsEditedUid() {
        Message editedMessage = new Message();
        editedMessage.setUId("edited-message");

        assertTrue(
                EditMessageHandler.shouldRefreshQuoteCardForEditedMessages(
                        newMessage("quoting-message", "edited-message"),
                        Collections.singleton(editedMessage.getUId())));
    }

    @Test
    public void shouldRefreshQuoteCardForEditedMessagesReturnsFalseForUnrelatedQuote() {
        Message editedMessage = new Message();
        editedMessage.setUId("edited-message");

        assertFalse(
                EditMessageHandler.shouldRefreshQuoteCardForEditedMessages(
                        newMessage("unrelated-message", "other-message"),
                        Collections.singleton(editedMessage.getUId())));
    }

    @Test
    public void processQuoteInfoDeleteStatusPreservesReplyMessageUiState() {
        EditMessageHandler handler = silentInstance(EditMessageHandler.class);
        Message quotedMessage = newMessage("quoted-message", null);
        Message replyMessage = newMessage("reply-message", "quoted-message");
        replyMessage.setMessageDirection(Message.MessageDirection.SEND);
        replyMessage.setSentStatus(Message.SentStatus.SENDING);
        UiMessage replyUiMessage = silentUiMessage(replyMessage);
        replyUiMessage.setState(State.NORMAL);

        handler.processMessageReferMsgStatus(
                quotedMessage,
                ReferenceMessage.ReferenceMessageStatus.DELETE,
                Collections.singletonList(replyUiMessage));

        assertEquals(
                QuoteInfo.QuoteMessageStatus.DELETED,
                replyMessage.getQuoteInfo().getQuoteMessageStatus());
        assertEquals(State.NORMAL, replyUiMessage.getState());
    }

    @Test
    public void processMessageEditStatusAndReferMsgStatusReturnsEditedQuoteMessageForRefresh() {
        EditMessageHandler handler = silentInstance(EditMessageHandler.class);
        Message editedMessage = newMessage("edited-message", null);
        Message quotingMessage = newMessage("quoting-message", "edited-message");
        UiMessage quotingUiMessage = silentUiMessage(quotingMessage);
        assertEquals(
                1,
                handler.processMessageEditStatusAndReferMsgStatus(
                                Collections.singletonList(editedMessage),
                                Collections.singletonList(quotingUiMessage))
                        .size());
    }

    private static Message newMessage(String uid, String quotedUid) {
        Message message = new Message();
        message.setUId(uid);
        message.setMessageId(uid.hashCode());
        message.setSenderUserId("sender");
        message.setTargetId("target");
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        if (quotedUid != null) {
            message.setQuoteInfo(
                    new QuoteInfo(
                            quotedUid,
                            "quoted-sender",
                            "RC:TxtMsg",
                            QuoteInfo.QuoteMessageStatus.DEFAULT));
        }
        return message;
    }

    private static UiMessage silentUiMessage(Message message) {
        UiMessage uiMessage = silentInstance(UiMessage.class);
        try {
            Field messageField = UiMessage.class.getDeclaredField("message");
            messageField.setAccessible(true);
            messageField.set(uiMessage, message);
            return uiMessage;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static <T> T silentInstance(Class<T> clazz) {
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
                                    reflectionFactory, clazz, objectConstructor);
            silentConstructor.setAccessible(true);
            return clazz.cast(silentConstructor.newInstance());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
