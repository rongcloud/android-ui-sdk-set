package io.rong.imkit.usermanage.group.nickname;

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
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.GroupMemberInfo;

/**
 * 功能描述: 创建增加群联系人页面
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupNicknameFragment extends BaseViewModelFragment<GroupNicknameViewModel> {

    protected HeadComponent headComponent;
    private EditText groupNicknameInput;

    @NonNull
    @Override
    protected GroupNicknameViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupNicknameViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_nickname, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        groupNicknameInput = view.findViewById(R.id.group_Nickname_input);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupNicknameViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    String newNickName = groupNicknameInput.getText().toString().trim();
                    viewModel.updateGroupNickName(
                            newNickName,
                            isSuccess -> {
                                if (isSuccess) {
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
                .getMyMemberInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupMemberInfo -> {
                            if (groupMemberInfo != null) {
                                groupNicknameInput.setText(groupMemberInfo.getNickname());
                                headComponent.setRightTextViewEnable(false);
                            }
                        });

        GroupMemberInfo groupMemberInfo = viewModel.getMyMemberInfoLiveData().getValue();
        if (groupMemberInfo != null) {
            groupNicknameInput.setText(groupMemberInfo.getNickname());
        }
        groupNicknameInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int length = s.length();
                        boolean isEnable = length >= 0 && length <= 256;
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(isEnable);
                        }
                    }
                });
    }
}
