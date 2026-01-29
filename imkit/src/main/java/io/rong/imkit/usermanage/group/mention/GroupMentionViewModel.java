package io.rong.imkit.usermanage.group.mention;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.GroupMembersPagedHandler;
import io.rong.imkit.usermanage.handler.GroupMembersSearchPagedHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.RongCoreClient;
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
public class GroupMentionViewModel extends BaseViewModel implements OnPagedDataLoader {

    private final MutableLiveData<List<ContactModel>> allContactsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ContactModel>> filteredContactsLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupMemberRole> mentionAllRoleLiveData = new MutableLiveData<>();

    protected final GroupMembersPagedHandler groupMembersPagedHandler;
    protected final GroupMembersSearchPagedHandler groupMembersSearchPagedHandler;
    private final GroupInfoHandler groupInfoHandler;
    private final String currentUserId;
    private final ConversationIdentifier conversationIdentifier;

    private boolean isSearchMode = false;

    public GroupMentionViewModel(@NonNull Bundle arguments) {
        super(arguments);
        conversationIdentifier = arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        currentUserId = RongCoreClient.getInstance().getCurrentUserId();

        // 初始化 GroupMembersPagedHandler 以获取群成员
        groupMembersPagedHandler = new GroupMembersPagedHandler(conversationIdentifier);
        groupMembersPagedHandler.addDataChangeListener(
                GroupMembersPagedHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        List<ContactModel> contactModels =
                                sortAndCategorizeContacts(groupMemberInfos);
                        allContactsLiveData.postValue(contactModels);
                        if (!isSearchMode) {
                            filteredContactsLiveData.postValue(contactModels);
                        }
                    }
                });

        groupMembersSearchPagedHandler = new GroupMembersSearchPagedHandler(conversationIdentifier);
        groupMembersSearchPagedHandler.addDataChangeListener(
                GroupMembersSearchPagedHandler.KEY_SEARCH_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        filteredContactsLiveData.postValue(
                                sortAndCategorizeContacts(groupMemberInfos));
                    }
                });

        groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
        groupInfoHandler.addDataChangeListener(
                GroupInfoHandler.KEY_GET_GROUP_MEMBERS,
                new SafeDataHandler<List<GroupMemberInfo>>() {
                    @Override
                    public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                        GroupMemberRole role = null;
                        if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                            role = groupMemberInfos.get(0).getRole();
                        }
                        if (role != null) {
                            mentionAllRoleLiveData.postValue(role);
                        }
                    }
                });
        if (!TextUtils.isEmpty(currentUserId)) {
            groupInfoHandler.getGroupMembers(Collections.singletonList(currentUserId));
        }
    }

    public LiveData<List<ContactModel>> getFilteredContactsLiveData() {
        return filteredContactsLiveData;
    }

    public LiveData<GroupMemberRole> getMentionAllRoleLiveData() {
        return mentionAllRoleLiveData;
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

    void refreshGroupManagerList() {
        groupMembersPagedHandler.getGroupMembersByRole(GroupMemberRole.Undef);
    }

    @NonNull
    private List<ContactModel> sortAndCategorizeContacts(List<GroupMemberInfo> groupMemberInfos) {
        if (groupMemberInfos == null || groupMemberInfos.isEmpty()) {
            return Collections.emptyList();
        }

        // 过滤当前用户
        List<ContactModel> contactModels = new ArrayList<>();
        for (GroupMemberInfo groupMemberInfo : groupMemberInfos) {
            if (TextUtils.isEmpty(currentUserId)
                    || !currentUserId.equals(groupMemberInfo.getUserId())) {
                // 添加联系人 ContactModel
                ContactModel<GroupMemberInfo> contactModel =
                        ContactModel.obtain(
                                groupMemberInfo,
                                ContactModel.ItemType.CONTENT,
                                ContactModel.CheckType.NONE);
                contactModel.putExtra(GroupMentionFragment.class.getSimpleName());
                contactModels.add(contactModel);
            }
        }

        return contactModels;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        groupMembersPagedHandler.stop();
        groupMembersSearchPagedHandler.stop();
        groupInfoHandler.stop();
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
}
