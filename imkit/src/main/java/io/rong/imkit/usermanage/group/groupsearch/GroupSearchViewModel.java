package io.rong.imkit.usermanage.group.groupsearch;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupJoinedSearchPagedHandler;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.GroupInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 群组搜索页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupSearchViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupInfo>> filteredGroupInfoListLiveData =
            new MutableLiveData<>();
    protected final GroupJoinedSearchPagedHandler groupJoinedSearchPagedHandler;

    public GroupSearchViewModel(@NonNull Bundle arguments) {
        super(arguments);
        int maxCount = arguments.getInt(KitConstants.KEY_MAX_COUNT_PAGED, 50);
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));

        groupJoinedSearchPagedHandler =
                new GroupJoinedSearchPagedHandler(validatedMaxMemberCountPaged);
        groupJoinedSearchPagedHandler.addDataChangeListener(
                GroupJoinedSearchPagedHandler.KEY_SEARCH_JOINED_GROUPS,
                new SafeDataHandler<List<GroupInfo>>() {
                    @Override
                    public void onDataChange(List<GroupInfo> groupInfos) {
                        filteredGroupInfoListLiveData.postValue(groupInfos);
                    }
                });
    }

    public LiveData<List<GroupInfo>> getFilteredGroupInfoListLiveData() {
        return filteredGroupInfoListLiveData;
    }

    /**
     * 搜索加入的群组
     *
     * @param query 查询关键字
     */
    public void searchJoinedGroups(@NonNull String query) {
        if (TextUtils.isEmpty(query)) {
            filteredGroupInfoListLiveData.postValue(new ArrayList<>());
            return;
        }
        groupJoinedSearchPagedHandler.searchJoinedGroups(query);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupJoinedSearchPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupJoinedSearchPagedHandler.stop();
    }
}
