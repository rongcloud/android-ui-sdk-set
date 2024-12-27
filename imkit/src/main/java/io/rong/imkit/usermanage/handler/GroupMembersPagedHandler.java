package io.rong.imkit.usermanage.handler;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.listener.GroupEventListener;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupInfoKeys;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.GroupOperation;
import io.rong.imlib.model.GroupOperationType;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 群组成员加载
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupMembersPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupMembersPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_GROUP_MEMBERS", (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final String groupId;
    private final List<GroupMemberInfo> groupMemberInfos = new ArrayList<>();

    private GroupMemberRole currentGroupMemberRole;
    private boolean isOnlyRole;
    private String nextPageToken = null;

    private volatile boolean isLoading = false;
    private volatile boolean isLoadNext = false;
    private GroupMemberRole groupMemberRole;

    public GroupMembersPagedHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this(conversationIdentifier, 50);
    }

    public GroupMembersPagedHandler(
            @NonNull ConversationIdentifier conversationIdentifier, int pageCount) {
        this.pageCount = pageCount;
        this.groupId = conversationIdentifier.getTargetId();
        IMCenter.getInstance().addGroupEventListener(groupEventListener);
    }

    /**
     * 获取所有群成员
     *
     * @param groupMemberRole 群成员角色
     */
    public void getGroupMembersByRole(@NonNull GroupMemberRole groupMemberRole) {
        if (isLoading) {
            RLog.d(TAG, "getGroupMembersByRole is loaded");
            return;
        }
        this.groupMemberRole = groupMemberRole;
        groupMemberInfos.clear();
        isLoading = true;
        isLoadNext = false;
        nextPageToken = null;
        if (groupMemberRole == GroupMemberRole.Undef) {
            // 如果传入的是Undef，则依次拉取Owner, Manager, Normal
            fetchNextRole(GroupMemberRole.Undef);
        } else {
            // 否则拉取指定角色的群成员
            getGroupMembersByRoleByPage(groupMemberRole, null, true);
        }
    }

    private void getGroupMembersByRoleByPage(
            GroupMemberRole groupMemberRole, String pageToken, boolean isOnlyRole) {
        this.currentGroupMemberRole = groupMemberRole;
        this.isOnlyRole = isOnlyRole;
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(pageCount);
        pagingQueryOption.setOrder(true);
        pagingQueryOption.setPageToken(pageToken);

        RongCoreClient.getInstance()
                .getGroupMembersByRole(
                        groupId,
                        groupMemberRole,
                        pagingQueryOption,
                        new IRongCoreCallback.PageResultCallback<GroupMemberInfo>() {
                            @Override
                            public void onSuccess(PagingQueryResult<GroupMemberInfo> result) {
                                if (result != null) {
                                    if (result.getData() != null && !result.getData().isEmpty()) {
                                        groupMemberInfos.addAll(result.getData());
                                    }
                                    if (!isOnlyRole) {
                                        if (!TextUtils.isEmpty(result.getPageToken())) {
                                            getGroupMembersByRoleByPage(
                                                    groupMemberRole, result.getPageToken(), false);
                                        } else {
                                            fetchNextRole(groupMemberRole);
                                        }
                                    } else {
                                        nextPageToken = result.getPageToken();
                                        isLoading = false;
                                        notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                                    }
                                } else {
                                    isLoading = false;
                                    notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                                }
                                notifyDataChange(KEY_LOAD_MORE, hasNext());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_GET_GROUP_MEMBERS, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    private void fetchNextRole(GroupMemberRole lastRole) {
        if (lastRole == GroupMemberRole.Undef) {
            getGroupMembersByRoleByPage(GroupMemberRole.Owner, null, false);
        } else if (lastRole == GroupMemberRole.Owner) {
            getGroupMembersByRoleByPage(GroupMemberRole.Manager, null, false);
        } else if (lastRole == GroupMemberRole.Manager) {
            getGroupMembersByRoleByPage(GroupMemberRole.Normal, null, true);
        }
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        isLoadNext = true;
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        getGroupMembersByRoleByPage(currentGroupMemberRole, nextPageToken, isOnlyRole);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken);
    }

    @Override
    public void stop() {
        super.stop();
        IMCenter.getInstance().removeGroupEventListener(groupEventListener);
    }

    // todo
    private final GroupEventListener groupEventListener =
            new GroupEventListener() {
                @Override
                public void onGroupOperation(
                        String groupId,
                        GroupMemberInfo operatorInfo,
                        GroupInfo groupInfo,
                        GroupOperation operation,
                        List<GroupMemberInfo> memberInfos,
                        long operationTime) {
                    if (!isLoadNext
                            && Objects.equals(groupId, GroupMembersPagedHandler.this.groupId)) {
                        getGroupMembersByRole(groupMemberRole);
                    }
                }

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
                    if (!isLoadNext
                            && Objects.equals(groupId, GroupMembersPagedHandler.this.groupId)) {
                        getGroupMembersByRole(groupMemberRole);
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
