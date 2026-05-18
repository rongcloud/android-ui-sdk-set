package io.rong.imkit.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.QuoteInfo;
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

    private Message newMessage(String uid, String quotedUid) {
        Message message = new Message();
        message.setUId(uid);
        message.setMessageId(uid.hashCode());
        message.setSenderUserId("sender");
        message.setTargetId("target");
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        message.setQuoteInfo(
                new QuoteInfo(
                        quotedUid,
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DEFAULT));
        return message;
    }
}
