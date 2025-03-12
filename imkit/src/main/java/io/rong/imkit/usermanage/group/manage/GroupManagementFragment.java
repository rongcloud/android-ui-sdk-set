package io.rong.imkit.usermanage.group.manage;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.group.managerlist.GroupManagerListActivity;
import io.rong.imkit.usermanage.group.transfer.GroupTransferActivity;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupJoinPermission;
import io.rong.imlib.model.GroupMemberInfoEditPermission;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.GroupOperationPermission;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 群管理页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupManagementFragment extends BaseViewModelFragment<GroupManagementViewModel> {

    protected HeadComponent headComponent;
    protected SettingItemView groupAdminView;
    protected SettingItemView groupEditInfoPermissionView;
    protected SettingItemView groupAddMemberPermissionView;
    protected SettingItemView groupRemoveMemberPermissionView;
    protected SettingItemView groupEditMemberInfoPermissionView;
    protected SettingItemView groupInvitationConfirmationView;
    protected SettingItemView groupTransferView;

    @NonNull
    @Override
    protected GroupManagementViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupManagementViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_management, container, false);

        // 初始化组件
        headComponent = view.findViewById(R.id.rc_head_component);
        groupAdminView = view.findViewById(R.id.siv_group_admin);
        groupEditInfoPermissionView = view.findViewById(R.id.siv_group_edit_info_permission);
        groupAddMemberPermissionView = view.findViewById(R.id.siv_group_add_member_permission);
        groupRemoveMemberPermissionView =
                view.findViewById(R.id.siv_group_remove_member_permission);
        groupEditMemberInfoPermissionView =
                view.findViewById(R.id.siv_group_edit_member_info_permission);
        groupInvitationConfirmationView = view.findViewById(R.id.siv_group_invitation_confirmation);
        groupTransferView = view.findViewById(R.id.siv_group_transfer);

        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupManagementViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        // 群管理员设置点击事件
        groupAdminView.setOnClickListener(
                v -> {
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                    if (groupInfo != null
                            && (groupInfo.getRole() == GroupMemberRole.Owner
                                    || groupInfo.getRole() == GroupMemberRole.Manager)) {
                        ConversationIdentifier conversationIdentifier =
                                getArguments()
                                        .getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
                        startActivity(
                                GroupManagerListActivity.newIntent(
                                        getContext(), conversationIdentifier, groupInfo.getRole()));
                    }
                });

        // 编辑群信息权限点击事件
        groupEditInfoPermissionView.setOnClickListener(
                v -> {
                    Map<GroupOperationPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupOperationPermission.Owner,
                            getLocalizedPermissionLabel(GroupOperationPermission.Owner));
                    map.put(
                            GroupOperationPermission.OwnerOrManager,
                            getLocalizedPermissionLabel(GroupOperationPermission.OwnerOrManager));
                    map.put(
                            GroupOperationPermission.Everyone,
                            getLocalizedPermissionLabel(GroupOperationPermission.Everyone));
                    showPermissionSelectionDialog(
                            getGroupInfoEditPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    groupInfo.setGroupInfoEditPermission(selectedPermission);
                                    updateGroupInfo(viewModel, groupInfo);
                                }
                            });
                });

        // 添加群成员权限点击事件
        groupAddMemberPermissionView.setOnClickListener(
                v -> {
                    Map<GroupOperationPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupOperationPermission.Owner,
                            getLocalizedPermissionLabel(GroupOperationPermission.Owner));
                    map.put(
                            GroupOperationPermission.OwnerOrManager,
                            getLocalizedPermissionLabel(GroupOperationPermission.OwnerOrManager));
                    map.put(
                            GroupOperationPermission.Everyone,
                            getLocalizedPermissionLabel(GroupOperationPermission.Everyone));
                    showPermissionSelectionDialog(
                            getAddMemberPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    groupInfo.setInvitePermission(selectedPermission);
                                    updateGroupInfo(viewModel, groupInfo);
                                }
                            });
                });

        // 移除群成员权限点击事件
        groupRemoveMemberPermissionView.setOnClickListener(
                v -> {
                    Map<GroupOperationPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupOperationPermission.Owner,
                            getLocalizedPermissionLabel(GroupOperationPermission.Owner));
                    map.put(
                            GroupOperationPermission.OwnerOrManager,
                            getLocalizedPermissionLabel(GroupOperationPermission.OwnerOrManager));
                    map.put(
                            GroupOperationPermission.Everyone,
                            getLocalizedPermissionLabel(GroupOperationPermission.Everyone));
                    showPermissionSelectionDialog(
                            getRemoveMemberPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    groupInfo.setRemoveMemberPermission(selectedPermission);
                                    updateGroupInfo(viewModel, groupInfo);
                                }
                            });
                });

        // 修改群成员资料权限点击事件
        groupEditMemberInfoPermissionView.setOnClickListener(
                v -> {
                    Map<GroupMemberInfoEditPermission, String> map = new LinkedHashMap<>();
                    map.put(
                            GroupMemberInfoEditPermission.OwnerOrSelf,
                            getLocalizedPermissionLabel(GroupMemberInfoEditPermission.OwnerOrSelf));
                    map.put(
                            GroupMemberInfoEditPermission.OwnerOrManagerOrSelf,
                            getLocalizedPermissionLabel(
                                    GroupMemberInfoEditPermission.OwnerOrManagerOrSelf));
                    showPermissionSelectionDialog(
                            getMemberInfoEditPermissionLabels(map),
                            selectedPermission -> {
                                GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                                if (groupInfo != null) {
                                    groupInfo.setMemberInfoEditPermission(selectedPermission);
                                    updateGroupInfo(viewModel, groupInfo);
                                }
                            });
                });

        // 群聊邀请确认开关事件
        groupInvitationConfirmationView.setSwitchCheckListener(
                (buttonView, isChecked) -> {
                    GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
                    if (groupInfo != null) {
                        groupInfo.setJoinPermission(
                                isChecked
                                        ? GroupJoinPermission.OwnerOrManagerVerify
                                        : GroupJoinPermission.Free);
                        viewModel.updateGroupInfo(
                                groupInfo,
                                isSuccess -> {
                                    if (!isSuccess) {
                                        groupInvitationConfirmationView
                                                .setCheckedImmediatelyWithOutEvent(!isChecked);
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.rc_set_failed),
                                                Toast.LENGTH_SHORT);
                                    } else {
                                        viewModel.refreshGroupInfo();
                                    }
                                });
                    }
                });

        // 设置转让群点击事件
        groupTransferView.setOnClickListener(
                v -> {
                    ConversationIdentifier conversationIdentifier =
                            getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
                    startActivity(
                            GroupTransferActivity.newIntent(
                                    getContext(), conversationIdentifier, GroupMemberRole.Undef));
                });

        // 实时更新界面
        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupInfo -> {
                            if (groupInfo != null) {
                                boolean isOwner = groupInfo.getRole() == GroupMemberRole.Owner;
                                groupTransferView.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                                groupEditInfoPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupEditInfoPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getGroupInfoEditPermission()));
                                groupEditInfoPermissionView.setRightImageVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);

                                groupAddMemberPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupAddMemberPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getInvitePermission()));

                                groupRemoveMemberPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupRemoveMemberPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getRemoveMemberPermission()));

                                groupEditMemberInfoPermissionView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupEditMemberInfoPermissionView.setValue(
                                        getLocalizedPermissionLabel(
                                                groupInfo.getMemberInfoEditPermission()));

                                groupInvitationConfirmationView.setVisibility(
                                        isOwner ? View.VISIBLE : View.GONE);
                                groupInvitationConfirmationView.setCheckedImmediatelyWithOutEvent(
                                        groupInfo.getJoinPermission()
                                                == GroupJoinPermission.OwnerOrManagerVerify);
                            }
                        });
    }

    protected Map<GroupMemberInfoEditPermission, String> getMemberInfoEditPermissionLabels(
            Map<GroupMemberInfoEditPermission, String> map) {
        return map;
    }

    protected Map<GroupOperationPermission, String> getRemoveMemberPermissionLabels(
            Map<GroupOperationPermission, String> map) {
        return map;
    }

    protected Map<GroupOperationPermission, String> getAddMemberPermissionLabels(
            Map<GroupOperationPermission, String> map) {
        return map;
    }

    protected Map<GroupOperationPermission, String> getGroupInfoEditPermissionLabels(
            Map<GroupOperationPermission, String> map) {
        return map;
    }

    @NonNull
    protected <T extends Enum<T>> String getLocalizedPermissionLabel(@NonNull T permission) {
        if (permission instanceof GroupOperationPermission) {
            switch ((GroupOperationPermission) permission) {
                case Owner:
                    return getString(R.string.rc_group_permission_owner_only);
                case OwnerOrManager:
                    return getString(R.string.rc_group_permission_owner_and_admins);
                case Everyone:
                    return getString(R.string.rc_group_permission_all);
            }
        } else if (permission instanceof GroupMemberInfoEditPermission) {
            switch ((GroupMemberInfoEditPermission) permission) {
                case OwnerOrSelf:
                    return getString(R.string.rc_group_permission_owner_only);
                case OwnerOrManagerOrSelf:
                    return getString(R.string.rc_group_permission_owner_and_admins);
            }
        }
        return "";
    }

    private void updateGroupInfo(
            @NonNull GroupManagementViewModel viewModel, @NonNull GroupInfo groupInfo) {
        viewModel.updateGroupInfo(
                groupInfo,
                isSuccess -> {
                    ToastUtils.show(
                            getContext(),
                            getString(isSuccess ? R.string.rc_set_success : R.string.rc_set_failed),
                            Toast.LENGTH_SHORT);
                    if (isSuccess) {
                        viewModel.refreshGroupInfo();
                    }
                });
    }

    private <T extends Enum<T>> void showPermissionSelectionDialog(
            Map<T, String> permissionLabels, OnActionClickListener<T> listener) {
        Dialog dialog = new Dialog(requireContext());
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(0, 32, 0, 0);
        container.setBackgroundResource(R.drawable.rc_bottom_menu_dialog_style);

        // 添加选项
        for (Map.Entry<T, String> entry : permissionLabels.entrySet()) {
            T permission = entry.getKey();
            String label = entry.getValue();
            container.addView(
                    createDialogOptionView(
                            label,
                            v -> {
                                listener.onActionClick(permission);
                                dialog.dismiss();
                            }));
        }

        // 添加取消按钮
        TextView cancelView =
                createDialogOptionView(
                        requireContext().getString(R.string.rc_cancel), v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelLayoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelLayoutParams.setMargins(0, 12, 0, 0);
        cancelView.setLayoutParams(cancelLayoutParams);
        container.addView(cancelView);

        dialog.setContentView(container);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            // 设置背景模糊效果（遮罩）
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.3f); // 调整遮罩透明度，0.0为全透明，1.0为全黑
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // 在 Dialog 显示之后添加动画
        container.post(() -> playBottomToTopAnimation(container));
        dialog.show();
    }

    private TextView createDialogOptionView(String text, View.OnClickListener clickListener) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(16);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(32, 48, 32, 48);
        textView.setBackgroundResource(R.drawable.rc_selector_item_hover);
        textView.setOnClickListener(clickListener);
        return textView;
    }

    private void playBottomToTopAnimation(View view) {
        // 创建从底部向上滑动的动画
        TranslateAnimation slideUp =
                new TranslateAnimation(
                        Animation.RELATIVE_TO_PARENT, 0f, // 从自身的水平位置
                        Animation.RELATIVE_TO_PARENT, 0f, // 到目标水平位置
                        Animation.RELATIVE_TO_PARENT, 1f, // 从屏幕底部
                        Animation.RELATIVE_TO_PARENT, 0f // 到自身位置
                        );
        slideUp.setDuration(150); // 动画持续时间
        slideUp.setInterpolator(new DecelerateInterpolator()); // 减速插值器
        view.startAnimation(slideUp);
    }
}
