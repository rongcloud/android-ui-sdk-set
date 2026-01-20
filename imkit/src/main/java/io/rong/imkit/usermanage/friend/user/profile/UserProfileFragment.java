package io.rong.imkit.usermanage.friend.user.profile;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.model.UiUserDetail;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.friend.my.nikename.UpdateNickNameActivity;
import io.rong.imkit.usermanage.group.nickname.GroupNicknameActivity;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CommonDialog;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imkit.widget.SimpleInputDialog;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.FriendInfo;

/**
 * 用户资料页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UserProfileFragment extends BaseViewModelFragment<UserProfileViewModel> {
    protected HeadComponent headComponent;
    private View nicknameContainer;
    protected Button btnStartChat;
    protected Button btnStartAudio;
    protected Button btnStartVideo;
    protected Button btnDeleteUser;
    protected Button btnAddFriend;
    private TextView tvDisplayName;
    private TextView tvNickname;
    private ImageView ivUserPortrait;
    private View llFriendActions;
    private View llNoFriendActions;

    /**
     * @since 5.12.2
     */
    private SettingItemView groupNicknameView;

    @NonNull
    @Override
    protected UserProfileViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(UserProfileViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @NonNull ViewGroup container,
            @NonNull Bundle args) {
        View rootView = inflater.inflate(R.layout.rc_page_user_profile, container, false);
        headComponent = rootView.findViewById(R.id.rc_head_component);
        nicknameContainer = rootView.findViewById(R.id.nickname_container);
        btnStartChat = rootView.findViewById(R.id.btn_start_chat);
        btnStartAudio = rootView.findViewById(R.id.btn_start_audio);
        btnStartVideo = rootView.findViewById(R.id.btn_start_video);
        btnDeleteUser = rootView.findViewById(R.id.btn_delete_user);
        btnAddFriend = rootView.findViewById(R.id.btn_add_friend);
        tvDisplayName = rootView.findViewById(R.id.tv_display_name);
        tvNickname = rootView.findViewById(R.id.tv_nickname);
        ivUserPortrait = rootView.findViewById(R.id.user_portrait);
        llFriendActions = rootView.findViewById(R.id.ll_friend_actions);
        llNoFriendActions = rootView.findViewById(R.id.ll_no_friend_actions);

        groupNicknameView = rootView.findViewById(R.id.siv_group_nickname);
        return rootView;
    }

    @Override
    protected void onViewReady(@NonNull UserProfileViewModel viewModel) {
        // 设置返回按钮点击事件
        headComponent.setLeftClickListener(v -> finishActivity());

        // 设置点击事件
        nicknameContainer.setOnClickListener(
                v -> {
                    ContactModel contactModel = viewModel.getContactModelLiveData().getValue();
                    if (contactModel != null && contactModel.getBean() instanceof FriendInfo) {
                        startActivity(
                                UpdateNickNameActivity.newIntent(
                                        getContext(), (FriendInfo) contactModel.getBean()));
                    }
                });

        btnStartChat.setOnClickListener(
                v -> {
                    UiUserDetail detail = viewModel.getUiUserDetail();
                    if (detail != null) {
                        RouteUtils.routeToConversationActivity(
                                getContext(),
                                Conversation.ConversationType.PRIVATE,
                                detail.getUserId());
                    }
                });

        btnDeleteUser.setOnClickListener(v -> deleteFromContact());
        btnAddFriend.setOnClickListener(v -> showAddFriendDialog());

        // 观察用户信息数据变化
        viewModel
                .getUserProfilesLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        userProfile -> {
                            // 更新好友状态相关按钮的显示状态
                            llFriendActions.setVisibility(
                                    userProfile.isFriend() ? View.VISIBLE : View.GONE);
                            llNoFriendActions.setVisibility(
                                    userProfile.isFriend() ? View.GONE : View.VISIBLE);
                            if (TextUtils.isEmpty(userProfile.getNickName())) {
                                tvDisplayName.setText(
                                        userProfile.getName() != null
                                                ? userProfile.getName()
                                                : getString(R.string.rc_unknow_type));
                                tvNickname.setVisibility(View.GONE);

                            } else {
                                tvNickname.setVisibility(View.VISIBLE);
                                tvDisplayName.setText(userProfile.getNickName());
                                tvNickname.setText(
                                        String.format(
                                                "%s: %s",
                                                getString(R.string.rc_nickname_label),
                                                userProfile.getName()));
                            }

                            // 更新用户头像
                            RongConfigCenter.featureConfig()
                                    .getKitImageEngine()
                                    .loadUserPortrait(
                                            getContext(),
                                            userProfile.getPortrait(),
                                            ivUserPortrait);
                        });

        if (groupNicknameView != null) {
            viewModel
                    .getMyGroupMemberInfoLiveData()
                    .observe(
                            getViewLifecycleOwner(),
                            groupMemberInfo -> {
                                if (groupMemberInfo != null) {
                                    groupNicknameView.setVisibility(View.VISIBLE);
                                    groupNicknameView.setValue(groupMemberInfo.getNickname());
                                }
                                groupNicknameView.setRightImageVisibility(
                                        viewModel.hasEditPermission() ? View.VISIBLE : View.GONE);
                            });
        }

        ConversationIdentifier conversationIdentifier =
                getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        String userId = getArguments().getString(KitConstants.KEY_USER_ID);
        if (groupNicknameView != null && conversationIdentifier != null) {
            groupNicknameView.setOnClickListener(
                    v -> {
                        if (viewModel.hasEditPermission()) {
                            startActivity(
                                    GroupNicknameActivity.newIntent(
                                            getContext(),
                                            conversationIdentifier,
                                            userId,
                                            getString(R.string.rc_group_nickname)));
                        }
                    });
        }
    }

    private void showAddFriendDialog() {
        SimpleInputDialog dialog = new SimpleInputDialog();
        dialog.setInputHint(getString(R.string.rc_add_friend_hint));
        dialog.setTitleText(getString(R.string.rc_add_as_friend));
        dialog.setInputDialogListener(
                input -> {
                    String inviteMsg = input.getText().toString();
                    getViewModel()
                            .applyFriend(
                                    inviteMsg,
                                    coreErrorCode -> {
                                        if (coreErrorCode == IRongCoreEnum.CoreErrorCode.SUCCESS
                                                || coreErrorCode
                                                        == IRongCoreEnum.CoreErrorCode
                                                                .RC_FRIEND_NEED_ACCEPT) {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_send_apply_success),
                                                    Toast.LENGTH_SHORT);
                                            getViewModel().getUserProfile();
                                        } else {
                                            ToastUtils.show(
                                                    getContext(),
                                                    getString(R.string.rc_send_apply_fail),
                                                    Toast.LENGTH_SHORT);
                                        }
                                    });
                    return true;
                });
        dialog.show(getParentFragmentManager(), null);
    }

    private void deleteFromContact() {
        CommonDialog dialog =
                new CommonDialog.Builder()
                        .setContentMessage(getString(R.string.rc_delete_friend_title))
                        .setDialogButtonClickListener(
                                new CommonDialog.OnDialogButtonClickListener() {
                                    @Override
                                    public void onPositiveClick(View v, Bundle bundle) {
                                        getViewModel()
                                                .deleteFriend(
                                                        result -> {
                                                            if (result) {
                                                                ToastUtils.show(
                                                                        getContext(),
                                                                        getString(
                                                                                R.string
                                                                                        .rc_delete_friend_success),
                                                                        Toast.LENGTH_SHORT);
                                                                getViewModel().getUserProfile();
                                                            } else {
                                                                ToastUtils.show(
                                                                        getContext(),
                                                                        getString(
                                                                                R.string
                                                                                        .rc_delete_friend_failed),
                                                                        Toast.LENGTH_SHORT);
                                                            }
                                                        });
                                    }

                                    @Override
                                    public void onNegativeClick(View v, Bundle bundle) {}
                                })
                        .build();
        dialog.show(getParentFragmentManager(), null);
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().getUserProfile();
    }
}
