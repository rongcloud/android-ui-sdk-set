package io.rong.imkit.handler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.Message;
import org.junit.Test;

public class MessageReactionToggleHandlerTest {

    @Test
    public void sendingFailedAndCanceledMessagesAreUnavailableForReaction() {
        assertTrue(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.SENDING));
        assertTrue(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.FAILED));
        assertTrue(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.CANCELED));
    }

    @Test
    public void stableMessageStatusesRemainAvailableForReaction() {
        assertFalse(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.SENT));
        assertFalse(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.RECEIVED));
        assertFalse(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.READ));
        assertFalse(
                MessageReactionToggleHandler.isReactionUnavailableForSentStatus(
                        Message.SentStatus.DESTROYED));
        assertFalse(MessageReactionToggleHandler.isReactionUnavailableForSentStatus(null));
    }
}
