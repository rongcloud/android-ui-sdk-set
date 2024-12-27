package io.rong.imkit.usermanage.friend.my.profile;

import android.os.Bundle;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.UserProfileHandler;
import io.rong.imkit.usermanage.handler.UserProfileOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.model.UserProfile;

/**
 * 我的资料页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class MyProfileViewModel extends BaseViewModel {

    private final MutableLiveData<UserProfile> mUserProfilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mUpdateUserProfileLiveData = new MutableLiveData<>();
    private final UserProfileHandler userProfileHandler;
    private final UserProfileOperationsHandler userProfileOperationsHandler;

    private UserProfile userProfile;

    public MyProfileViewModel(Bundle bundle) {
        super(bundle);
        userProfileHandler = new UserProfileHandler();
        userProfileHandler.addDataChangeListener(
                UserProfileHandler.KEY_GET_MY_USER_PROFILE,
                new OnDataChangeListener<UserProfile>() {
                    @Override
                    public void onDataChange(UserProfile userProfile) {
                        MyProfileViewModel.this.userProfile = userProfile;
                        mUserProfilesLiveData.postValue(userProfile);
                    }
                });
        userProfileOperationsHandler = new UserProfileOperationsHandler();
        userProfileOperationsHandler.addDataChangeListener(
                UserProfileOperationsHandler.KEY_UPDATE_MY_USER_PROFILE,
                result -> mUpdateUserProfileLiveData.postValue(result));
    }

    public MutableLiveData<UserProfile> getUserProfilesLiveData() {
        return mUserProfilesLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userProfileHandler.stop();
    }

    public void loadMyUserProfile() {
        userProfileHandler.getMyUserProfile();
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }
}
