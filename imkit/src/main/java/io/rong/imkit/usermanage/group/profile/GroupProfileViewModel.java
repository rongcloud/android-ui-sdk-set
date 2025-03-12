package io.rong.imkit.usermanage.group.profile;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.usermanage.handler.ConversationOperationsHandler;
import io.rong.imkit.usermanage.handler.ConversationStatusHandler;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.QuitGroupConfig;
import io.rong.imlib.model.UserInfo;
import java.util.Arrays;
import java.util.List;

/**
 * 群组资料页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupProfileViewModel extends BaseViewModel {

    private final MutableLiveData<List<GroupMemberInfo>> GroupMemberInfosLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> myMemberInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isConversationTopLiveData = new MutableLiveData<>();
    private final MutableLiveData<Conversation.ConversationNotificationStatus>
            conversationNotificationStatusLiveData = new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupOperationsHandler groupOperationsHandler;
    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    /** @since 5.12.2 */
    protected final ConversationStatusHandler conversationStatusHandler;
    /** @since 5.12.2 */
    protected final ConversationOperationsHandler conversationOperationsHandler;

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

        conversationStatusHandler = new ConversationStatusHandler(conversationIdentifier);
        conversationStatusHandler.addDataChangeListener(
                ConversationStatusHandler.KEY_GET_CONVERSATION_TOP_STATUS,
                new SafeDataHandler<Boolean>() {
                    @Override
                    public void onDataChange(Boolean isTop) {
                        isConversationTopLiveData.postValue(isTop);
                    }
                });
        conversationStatusHandler.addDataChangeListener(
                ConversationStatusHandler.KEY_GET_CONVERSATION_NOTIFICATION_STATUS,
                new SafeDataHandler<Conversation.ConversationNotificationStatus>() {
                    @Override
                    public void onDataChange(
                            Conversation.ConversationNotificationStatus
                                    conversationNotificationStatus) {
                        conversationNotificationStatusLiveData.postValue(
                                conversationNotificationStatus);
                    }
                });

        conversationOperationsHandler = new ConversationOperationsHandler(conversationIdentifier);

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

    /** @since 5.12.2 */
    public MutableLiveData<Boolean> getIsConversationTopLiveData() {
        return isConversationTopLiveData;
    }

    /** @since 5.12.2 */
    public MutableLiveData<Conversation.ConversationNotificationStatus>
            getConversationNotificationStatusLiveData() {
        return conversationNotificationStatusLiveData;
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

    /**
     * 设置会话免打扰状态
     *
     * @param conversationNotificationStatus 会话免打扰状态
     * @param listener 数据变化监听器
     * @since 5.12.2
     */
    public void setConversationNotificationStatus(
            Conversation.ConversationNotificationStatus conversationNotificationStatus,
            OnDataChangeListener<Conversation.ConversationNotificationStatus> listener) {
        conversationOperationsHandler.replaceDataChangeListener(
                ConversationOperationsHandler.KEY_SET_CONVERSATION_NOTIFICATION_STATUS, listener);
        conversationOperationsHandler.setConversationNotificationStatus(
                conversationNotificationStatus);
    }

    /**
     * 设置会话置顶状态
     *
     * @param isTop 会话置顶状态
     * @param listener 数据变化监听器
     * @since 5.12.2
     */
    public void setConversationTopStatus(boolean isTop, OnDataChangeListener<Boolean> listener) {
        conversationOperationsHandler.replaceDataChangeListener(
                ConversationOperationsHandler.KEY_SET_CONVERSATION_TO_TOP, listener);
        conversationOperationsHandler.setConversationToTop(isTop);
    }

    void refreshGroupInfo() {
        conversationStatusHandler.getConversationTopStatus();
        conversationStatusHandler.getConversationNotificationStatus();
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
        conversationStatusHandler.stop();
        conversationOperationsHandler.stop();
    }
}
