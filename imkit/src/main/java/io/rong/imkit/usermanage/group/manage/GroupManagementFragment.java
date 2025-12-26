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
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.group.managerlist.GroupManagerListActivity;
import io.rong.imkit.usermanage.group.transfer.GroupTransferActivity;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imkit.widget.dialog.TipLoadingDialog;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupJoinPermission;
import io.rong.imlib.model.GroupMemberInfoEditPermission;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.GroupOperationPermission;
import java.util.LinkedHashMap;
import java.util.List;
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
    private TipLoadingDialog dialog;

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
                new OnDataChangeEnhancedListener<Boolean>() {
                    @Override
                    public void onDataChange(Boolean isSuccess) {
                        dismissLoadingDialog();
                        if (isSuccess) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.rc_set_success),
                                    Toast.LENGTH_SHORT);
                            viewModel.refreshGroupInfo();
                        }
                    }

                    @Override
                    public void onDataError(
                            IRongCoreEnum.CoreErrorCode coreErrorCode, List<String> errorMsgs) {
                        String tips = getString(R.string.rc_set_failed);
                        if (coreErrorCode
                                == IRongCoreEnum.CoreErrorCode
                                        .RC_SERVICE_INFORMATION_AUDIT_FAILED) {
                            tips = getString(R.string.rc_content_contain_sensitive);
                        }
                        ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
                    }
                });
    }

    private <T extends Enum<T>> void showPermissionSelectionDialog(
            Map<T, String> permissionLabels, OnActionClickListener<T> listener) {
        Dialog dialog = new Dialog(requireContext());

        // 主容器
        LinearLayout mainContainer = new LinearLayout(requireContext());
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        int horizontalMargin = ScreenUtils.dip2px(requireContext(), 18);
        mainContainer.setPadding(
                horizontalMargin,
                0,
                horizontalMargin,
                (int) (34 * requireContext().getResources().getDisplayMetrics().density));

        // 选项容器
        LinearLayout optionsContainer = new LinearLayout(requireContext());
        optionsContainer.setOrientation(LinearLayout.VERTICAL);
        optionsContainer.setBackgroundResource(
                R.drawable.rc_bottom_dialog_background_color_radius_10);

        // 添加选项
        int index = 0;
        for (Map.Entry<T, String> entry : permissionLabels.entrySet()) {
            T permission = entry.getKey();
            String label = entry.getValue();

            // 如果不是第一个选项，添加分割线
            if (index > 0) {
                View divider = new View(requireContext());
                LinearLayout.LayoutParams dividerParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                (int)
                                        (1
                                                * requireContext()
                                                        .getResources()
                                                        .getDisplayMetrics()
                                                        .density));
                int dividerMargin =
                        (int) (10 * requireContext().getResources().getDisplayMetrics().density);
                dividerParams.setMargins(dividerMargin, 0, dividerMargin, 0);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(
                        IMKitThemeManager.getColorFromAttrId(
                                divider.getContext(),
                                R.attr.rc_line_background_color)); // rgba(255, 255, 255, 0.1)
                optionsContainer.addView(divider);
            }

            optionsContainer.addView(
                    createPermissionOptionView(
                            label,
                            v -> {
                                listener.onActionClick(permission);
                                dialog.dismiss();
                            }));
            index++;
        }

        mainContainer.addView(optionsContainer);

        // 添加取消按钮
        TextView cancelView =
                createCancelButtonView(
                        requireContext().getString(R.string.rc_cancel), v -> dialog.dismiss());
        mainContainer.addView(cancelView);

        dialog.setContentView(mainContainer);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            // 设置背景模糊效果（遮罩）
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.5f); // 根据 Figma 设计，遮罩透明度为 0.5
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // 在 Dialog 显示之后添加动画
        mainContainer.post(() -> playBottomToTopAnimation(mainContainer));
        dialog.show();
    }

    /** 创建权限选项视图 */
    private TextView createPermissionOptionView(String text, View.OnClickListener clickListener) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(14); // 根据 Figma 设计使用 14sp
        textView.setGravity(Gravity.CENTER);

        // 设置高度为 49dp
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        ScreenUtils.dip2px(requireContext(), 49));
        textView.setLayoutParams(params);

        textView.setBackgroundResource(R.drawable.rc_bottom_dialog_background_color_radius_10);
        textView.setOnClickListener(clickListener);
        textView.setTextColor(
                IMKitThemeManager.getColorFromAttrId(
                        textView.getContext(), R.attr.rc_text_primary_color));
        return textView;
    }

    /** 创建取消按钮视图 */
    private TextView createCancelButtonView(String text, View.OnClickListener clickListener) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextSize(14); // 根据 Figma 设计使用 14sp
        textView.setGravity(Gravity.CENTER);

        // 设置高度为 49dp
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        ScreenUtils.dip2px(requireContext(), 49));
        params.setMargins(0, ScreenUtils.dip2px(requireContext(), 12), 0, 0);
        textView.setLayoutParams(params);

        textView.setBackgroundResource(R.drawable.rc_bottom_dialog_background_color_radius_10);
        textView.setOnClickListener(clickListener);
        textView.setTextColor(
                IMKitThemeManager.getColorFromAttrId(
                        textView.getContext(), R.attr.rc_text_primary_color));
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

    /** loading dialog */
    private void showLoadingDialog() {
        if (dialog != null) {
            dismissLoadingDialog();
        }
        dialog = new TipLoadingDialog(getContext());
        dialog.setTips(getString(R.string.rc_loading_saving));
        dialog.show();
    }

    /** dismiss dialog */
    private void dismissLoadingDialog() {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        } catch (Exception e) {
            dialog = null;
        }
    }
}
