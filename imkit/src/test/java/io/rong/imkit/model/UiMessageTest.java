package io.rong.imkit.model;

import static org.junit.Assert.assertEquals;

import io.rong.imlib.model.Message;
import org.junit.Test;

public class UiMessageTest {
    @Test
    public void resolveStateClearsProgressWhenOutgoingMessageIsSent() {
        assertEquals(
                State.NORMAL,
                UiMessage.resolveStateForSentStatus(
                        Message.MessageDirection.SEND, Message.SentStatus.SENT, State.PROGRESS));
    }

    @Test
    public void resolveStateClearsProgressWhenOutgoingMessageBecomesRead() {
        assertEquals(
                State.NORMAL,
                UiMessage.resolveStateForSentStatus(
                        Message.MessageDirection.SEND, Message.SentStatus.READ, State.PROGRESS));
    }

    @Test
    public void resolveStatePreservesProgressForReceivedMessages() {
        assertEquals(
                State.PROGRESS,
                UiMessage.resolveStateForSentStatus(
                        Message.MessageDirection.RECEIVE, Message.SentStatus.SENT, State.PROGRESS));
    }
}
