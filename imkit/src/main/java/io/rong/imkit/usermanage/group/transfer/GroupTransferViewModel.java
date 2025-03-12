package io.rong.imkit.usermanage.group.transfer;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.handler.GroupMembersSearchPagedHandler;
import io.rong.imkit.usermanage.handler.GroupOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.QuitGroupConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * 移交群主页面 ViewModel
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupTransferViewModel extends BaseViewModel implements OnPagedDataLoader {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupMembersSearchPagedHandler groupMembersSearchPagedHandler;
    protected final GroupOperationsHandler groupOperationsHandler;

    private boolean isSearchMode = false;
    private final String groupId;

    public GroupTransferViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupId = conversationIdentifier.getTargetId();
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
     * 转移群主
     *
     * @param groupMemberInfo 被转移的群成员信息
     */
    public void transferGroupOwner(
            GroupMemberInfo groupMemberInfo, OnDataChangeListener<Boolean> listener) {
        groupOperationsHandler.addDataChangeListener(
                GroupOperationsHandler.KEY_TRANSFER_GROUP_OWNER, listener);
        groupOperationsHandler.transferGroupOwner(
                groupMemberInfo.getUserId(), false, new QuitGroupConfig());
    }

    String getGroupId() {
        return groupId;
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

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupOperationsHandler.stop();
        groupMembersSearchPagedHandler.stop();
    }

    @NonNull
    private List<ContactModel> getAllContactsContactModels(List<GroupMemberInfo> groupMemberInfos) {
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo memberInfo : groupMemberInfos) {
            if (memberInfo.getRole() == GroupMemberRole.Owner) {
                continue;
            }
            contactModels.add(
                    ContactModel.obtain(
                            memberInfo,
                            ContactModel.ItemType.CONTENT,
                            ContactModel.CheckType.NONE));
        }
        return contactModels;
    }
}
