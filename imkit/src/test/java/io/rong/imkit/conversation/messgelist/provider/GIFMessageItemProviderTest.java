package io.rong.imkit.conversation.messgelist.provider;

import static org.junit.Assert.assertEquals;

import android.view.Gravity;
import org.junit.Test;

public class GIFMessageItemProviderTest {
    @Test
    public void resolveGifRootGravityReturnsStartForSentQuotedGifMessage() {
        assertEquals(Gravity.START, GIFMessageItemProvider.resolveGifRootGravity(true, true));
    }

    @Test
    public void resolveGifRootGravityReturnsStartForReceivedQuotedGifMessage() {
        assertEquals(Gravity.START, GIFMessageItemProvider.resolveGifRootGravity(true, false));
    }

    @Test
    public void resolveGifRootGravityKeepsDefaultWithoutQuote() {
        assertEquals(-1, GIFMessageItemProvider.resolveGifRootGravity(false, true));
    }

    @Test
    public void resolveGifRootMarginUsesQuoteLineStartForQuotedGifMessage() {
        assertEquals(12, GIFMessageItemProvider.resolveGifRootMargin(true, 12));
    }

    @Test
    public void resolveGifRootEndMarginUsesQuoteLineStartForQuotedGifMessage() {
        assertEquals(12, GIFMessageItemProvider.resolveGifRootEndMargin(true, 12));
    }

    @Test
    public void resolveGifRootBottomMarginUsesQuoteLineStartForQuotedGifMessage() {
        assertEquals(12, GIFMessageItemProvider.resolveGifRootBottomMargin(true, 12));
    }

    @Test
    public void resolveGifRootMarginClearsMarginWithoutQuote() {
        assertEquals(0, GIFMessageItemProvider.resolveGifRootMargin(false, 12));
    }

    @Test
    public void resolveGifRootEndMarginClearsMarginWithoutQuote() {
        assertEquals(0, GIFMessageItemProvider.resolveGifRootEndMargin(false, 12));
    }

    @Test
    public void resolveGifRootBottomMarginClearsMarginWithoutQuote() {
        assertEquals(0, GIFMessageItemProvider.resolveGifRootBottomMargin(false, 12));
    }
}
