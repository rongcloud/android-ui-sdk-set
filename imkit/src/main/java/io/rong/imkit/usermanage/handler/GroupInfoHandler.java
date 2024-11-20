package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组详情
 *
 * <p>用于获取群组信息、群成员信息等
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
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

    private final String groupId;

    // 构造方法初始化会话标识符和群组ID
    public GroupInfoHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this.groupId = conversationIdentifier.getTargetId();
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
     * @param userIds 用户ID列表
     */
    public void getGroupMembers(List<String> userIds) {
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
     * @param name 用户名
     */
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
}