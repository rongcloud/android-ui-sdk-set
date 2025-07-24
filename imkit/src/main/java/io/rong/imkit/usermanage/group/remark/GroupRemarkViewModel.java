package io.rong.imkit.usermanage.group.remark;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;

/**
 * 修改群备注 ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupRemarkViewModel extends BaseViewModel {

    private final MutableLiveData<GroupInfo> groupInfoMutableLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupRemarkViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_INFO,
                new SafeDataHandler<GroupInfo>() {
                    @Override
                    public void onDataChange(GroupInfo groupInfo) {
                        groupInfoMutableLiveData.postValue(groupInfo);
                    }
                });
        groupInfoHandler.getGroupsInfo();
    }

    public MutableLiveData<GroupInfo> getGroupInfoLiveData() {
        return groupInfoMutableLiveData;
    }

    /**
     * 更新群昵称
     *
     * @param newNickName 新的群昵称
     * @param onDataChangeListener 数据变化监听
     */
    public void setGroupRemark(
            String newNickName, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_SET_GROUP_REMARK, onDataChangeListener);
        groupOperationsHandler.setGroupRemark(newNickName);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupOperationsHandler.stop();
    }
}
