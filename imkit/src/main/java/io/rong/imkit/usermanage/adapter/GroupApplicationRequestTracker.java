package io.rong.imkit.usermanage.adapter;

import io.rong.imlib.model.GroupApplicationDirection;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationType;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** 跟踪正在处理的群申请，避免同一申请重复提交。 */
final class GroupApplicationRequestTracker {
    private final Set<RequestKey> pendingRequestKeys = new HashSet<>();

    synchronized boolean tryStart(GroupApplicationInfo applicationInfo) {
        return pendingRequestKeys.add(requestKey(applicationInfo));
    }

    synchronized boolean isPending(GroupApplicationInfo applicationInfo) {
        return pendingRequestKeys.contains(requestKey(applicationInfo));
    }

    synchronized void finish(GroupApplicationInfo applicationInfo) {
        pendingRequestKeys.remove(requestKey(applicationInfo));
    }

    static RequestKey requestKey(GroupApplicationInfo applicationInfo) {
        if (applicationInfo == null) {
            return new RequestKey(null, null, null, null, null, null, 0);
        }
        return new RequestKey(
                applicationInfo.getDirection(),
                applicationInfo.getType(),
                applicationInfo.getGroupId(),
                userId(applicationInfo.getInviterInfo()),
                userId(applicationInfo.getJoinMemberInfo()),
                userId(applicationInfo.getOperatorInfo()),
                applicationInfo.getOperationTime());
    }

    private static String userId(GroupMemberInfo memberInfo) {
        return memberInfo == null ? "" : String.valueOf(memberInfo.getUserId());
    }

    static final class RequestKey {
        private final GroupApplicationDirection direction;
        private final GroupApplicationType type;
        private final String groupId;
        private final String inviterId;
        private final String joinMemberId;
        private final String operatorId;
        private final long operationTime;

        RequestKey(
                GroupApplicationDirection direction,
                GroupApplicationType type,
                String groupId,
                String inviterId,
                String joinMemberId,
                String operatorId,
                long operationTime) {
            this.direction = direction;
            this.type = type;
            this.groupId = groupId;
            this.inviterId = inviterId;
            this.joinMemberId = joinMemberId;
            this.operatorId = operatorId;
            this.operationTime = operationTime;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof RequestKey)) {
                return false;
            }
            RequestKey that = (RequestKey) object;
            return operationTime == that.operationTime
                    && Objects.equals(direction, that.direction)
                    && Objects.equals(type, that.type)
                    && Objects.equals(groupId, that.groupId)
                    && Objects.equals(inviterId, that.inviterId)
                    && Objects.equals(joinMemberId, that.joinMemberId)
                    && Objects.equals(operatorId, that.operatorId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    direction, type, groupId, inviterId, joinMemberId, operatorId, operationTime);
        }
    }
}
