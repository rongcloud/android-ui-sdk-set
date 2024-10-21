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
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.GroupOperationPermission;

/**
 * 功能描述: 创建增加群联系人页面
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupNoticeFragment extends BaseViewModelFragment<GroupNoticeViewModel> {

    protected HeadComponent headComponent;
    private EditText etGroupNotice;
    private LinearLayout llGroupNoticeDisplay;
    private TextView tvNoticeEmpty;
    private TextView tvNoticeContent;

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
                        viewModel.updateGroupNotice(
                                groupInfo,
                                isSuccess -> {
                                    if (isSuccess) {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.rc_group_notice_success),
                                                Toast.LENGTH_SHORT);
                                        finishActivity();
                                    } else {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.rc_group_notice_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                });
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
}
