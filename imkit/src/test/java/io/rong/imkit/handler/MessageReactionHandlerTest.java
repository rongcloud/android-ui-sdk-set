package io.rong.imkit.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionOperationType;
import io.rong.imlib.model.MessageReactionUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class MessageReactionHandlerTest {

    @Test
    public void applyDeltaAddsNewReactionWhenCountIsPositive() {
        MessageReaction delta = reaction("m1", "👍", 1, user("u1"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.emptyList(), MessageReactionOperationType.ADDED, delta, "u1");

        assertEquals(1, result.size());
        assertEquals("👍", result.get(0).getReactionId());
        assertEquals(1, result.get(0).getTotalCount());
        assertTrue(result.get(0).isHasCurrentUserReacted());
    }

    @Test
    public void applyDeltaMergesAddedUserIntoExistingReaction() {
        MessageReaction existing = reaction("m1", "👍", 1, user("u1"));
        existing.setHasCurrentUserReacted(false);
        MessageReaction delta = reaction("m1", "👍", 2, user("me"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.singletonList(existing),
                        MessageReactionOperationType.ADDED,
                        delta,
                        "me");

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getTotalCount());
        assertEquals(2, result.get(0).getUsers().size());
        assertTrue(result.get(0).isHasCurrentUserReacted());
    }

    @Test
    public void applyDeltaKeepsLocalReactionWhenRemoteAddCountIsMissing() {
        MessageReaction existing = reaction("m1", "👍", 2, user("u1"), user("me"));
        existing.setHasCurrentUserReacted(true);
        MessageReaction delta = reaction("m1", "👍", 0, user("u2"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.singletonList(existing),
                        MessageReactionOperationType.ADDED,
                        delta,
                        "me");

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).getTotalCount());
        assertEquals(3, result.get(0).getUsers().size());
        assertTrue(result.get(0).isHasCurrentUserReacted());
    }

    @Test
    public void applyDeltaAddsNewReactionWhenAddCountIsMissing() {
        MessageReaction delta = reaction("m1", "👍", 0, user("u1"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.emptyList(), MessageReactionOperationType.ADDED, delta, "me");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getTotalCount());
        assertFalse(result.get(0).isHasCurrentUserReacted());
    }

    @Test
    public void applyDeltaRemovesReactionWhenCountIsZero() {
        MessageReaction existing = reaction("m1", "👍", 1, user("u1"));
        MessageReaction delta = reaction("m1", "👍", 0, user("u1"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.singletonList(existing),
                        MessageReactionOperationType.REMOVED,
                        delta,
                        "u1");

        assertTrue(result.isEmpty());
    }

    @Test
    public void applyDeltaRemovesUserButPreservesReactionWhenCountRemainsPositive() {
        MessageReaction existing = reaction("m1", "👍", 2, user("u1"), user("me"));
        existing.setHasCurrentUserReacted(true);
        MessageReaction delta = reaction("m1", "👍", 1, user("me"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.singletonList(existing),
                        MessageReactionOperationType.REMOVED,
                        delta,
                        "me");

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getTotalCount());
        assertEquals(1, result.get(0).getUsers().size());
        assertEquals("u1", result.get(0).getUsers().get(0).getUserId());
        assertFalse(result.get(0).isHasCurrentUserReacted());
    }

    @Test
    public void applyDeltaDoesNotCreateReactionForRemoveWhenMissingLocally() {
        MessageReaction delta = reaction("m1", "👍", 1, user("u1"));

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        Collections.emptyList(), MessageReactionOperationType.REMOVED, delta, "me");

        assertTrue(result.isEmpty());
    }

    @Test
    public void applyDeltaClearsAllReactionsWhenClearedWithoutReactionId() {
        List<MessageReaction> current = new ArrayList<>();
        current.add(reaction("m1", "👍", 2, user("u1"), user("me")));
        current.add(reaction("m1", "😂", 1, user("u2")));

        // CLEARED 且 reactionId 为空：清空整条消息的全部回应。
        MessageReaction delta = new MessageReaction();
        delta.setMessageUId("m1");

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        current, MessageReactionOperationType.CLEARED, delta, "me");

        assertTrue(result.isEmpty());
    }

    @Test
    public void applyDeltaRemovesOnlyTargetReactionWhenClearedWithReactionId() {
        List<MessageReaction> current = new ArrayList<>();
        current.add(reaction("m1", "👍", 2, user("u1"), user("me")));
        current.add(reaction("m1", "😂", 1, user("u2")));

        // CLEARED 且 reactionId 非空：仅移除指定的那一个回应，其余保留。
        MessageReaction delta = new MessageReaction();
        delta.setMessageUId("m1");
        delta.setReactionId("👍");

        List<MessageReaction> result =
                ReactionMergeUtils.applyDelta(
                        current, MessageReactionOperationType.CLEARED, delta, "me");

        assertEquals(1, result.size());
        assertEquals("😂", result.get(0).getReactionId());
    }

    @Test
    public void applyLocalToggleAppendsNewReactionAfterExistingOrder() {
        List<MessageReaction> current = new ArrayList<>();
        current.add(reaction("m1", "❤️", 4, user("u3")));
        current.add(reaction("m1", "👍", 2, user("u1")));

        MessageReaction local = reaction("m1", "😂", 0);

        List<MessageReaction> result =
                ReactionMergeUtils.applyLocalToggle(
                        current, MessageReactionOperationType.ADDED, local, "me");

        assertEquals(3, result.size());
        assertEquals("❤️", result.get(0).getReactionId());
        assertEquals("👍", result.get(1).getReactionId());
        assertEquals("😂", result.get(2).getReactionId());
        assertEquals(1, result.get(2).getTotalCount());
        assertTrue(result.get(2).isHasCurrentUserReacted());
        assertEquals("me", result.get(2).getUsers().get(0).getUserId());
    }

    @Test
    public void applyLocalToggleUpdatesExistingReactionInPlace() {
        List<MessageReaction> current = new ArrayList<>();
        current.add(reaction("m1", "❤️", 4, user("u3")));
        current.add(reaction("m1", "👍", 2, user("u1")));

        MessageReaction local = reaction("m1", "👍", 0);

        List<MessageReaction> result =
                ReactionMergeUtils.applyLocalToggle(
                        current, MessageReactionOperationType.ADDED, local, "me");

        assertEquals(2, result.size());
        assertEquals("❤️", result.get(0).getReactionId());
        assertEquals("👍", result.get(1).getReactionId());
        assertEquals(3, result.get(1).getTotalCount());
        assertTrue(result.get(1).isHasCurrentUserReacted());
        assertEquals(2, result.get(1).getUsers().size());
        assertEquals("me", result.get(1).getUsers().get(1).getUserId());
    }

    @Test
    public void applyLocalToggleRemovesExistingReactionInPlace() {
        List<MessageReaction> current = new ArrayList<>();
        current.add(reaction("m1", "❤️", 4, user("u3")));
        MessageReaction target = reaction("m1", "👍", 2, user("u1"), user("me"));
        target.setHasCurrentUserReacted(true);
        current.add(target);

        MessageReaction local = reaction("m1", "👍", 0);

        List<MessageReaction> result =
                ReactionMergeUtils.applyLocalToggle(
                        current, MessageReactionOperationType.REMOVED, local, "me");

        assertEquals(2, result.size());
        assertEquals("❤️", result.get(0).getReactionId());
        assertEquals("👍", result.get(1).getReactionId());
        assertEquals(1, result.get(1).getTotalCount());
        assertFalse(result.get(1).isHasCurrentUserReacted());
    }

    @Test
    public void applyLocalToggleDropsReactionWhenLocalRemoveMakesCountZero() {
        MessageReaction target = reaction("m1", "👍", 1, user("me"));
        target.setHasCurrentUserReacted(true);
        MessageReaction local = reaction("m1", "👍", 0);

        List<MessageReaction> result =
                ReactionMergeUtils.applyLocalToggle(
                        Collections.singletonList(target),
                        MessageReactionOperationType.REMOVED,
                        local,
                        "me");

        assertTrue(result.isEmpty());
    }

    @Test
    public void copySummaryInLatestOrderUsesServerSummaryOrder() {
        List<MessageReaction> latest = new ArrayList<>();
        latest.add(reaction("m1", "❤️", 4, user("u3"), user("me")));
        latest.add(reaction("m1", "👍", 2, user("u1"), user("me")));
        latest.add(reaction("m1", "😂", 1, user("u2")));

        List<MessageReaction> result = ReactionMergeUtils.copySummaryInLatestOrder(latest);

        assertEquals("❤️", result.get(0).getReactionId());
        assertEquals(4, result.get(0).getTotalCount());
        assertEquals("👍", result.get(1).getReactionId());
        assertEquals(2, result.get(1).getTotalCount());
        assertEquals("😂", result.get(2).getReactionId());
        assertEquals(1, result.get(2).getTotalCount());
    }

    @Test
    public void copySummaryInLatestOrderDropsInvalidReactionIds() {
        List<MessageReaction> latest = new ArrayList<>();
        latest.add(reaction("m1", "❤️", 1, user("u3")));
        latest.add(reaction("m1", "", 1, user("u4")));
        latest.add(null);
        latest.add(reaction("m1", "😂", 3, user("u2"), user("me")));
        latest.add(reaction("m1", "👍", 2, user("u1"), user("me")));

        List<MessageReaction> result = ReactionMergeUtils.copySummaryInLatestOrder(latest);

        assertEquals(3, result.size());
        assertEquals("❤️", result.get(0).getReactionId());
        assertEquals("😂", result.get(1).getReactionId());
        assertEquals("👍", result.get(2).getReactionId());
    }

    @Test
    public void copySummaryInLatestOrderKeepsMoreThanFiftyReactions() {
        List<MessageReaction> latest = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            latest.add(reaction("m1", "reaction-" + i, 1, user("me")));
        }

        List<MessageReaction> result = ReactionMergeUtils.copySummaryInLatestOrder(latest);

        assertEquals(100, result.size());
        assertEquals("reaction-0", result.get(0).getReactionId());
        assertEquals("reaction-99", result.get(99).getReactionId());
    }

    private static MessageReaction reaction(
            String messageUId, String reactionId, int count, MessageReactionUser... users) {
        MessageReaction reaction = new MessageReaction();
        reaction.setMessageUId(messageUId);
        reaction.setReactionId(reactionId);
        reaction.setTotalCount(count);
        List<MessageReactionUser> userList = new ArrayList<>();
        Collections.addAll(userList, users);
        reaction.setUsers(userList);
        return reaction;
    }

    private static MessageReactionUser user(String userId) {
        return new MessageReactionUser(userId, 1L);
    }
}
