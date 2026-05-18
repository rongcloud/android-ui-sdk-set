package io.rong.imkit.conversation.messgelist.provider;

import static org.junit.Assert.assertEquals;

import android.view.Gravity;
import org.junit.Test;

public class ImageMessageItemProviderTest {
    @Test
    public void resolveImageRootGravityReturnsStartForSentQuotedImageMessage() {
        assertEquals(Gravity.START, ImageMessageItemProvider.resolveImageRootGravity(true, true));
    }

    @Test
    public void resolveImageRootGravityReturnsStartForReceivedQuotedImageMessage() {
        assertEquals(Gravity.START, ImageMessageItemProvider.resolveImageRootGravity(true, false));
    }

    @Test
    public void resolveImageRootGravityKeepsDefaultWithoutQuote() {
        assertEquals(-1, ImageMessageItemProvider.resolveImageRootGravity(false, true));
    }

    @Test
    public void resolveImageRootMarginUsesQuoteLineStartForQuotedImageMessage() {
        assertEquals(12, ImageMessageItemProvider.resolveImageRootMargin(true, 12));
    }

    @Test
    public void resolveImageRootEndMarginUsesQuoteLineStartForQuotedImageMessage() {
        assertEquals(12, ImageMessageItemProvider.resolveImageRootEndMargin(true, 12));
    }

    @Test
    public void resolveImageRootBottomMarginUsesQuoteLineStartForQuotedImageMessage() {
        assertEquals(12, ImageMessageItemProvider.resolveImageRootBottomMargin(true, 12));
    }

    @Test
    public void resolveImageRootMarginClearsMarginWithoutQuote() {
        assertEquals(0, ImageMessageItemProvider.resolveImageRootMargin(false, 12));
    }

    @Test
    public void resolveImageRootEndMarginClearsMarginWithoutQuote() {
        assertEquals(0, ImageMessageItemProvider.resolveImageRootEndMargin(false, 12));
    }

    @Test
    public void resolveImageRootBottomMarginClearsMarginWithoutQuote() {
        assertEquals(0, ImageMessageItemProvider.resolveImageRootBottomMargin(false, 12));
    }
}
