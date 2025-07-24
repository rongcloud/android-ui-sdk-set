package io.rong.imkit.usermanage.group.grouplist;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupJoinedPagedHandler;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberRole;
import java.util.List;

/**
 * 群组列表页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupListViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupInfo>> allGroupInfoListLiveData =
            new MutableLiveData<>();

    protected final GroupJoinedPagedHandler groupJoinedPagedHandler;

    public GroupListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        int maxCount = arguments.getInt(KitConstants.KEY_MAX_COUNT_PAGED, 50);
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));
        groupJoinedPagedHandler = new GroupJoinedPagedHandler(validatedMaxMemberCountPaged);
        groupJoinedPagedHandler.addDataChangeListener(
                GroupJoinedPagedHandler.KEY_GET_JOINED_GROUPS_BY_ROLE,
                new SafeDataHandler<List<GroupInfo>>() {
                    @Override
                    public void onDataChange(List<GroupInfo> groupInfos) {
                        allGroupInfoListLiveData.postValue(groupInfos);
                    }
                });

        refreshJoinedGroupList();
    }

    public LiveData<List<GroupInfo>> getAllGroupInfoListLiveData() {
        return allGroupInfoListLiveData;
    }

    void refreshJoinedGroupList() {
        groupJoinedPagedHandler.getJoinedGroupsByRole(GroupMemberRole.Undef);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupJoinedPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupJoinedPagedHandler.stop();
    }
}
