package io.rong.imkit.usermanage.group.add;

import static io.rong.imkit.usermanage.handler.FriendInfoHandler.KEY_GET_FRIENDS;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.FriendInfoHandler;
import io.rong.imkit.usermanage.handler.GroupMembersFullHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.QueryFriendsDirectionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 功能描述: 创建增加群联系人 ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class AddGroupMembersViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ContactModel>> selectedContactsLiveData =
            new MutableLiveData<>(new ArrayList<>());
    private final Set<String> existingGroupMemberIds = new HashSet<>();

    protected final FriendInfoHandler friendInfoHandler;
    protected final GroupMembersFullHandler groupMembersFullHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private boolean isJoining = false;
    private final String groupId;

    public AddGroupMembersViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupId = conversationIdentifier.getTargetId();

        friendInfoHandler = new FriendInfoHandler();
        groupOperationsHandler = new GroupOperationsHandler(conversationIdentifier);

        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_SEARCH_FRIENDS,
                friendInfos ->
                        filteredContactsLiveData.postValue(sortAndCategorizeContacts(friendInfos)));

        // Initialize GroupDetailHandler to fetch group members
        groupMembersFullHandler = new GroupMembersFullHandler(conversationIdentifier);
        groupMembersFullHandler.addDataChangeListener(
                GroupMembersFullHandler.KEY_GET_ALL_GROUP_MEMBERS_BY_ROLES,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        if (groupMemberInfos != null) {
                            for (GroupMemberInfo memberInfo : groupMemberInfos) {
                                existingGroupMemberIds.add(memberInfo.getUserId());
                            }
                        }
                        // After fetching group members, fetch friends and update their status
                        fetchAndFilterFriendInfo();
                    }
                });
        groupMembersFullHandler.getAllGroupMembersByRole(GroupMemberRole.Undef);
    }

    private void fetchAndFilterFriendInfo() {
        friendInfoHandler.addDataChangeListener(
                KEY_GET_FRIENDS,
                new SafeDataHandler<List<FriendInfo>>() {
                    @Override
                    public void onDataChange(List<FriendInfo> friendInfos) {
                        List<ContactModel> contactModels = sortAndCategorizeContacts(friendInfos);
                        filteredContactsLiveData.postValue(contactModels); // 初始状态下，展示所有联系人
                    }
                });
        friendInfoHandler.getFriends(QueryFriendsDirectionType.Both);
    }

    /**
     * 加入群组
     *
     * @param onDataChangeListener 数据变化监听器
     * @since 5.12.2
     */
    public void inviteUsersToGroup(
            @NonNull OnDataChangeListener<IRongCoreEnum.CoreErrorCode> onDataChangeListener) {
        if (isJoining) {
            return; // 如果正在执行加入操作，直接返回
        }

        List<String> userIds = getSelectUserIds();
        if (userIds.isEmpty()) {
            return;
        }
        isJoining = true;
        groupOperationsHandler.replaceDataChangeListener(
                GroupOperationsHandler.KEY_INVITE_USERS_TO_GROUP,
                new OnDataChangeListener<IRongCoreEnum.CoreErrorCode>() {
                    @Override
                    public void onDataChange(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                        isJoining = false; // 处理完成后解除锁定
                        onDataChangeListener.onDataChange(coreErrorCode);
                    }

                    @Override
                    public void onDataError(
                            IRongCoreEnum.CoreErrorCode errorCode, String errorMsg) {
                        isJoining = false; // 错误时也解除锁定
                        onDataChangeListener.onDataChange(errorCode);
                        onDataChangeListener.onDataError(errorCode, errorMsg);
                    }
                });

        groupOperationsHandler.inviteUsersToGroup(userIds);
    }

    /**
     * @since 5.12.2
     */
    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    /**
     * @since 5.12.2
     */
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
            if (contact.getBean() instanceof FriendInfo
                    && updatedContact.getBean() instanceof FriendInfo) {
                FriendInfo friendInfo = (FriendInfo) contact.getBean();
                FriendInfo updatedFriendInfo = (FriendInfo) updatedContact.getBean();
                if (friendInfo.getUserId().equals(updatedFriendInfo.getUserId())) {
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
     * 查询联系人
     *
     * @param query 查询关键字
     */
    public void queryContacts(String query) {
        if (TextUtils.isEmpty(query)) {
            friendInfoHandler.getFriends(QueryFriendsDirectionType.Both);
            return;
        }
        friendInfoHandler.searchFriendsInfo(query);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendInfoHandler.stop();
        groupMembersFullHandler.stop();
        groupOperationsHandler.stop();
    }

    List<String> getSelectUserIds() {
        List<String> userIds = new ArrayList<>();
        List<ContactModel> selectedList = selectedContactsLiveData.getValue();
        if (selectedList == null || selectedList.isEmpty()) {
            return userIds;
        }
        for (ContactModel contact : selectedList) {
            if (contact.getBean() instanceof FriendInfo) {
                FriendInfo friendInfo = (FriendInfo) contact.getBean();
                if (friendInfo != null) {
                    userIds.add(friendInfo.getUserId());
                }
            }
        }
        return userIds;
    }

    String getGroupId() {
        return groupId;
    }

    private List<ContactModel> sortAndCategorizeContacts(List<FriendInfo> friendInfos) {
        // 排序处理，中文按首字母，英文按整个名字排序
        Collections.sort(
                friendInfos,
                (friend1, friend2) -> {
                    String name1 = getValidName(friend1);
                    String name2 = getValidName(friend2);
                    char firstChar1 = StringUtils.getFirstChar(name1.charAt(0));
                    char firstChar2 = StringUtils.getFirstChar(name2.charAt(0));
                    // 如果 firstChar1 是 #，把它放到最后
                    if (firstChar1 == '#') {
                        return 1;
                    }
                    // 如果 firstChar2 是 #，把它放到最后
                    if (firstChar2 == '#') {
                        return -1;
                    }
                    if (Character.isLetter(firstChar1) && Character.isLetter(firstChar2)) {
                        return firstChar1 - firstChar2;
                    } else {
                        return name1.compareToIgnoreCase(name2);
                    }
                });

        List<ContactModel> contactModels = new ArrayList<>();
        char lastCategory = '\0';
        List<String> selectUserIds = getSelectUserIds();
        for (FriendInfo friendInfo : friendInfos) {
            String name = getValidName(friendInfo);
            char firstChar = StringUtils.getFirstChar(name.charAt(0));

            // 如果首字母不同，添加一个标题 ContactModel
            if (firstChar != lastCategory) {
                contactModels.add(
                        ContactModel.obtain(
                                String.valueOf(firstChar),
                                ContactModel.ItemType.TITLE,
                                ContactModel.CheckType.NONE));
                lastCategory = firstChar;
            }

            ContactModel.CheckType checkType = ContactModel.CheckType.UNCHECKED;
            if (selectUserIds.contains(friendInfo.getUserId())) {
                checkType = ContactModel.CheckType.CHECKED;
            }
            if (existingGroupMemberIds.contains(friendInfo.getUserId())) {
                checkType = ContactModel.CheckType.DISABLE;
            }

            // 添加联系人 ContactModel
            contactModels.add(
                    ContactModel.obtain(friendInfo, ContactModel.ItemType.CONTENT, checkType));
        }

        return contactModels;
    }

    private String getValidName(FriendInfo friendInfo) {
        // 返回有效的名称，优先使用备注，其次是姓名，最后使用 "#" 作为占位符
        String name =
                !TextUtils.isEmpty(friendInfo.getRemark())
                        ? friendInfo.getRemark()
                        : friendInfo.getName();
        return !TextUtils.isEmpty(name) ? name : "#";
    }
}
