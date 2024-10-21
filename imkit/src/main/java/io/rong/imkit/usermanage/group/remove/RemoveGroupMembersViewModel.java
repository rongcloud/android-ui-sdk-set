package io.rong.imkit.usermanage.group.remove;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.QuitGroupConfig;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 功能描述: 创建移除群成员 ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class RemoveGroupMembersViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> friendInfoListLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final Set<String> existingGroupMemberIds = new HashSet<>();

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private String currentQuery;

    public RemoveGroupMembersViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        GroupMemberRole groupMemberRole =
                (GroupMemberRole) arguments.getSerializable(KitConstants.KEY_GROUP_MEMBER_ROLE);
        // 初始化 GroupDetailHandler 以获取群成员
        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        if (groupMemberInfos != null) {
                            List<ContactModel> contactModels = new ArrayList<>();
                            for (GroupMemberInfo memberInfo : groupMemberInfos) {
                                existingGroupMemberIds.add(memberInfo.getUserId());
                                ContactModel.CheckType checkType =
                                        memberInfo.getRole() == groupMemberRole
                                                ? ContactModel.CheckType.DISABLE
                                                : ContactModel.CheckType.UNCHECKED;
                                contactModels.add(
                                        ContactModel.obtain(
                                                memberInfo,
                                                ContactModel.ItemType.CONTENT,
                                                checkType));
                            }
                            friendInfoListLiveData.postValue(contactModels);
                            filteredContactsLiveData.postValue(contactModels);
                            queryContacts(currentQuery);
                        }
                    }
                });
        GroupMemberRole getGroupMemberRole = GroupMemberRole.Undef;
        if (groupMemberRole == GroupMemberRole.Owner) {
            getGroupMemberRole = GroupMemberRole.Undef;
        } else if (groupMemberRole == GroupMemberRole.Manager) {
            getGroupMemberRole = GroupMemberRole.Normal;
        }
        groupMembersPagedHandler.getGroupMembersByRole(getGroupMemberRole);

        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<List<ContactModel>> getSelectedContactsLiveData() {
        return selectedContactsLiveData;
    }

    /**
     * 更新联系人状态
     *
     * @param updatedContact 更新后的联系人
     */
    public void updateContact(ContactModel updatedContact) {
        List<ContactModel> allContacts = friendInfoListLiveData.getValue();
        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (allContacts == null || selectedList == null) {
            return;
        }

        for (int i = 0; i < allContacts.size(); i++) {
            ContactModel contact = allContacts.get(i);
            if (contact.getBean() instanceof GroupMemberInfo
                    && updatedContact.getBean() instanceof GroupMemberInfo) {
                GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contact.getBean();
                GroupMemberInfo updatedGroupMemberInfo = (GroupMemberInfo) updatedContact.getBean();
                if (groupMemberInfo != null
                        && groupMemberInfo.getUserId().equals(updatedGroupMemberInfo.getUserId())) {
                    contact.setCheckType(updatedContact.getCheckType());

                    if (updatedContact.getCheckType() == ContactModel.CheckType.CHECKED) {
                        selectedList.add(updatedContact);
                    } else {
                        selectedList.remove(updatedContact);
                    }

                    selectedContactsLiveData.postValue(selectedList);
                    break;
                }
            }
        }

        friendInfoListLiveData.postValue(allContacts);
    }

    /**
     * 查询联系人
     *
     * @param query 查询关键字
     */
    public void queryContacts(String query) {
        if (friendInfoListLiveData.getValue() == null || query == null) {
            return;
        }
        this.currentQuery = query;
        query = query.toLowerCase();

        List<ContactModel> allContacts = friendInfoListLiveData.getValue();
        List<ContactModel> filteredList = new ArrayList<>();

        for (ContactModel contact : allContacts) {
            if (contact.getBean() instanceof GroupMemberInfo) {
                GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contact.getBean();
                if (groupMemberInfo != null
                        && (groupMemberInfo.getName().toLowerCase().contains(query))) {
                    filteredList.add(contact);
                }
            } else {
                filteredList.add(contact);
            }
        }

        filteredContactsLiveData.postValue(filteredList);
    }

    /**
     * 踢出群成员
     *
     * @param onDataChangeListener 数据变化监听器
     */
    public void kickGroupMembers(@NonNull OnDataChangeListener<Boolean> onDataChangeListener) {

        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (selectedList == null || selectedList.isEmpty()) {
            return;
        }
        List<String> userIds = new ArrayList<>();
        for (ContactModel contact : selectedList) {
            if (contact.getBean() instanceof GroupMemberInfo) {
                GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contact.getBean();
                if (groupMemberInfo != null) {
                    userIds.add(groupMemberInfo.getUserId());
                }
            }
        }

        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_KICK_GROUP_MEMBERS,
                new OnDataChangeListener<Boolean>() {
                    @Override
                    public void onDataChange(Boolean isSuccess) {
                        onDataChangeListener.onDataChange(isSuccess);
                    }

                    @Override
                    public void onDataError(
                            IRongCoreEnum.CoreErrorCode errorCode, String errorMsg) {
                        onDataChangeListener.onDataError(errorCode, errorMsg);
                    }
                });
        groupOperationsHandler.kickGroupMembers(userIds, new QuitGroupConfig(true, true, true));
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupMembersPagedHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupOperationsHandler.stop();
    }
}
