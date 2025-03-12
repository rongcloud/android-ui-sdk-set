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
public class GroupJoinedSearchPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupJoinedSearchPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupInfo>> KEY_SEARCH_JOINED_GROUPS =
            DataKey.obtain(
                    "KEY_SEARCH_JOINED_GROUPS", (Class<List<GroupInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupInfo> groupInfos = new ArrayList<>();

    private String currentGroupName;
    private String nextPageToken = null;

    private volatile boolean isLoading = false;

    public GroupJoinedSearchPagedHandler() {
        this(50);
    }

    public GroupJoinedSearchPagedHandler(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * 搜索加入的群组
     *
     * @param groupName 群组名称
     */
    public void searchJoinedGroups(@NonNull String groupName) {
        if (isLoading) {
            RLog.d(TAG, "getJoinedGroupsByRole is loaded");
            return;
        }
        groupInfos.clear();
        isLoading = true;
        nextPageToken = null;
        searchJoinedGroupsByPage(groupName, null);
    }

    private void searchJoinedGroupsByPage(String groupName, String pageToken) {
        currentGroupName = groupName;
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(pageCount);
        pagingQueryOption.setPageToken(pageToken);

        RongCoreClient.getInstance()
                .searchJoinedGroups(
                        groupName,
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
                                notifyDataChange(KEY_SEARCH_JOINED_GROUPS, groupInfos);
                                notifyDataChange(KEY_LOAD_MORE, hasNext());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_SEARCH_JOINED_GROUPS, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        searchJoinedGroupsByPage(currentGroupName, nextPageToken);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken);
    }
}
