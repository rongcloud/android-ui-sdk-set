package io.rong.imkit.conversation.messgelist.provider;

import static org.junit.Assert.assertEquals;

import android.view.Gravity;
import org.junit.Test;

public class SightMessageItemProviderTest {
    @Test
    public void resolveSightRootGravityReturnsStartForSentQuotedSightMessage() {
        assertEquals(Gravity.START, SightMessageItemProvider.resolveSightRootGravity(true, true));
    }

    @Test
    public void resolveSightRootGravityReturnsStartForReceivedQuotedSightMessage() {
        assertEquals(Gravity.START, SightMessageItemProvider.resolveSightRootGravity(true, false));
    }

    @Test
    public void resolveSightRootGravityKeepsDefaultWithoutQuote() {
        assertEquals(-1, SightMessageItemProvider.resolveSightRootGravity(false, true));
    }

    @Test
    public void resolveSightRootMarginUsesQuoteLineStartForQuotedSightMessage() {
        assertEquals(12, SightMessageItemProvider.resolveSightRootMargin(true, 12));
    }

    @Test
    public void resolveSightRootEndMarginUsesQuoteLineStartForQuotedSightMessage() {
        assertEquals(12, SightMessageItemProvider.resolveSightRootEndMargin(true, 12));
    }

    @Test
    public void resolveSightRootBottomMarginUsesQuoteLineStartForQuotedSightMessage() {
        assertEquals(12, SightMessageItemProvider.resolveSightRootBottomMargin(true, 12));
    }

    @Test
    public void resolveSightRootMarginClearsMarginWithoutQuote() {
        assertEquals(0, SightMessageItemProvider.resolveSightRootMargin(false, 12));
    }

    @Test
    public void resolveSightRootEndMarginClearsMarginWithoutQuote() {
        assertEquals(0, SightMessageItemProvider.resolveSightRootEndMargin(false, 12));
    }

    @Test
    public void resolveSightRootBottomMarginClearsMarginWithoutQuote() {
        assertEquals(0, SightMessageItemProvider.resolveSightRootBottomMargin(false, 12));
    }
}
