package io.rong.imkit.usermanage.group.notice;

import android.os.Bundle;
import androidx.annotation.NonNull;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;

/**
 * 群公告页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupNoticeViewModel extends BaseViewModel {

    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupNoticeViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
    }

    /**
     * 更新群公告
     *
     * @param groupInfo 新的群公告
     * @param onDataChangeListener 数据变化监听
     */
    public void updateGroupNotice(
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
