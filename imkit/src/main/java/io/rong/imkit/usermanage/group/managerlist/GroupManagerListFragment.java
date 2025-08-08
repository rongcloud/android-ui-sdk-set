package io.rong.imkit.usermanage.group.managerlist;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.ContactListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.group.memberselect.impl.GroupAddManagerActivity;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CommonDialog;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 群管理员列表页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupManagerListFragment extends BaseViewModelFragment<GroupManagerListViewModel> {

    protected HeadComponent headComponent;
    protected ContactListComponent memberListComponent;
    protected SettingItemView groupAddManager;
    private static final int MAX_COUNT = 10;

    @NonNull
    @Override
    protected GroupManagerListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupManagerListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_manager_list, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        groupAddManager = view.findViewById(R.id.siv_group_add_manager);
        memberListComponent = view.findViewById(R.id.rc_group_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupManagerListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        GroupMemberRole groupMemberRole =
                (GroupMemberRole)
                        getArguments().getSerializable(KitConstants.KEY_GROUP_MEMBER_ROLE);

        ConversationIdentifier conversationIdentifier =
                getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        if (conversationIdentifier == null) {
            return;
        }

        boolean isOwner = groupMemberRole == GroupMemberRole.Owner;
        groupAddManager.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        memberListComponent.setShowItemRemoveButton(isOwner);

        // 添加群管理员
        groupAddManager.setOnClickListener(
                v -> {
                    List<String> userIdList = new ArrayList<>();
                    List<ContactModel> contactModels =
                            viewModel.getAllGroupManagersLiveData().getValue();
                    if (contactModels != null) {
                        for (ContactModel contactModel : contactModels) {
                            if (contactModel.getBean() instanceof GroupMemberInfo) {
                                GroupMemberInfo groupMemberInfo =
                                        (GroupMemberInfo) contactModel.getBean();
                                userIdList.add(groupMemberInfo.getUserId());
                            }
                        }
                    }
                    addGroupManager(conversationIdentifier, userIdList);
                });

        memberListComponent.setOnItemRemoveClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        onRemoveGroupManager(conversationIdentifier.getTargetId(), groupMemberInfo);
                    }
                });

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getAllGroupManagersLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && memberListComponent != null) {
                                memberListComponent.setContactList(contactModels);
                            }
                        });
    }

    /**
     * 添加群管理员
     *
     * @param conversationIdentifier 会话标识
     * @param UserIdList 用户 ID 列表
     */
    protected void addGroupManager(
            ConversationIdentifier conversationIdentifier, List<String> UserIdList) {
        startActivity(
                GroupAddManagerActivity.newIntent(
                        getContext(),
                        conversationIdentifier,
                        UserIdList,
                        MAX_COUNT - UserIdList.size()));
    }

    /**
     * 移除群管理员
     *
     * @param groupId 群组 ID
     * @param groupMemberInfo 群成员信息
     */
    protected void onRemoveGroupManager(String groupId, GroupMemberInfo groupMemberInfo) {
        RongCoreClient.getInstance()
                .getFriendsInfo(
                        Arrays.asList(groupMemberInfo.getUserId()),
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                            @Override
                            public void onSuccess(List<FriendInfo> friendInfos) {
                                removeGroupManager(groupId, friendInfos, groupMemberInfo);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                removeGroupManager(groupId, null, groupMemberInfo);
                            }
                        });
    }

    private void removeGroupManager(
            String groupId, List<FriendInfo> friendInfos, GroupMemberInfo groupMemberInfo) {
        String name = concatenateUserDisplayNames(friendInfos, groupMemberInfo);
        new CommonDialog.Builder()
                .setContentMessage(getString(R.string.rc_remove_manager_hint, name))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            getViewModel()
                                    .removeGroupManager(
                                            Arrays.asList(groupMemberInfo.getUserId()),
                                            isSuccess -> {
                                                onGroupManagerRemovalResult(
                                                        groupId, groupMemberInfo, isSuccess);
                                            });
                        })
                .build()
                .show(getParentFragmentManager(), null);
    }

    /**
     * 移除群管理员结果
     *
     * @param groupId 群组 ID
     * @param groupMemberInfo 群成员信息
     * @param isSuccess 是否成功
     */
    protected void onGroupManagerRemovalResult(
            String groupId, GroupMemberInfo groupMemberInfo, boolean isSuccess) {
        if (isSuccess) {
            getViewModel().refreshGroupManagerList();
        }
        ToastUtils.show(
                getContext(),
                isSuccess
                        ? getString(R.string.rc_remove_success)
                        : getString(R.string.rc_remove_failed),
                Toast.LENGTH_SHORT);
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupManagerList();
    }

    private String concatenateUserDisplayNames(
            List<FriendInfo> friendInfos, GroupMemberInfo groupMemberInfo) {
        String displayName =
                TextUtils.isEmpty(groupMemberInfo.getNickname())
                        ? groupMemberInfo.getName()
                        : groupMemberInfo.getNickname();
        if (friendInfos != null && !friendInfos.isEmpty()) {
            String remark = friendInfos.get(0).getRemark();
            if (!TextUtils.isEmpty(remark)) {
                displayName = remark;
            }
        }
        return displayName;
    }
}
