package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.listener.GroupEventListener;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupInfoKeys;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupOperation;
import io.rong.imlib.model.GroupOperationType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 通过用户id拉取群成员信息
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupMembersByUserIdsHandler extends MultiDataHandler {

    private static final String TAG = GroupMembersByUserIdsHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_GROUP_MEMBERS", (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private static final int MAX_BATCH_SIZE = 100;

    private final String groupId;
    private final List<GroupMemberInfo> groupMemberInfoList = new ArrayList<>();

    private volatile boolean isLoading = false;
    private List<String> userIds;

    public GroupMembersByUserIdsHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.groupId = conversationIdentifier.getTargetId();
        IMCenter.getInstance().addGroupEventListener(groupEventListener);
    }

    public void getGroupMembers(List<String> userIds) {
        if (userIds == null) {
            notifyDataError(
                    KEY_GET_GROUP_MEMBERS,
                    IRongCoreEnum.CoreErrorCode.RC_INVALID_PARAMETER_USERIDLIST);
            return;
        }
        if (isLoading) {
            RLog.d(TAG, "getGroupMembers is loaded");
            return;
        }
        isLoading = true;
        groupMemberInfoList.clear();
        this.userIds = userIds;
        // Start fetching group members in batches
        fetchGroupMembersInBatches(userIds, 0);
    }

    private void fetchGroupMembersInBatches(List<String> userIds, int startIndex) {
        if (startIndex >= userIds.size()) {
            isLoading = false;
            notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfoList);
            return;
        }

        int endIndex = Math.min(startIndex + MAX_BATCH_SIZE, userIds.size());
        List<String> batch = userIds.subList(startIndex, endIndex);

        RongCoreClient.getInstance()
                .getGroupMembers(
                        groupId,
                        batch,
                        new IRongCoreCallback.ResultCallback<List<GroupMemberInfo>>() {
                            @Override
                            public void onSuccess(List<GroupMemberInfo> groupMemberInfos) {
                                if (groupMemberInfos != null) {
                                    groupMemberInfoList.addAll(groupMemberInfos);
                                }

                                // Check if there are more batches to fetch
                                if (endIndex < userIds.size()) {
                                    fetchGroupMembersInBatches(userIds, endIndex);
                                } else {
                                    // All batches fetched
                                    isLoading = false;
                                    notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfoList);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_GET_GROUP_MEMBERS, e);
                            }
                        });
    }

    @Override
    public void stop() {
        super.stop();
        IMCenter.getInstance().removeGroupEventListener(groupEventListener);
    }

    private final GroupEventListener groupEventListener =
            new GroupEventListener() {
                @Override
                public void onGroupOperation(
                        String groupId,
                        GroupMemberInfo operatorInfo,
                        GroupInfo groupInfo,
                        GroupOperation operation,
                        List<GroupMemberInfo> memberInfos,
                        long operationTime) {}

                @Override
                public void onGroupInfoChanged(
                        GroupMemberInfo operatorInfo,
                        GroupInfo groupInfo,
                        List<GroupInfoKeys> updateKeys,
                        long operationTime) {}

                @Override
                public void onGroupMemberInfoChanged(
                        String groupId,
                        GroupMemberInfo operatorInfo,
                        GroupMemberInfo memberInfo,
                        long operationTime) {
                    if (Objects.equals(groupId, GroupMembersByUserIdsHandler.this.groupId)
                            && GroupMembersByUserIdsHandler.this.userIds != null
                            && GroupMembersByUserIdsHandler.this.userIds.contains(
                                    memberInfo.getUserId())) {
                        getGroupMembers(userIds);
                    }
                }

                @Override
                public void onGroupApplicationEvent(GroupApplicationInfo info) {}

                @Override
                public void onGroupRemarkChangedSync(
                        String groupId,
                        GroupOperationType operationType,
                        String groupRemark,
                        long operationTime) {}

                @Override
                public void onGroupFollowsChangedSync(
                        String groupId,
                        GroupOperationType operationType,
                        List<String> userIds,
                        long operationTime) {}
            };
}
