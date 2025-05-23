package io.rong.imkit.usermanage.friend.my.gender;

import android.os.Bundle;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.UserProfileOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.UserProfile;

/**
 * 更新性别页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UpdateGenderViewModel extends BaseViewModel {
    private final UserProfileOperationsHandler userProfileOperationsHandler;
    private UserProfile userProfile;

    public UpdateGenderViewModel(Bundle bundle) {
        super(bundle);
        this.userProfile = bundle.getParcelable(KitConstants.KEY_USER_PROFILER);
        userProfileOperationsHandler = new UserProfileOperationsHandler();
    }

    @Deprecated
    public void updateUserProfile(UserProfile userProfile, OnDataChangeListener<Boolean> listener) {
        userProfileOperationsHandler.replaceDataChangeListener(
                UserProfileOperationsHandler.KEY_UPDATE_MY_USER_PROFILE, listener);
        userProfileOperationsHandler.updateMyUserProfile(userProfile);
    }

    public void updateUserProfile(
            UserProfile userProfile, OnDataChangeEnhancedListener<Boolean> listener) {
        userProfileOperationsHandler.replaceDataChangeListener(
                UserProfileOperationsHandler.KEY_UPDATE_MY_USER_PROFILE_EXAMINE, listener);
        userProfileOperationsHandler.updateMyUserProfileExamine(userProfile);
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userProfileOperationsHandler.stop();
    }
}
