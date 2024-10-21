package io.rong.imkit.usermanage.group.profile;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.QuitGroupConfig;
import io.rong.imlib.model.UserInfo;
import java.util.Arrays;
import java.util.List;

/**
 * 功能描述: 创建群组ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupProfileViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupMemberInfo>> GroupMemberInfosLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> myMemberInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;
    protected final GroupMembersPagedHandler groupMembersPagedHandler;

    public GroupProfileViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        final int maxCount =
                Math.max(
                        5,
                        Math.min(
                                50,
                                arguments.getInt(KitConstants.KEY_MAX_MEMBER_COUNT_DISPLAY, 30)));

        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_INFO, groupInfoLiveData::postValue);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                            myMemberInfoLiveData.postValue(groupMemberInfos.get(0));
                        }
                    }
                });

        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier, maxCount);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                GroupMemberInfosLiveData::postValue);

        refreshGroupInfo();
    }

    public LiveData<GroupInfo> getGroupInfoLiveData() {
        return groupInfoLiveData;
    }

    public LiveData<List<GroupMemberInfo>> getGroupMemberInfosLiveData() {
        return GroupMemberInfosLiveData;
    }

    public MutableLiveData<GroupMemberInfo> getMyMemberInfoLiveData() {
        return myMemberInfoLiveData;
    }

    /**
     * 解散群组
     *
     * @param listener 数据变化监听器
     */
    public void dismissGroup(OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_DISMISS_GROUP, listener);
        groupOperationsHandler.dismissGroup();
    }

    /**
     * 退出群组
     *
     * @param listener 数据变化监听器
     */
    public void quitGroup(OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_QUIT_GROUP, listener);
        groupOperationsHandler.quitGroup(new QuitGroupConfig(true, true, true));
    }

    void refreshGroupInfo() {
        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.Undef);
        groupInfoHandler.getGroupsInfo();
        UserInfo userInfo = RongUserInfoManager.getInstance().getCurrentUserInfo();
        if (userInfo != null) {
            String userId = userInfo.getUserId();
            groupInfoHandler.getGroupMembers(Arrays.asList(userId));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
        groupMembersPagedHandler.stop();
    }
}
