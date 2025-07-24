package io.rong.imkit.usermanage.group.remark;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.ExtendedGroup;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.GroupInfo;

/**
 * 修改群备注页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupRemarkFragment extends BaseViewModelFragment<GroupRemarkViewModel> {

    protected HeadComponent headComponent;
    protected EditText groupRemarkInput;

    @NonNull
    @Override
    protected GroupRemarkViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupRemarkViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_remark, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        groupRemarkInput = view.findViewById(R.id.group_remark_input);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupRemarkViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    String newNickName = groupRemarkInput.getText().toString().trim();
                    viewModel.setGroupRemark(
                            newNickName,
                            isSuccess -> {
                                if (isSuccess) {
                                    // 刷新缓存
                                    GroupInfo groupInfo =
                                            viewModel.getGroupInfoLiveData().getValue();
                                    if (groupInfo != null
                                            && RongUserInfoManager.getInstance()
                                                            .getGroupInfo(groupInfo.getGroupId())
                                                    != null) {
                                        groupInfo.setRemark(newNickName);
                                        RongUserInfoManager.getInstance()
                                                .refreshGroupInfoCache(
                                                        ExtendedGroup.obtain(groupInfo));
                                    }
                                    ToastUtils.show(
                                            getActivity(),
                                            getString(R.string.rc_set_success),
                                            Toast.LENGTH_SHORT);
                                    finishActivity();
                                } else {
                                    ToastUtils.show(
                                            getActivity(),
                                            getString(R.string.rc_set_failed),
                                            Toast.LENGTH_SHORT);
                                }
                            });
                });

        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfo -> {
                            if (groupMemberInfo != null) {
                                groupRemarkInput.setText(groupMemberInfo.getRemark());
                                headComponent.setRightTextViewEnable(false);
                            }
                        });

        GroupInfo groupInfo = viewModel.getGroupInfoLiveData().getValue();
        if (groupInfo != null) {
            groupRemarkInput.setText(groupInfo.getRemark());
            headComponent.setRightTextViewEnable(false);
        }

        groupRemarkInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int length = s.length();
                        boolean isEnable = length >= 0 && length <= 64;
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(isEnable);
                        }
                    }
                });
    }
}
