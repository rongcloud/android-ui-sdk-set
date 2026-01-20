package io.rong.imkit.usermanage.group.notice;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CommonDialog;
import io.rong.imkit.widget.dialog.TipLoadingDialog;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.GroupOperationPermission;
import java.util.List;

/**
 * 群公告页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupNoticeFragment extends BaseViewModelFragment<GroupNoticeViewModel> {

    protected HeadComponent headComponent;
    private EditText etGroupNotice;

    /**
     * @since 5.12.2
     */
    private TextView tvEditPermission;

    private LinearLayout llGroupNoticeDisplay;
    private TextView tvNoticeEmpty;
    private TextView tvNoticeContent;
    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupNoticeViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupNoticeViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_notice, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        etGroupNotice = view.findViewById(R.id.group_notice_input);
        tvEditPermission = view.findViewById(R.id.tv_edit_permission);
        llGroupNoticeDisplay = view.findViewById(R.id.ll_group_notice_display);
        tvNoticeContent = view.findViewById(R.id.tv_notice_content);
        tvNoticeEmpty = view.findViewById(R.id.tv_empty_notice);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupNoticeViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        GroupInfo groupInfo = getArguments().getParcelable(KitConstants.KEY_GROUP_INFO);
        boolean canEditNotice =
                (groupInfo.getGroupInfoEditPermission() == GroupOperationPermission.Everyone)
                        || (groupInfo.getGroupInfoEditPermission()
                                        == GroupOperationPermission.OwnerOrManager
                                && (groupInfo.getRole() == GroupMemberRole.Manager
                                        || groupInfo.getRole() == GroupMemberRole.Owner))
                        || (groupInfo.getGroupInfoEditPermission() == GroupOperationPermission.Owner
                                && groupInfo.getRole() == GroupMemberRole.Owner);
        if (canEditNotice) {
            headComponent.setRightClickListener(
                    v -> {
                        String newNotice = etGroupNotice.getText().toString().trim();
                        groupInfo.setNotice(newNotice);
                        onConfirmGroupNoticeUpdate(viewModel, groupInfo);
                    });
            etGroupNotice.setVisibility(View.VISIBLE);
            headComponent.setRightTextViewEnable(false);
            etGroupNotice.setText(groupInfo.getNotice());
            llGroupNoticeDisplay.setVisibility(View.GONE);
        } else {
            headComponent.getRightTextView().setVisibility(View.GONE);
            headComponent.setRightTextViewEnable(false);
            llGroupNoticeDisplay.setVisibility(View.VISIBLE);
            if (groupInfo.getNotice() != null && !groupInfo.getNotice().isEmpty()) {
                tvNoticeContent.setText(groupInfo.getNotice());
                tvNoticeEmpty.setVisibility(View.GONE);
            } else {
                tvNoticeEmpty.setVisibility(View.VISIBLE);
                tvNoticeContent.setVisibility(View.GONE);
            }
            if (tvEditPermission != null) {
                tvEditPermission.setText(
                        getString(
                                groupInfo.getGroupInfoEditPermission()
                                                == GroupOperationPermission.Owner
                                        ? R.string.rc_group_edit_permission_owner_only
                                        : R.string.rc_edit_permission));
            }
        }

        etGroupNotice.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(true);
                        }
                    }
                });
    }

    /**
     * 确认更新群公告
     *
     * @param viewModel 群公告 ViewModel
     * @param groupInfo 群组信息
     */
    protected void onConfirmGroupNoticeUpdate(
            @NonNull GroupNoticeViewModel viewModel, GroupInfo groupInfo) {
        // 弹出删除好友确认对话框
        new CommonDialog.Builder()
                .setContentMessage(getString(R.string.rc_publish_announcement_hint))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            showLoadingDialog();
                            viewModel.updateGroupNotice(
                                    groupInfo,
                                    new OnDataChangeEnhancedListener<Boolean>() {
                                        @Override
                                        public void onDataChange(Boolean aBoolean) {
                                            onGroupNoticeUpdateResult(
                                                    groupInfo.getGroupId(),
                                                    groupInfo.getNotice(),
                                                    IRongCoreEnum.CoreErrorCode.SUCCESS);
                                        }

                                        @Override
                                        public void onDataError(
                                                IRongCoreEnum.CoreErrorCode coreErrorCode,
                                                List<String> errorMsgs) {
                                            onGroupNoticeUpdateResult(
                                                    groupInfo.getGroupId(),
                                                    groupInfo.getNotice(),
                                                    coreErrorCode);
                                        }
                                    });
                        })
                .build()
                .show(getParentFragmentManager(), null);
    }

    /**
     * 更新群公告结果
     *
     * @param groupId 群组 ID
     * @param notice 群公告
     * @param isSuccess 是否成功
     */
    protected void onGroupNoticeUpdateResult(
            String groupId, String notice, IRongCoreEnum.CoreErrorCode coreErrorCode) {
        dismissLoadingDialog();
        if (coreErrorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
            ToastUtils.show(
                    getActivity(), getString(R.string.rc_group_notice_success), Toast.LENGTH_SHORT);
            finishActivity();
        } else {
            String tips = getString(R.string.rc_group_notice_failed);
            if (coreErrorCode == IRongCoreEnum.CoreErrorCode.RC_SERVICE_INFORMATION_AUDIT_FAILED) {
                tips = getString(R.string.rc_content_contain_sensitive);
            }
            ToastUtils.show(getActivity(), tips, Toast.LENGTH_SHORT);
        }
        onGroupNoticeUpdateResult(
                groupId, notice, (coreErrorCode == IRongCoreEnum.CoreErrorCode.SUCCESS));
    }

    protected void onGroupNoticeUpdateResult(String groupId, String notice, boolean isSuccess) {}

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
