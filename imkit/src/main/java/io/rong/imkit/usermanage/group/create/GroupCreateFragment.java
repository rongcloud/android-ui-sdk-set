package io.rong.imkit.usermanage.group.create;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.friend.select.FriendSelectFragment;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.dialog.TipLoadingDialog;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import java.util.List;

/**
 * 功能描述: 创建群组页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupCreateFragment extends BaseViewModelFragment<GroupCreateViewModel> {

    protected HeadComponent headComponent;
    protected EditText etGroupName;
    protected ImageView ivGroupIcon;
    protected Button btnCreateGroup;

    protected TipLoadingDialog dialog;

    @NonNull
    @Override
    protected GroupCreateViewModel onCreateViewModel(@NonNull Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(getArguments()))
                .get(GroupCreateViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_create, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        etGroupName = view.findViewById(R.id.et_group_name);
        ivGroupIcon = view.findViewById(R.id.iv_group_icon);
        btnCreateGroup = view.findViewById(R.id.btn_create_group);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupCreateViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightTextViewEnable(false);

        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadGroupPortrait(ivGroupIcon.getContext(), null, ivGroupIcon);

        btnCreateGroup.setOnClickListener(
                v -> {
                    if (etGroupName != null) {
                        String groupName = etGroupName.getText().toString().trim();
                        if (TextUtils.isEmpty(groupName)) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.rc_group_name_cannot_be_empty),
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                        if (TextUtils.isEmpty(groupName) || groupName.length() > 64) {
                            ToastUtils.show(
                                    getContext(),
                                    getString(R.string.rc_input_length_invalid),
                                    Toast.LENGTH_SHORT);
                            return;
                        }
                        showLoadingDialog();
                        getViewModel()
                                .createGroup(
                                        groupName,
                                        new OnDataChangeEnhancedListener<
                                                IRongCoreEnum.CoreErrorCode>() {
                                            @Override
                                            public void onDataChange(
                                                    IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                                onCreateGroupResult(
                                                        viewModel.getGroupId(),
                                                        viewModel.getInviteeUserIds(),
                                                        coreErrorCode);
                                            }
                                        });
                    }
                });
    }

    /**
     * 创建群组结果
     *
     * @param groupId 群组ID
     * @param inviteeUserIds 邀请的用户ID列表
     * @param coreErrorCode 错误码
     * @since 5.12.2
     */
    protected void onCreateGroupResult(
            String groupId,
            List<String> inviteeUserIds,
            IRongCoreEnum.CoreErrorCode coreErrorCode) {
        dismissLoadingDialog();
        if (coreErrorCode == IRongCoreEnum.CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT
                || coreErrorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
            ConversationIdentifier conversationIdentifier =
                    ConversationIdentifier.obtain(Conversation.ConversationType.GROUP, groupId, "");
            RouteUtils.routeToConversationActivity(getContext(), conversationIdentifier);
            sendFinishActivityBroadcast(FriendSelectFragment.class);
            finishActivity();
        } else {
            String tips = getString(R.string.rc_create_group_failure);
            if (coreErrorCode == IRongCoreEnum.CoreErrorCode.SERVICE_INFORMATION_AUDIT_FAILED) {
                tips = getString(R.string.rc_content_contain_sensitive);
            }
            ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
        }
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
