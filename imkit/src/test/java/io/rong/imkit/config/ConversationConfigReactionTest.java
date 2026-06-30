package io.rong.imkit.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConversationConfigReactionTest {

    @Test
    public void reactionDisplayModeDefaultsToCountOnly() {
        ConversationConfig config = new ConversationConfig();

        assertEquals(MessageReactionDisplayMode.COUNT_ONLY, config.getMessageReactionDisplayMode());
    }

    @Test
    public void reactionDisplayModeCanBeChangedToDetail() {
        ConversationConfig config = new ConversationConfig();

        config.setMessageReactionDisplayMode(MessageReactionDisplayMode.DETAIL);

        assertEquals(MessageReactionDisplayMode.DETAIL, config.getMessageReactionDisplayMode());
    }

    @Test
    public void reactionFrequentDisplayCountUsesDefaultWhenOutOfRange() {
        ConversationConfig config = new ConversationConfig();

        config.setMessageReactionFrequentDisplayCount(0);

        assertEquals(14, config.getMessageReactionFrequentDisplayCount());

        config.setMessageReactionFrequentDisplayCount(21);

        assertEquals(14, config.getMessageReactionFrequentDisplayCount());
    }
}
