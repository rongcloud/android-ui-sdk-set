package io.rong.imkit.usermanage.group.nickname;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.UserInfo;
import java.util.Arrays;
import java.util.List;

/**
 * 功能描述: 创建增加群联系人 ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupNicknameViewModel extends BaseViewModel {

    private final MutableLiveData<GroupMemberInfo> myMemberInfoLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupNicknameViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        String userId = RongUserInfoManager.getInstance().getCurrentUserInfo().getUserId();
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
        groupInfoHandler.getGroupMembers(Arrays.asList(userId));
    }

    public MutableLiveData<GroupMemberInfo> getMyMemberInfoLiveData() {
        return myMemberInfoLiveData;
    }

    /**
     * 更新群昵称
     *
     * @param newNickName 新的群昵称
     * @param onDataChangeListener 数据变化监听
     */
    public void updateGroupNickName(
            String newNickName, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_SET_GROUP_MEMBER_INFO, onDataChangeListener);
        UserInfo userInfo = RongUserInfoManager.getInstance().getCurrentUserInfo();
        if (userInfo != null) {
            String userId = userInfo.getUserId();
            groupOperationsHandler.setGroupMemberInfo(userId, newNickName, null);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
    }
}
