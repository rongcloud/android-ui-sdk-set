package io.rong.imkit.usermanage.handler;

import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.GroupApplicationDirection;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationStatus;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页获取群组申请
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationsPagedHandler extends MultiDataHandler implements OnPagedDataLoader {

    private static final String TAG = GroupApplicationsPagedHandler.class.getSimpleName();

    public static final DataKey<List<GroupApplicationInfo>> KEY_GET_GROUP_APPLICATIONS =
            DataKey.obtain(
                    "KEY_GET_GROUP_APPLICATIONS",
                    (Class<List<GroupApplicationInfo>>) (Class<?>) List.class);

    private static final DataKey<Boolean> KEY_LOAD_MORE =
            DataKey.obtain("KEY_LOAD_MORE", Boolean.class);

    private final int pageCount;

    private final List<GroupApplicationInfo> groupApplicationInfos = new ArrayList<>();

    private GroupApplicationDirection[] currentDirections;
    private GroupApplicationStatus[] currentStatus;

    private String nextPageToken = null;

    private volatile boolean isLoading = false;

    public GroupApplicationsPagedHandler() {
        this(50);
    }

    public GroupApplicationsPagedHandler(int pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * 获取群组申请
     *
     * @param directions 申请方向
     * @param status 申请状态
     */
    public void getGroupApplications(
            final GroupApplicationDirection[] directions, final GroupApplicationStatus[] status) {
        if (isLoading) {
            RLog.d(TAG, "getGroupApplications is loaded");
            return;
        }
        groupApplicationInfos.clear();
        isLoading = true;
        nextPageToken = null;
        this.currentDirections = directions;
        this.currentStatus = status;
        getGroupApplicationsByPage(directions, status, null);
    }

    private void getGroupApplicationsByPage(
            final GroupApplicationDirection[] directions,
            final GroupApplicationStatus[] status,
            String pageToken) {
        PagingQueryOption pagingQueryOption = new PagingQueryOption();
        pagingQueryOption.setCount(pageCount);
        pagingQueryOption.setPageToken(pageToken);

        RongCoreClient.getInstance()
                .getGroupApplications(
                        pagingQueryOption,
                        directions,
                        status,
                        new IRongCoreCallback.PageResultCallback<GroupApplicationInfo>() {
                            @Override
                            public void onSuccess(PagingQueryResult<GroupApplicationInfo> result) {
                                if (result != null) {
                                    nextPageToken = result.getPageToken();
                                    if (result.getData() != null && !result.getData().isEmpty()) {
                                        groupApplicationInfos.addAll(result.getData());
                                    }
                                }
                                isLoading = false;
                                notifyDataChange(KEY_GET_GROUP_APPLICATIONS, groupApplicationInfos);
                                notifyDataChange(KEY_LOAD_MORE, hasNext());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                isLoading = false;
                                notifyDataError(KEY_GET_GROUP_APPLICATIONS, e);
                                notifyDataChange(KEY_LOAD_MORE, false);
                            }
                        });
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_LOAD_MORE, listener);
        getGroupApplicationsByPage(currentDirections, currentStatus, nextPageToken);
    }

    @Override
    public boolean hasNext() {
        return !TextUtils.isEmpty(nextPageToken);
    }
}
