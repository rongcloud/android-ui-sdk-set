package io.rong.imkit.conversation.messgelist.provider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BaseMessageItemProviderTest {
    @Test
    public void speechToTextContainerIsPlacedImmediatelyAfterMessageRow() {
        assertEquals(2, BaseMessageItemProvider.resolveSpeechToTextInsertIndex(1, 4));
    }

    @Test
    public void speechToTextContainerIsAppendedWhenMessageRowIsMissing() {
        assertEquals(4, BaseMessageItemProvider.resolveSpeechToTextInsertIndex(-1, 4));
    }
}
