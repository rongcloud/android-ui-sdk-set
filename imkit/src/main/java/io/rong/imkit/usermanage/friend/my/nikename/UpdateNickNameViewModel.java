package io.rong.imkit.usermanage.friend.my.nikename;

import android.os.Bundle;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.UserProfileOperationsHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.model.UserProfile;

/**
 * 更新昵称页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UpdateNickNameViewModel extends BaseViewModel {
    private final UserProfileOperationsHandler userProfileOperationsHandler;

    public UpdateNickNameViewModel(Bundle args) {
        super(args);
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

    @Deprecated
    public void setFriendInfo(
            final String userId, final String remark, OnDataChangeListener<Boolean> listener) {
        userProfileOperationsHandler.replaceDataChangeListener(
                UserProfileOperationsHandler.KEY_SET_FRIEND_INFO, listener);
        userProfileOperationsHandler.setFriendInfo(userId, remark, null);
    }

    public void setFriendInfo(
            final String userId,
            final String remark,
            OnDataChangeEnhancedListener<Boolean> listener) {
        userProfileOperationsHandler.replaceDataChangeListener(
                UserProfileOperationsHandler.KEY_SET_FRIEND_INFO_EXMAINE, listener);
        userProfileOperationsHandler.setFriendInfoExamine(userId, remark, null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userProfileOperationsHandler.stop();
    }
}
