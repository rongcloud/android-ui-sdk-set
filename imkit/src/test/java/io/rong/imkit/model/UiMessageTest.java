package io.rong.imkit.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    @Test
    public void hasReactionUserMatchesUsersInReactionSummary() {
        List<MessageReaction> reactions = new ArrayList<>();
        MessageReaction reaction = new MessageReaction();
        reaction.setUsers(Collections.singletonList(new MessageReactionUser("user-1", 1L)));
        reactions.add(reaction);

        assertTrue(UiMessage.hasReactionUser(reactions, "user-1"));
        assertFalse(UiMessage.hasReactionUser(reactions, "user-2"));
    }
}
