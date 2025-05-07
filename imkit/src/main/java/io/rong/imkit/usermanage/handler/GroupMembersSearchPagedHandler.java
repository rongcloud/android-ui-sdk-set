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
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页搜索加入群组
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupMembersSearchPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupMembersSearchPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupMemberInfo>> KEY_SEARCH_GROUP_MEMBERS =
            DataKey.obtain(
                    "KEY_SEARCH_GROUP_MEMBERS",
                    (Class<List<GroupMemberInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupMemberInfo> groupMemberInfos = new ArrayList<>();

    private final String groupId;
    private String currentSearchName;
    private String nextPageToken = null;

    private volatile boolean isLoading = false;

    public GroupMembersSearchPagedHandler(@NonNull ConversationIdentifier conversationIdentifier) {
        this(conversationIdentifier, 50);
    }

    public GroupMembersSearchPagedHandler(
            @NonNull ConversationIdentifier conversationIdentifier, int pageCount) {
        this.pageCount = pageCount;
        this.groupId = conversationIdentifier.getTargetId();
    }

    public void searchGroupMembers(@NonNull String name) {
        if (isLoading) {
            RLog.d(TAG, "searchGroupMembers is loaded");
            return;
        }
        groupMemberInfos.clear();
        isLoading = true;
        nextPageToken = null;
        searchGroupMembersByPage(name, null);
    }

    private void searchGroupMembersByPage(String name, String pageToken) {
        currentSearchName = name;
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(pageCount);
        pagingQueryOption.setOrder(true);
        pagingQueryOption.setPageToken(pageToken);

        RongCoreClient.getInstance()
                .searchGroupMembers(
                        groupId,
                        name,
                        pagingQueryOption,
                        new IRongCoreCallback.PageResultCallback<GroupMemberInfo>() {
                            @Override
                            public void onSuccess(PagingQueryResult<GroupMemberInfo> result) {
                                if (result != null) {
                                    nextPageToken = result.getPageToken();
                                    if (result.getData() != null && !result.getData().isEmpty()) {
                                        groupMemberInfos.addAll(result.getData());
                                    }
                                }
                                isLoading = false;
                                notifyDataChange(KEY_SEARCH_GROUP_MEMBERS, groupMemberInfos);
                                notifyDataChange(KEY_LOAD_MORE, hasNext());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_SEARCH_GROUP_MEMBERS, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        searchGroupMembersByPage(currentSearchName, nextPageToken);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken);
    }
}
