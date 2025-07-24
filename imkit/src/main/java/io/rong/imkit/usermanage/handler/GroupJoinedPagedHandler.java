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
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页获取加入群组
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupJoinedPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupJoinedPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupInfo>> KEY_GET_JOINED_GROUPS_BY_ROLE =
            DataKey.obtain(
                    "KEY_GET_JOINED_GROUPS_BY_ROLE",
                    (Class<List<GroupInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupInfo> groupInfos = new ArrayList<>();

    private GroupMemberRole currentGroupMemberRole;
    private String nextPageToken = null;

    private volatile boolean isLoading = false;

    public GroupJoinedPagedHandler() {
        this(50);
    }

    public GroupJoinedPagedHandler(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * 获取加入的群组
     *
     * @param groupMemberRole 群成员角色
     */
    public void getJoinedGroupsByRole(@NonNull GroupMemberRole groupMemberRole) {
        if (isLoading) {
            RLog.d(TAG, "getJoinedGroupsByRole is loaded");
            return;
        }
        groupInfos.clear();
        isLoading = true;
        nextPageToken = null;
        getJoinedGroupsByRoleByPage(groupMemberRole, null);
    }

    private void getJoinedGroupsByRoleByPage(GroupMemberRole groupMemberRole, String pageToken) {
        currentGroupMemberRole = groupMemberRole;
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(pageCount);
        pagingQueryOption.setPageToken(pageToken);

        RongCoreClient.getInstance()
                .getJoinedGroupsByRole(
                        groupMemberRole,
                        pagingQueryOption,
                        new IRongCoreCallback.PageResultCallback<GroupInfo>() {
                            @Override
                            public void onSuccess(PagingQueryResult<GroupInfo> result) {
                                if (result != null) {
                                    nextPageToken = result.getPageToken();
                                    if (result.getData() != null && !result.getData().isEmpty()) {
                                        groupInfos.addAll(result.getData());
                                    }
                                }
                                isLoading = false;
                                notifyDataChange(KEY_GET_JOINED_GROUPS_BY_ROLE, groupInfos);
                                notifyDataChange(KEY_LOAD_MORE, hasNext());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_GET_JOINED_GROUPS_BY_ROLE, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        getJoinedGroupsByRoleByPage(currentGroupMemberRole, nextPageToken);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken);
    }
}
