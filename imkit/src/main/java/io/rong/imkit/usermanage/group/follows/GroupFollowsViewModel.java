package io.rong.imkit.usermanage.group.follows;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupMembersByUserIdsHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.FollowInfo;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 群组关注人列表页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupFollowsViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allGroupFollowsLiveData =
            new MutableLiveData<>();

    protected final GroupInfoHandler groupInfoHandler;
    protected final GroupMembersByUserIdsHandler groupMembersByUserIdsHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupFollowsViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupMembersByUserIdsHandler = new GroupMembersByUserIdsHandler(conversationIdentifier);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        groupMembersByUserIdsHandler.addDataChangeListener(
                GroupMembersByUserIdsHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        allGroupFollowsLiveData.postValue(
                                sortAndCategorizeContacts(groupMemberInfos));
                    }
                });
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_FOLLOWS,
                groupFollowsInfos -> {
                    if (groupFollowsInfos == null || groupFollowsInfos.isEmpty()) {
                        allGroupFollowsLiveData.postValue(Collections.emptyList());
                        return;
                    }
                    List<String> groupFollowsIds = new ArrayList<>();
                    for (FollowInfo followInfo : groupFollowsInfos) {
                        groupFollowsIds.add(followInfo.getUserId());
                    }
                    groupMembersByUserIdsHandler.getGroupMembers(groupFollowsIds);
                });
    }

    public LiveData<List<ContactModel>> getAllGroupFollowsLiveData() {
        return allGroupFollowsLiveData;
    }

    /**
     * 移除群组关注人
     *
     * @param userIds 用户ID列表
     * @param listener 数据变化监听
     */
    public void removeGroupFollows(List<String> userIds, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_REMOVE_GROUP_FOLLOWS, listener);
        groupOperationsHandler.removeGroupFollows(userIds);
    }

    @NonNull
    private List<ContactModel> sortAndCategorizeContacts(List<GroupMemberInfo> groupMemberInfos) {
        if (groupMemberInfos == null || groupMemberInfos.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo groupMemberInfo : groupMemberInfos) {
            // 添加联系人 ContactModel
            contactModels.add(
                    ContactModel.obtain(
                            groupMemberInfo,
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.NONE));
        }

        return contactModels;
    }

    void refreshGroupFollows() {
        groupInfoHandler.getGroupFollows();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupInfoHandler.stop();
        groupMembersByUserIdsHandler.stop();
        groupOperationsHandler.stop();
    }
}
