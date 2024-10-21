package io.rong.imkit.usermanage.group.name;

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
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.GroupInfo;

/**
 * 功能描述: 创建增加群联系人页面
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupNameFragment extends BaseViewModelFragment<GroupNameViewModel> {

    protected HeadComponent headComponent;
    private EditText groupNameInput;

    @NonNull
    @Override
    protected GroupNameViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupNameViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_name, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        groupNameInput = view.findViewById(R.id.group_name_input);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupNameViewModel viewModel) {
        GroupInfo groupInfo = getArguments().getParcelable(KitConstants.KEY_GROUP_INFO);
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightTextViewEnable(false);
        headComponent.setRightClickListener(
                v -> {
                    if (groupInfo != null) {
                        String groupName = groupNameInput.getText().toString().trim();
                        groupInfo.setGroupName(groupName);
                        viewModel.updateGroupInfo(
                                groupInfo,
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
                    }
                });

        if (groupInfo != null) {
            groupNameInput.setText(groupInfo.getGroupName());
        }
        groupNameInput.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int length = s.length();
                        boolean isEnable = length >= 1 && length <= 64;
                        if (headComponent != null) {
                            headComponent.setRightTextViewEnable(isEnable);
                        }
                    }
                });
    }
}
