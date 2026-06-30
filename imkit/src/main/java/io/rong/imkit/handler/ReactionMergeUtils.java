package io.rong.imkit.handler;

import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionOperationType;
import io.rong.imlib.model.MessageReactionUser;
import java.util.ArrayList;
import java.util.List;

/** Utilities for merging realtime reaction deltas into the message summary snapshot. */
public final class ReactionMergeUtils {

    private ReactionMergeUtils() {}

    public static List<MessageReaction> applyDelta(
            List<MessageReaction> current,
            MessageReactionOperationType operationType,
            MessageReaction delta) {
        return applyDelta(
                current, operationType, delta, RongIMClient.getInstance().getCurrentUserId());
    }

    public static List<MessageReaction> applyDelta(
            List<MessageReaction> current,
            MessageReactionOperationType operationType,
            MessageReaction delta,
            String currentUserId) {
        List<MessageReaction> list = current == null ? new ArrayList<>() : new ArrayList<>(current);
        // CLEARED 区分两种语义（与 iOS RCConversationViewController#applyMessageReactionEvent 对齐）：
        // reactionId 为空表示清空该消息的全部回应；reactionId 非空表示仅移除指定的那一个回应。
        if (operationType == MessageReactionOperationType.CLEARED) {
            if (delta == null || isEmpty(delta.getReactionId())) {
                return new ArrayList<>();
            }
            removeReactionById(list, delta.getReactionId());
            return list;
        }
        if (delta == null || isEmpty(delta.getReactionId())) {
            return list;
        }
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            MessageReaction reaction = list.get(i);
            if (reaction != null && delta.getReactionId().equals(reaction.getReactionId())) {
                index = i;
                break;
            }
        }
        if (operationType == MessageReactionOperationType.REMOVED) {
            if (index < 0) {
                return list;
            }
            if (delta.getTotalCount() <= 0) {
                list.remove(index);
                return list;
            }
            list.set(
                    index,
                    mergeReactionDelta(list.get(index), operationType, delta, currentUserId));
        } else if (operationType == MessageReactionOperationType.ADDED) {
            if (index >= 0) {
                list.set(
                        index,
                        mergeReactionDelta(list.get(index), operationType, delta, currentUserId));
                return list;
            }
            list.add(createReactionDelta(operationType, delta, currentUserId));
        }
        return list;
    }

    public static List<MessageReaction> copySummaryInLatestOrder(List<MessageReaction> reactions) {
        List<MessageReaction> result = new ArrayList<>();
        if (reactions == null || reactions.isEmpty()) {
            return result;
        }
        for (MessageReaction reaction : reactions) {
            if (reaction != null && !isEmpty(reaction.getReactionId())) {
                result.add(reaction);
            }
        }
        return result;
    }

    public static List<MessageReaction> applyLocalToggle(
            List<MessageReaction> current,
            MessageReactionOperationType operationType,
            MessageReaction reaction) {
        return applyLocalToggle(
                current, operationType, reaction, RongIMClient.getInstance().getCurrentUserId());
    }

    public static List<MessageReaction> applyLocalToggle(
            List<MessageReaction> current,
            MessageReactionOperationType operationType,
            MessageReaction reaction,
            String currentUserId) {
        List<MessageReaction> list = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (operationType == MessageReactionOperationType.CLEARED) {
            return new ArrayList<>();
        }
        if (operationType == null || reaction == null || isEmpty(reaction.getReactionId())) {
            return list;
        }
        int index = findReactionIndex(list, reaction.getReactionId());
        if (operationType == MessageReactionOperationType.ADDED) {
            if (index < 0) {
                list.add(createLocalReaction(reaction, currentUserId));
                return list;
            }
            MessageReaction existing = list.get(index);
            if (existing == null || existing.isHasCurrentUserReacted()) {
                return list;
            }
            MessageReaction updated = copyReaction(existing);
            updated.setTotalCount(Math.max(existing.getTotalCount(), 0) + 1);
            updated.setHasCurrentUserReacted(true);
            updated.setUsers(
                    appendUser(updated.getUsers(), currentUserId, reaction.getReactionTime()));
            list.set(index, updated);
        } else if (operationType == MessageReactionOperationType.REMOVED) {
            if (index < 0) {
                return list;
            }
            MessageReaction existing = list.get(index);
            if (existing == null || !existing.isHasCurrentUserReacted()) {
                return list;
            }
            int totalCount = Math.max(existing.getTotalCount() - 1, 0);
            if (totalCount <= 0) {
                list.remove(index);
                return list;
            }
            MessageReaction updated = copyReaction(existing);
            updated.setTotalCount(totalCount);
            updated.setHasCurrentUserReacted(false);
            updated.setUsers(copyUsersExcluding(existing.getUsers(), currentUserId));
            list.set(index, updated);
        }
        return list;
    }

    private static MessageReaction createReactionDelta(
            MessageReactionOperationType operationType,
            MessageReaction delta,
            String currentUserId) {
        if (operationType == null) {
            return delta;
        }
        MessageReaction merged = new MessageReaction();
        merged.setMessageUId(delta.getMessageUId());
        merged.setReactionId(delta.getReactionId());
        merged.setTotalCount(resolveDeltaTotalCount(0, operationType, delta.getTotalCount()));
        merged.setReactionTime(delta.getReactionTime());
        merged.setUsers(delta.getUsers());
        boolean currentUserTouched = containsUser(delta.getUsers(), currentUserId);
        if (currentUserTouched) {
            merged.setHasCurrentUserReacted(operationType == MessageReactionOperationType.ADDED);
        } else {
            merged.setHasCurrentUserReacted(delta.isHasCurrentUserReacted());
        }
        return merged;
    }

    private static int findReactionIndex(List<MessageReaction> reactions, String reactionId) {
        if (reactions == null || isEmpty(reactionId)) {
            return -1;
        }
        for (int i = 0; i < reactions.size(); i++) {
            MessageReaction reaction = reactions.get(i);
            if (reaction != null && reactionId.equals(reaction.getReactionId())) {
                return i;
            }
        }
        return -1;
    }

    private static MessageReaction createLocalReaction(
            MessageReaction reaction, String currentUserId) {
        MessageReaction localReaction = new MessageReaction();
        localReaction.setMessageUId(reaction.getMessageUId());
        localReaction.setReactionId(reaction.getReactionId());
        localReaction.setTotalCount(1);
        localReaction.setReactionTime(reaction.getReactionTime());
        localReaction.setHasCurrentUserReacted(true);
        localReaction.setUsers(appendUser(null, currentUserId, reaction.getReactionTime()));
        return localReaction;
    }

    private static MessageReaction copyReaction(MessageReaction reaction) {
        MessageReaction copy = new MessageReaction();
        copy.setMessageUId(reaction.getMessageUId());
        copy.setReactionId(reaction.getReactionId());
        copy.setTotalCount(reaction.getTotalCount());
        copy.setReactionTime(reaction.getReactionTime());
        copy.setHasCurrentUserReacted(reaction.isHasCurrentUserReacted());
        copy.setUsers(copyUsers(reaction.getUsers()));
        return copy;
    }

    private static List<MessageReactionUser> copyUsers(List<MessageReactionUser> users) {
        return users == null ? new ArrayList<>() : new ArrayList<>(users);
    }

    private static List<MessageReactionUser> appendUser(
            List<MessageReactionUser> users, String userId, long reactionTime) {
        List<MessageReactionUser> result = copyUsers(users);
        if (isEmpty(userId) || containsUser(result, userId)) {
            return result;
        }
        result.add(new MessageReactionUser(userId, reactionTime));
        return result;
    }

    private static List<MessageReactionUser> copyUsersExcluding(
            List<MessageReactionUser> users, String userId) {
        List<MessageReactionUser> result = new ArrayList<>();
        if (users == null) {
            return result;
        }
        for (MessageReactionUser user : users) {
            if (user == null) {
                continue;
            }
            if (isEmpty(userId) || !userId.equals(user.getUserId())) {
                result.add(user);
            }
        }
        return result;
    }

    private static MessageReaction mergeReactionDelta(
            MessageReaction existing,
            MessageReactionOperationType operationType,
            MessageReaction delta,
            String currentUserId) {
        if (existing == null || operationType == null) {
            return delta;
        }
        MessageReaction merged = new MessageReaction();
        merged.setMessageUId(delta.getMessageUId());
        merged.setReactionId(delta.getReactionId());
        merged.setTotalCount(
                resolveDeltaTotalCount(
                        existing.getTotalCount(), operationType, delta.getTotalCount()));
        merged.setReactionTime(delta.getReactionTime());
        merged.setUsers(mergeUsers(existing.getUsers(), delta.getUsers(), operationType));

        boolean currentUserTouched = containsUser(delta.getUsers(), currentUserId);
        if (currentUserTouched) {
            merged.setHasCurrentUserReacted(operationType == MessageReactionOperationType.ADDED);
        } else {
            merged.setHasCurrentUserReacted(existing.isHasCurrentUserReacted());
        }
        return merged;
    }

    private static List<MessageReactionUser> mergeUsers(
            List<MessageReactionUser> existingUsers,
            List<MessageReactionUser> deltaUsers,
            MessageReactionOperationType operationType) {
        List<MessageReactionUser> merged =
                existingUsers == null ? new ArrayList<>() : new ArrayList<>(existingUsers);
        if (deltaUsers == null || deltaUsers.isEmpty()) {
            return merged;
        }
        for (MessageReactionUser user : deltaUsers) {
            if (user == null || isEmpty(user.getUserId())) {
                continue;
            }
            removeUser(merged, user.getUserId());
            if (operationType == MessageReactionOperationType.ADDED) {
                merged.add(user);
            }
        }
        return merged;
    }

    private static int resolveDeltaTotalCount(
            int existingTotalCount,
            MessageReactionOperationType operationType,
            int deltaTotalCount) {
        if (operationType == MessageReactionOperationType.ADDED && deltaTotalCount <= 0) {
            return Math.max(existingTotalCount + 1, 1);
        }
        if (operationType == MessageReactionOperationType.REMOVED) {
            return Math.max(deltaTotalCount, 0);
        }
        return deltaTotalCount;
    }

    private static void removeReactionById(List<MessageReaction> reactions, String reactionId) {
        if (reactions == null || isEmpty(reactionId)) {
            return;
        }
        for (int i = reactions.size() - 1; i >= 0; i--) {
            MessageReaction reaction = reactions.get(i);
            if (reaction != null && reactionId.equals(reaction.getReactionId())) {
                reactions.remove(i);
            }
        }
    }

    private static void removeUser(List<MessageReactionUser> users, String userId) {
        if (users == null || isEmpty(userId)) {
            return;
        }
        for (int i = users.size() - 1; i >= 0; i--) {
            MessageReactionUser user = users.get(i);
            if (user != null && userId.equals(user.getUserId())) {
                users.remove(i);
            }
        }
    }

    private static boolean containsUser(List<MessageReactionUser> users, String userId) {
        if (users == null || isEmpty(userId)) {
            return false;
        }
        for (MessageReactionUser user : users) {
            if (user != null && userId.equals(user.getUserId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }
}
