package io.rong.imkit.usermanage.friend.my.gender;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imkit.widget.dialog.TipLoadingDialog;
import io.rong.imlib.IRongCoreEnum;
import java.util.List;

/**
 * @since 5.12.0
 * @author rongcloud
 * @since 5.12.0
 */
public class UpdateGenderFragment extends BaseViewModelFragment<UpdateGenderViewModel>
        implements View.OnClickListener {
    protected SettingItemView manSiv;
    protected SettingItemView femaleSiv;
    protected HeadComponent headComponent;

    private TipLoadingDialog dialog;

    @NonNull
    @Override
    protected UpdateGenderViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(UpdateGenderViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_update_gender, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        manSiv = view.findViewById(R.id.siv_gender_man);
        manSiv.setOnClickListener(this);
        femaleSiv = view.findViewById(R.id.siv_gender_female);
        femaleSiv.setOnClickListener(this);

        return view;
    }

    @Override
    protected void onViewReady(@NonNull UpdateGenderViewModel viewModel) {
        int gender = viewModel.getUserProfile().getGender();
        if (gender == 1) {
            manSiv.setRightImageVisibility(View.VISIBLE);
            femaleSiv.setRightImageVisibility(View.GONE);
        } else if (gender == 2) {
            manSiv.setRightImageVisibility(View.GONE);
            femaleSiv.setRightImageVisibility(View.VISIBLE);
        } else {
            manSiv.setRightImageVisibility(View.GONE);
            femaleSiv.setRightImageVisibility(View.GONE);
        }

        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    if (getViewModel().getUserProfile().getUserProfile() != null) {
                        showLoadingDialog();
                        getViewModel()
                                .updateUserProfile(
                                        getViewModel().getUserProfile(),
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
                                                                .RC_SERVICE_INFORMATION_AUDIT_FAILED) {
                                                    tips =
                                                            getString(
                                                                    R.string
                                                                            .rc_content_contain_sensitive);
                                                }
                                                ToastUtils.show(
                                                        getContext(), tips, Toast.LENGTH_SHORT);
                                            }
                                        });
                    }
                });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.siv_gender_man) {
            manSiv.setRightImageVisibility(View.VISIBLE);
            femaleSiv.setRightImageVisibility(View.GONE);
            getViewModel().getUserProfile().setGender(1);
        } else if (id == R.id.siv_gender_female) {
            manSiv.setRightImageVisibility(View.GONE);
            femaleSiv.setRightImageVisibility(View.VISIBLE);
            getViewModel().getUserProfile().setGender(2);
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
