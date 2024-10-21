package io.rong.imkit.usermanage.friend.my.gender;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.widget.SettingItemView;

/**
 * 功能描述:
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class UpdateGenderFragment extends BaseViewModelFragment<UpdateGenderViewModel>
        implements View.OnClickListener {
    protected SettingItemView manSiv;
    protected SettingItemView femaleSiv;
    protected HeadComponent headComponent;

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
                        getViewModel()
                                .updateUserProfile(
                                        getViewModel().getUserProfile(),
                                        isSuccess -> {
                                            if (isSuccess) {
                                                finishActivity();
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
}
