package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组成员加载
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 */
public class GroupMembersFullHandler extends MultiDataHandler {

    private static final String TAG = GroupMembersFullHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES =
            DataKey.obtain(
                    "KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES",
                    (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private final String groupId;
    private final List<GroupMemberInfo> groupMemberInfos = new ArrayList<>();

    private volatile boolean isLoading = false;

    public GroupMembersFullHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.groupId = conversationIdentifier.getTargetId();
    }

    /**
     * 获取所有群成员
     *
     * @param groupMemberRole 群成员角色
     */
    public void getAllGroupMembersByRole(@NonNull GroupMemberRole groupMemberRole) {
        if (isLoading) {
            RLog.d(TAG, "getGroupMembersByRole is loaded");
            return;
        }
        groupMemberInfos.clear();
        isLoading = true;
        // 如果是 Undef 则依次拉取所有角色
        if (groupMemberRole == GroupMemberRole.Undef) {
            allFetchNextRole(GroupMemberRole.Undef, null);
        } else {
            // 否则仅拉取指定角色的成员
            getAllGroupMembersByRoleByPage(groupMemberRole, null, true);
        }
    }

    private void allFetchNextRole(GroupMemberRole lastRole, String pageToken) {
        if (lastRole == GroupMemberRole.Undef) {
            getAllGroupMembersByRoleByPage(GroupMemberRole.Owner, pageToken, false);
        } else if (lastRole == GroupMemberRole.Owner) {
            getAllGroupMembersByRoleByPage(GroupMemberRole.Manager, pageToken, false);
        } else if (lastRole == GroupMemberRole.Manager) {
            getAllGroupMembersByRoleByPage(GroupMemberRole.Normal, pageToken, false);
        }
    }

    private void getAllGroupMembersByRoleByPage(
            GroupMemberRole groupMemberRole, String pageToken, boolean isOnlyRole) {
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(100);
        pagingQueryOption.setPageToken(pageToken);
        if (groupMemberRole == GroupMemberRole.Normal || groupMemberRole == GroupMemberRole.Undef) {
            pagingQueryOption.setOrder(true);
        }
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
                                    // 如果还有更多数据，继续分页拉取
                                    if (result.getPageToken() != null
                                            && !result.getPageToken().isEmpty()) {
                                        getAllGroupMembersByRoleByPage(
                                                groupMemberRole, result.getPageToken(), isOnlyRole);
                                    } else if (!isOnlyRole
                                            && groupMemberRole != GroupMemberRole.Normal) {
                                        allFetchNextRole(groupMemberRole, result.getPageToken());
                                    } else {
                                        isLoading = false;
                                        notifyDataChange(
                                                KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES,
                                                groupMemberInfos);
                                    }
                                } else {
                                    isLoading = false;
                                    notifyDataChange(
                                            KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES, groupMemberInfos);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES, e);
                            }
                        });
    }
}
