package io.rong.imkit.usermanage.friend.my.nikename;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import io.rong.imkit.widget.dialog.TipLoadingDialog;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.UserProfile;
import java.util.List;

/**
 * 更新昵称页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UpdateNickNameFragment extends BaseViewModelFragment<UpdateNickNameViewModel> {
    protected EditText etContent;
    protected TextView tvTitle;
    protected HeadComponent headComponent;
    protected UserProfile userProfile;
    protected FriendInfo friendInfo;
    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected UpdateNickNameViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(UpdateNickNameViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_update_nickname, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        tvTitle = view.findViewById(R.id.tv_title);
        etContent = view.findViewById(R.id.et_content);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull UpdateNickNameViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        userProfile = getArguments().getParcelable(KitConstants.KEY_USER_PROFILER);
        friendInfo = getArguments().getParcelable(KitConstants.KEY_FRIEND_INFO);
        if (userProfile != null) {
            headComponent.setTitleText(R.string.rc_set_nick_name);
            etContent.setHint(R.string.rc_nickname_hint);
            tvTitle.setText(R.string.rc_nickname_label);
        }
        if (friendInfo != null) {
            headComponent.setTitleText(R.string.rc_set_remark_name);
            etContent.setHint(R.string.rc_friend_nickname_hint);
            tvTitle.setText(R.string.rc_remark);
        }
        headComponent.setRightClickListener(
                v -> {
                    String name = etContent.getText().toString();
                    if (userProfile != null) {
                        userProfile.setName(name);
                        showLoadingDialog();
                        getViewModel()
                                .updateUserProfile(
                                        userProfile,
                                        new OnDataChangeEnhancedListener<Boolean>() {
                                            @Override
                                            public void onDataChange(Boolean isSuccess) {
                                                dismissLoadingDialog();
                                                if (isSuccess) {
                                                    finishActivity();
                                                }
                                            }

                                            @Override
                                            public void onDataError(
                                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                                    List<String> errorMsgs) {
                                                dismissLoadingDialog();
                                                String tips = getString(R.string.rc_set_failed);
                                                if (coreErrorCode
                                                        == IRongCoreEnum.CoreErrorCode
                                                                .SERVICE_INFORMATION_AUDIT_FAILED) {
                                                    tips =
                                                            getString(
                                                                    R.string
                                                                            .rc_content_contain_sensitive);
                                                }
                                                ToastUtils.show(
                                                        getContext(), tips, Toast.LENGTH_SHORT);
                                            }
                                        });
                    } else if (friendInfo != null) {
                        showLoadingDialog();
                        viewModel.setFriendInfo(
                                friendInfo.getUserId(),
                                name,
                                new OnDataChangeEnhancedListener<Boolean>() {
                                    @Override
                                    public void onDataChange(Boolean isSuccess) {
                                        dismissLoadingDialog();
                                        if (isSuccess) {
                                            finishActivity();
                                        }
                                    }

                                    @Override
                                    public void onDataError(
                                            IRongCoreEnum.CoreErrorCode coreErrorCode,
                                            List<String> errorMsgs) {
                                        dismissLoadingDialog();
                                        String tips = getString(R.string.rc_set_failed);
                                        if (coreErrorCode
                                                == IRongCoreEnum.CoreErrorCode
                                                        .SERVICE_INFORMATION_AUDIT_FAILED) {
                                            tips = getString(R.string.rc_content_contain_sensitive);
                                        }
                                        ToastUtils.show(getContext(), tips, Toast.LENGTH_SHORT);
                                    }
                                });
                    }
                });

        etContent.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        updateConfirmEnable();
                    }
                });

        String nickName =
                userProfile != null
                        ? userProfile.getName()
                        : friendInfo != null ? friendInfo.getRemark() : "";
        etContent.setText(nickName);
    }

    private void updateConfirmEnable() {
        if (userProfile != null) {
            if (!TextUtils.isEmpty(etContent.getText())) {
                headComponent.setRightTextViewEnable(true);
            } else {
                headComponent.setRightTextViewEnable(false);
            }
        } else {
            headComponent.setRightTextViewEnable(true);
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
