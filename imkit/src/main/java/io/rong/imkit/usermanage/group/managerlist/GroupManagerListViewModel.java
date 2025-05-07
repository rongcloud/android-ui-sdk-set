package io.rong.imkit.usermanage.group.managerlist;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.GroupMembersFullHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 群管理员页面 ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupManagerListViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allGroupManagersLiveData =
            new MutableLiveData<>();

    protected final GroupMembersFullHandler groupMembersFullHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    public GroupManagerListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
        groupMembersFullHandler = new GroupMembersFullHandler(conversationIdentifier);
        groupMembersFullHandler.addDataChangeListener(
                GroupMembersFullHandler.KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        allGroupManagersLiveData.postValue(
                                sortAndCategorizeContacts(groupMemberInfos));
                    }
                });
    }

    public MutableLiveData<List<ContactModel>> getAllGroupManagersLiveData() {
        return allGroupManagersLiveData;
    }

    public void removeGroupManager(List<String> userIds, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_REMOVE_GROUP_MANAGERS, listener);
        groupOperationsHandler.removeGroupManagers(userIds);
    }

    void refreshGroupManagerList() {
        groupMembersFullHandler.getAllGroupMembersByRole(GroupMemberRole.Manager);
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

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersFullHandler.stop();
        groupOperationsHandler.stop();
    }
}
