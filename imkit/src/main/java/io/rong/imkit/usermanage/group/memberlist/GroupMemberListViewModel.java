package io.rong.imkit.usermanage.group.memberlist;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 功能描述: 群组联系人页面ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupMemberListViewModel extends BaseViewModel {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupInfoHandler groupInfoHandler;

    public GroupMemberListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        int maxCount = arguments.getInt(KitConstants.KEY_MAX_MEMBER_COUNT_PAGED, 50);
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));
        groupMembersPagedHandler =
                new GroupMembersPagedHandler(conversationIdentifier, validatedMaxMemberCountPaged);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        List<ContactModel> contactModels =
                                sortAndCategorizeContacts(groupMemberInfos);
                        allContactsLiveData.postValue(contactModels);
                        filteredContactsLiveData.postValue(contactModels); // 初始状态下，展示所有联系人
                    }
                });
        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GROUP_INFO, groupInfoLiveData::postValue);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_SEARCH_GROUP_MEMBERS,
                groupMemberInfos -> {
                    List<ContactModel> contactModels = sortAndCategorizeContacts(groupMemberInfos);
                    filteredContactsLiveData.postValue(contactModels);
                });
        refreshGroupMembers();
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<GroupInfo> getGroupInfoLiveData() {
        return groupInfoLiveData;
    }

    /**
     * 查询联系人
     *
     * @param query 查询关键字
     */
    public void queryContacts(@NonNull String query) {
        if (TextUtils.isEmpty(query)) {
            filteredContactsLiveData.postValue(allContactsLiveData.getValue());
            return;
        }
        groupInfoHandler.searchGroupMembers(query);
    }

    OnPagedDataLoader getOnPageDataLoader() {
        return groupMembersPagedHandler;
    }

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

    void refreshGroupMembers() {
        groupInfoHandler.getGroupsInfo();
        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.Undef);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupInfoHandler.stop();
    }
}