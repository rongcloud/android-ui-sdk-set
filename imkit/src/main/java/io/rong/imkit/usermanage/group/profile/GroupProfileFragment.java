package io.rong.imkit.usermanage.group.profile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.adapter.GroupMembersAdapter;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.friend.my.profile.MyProfileActivity;
import io.rong.imkit.usermanage.friend.user.profile.UserProfileActivity;
import io.rong.imkit.usermanage.group.add.AddGroupMembersActivity;
import io.rong.imkit.usermanage.group.follows.GroupFollowsActivity;
import io.rong.imkit.usermanage.group.manage.GroupManagementActivity;
import io.rong.imkit.usermanage.group.memberlist.GroupMemberListActivity;
import io.rong.imkit.usermanage.group.name.GroupNameActivity;
import io.rong.imkit.usermanage.group.nickname.GroupNicknameActivity;
import io.rong.imkit.usermanage.group.notice.GroupNoticeActivity;
import io.rong.imkit.usermanage.group.remark.GroupRemarkActivity;
import io.rong.imkit.usermanage.group.remove.RemoveGroupMembersActivity;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.GroupOperationPermission;
import io.rong.imlib.model.UserInfo;
import java.util.Objects;

/**
 * 群组详情页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupProfileFragment extends BaseViewModelFragment<GroupProfileViewModel> {

    private int displayMaxMemberCount;

    protected HeadComponent headComponent;
    private RecyclerView groupMemberRecyclerView;
    private GroupMembersAdapter groupMembersAdapter;
    protected SettingItemView groupAvatarView;
    protected SettingItemView groupNameView;
    protected SettingItemView groupNoticeView;
    protected SettingItemView groupNicknameView;
    /** @since 5.12.2 */
    protected SettingItemView groupRemarkView;
    /** @since 5.12.2 */
    protected SettingItemView groupManageView;
    /** @since 5.12.2 */
    protected SettingItemView messageDisturbView;
    /** @since 5.12.2 */
    protected SettingItemView groupFollowsView;
    /** @since 5.12.2 */
    protected SettingItemView conversationSetTopView;

    private TextView groupMembersLabel;
    private LinearLayout groupMembersLayout;
    protected Button dismissGroupButton;

    @NonNull
    @Override
    protected GroupProfileViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupProfileViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_profile, container, false);

        // 初始化组件
        headComponent = view.findViewById(R.id.rc_head_component);
        groupMemberRecyclerView = view.findViewById(R.id.rv_group_members);
        groupAvatarView = view.findViewById(R.id.siv_group_avatar);
        groupNameView = view.findViewById(R.id.siv_group_name);
        groupNoticeView = view.findViewById(R.id.siv_group_announcement);
        groupNicknameView = view.findViewById(R.id.siv_my_nickname);
        groupRemarkView = view.findViewById(R.id.siv_group_remark);
        groupManageView = view.findViewById(R.id.siv_group_manage);
        messageDisturbView = view.findViewById(R.id.siv_message_disturb);
        groupFollowsView = view.findViewById(R.id.siv_group_follows);
        conversationSetTopView = view.findViewById(R.id.siv_conversation_set_top);

        dismissGroupButton = view.findViewById(R.id.btn_dissolve_group);
        groupMembersLabel = view.findViewById(R.id.tv_group_members_label);
        groupMembersLayout = view.findViewById(R.id.ll_group_members);

        // 配置 RecyclerView
        groupMemberRecyclerView.setLayoutManager(new GridLayoutManager(context, 5));
        displayMaxMemberCount =
                Math.max(
                        5,
                        Math.min(
                                50,
                                getArguments()
                                        .getInt(KitConstants.KEY_MAX_MEMBER_COUNT_DISPLAY, 30)));
        groupMembersAdapter = new GroupMembersAdapter(context, displayMaxMemberCount);
        groupMemberRecyclerView.setAdapter(groupMembersAdapter);

        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupProfileViewModel viewModel) {

        headComponent.setLeftClickListener(v -> finishActivity());

        ConversationIdentifier conversationIdentifier =
                getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        groupMembersLayout.setOnClickListener(
                v ->
                        startActivity(
                                GroupMemberListActivity.newIntent(
                                        getContext(), conversationIdentifier)));

        // 观察 ViewModel 中的群成员数据
        viewModel
                .getGroupMemberInfosLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfos -> {
                            if (groupMembersAdapter != null) {
                                groupMemberRecyclerView.post(
                                        () ->
                                                groupMembersAdapter.updateGroupInfoList(
                                                        groupMemberInfos));
                            }
                        });

        // 观察 ViewModel 中的群信息数据
        groupAvatarView.setSelected(true);
        groupAvatarView.setRightImageVisibility(View.GONE);
        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupInfo -> {
                            if (groupMembersLabel != null && headComponent != null) {
                                groupMembersLabel.setText(
                                        getString(
                                                R.string.rc_group_members_label,
                                                groupInfo.getMembersCount()));
                                headComponent.setTitleText(
                                        getString(
                                                R.string.rc_group_info,
                                                groupInfo.getMembersCount()));
                            }
                            // 设置群组头像
                            RongConfigCenter.featureConfig()
                                    .getKitImageEngine()
                                    .loadGroupPortrait(
                                            groupAvatarView.getContext(),
                                            groupInfo.getPortraitUri(),
                                            groupAvatarView.getSelectImage());

                            // 设置群组名称
                            groupNameView.setValue(groupInfo.getGroupName());
                            // 设置群备注
                            if (groupRemarkView != null) {
                                groupRemarkView.setValue(groupInfo.getRemark());
                            }
                            boolean canAddMembers =
                                    (groupInfo.getInvitePermission()
                                                    == GroupOperationPermission.Everyone)
                                            || (groupInfo.getInvitePermission()
                                                            == GroupOperationPermission
                                                                    .OwnerOrManager
                                                    && (groupInfo.getRole()
                                                                    == GroupMemberRole.Manager
                                                            || groupInfo.getRole()
                                                                    == GroupMemberRole.Owner))
                                            || (groupInfo.getInvitePermission()
                                                            == GroupOperationPermission.Owner
                                                    && groupInfo.getRole()
                                                            == GroupMemberRole.Owner);
                            groupMembersAdapter.setAllowGroupAddition(canAddMembers);

                            boolean canRemoveMembers =
                                    (groupInfo.getRemoveMemberPermission()
                                                    == GroupOperationPermission.Everyone)
                                            || (groupInfo.getRemoveMemberPermission()
                                                            == GroupOperationPermission
                                                                    .OwnerOrManager
                                                    && (groupInfo.getRole()
                                                                    == GroupMemberRole.Manager
                                                            || groupInfo.getRole()
                                                                    == GroupMemberRole.Owner))
                                            || (groupInfo.getRemoveMemberPermission()
                                                            == GroupOperationPermission.Owner
                                                    && groupInfo.getRole()
                                                            == GroupMemberRole.Owner);
                            groupMembersAdapter.setAllowGroupRemoval(canRemoveMembers);

                            if (groupManageView != null) {
                                boolean canManageGroup =
                                        groupInfo.getRole() == GroupMemberRole.Manager
                                                || groupInfo.getRole() == GroupMemberRole.Owner;
                                groupManageView.setVisibility(
                                        canManageGroup ? View.VISIBLE : View.GONE);
                            }
                        });

        viewModel
                .getMyMemberInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfo -> {
                            // 设置"我在本群的昵称"
                            groupNicknameView.setValue(groupMemberInfo.getNickname());
                            // 设置解散群组按钮是否可见
                            boolean isOwner = groupMemberInfo.getRole() == GroupMemberRole.Owner;
                            dismissGroupButton.setText(
                                    isOwner ? R.string.rc_dissolve_group : R.string.rc_leave_group);
                        });

        // 处理添加/删除成员的点击事件
        groupMembersAdapter.setOnGroupActionListener(
                new GroupMembersAdapter.OnGroupActionListener() {
                    @Override
                    public void addMemberClick() {
                        onAddMemberClick(conversationIdentifier);
                    }

                    @Override
                    public void removeMemberClick() {
                        onRemoveMemberClick(conversationIdentifier);
                    }

                    @Override
                    public void onGroupClicked(GroupMemberInfo groupMemberInfo) {
                        // 显示成员详情
                        onGroupMemberClick(conversationIdentifier, groupMemberInfo);
                    }
                });

        // 群名称
        groupNameView.setOnClickListener(
                v -> {
                    // 处理群组名称点击事件，可能是编辑群组名称
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();

                    if (groupInfo != null) {
                        boolean canEditNotice =
                                (groupInfo.getGroupInfoEditPermission()
                                                == GroupOperationPermission.Everyone)
                                        || (groupInfo.getGroupInfoEditPermission()
                                                        == GroupOperationPermission.OwnerOrManager
                                                && (groupInfo.getRole() == GroupMemberRole.Manager
                                                        || groupInfo.getRole()
                                                                == GroupMemberRole.Owner))
                                        || (groupInfo.getGroupInfoEditPermission()
                                                        == GroupOperationPermission.Owner
                                                && groupInfo.getRole() == GroupMemberRole.Owner);
                        if (!canEditNotice) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.rc_no_permission_to_modify_group_info),
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                        startActivity(
                                GroupNameActivity.newIntent(
                                        getContext(), conversationIdentifier, groupInfo));
                    }
                });

        // 群公告
        groupNoticeView.setOnClickListener(
                v -> {
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                    if (groupInfo != null) {
                        startActivity(
                                GroupNoticeActivity.newIntent(
                                        getContext(), conversationIdentifier, groupInfo));
                    }
                });
        // 群昵称
        groupNicknameView.setOnClickListener(
                v -> {
                    UserInfo currentUserInfo =
                            RongUserInfoManager.getInstance().getCurrentUserInfo();
                    if (currentUserInfo != null) {
                        startActivity(
                                GroupNicknameActivity.newIntent(
                                        getContext(),
                                        conversationIdentifier,
                                        currentUserInfo.getUserId(),
                                        getString(R.string.rc_my_nickname_in_group)));
                    }
                });

        // 群聊备注
        if (groupRemarkView != null) {
            groupRemarkView.setOnClickListener(
                    v ->
                            startActivity(
                                    GroupRemarkActivity.newIntent(
                                            getContext(), conversationIdentifier)));
        }
        // 群管理
        if (groupManageView != null) {
            groupManageView.setOnClickListener(
                    v ->
                            startActivity(
                                    GroupManagementActivity.newIntent(
                                            getContext(), conversationIdentifier)));
        }

        // 会话置顶和消息免打扰
        viewModel
                .getConversationNotificationStatusLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        conversationNotificationStatus -> {
                            if (messageDisturbView != null && groupFollowsView != null) {
                                boolean isNotDisturb =
                                        conversationNotificationStatus
                                                == Conversation.ConversationNotificationStatus
                                                        .DO_NOT_DISTURB;
                                messageDisturbView.setChecked(isNotDisturb);
                                groupFollowsView.setVisibility(
                                        isNotDisturb ? View.VISIBLE : View.GONE);
                            }
                        });
        viewModel
                .getIsConversationTopLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        isTop -> {
                            if (conversationSetTopView != null) {
                                conversationSetTopView.setChecked(Boolean.TRUE.equals(isTop));
                            }
                        });
        // 消息免打扰
        if (messageDisturbView != null) {
            messageDisturbView.setSwitchCheckListener(
                    (buttonView, isChecked) ->
                            viewModel.setConversationNotificationStatus(
                                    isChecked
                                            ? Conversation.ConversationNotificationStatus
                                                    .DO_NOT_DISTURB
                                            : Conversation.ConversationNotificationStatus.NOTIFY,
                                    conversationNotificationStatus -> {
                                        if (messageDisturbView != null
                                                && groupFollowsView != null) {
                                            boolean isNodDisturb =
                                                    conversationNotificationStatus
                                                            == Conversation
                                                                    .ConversationNotificationStatus
                                                                    .DO_NOT_DISTURB;
                                            messageDisturbView.setChecked(isNodDisturb);
                                            groupFollowsView.setVisibility(
                                                    isNodDisturb ? View.VISIBLE : View.GONE);
                                        }
                                    }));
        }
        // 特别关注
        if (groupFollowsView != null) {
            groupFollowsView.setContent("   --" + getString(R.string.rc_group_follows));
            groupFollowsView.setOnClickListener(
                    v ->
                            startActivity(
                                    GroupFollowsActivity.newIntent(
                                            getContext(), conversationIdentifier)));
        }

        // 会话置顶
        if (conversationSetTopView != null) {
            conversationSetTopView.setSwitchCheckListener(
                    (buttonView, isChecked) ->
                            viewModel.setConversationTopStatus(
                                    isChecked,
                                    isTop -> {
                                        if (isTop != null && !isTop) {
                                            conversationSetTopView.setChecked(
                                                    Boolean.TRUE.equals(
                                                            viewModel
                                                                    .getIsConversationTopLiveData()
                                                                    .getValue()));
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_set_failed),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    }));
        }

        // 设置解散群组按钮点击事件
        dismissGroupButton.setOnClickListener(
                v -> {
                    GroupMemberInfo groupMemberInfo =
                            viewModel.getMyMemberInfoLiveData().getValue();
                    if (groupMemberInfo != null) {
                        boolean isOwner = groupMemberInfo.getRole() == GroupMemberRole.Owner;
                        if (isOwner) {
                            viewModel.dismissGroup(
                                    isSuccess -> {
                                        if (isSuccess) {
                                            finishActivity();
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_group_dismiss_success),
                                                    Toast.LENGTH_SHORT);
                                        } else {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_group_dismiss_failed),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    });
                        } else {
                            viewModel.quitGroup(
                                    isSuccess -> {
                                        if (isSuccess) {
                                            finishActivity();
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_group_quit_success),
                                                    Toast.LENGTH_SHORT);
                                        } else {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_group_quit_failed),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    });
                        }
                    }
                });
    }

    /**
     * 处理添加成员点击事件
     *
     * @param conversationIdentifier 会话标识
     * @since 5.12.2
     */
    protected void onAddMemberClick(ConversationIdentifier conversationIdentifier) {
        // 处理添加成员的逻辑
        startActivity(
                AddGroupMembersActivity.newIntent(
                        getContext(),
                        getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER)));
    }

    /**
     * 处理删除成员点击事件
     *
     * @param conversationIdentifier 会话标识
     * @since 5.12.2
     */
    protected void onRemoveMemberClick(ConversationIdentifier conversationIdentifier) {
        // 处理删除成员的逻辑
        GroupInfo groupInfo = getViewModel().getGroupInfoLiveData().getValue();
        if (groupInfo != null) {
            startActivity(
                    RemoveGroupMembersActivity.newIntent(
                            getContext(), conversationIdentifier, groupInfo.getRole()));
        }
    }

    /**
     * 处理群成员点击事件
     *
     * @param conversationIdentifier 会话标识
     * @param groupMemberInfo 群成员信息
     * @since 5.12.2
     */
    protected void onGroupMemberClick(
            ConversationIdentifier conversationIdentifier, GroupMemberInfo groupMemberInfo) {
        if (groupMemberInfo != null) {
            String currentUserId = RongCoreClient.getInstance().getCurrentUserId();
            if (Objects.equals(groupMemberInfo.getUserId(), currentUserId)) {
                startActivity(MyProfileActivity.newIntent(getContext()));
            } else {
                startActivity(
                        UserProfileActivity.newIntent(
                                getContext(), groupMemberInfo.getUserId(), conversationIdentifier));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupInfo();
    }
}
