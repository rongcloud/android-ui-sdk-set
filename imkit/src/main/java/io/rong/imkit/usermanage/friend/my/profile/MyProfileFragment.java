package io.rong.imkit.usermanage.friend.my.profile;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.friend.my.gender.UpdateGenderActivity;
import io.rong.imkit.usermanage.friend.my.nikename.UpdateNickNameActivity;
import io.rong.imlib.model.UserProfile;

/**
 * 我的资料页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class MyProfileFragment extends BaseViewModelFragment<MyProfileViewModel>
        implements View.OnClickListener {
    protected HeadComponent headComponent;
    @NonNull View rootView;

    @NonNull
    @Override
    protected MyProfileViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory()).get(MyProfileViewModel.class);
    }

    /**
     * View 创建之后
     *
     * @param viewModel VM
     */
    @Override
    protected void onViewReady(@NonNull MyProfileViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        viewModel
                .getUserProfilesLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        userProfile -> {
                            // 更新UI
                            notifyUserProfileChanged(userProfile);
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        getViewModel().loadMyUserProfile();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        rootView = inflater.inflate(R.layout.rc_page_my_profile, container, false);
        headComponent = rootView.findViewById(R.id.rc_head_component);
        rootView.findViewById(R.id.cl_head).setOnClickListener(this);
        rootView.findViewById(R.id.cl_nickname).setOnClickListener(this);
        rootView.findViewById(R.id.cl_app).setOnClickListener(this);
        rootView.findViewById(R.id.cl_gender).setOnClickListener(this);
        return rootView;
    }

    protected void notifyUserProfileChanged(@NonNull UserProfile userProfile) {
        // 更新UI
        setText(R.id.tv_nickname_content, userProfile.getName());
        setText(R.id.tv_app_content, userProfile.getUniqueId());
        int nGender = userProfile.getGender();
        String sGender = getString(R.string.rc_unknow_type);
        if (nGender == 1) {
            sGender = getString(R.string.rc_gender_man);
        } else if (nGender == 2) {
            sGender = getString(R.string.rc_gender_female);
        }
        setText(R.id.tv_gender_content, sGender);
        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadUserPortrait(
                        rootView.getContext(),
                        userProfile.getPortraitUri(),
                        rootView.<ImageView>findViewById(R.id.iv_head));
    }

    private void setText(@IdRes int id, String text) {
        TextView view = rootView.findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cl_head) {
            onUserHeaderClick(v);
        } else if (id == R.id.cl_nickname) {
            onNickNameClick(v);
        } else if (id == R.id.cl_gender) {
            onGenderClick(v);
        }
    }

    /**
     * 用户头像点击
     *
     * @param view View
     */
    protected void onUserHeaderClick(View view) {}

    /**
     * 昵称点击
     *
     * @param view View
     */
    protected void onNickNameClick(View view) {
        UserProfile value = getViewModel().getUserProfilesLiveData().getValue();
        if (value != null) {
            startActivity(UpdateNickNameActivity.newIntent(getActivity(), value));
        }
    }

    /**
     * 性别点击
     *
     * @param view View
     */
    protected void onGenderClick(View view) {
        UserProfile value = getViewModel().getUserProfilesLiveData().getValue();
        if (value != null) {
            startActivity(UpdateGenderActivity.newIntent(getActivity(), value));
        }
    }
}
