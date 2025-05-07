package io.rong.imkit.usermanage.group.manage;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;

/**
 * 群管理页面 ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupManagementViewModel extends BaseViewModel {

    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupManagementViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_INFO, groupInfoLiveData::postValue);

        refreshGroupInfo();
    }

    public LiveData<GroupInfo> getGroupInfoLiveData() {
        return groupInfoLiveData;
    }

    /**
     * 更新群组信息
     *
     * @param groupInfo 群组信息
     * @param listener 数据变化监听器
     */
    @Deprecated
    public void updateGroupInfo(GroupInfo groupInfo, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO, listener);
        groupOperationsHandler.updateGroupInfo(groupInfo);
    }

    /**
     * 更新群组信息
     *
     * @param groupInfo 群组信息
     * @param listener 数据变化监听器
     */
    public void updateGroupInfo(
            GroupInfo groupInfo, OnDataChangeEnhancedListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO_EXAMINE, listener);
        groupOperationsHandler.updateGroupInfoExamine(groupInfo);
    }

    void refreshGroupInfo() {
        groupInfoHandler.getGroupsInfo();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
    }
}
