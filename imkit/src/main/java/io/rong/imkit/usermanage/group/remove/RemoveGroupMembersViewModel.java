package io.rong.imkit.usermanage.group.remove;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.handler.GroupMembersSearchPagedHandler;
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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 移除群成员页面 ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class RemoveGroupMembersViewModel extends BaseViewModel implements OnPagedDataLoader {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupMembersSearchPagedHandler groupMembersSearchPagedHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private boolean isSearchMode = false;

    public RemoveGroupMembersViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        // 初始化 GroupDetailHandler 以获取群成员
        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        List<ContactModel> contactModels =
                                getAllContactsContactModels(groupMemberInfos);
                        allContactsLiveData.postValue(contactModels);
                        filteredContactsLiveData.postValue(contactModels);
                    }
                });

        groupMembersSearchPagedHandler = new GroupMembersSearchPagedHandler(conversationIdentifier);
        groupMembersSearchPagedHandler.addDataChangeListener(
                GroupMembersSearchPagedHandler.KEY_SEARCH_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        filteredContactsLiveData.postValue(
                                getAllContactsContactModels(groupMemberInfos));
                    }
                });

        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.Undef);

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
        List<ContactModel> selectedList = new ArrayList<>(selectedContactsLiveData.getValue());

        // 使用迭代器检查并移除或添加元素
        Iterator<ContactModel> iterator = selectedList.iterator();
        boolean found = false;

        while (iterator.hasNext()) {
            ContactModel contact = iterator.next();
            if (contact.getBean() instanceof GroupMemberInfo
                    && updatedContact.getBean() instanceof GroupMemberInfo) {
                GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contact.getBean();
                GroupMemberInfo updatedGroupMemberInfo = (GroupMemberInfo) updatedContact.getBean();
                if (groupMemberInfo.getUserId().equals(updatedGroupMemberInfo.getUserId())) {
                    found = true;
                    if (updatedContact.getCheckType() == ContactModel.CheckType.UNCHECKED) {
                        iterator.remove(); // 未选中时移除
                    }
                    break;
                }
            }
        }

        if (!found && updatedContact.getCheckType() == ContactModel.CheckType.CHECKED) {
            // 未找到且选中时，添加
            selectedList.add(updatedContact);
        }

        // 更新 LiveData
        selectedContactsLiveData.postValue(selectedList);
    }

    /**
     * 查询群成员
     *
     * @param query 查询关键字
     */
    public void queryGroupMembers(String query) {
        if (TextUtils.isEmpty(query)) {
            isSearchMode = false;
            groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.Undef);
            return;
        }
        isSearchMode = true;
        groupMembersSearchPagedHandler.searchGroupMembers(query);
    }

    /**
     * 踢出群成员
     *
     * @param onDataChangeListener 数据变化监听器
     */
    public void kickGroupMembers(@NonNull OnDataChangeListener<Boolean> onDataChangeListener) {
        List<String> userIds = getSelectUserIds();
        if (userIds.isEmpty()) {
            return;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupOperationsHandler.stop();
        groupMembersSearchPagedHandler.stop();
    }

    @Override
    public void loadNext(OnDataChangeListener<Boolean> listener) {
        if (isSearchMode) {
            groupMembersSearchPagedHandler.loadNext(listener);
        } else {
            groupMembersPagedHandler.loadNext(listener);
        }
    }

    @Override
    public boolean hasNext() {
        if (isSearchMode) {
            return groupMembersSearchPagedHandler.hasNext();
        } else {
            return groupMembersPagedHandler.hasNext();
        }
    }

    @NonNull
    private List<ContactModel> getAllContactsContactModels(List<GroupMemberInfo> groupMemberInfos) {
        List<ContactModel> contactModels = new ArrayList<>();
        List<String> selectUserIds = getSelectUserIds();
        for (GroupMemberInfo memberInfo : groupMemberInfos) {
            if (RongUserInfoManager.getInstance().getCurrentUserInfo() != null
                    && Objects.equals(
                            RongUserInfoManager.getInstance().getCurrentUserInfo().getUserId(),
                            memberInfo.getUserId())) {
                continue;
            }
            ContactModel.CheckType checkType = ContactModel.CheckType.UNCHECKED;
            if (selectUserIds.contains(memberInfo.getUserId())) {
                checkType = ContactModel.CheckType.CHECKED;
            }
            contactModels.add(
                    ContactModel.obtain(memberInfo, ContactModel.ItemType.CONTENT, checkType));
        }
        return contactModels;
    }

    @NonNull
    private List<String> getSelectUserIds() {
        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (selectedList == null || selectedList.isEmpty()) {
            return new ArrayList<>();
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
        return userIds;
    }
}
