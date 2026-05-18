package io.rong.imkit.feature.reference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.QuoteInfo;
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
}
