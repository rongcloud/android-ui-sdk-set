package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.listener.GroupEventListener;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.FollowInfo;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupInfoKeys;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupOperation;
import io.rong.imlib.model.GroupOperationType;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 用于获取群组信息、群成员信息等
 *
 * <p>用于获取群组信息、群成员信息等
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupInfoHandler extends MultiDataHandler {

    public static final DataKey<GroupInfo> KEY_GROUP_INFO =
            MultiDataHandler.DataKey.obtain("KEY_GROUP_INFO", GroupInfo.class);

    public static final DataKey<List<GroupMemberInfo>> KEY_GET_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GET_GROUP_MEMBERS", (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    public static final DataKey<List<GroupMemberInfo>> KEY_SEARCH_GROUP_MEMBERS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_SEARCH_GROUP_MEMBERS",
                    (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    public static final DataKey<List<FollowInfo>> KEY_GROUP_FOLLOWS =
            MultiDataHandler.DataKey.obtain(
                    "KEY_GROUP_FOLLOWS", (Class<List<FollowInfo>>) (Class<?>) List.class);

    private final String groupId;
    private List<String> userIds;

    // 构造方法初始化会话标识符和群组ID
    public GroupInfoHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.groupId = conversationIdentifier.getTargetId();
        IMCenter.getInstance().addGroupEventListener(groupEventListener);
    }

    /** 获取群组信息 */
    public void getGroupsInfo() {
        ArrayList<String> groupIds = new ArrayList<>();
        groupIds.add(groupId);
        RongCoreClient.getInstance()
                .getGroupsInfo(
                        groupIds,
                        new IRongCoreCallback.ResultCallback<List<GroupInfo>>() {
                            @Override
                            public void onSuccess(List<GroupInfo> groupInfos) {
                                if (groupInfos != null && !groupInfos.isEmpty()) {
                                    notifyDataChange(KEY_GROUP_INFO, groupInfos.get(0));
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GROUP_INFO, e);
                            }
                        });
    }

    /**
     * 获取群成员信息
     *
     * @param userIds 用户ID列表 如果userIds.size > 100; 请使用 {@link
     *     GroupMembersByUserIdsHandler#getGroupMembers(List)}
     */
    public void getGroupMembers(List<String> userIds) {
        this.userIds = userIds;
        RongCoreClient.getInstance()
                .getGroupMembers(
                        groupId,
                        userIds,
                        new IRongCoreCallback.ResultCallback<List<GroupMemberInfo>>() {
                            @Override
                            public void onSuccess(List<GroupMemberInfo> groupMemberInfos) {
                                notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GET_GROUP_MEMBERS, e);
                            }
                        });
    }

    /**
     * 搜索群成员
     *
     * <p>请使用 {@link GroupMembersSearchPagedHandler} 类进行搜索
     *
     * @param name 用户名
     */
    @Deprecated
    public void searchGroupMembers(String name) {
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(100);
        RongCoreClient.getInstance()
                .searchGroupMembers(
                        groupId,
                        name,
                        pagingQueryOption,
                        new IRongCoreCallback.PageResultCallback<GroupMemberInfo>() {
                            @Override
                            public void onSuccess(PagingQueryResult<GroupMemberInfo> result) {
                                if (result != null) {
                                    notifyDataChange(KEY_SEARCH_GROUP_MEMBERS, result.getData());
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_SEARCH_GROUP_MEMBERS, e);
                            }
                        });
    }

    /** 查询特别关注群成员 */
    public void getGroupFollows() {
        RongCoreClient.getInstance()
                .getGroupFollows(
                        groupId,
                        new IRongCoreCallback.ResultCallback<List<FollowInfo>>() {
                            @Override
                            public void onSuccess(List<FollowInfo> followInfos) {
                                notifyDataChange(
                                        KEY_GROUP_FOLLOWS,
                                        followInfos == null ? new ArrayList<>() : followInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GROUP_FOLLOWS, e);
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
                        long operationTime) {
                    if (Objects.equals(groupId, GroupInfoHandler.this.groupId)) {
                        getGroupsInfo();
                    }
                }

                @Override
                public void onGroupInfoChanged(
                        GroupMemberInfo operatorInfo,
                        GroupInfo groupInfo,
                        List<GroupInfoKeys> updateKeys,
                        long operationTime) {
                    if (groupInfo != null
                            && Objects.equals(
                                    groupInfo.getGroupId(), GroupInfoHandler.this.groupId)) {
                        getGroupsInfo();
                    }
                }

                @Override
                public void onGroupMemberInfoChanged(
                        String groupId,
                        GroupMemberInfo operatorInfo,
                        GroupMemberInfo memberInfo,
                        long operationTime) {
                    if (Objects.equals(groupId, GroupInfoHandler.this.groupId)
                            && GroupInfoHandler.this.userIds != null
                            && GroupInfoHandler.this.userIds.contains(memberInfo.getUserId())) {
                        getGroupMembers(GroupInfoHandler.this.userIds);
                    }
                }

                @Override
                public void onGroupApplicationEvent(GroupApplicationInfo info) {}

                @Override
                public void onGroupRemarkChangedSync(
                        String groupId,
                        GroupOperationType operationType,
                        String groupRemark,
                        long operationTime) {
                    if (Objects.equals(groupId, GroupInfoHandler.this.groupId)) {
                        getGroupsInfo();
                    }
                }

                @Override
                public void onGroupFollowsChangedSync(
                        String groupId,
                        GroupOperationType operationType,
                        List<String> userIds,
                        long operationTime) {
                    if (Objects.equals(groupId, GroupInfoHandler.this.groupId)) {
                        getGroupFollows();
                    }
                }
            };
}
