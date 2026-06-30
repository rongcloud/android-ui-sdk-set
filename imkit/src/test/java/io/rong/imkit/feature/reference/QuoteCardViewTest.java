package io.rong.imkit.feature.reference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.QuoteInfo;
import io.rong.message.TextMessage;
import org.junit.Test;

public class QuoteCardViewTest {
    @Test
    public void shouldQueryQuotedMessageReturnsTrueForNormalStatus() {
        QuoteInfo quoteInfo =
                new QuoteInfo(
                        "quoted-message",
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DEFAULT);

        assertTrue(QuoteCardView.shouldQueryQuotedMessage(quoteInfo));
    }

    @Test
    public void shouldQueryQuotedMessageReturnsFalseForEmptyUid() {
        QuoteInfo quoteInfo =
                new QuoteInfo(
                        "", "quoted-sender", "RC:TxtMsg", QuoteInfo.QuoteMessageStatus.DEFAULT);

        assertFalse(QuoteCardView.shouldQueryQuotedMessage(quoteInfo));
    }

    @Test
    public void shouldQueryQuotedMessageReturnsFalseForDeletedStatus() {
        QuoteInfo quoteInfo =
                new QuoteInfo(
                        "quoted-message",
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.DELETED);

        assertFalse(QuoteCardView.shouldQueryQuotedMessage(quoteInfo));
    }

    @Test
    public void shouldQueryQuotedMessageReturnsFalseForRecalledStatus() {
        QuoteInfo quoteInfo =
                new QuoteInfo(
                        "quoted-message",
                        "quoted-sender",
                        "RC:TxtMsg",
                        QuoteInfo.QuoteMessageStatus.RECALLED);

        assertFalse(QuoteCardView.shouldQueryQuotedMessage(quoteInfo));
    }

    @Test
    public void bindingStateReusesLoadedQuoteForSameMessageUid() {
        QuoteCardBindingState bindingState = new QuoteCardBindingState();
        bindingState.bind("quoted-message");
        bindingState.setQuotedMessage(quotedMessage("quoted-message"));

        assertTrue(bindingState.shouldReuse("quoted-message"));
        assertFalse(bindingState.shouldReuse("quoted-message", true));
        assertFalse(bindingState.shouldReuse("other-message"));
    }

    @Test
    public void bindingStateDoesNotReuseUnavailableQuote() {
        QuoteCardBindingState bindingState = new QuoteCardBindingState();
        bindingState.bind("quoted-message");
        bindingState.markUnavailable();

        assertFalse(bindingState.shouldReuse("quoted-message"));
    }

    private static Message quotedMessage(String uid) {
        Message message =
                Message.obtain(
                        "target",
                        Conversation.ConversationType.PRIVATE,
                        TextMessage.obtain("hello"));
        message.setUId(uid);
        return message;
    }
}
