package io.rong.imkit.usermanage.handler;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
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
    private String nextPageToken = null;

    private volatile boolean isLoading = false;

    public GroupMembersPagedHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this(conversationIdentifier, 50);
    }

    public GroupMembersPagedHandler(
            @NonNull ConversationIdentifier conversationIdentifier, int pageCount) {
        this.pageCount = pageCount;
        this.groupId = conversationIdentifier.getTargetId();
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
        groupMemberInfos.clear();
        isLoading = true;
        nextPageToken = null;
        if (groupMemberRole == GroupMemberRole.Undef) {
            // 如果传入的是Undef，则依次拉取Owner, Manager, Normal
            fetchNextRole(GroupMemberRole.Undef, null);
        } else {
            // 否则拉取指定角色的群成员
            getGroupMembersByRoleByPage(groupMemberRole, null);
        }
    }

    private void getGroupMembersByRoleByPage(GroupMemberRole groupMemberRole, String pageToken) {
        currentGroupMemberRole = groupMemberRole;
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(pageCount);
        if (groupMemberRole == GroupMemberRole.Normal || groupMemberRole == GroupMemberRole.Undef) {
            pagingQueryOption.setOrder(true);
        }
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
                                    if (groupMemberRole == GroupMemberRole.Normal) {
                                        nextPageToken = result.getPageToken();
                                    }
                                    if (result.getData() != null && !result.getData().isEmpty()) {
                                        groupMemberInfos.addAll(result.getData());
                                    }
                                    if (groupMemberRole != GroupMemberRole.Normal) {
                                        fetchNextRole(groupMemberRole, nextPageToken);
                                    } else {
                                        isLoading = false;
                                        notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                                    }
                                } else {
                                    isLoading = false;
                                    notifyDataChange(KEY_GET_GROUP_MEMBERS, groupMemberInfos);
                                }
                                notifyDataChange(KEY_LOAD_MORE, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_GET_GROUP_MEMBERS, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    private void fetchNextRole(GroupMemberRole lastRole, String pageToken) {
        if (lastRole == GroupMemberRole.Undef) {
            getGroupMembersByRoleByPage(GroupMemberRole.Owner, pageToken);
        } else if (lastRole == GroupMemberRole.Owner) {
            getGroupMembersByRoleByPage(GroupMemberRole.Manager, pageToken);
        } else if (lastRole == GroupMemberRole.Manager) {
            getGroupMembersByRoleByPage(GroupMemberRole.Normal, pageToken);
        }
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        getGroupMembersByRoleByPage(currentGroupMemberRole, nextPageToken);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken);
    }
}
