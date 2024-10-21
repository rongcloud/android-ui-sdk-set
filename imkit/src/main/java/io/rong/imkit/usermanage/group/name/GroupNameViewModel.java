package io.rong.imkit.usermanage.group.name;

import android.os.Bundle;
import androidx.annotation.NonNull;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;

/**
 * 功能描述: 创建增加群联系人 ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupNameViewModel extends BaseViewModel {

    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupNameViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
    }

    /**
     * 更新群组信息
     *
     * @param groupInfo 群组信息
     * @param onDataChangeListener 数据变化监听
     */
    public void updateGroupInfo(
            GroupInfo groupInfo, OnDataChangeListener<Boolean> onDataChangeListener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_UPDATE_GROUP_INFO, onDataChangeListener);
        groupOperationsHandler.updateGroupInfo(groupInfo);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupOperationsHandler.stop();
    }
}
