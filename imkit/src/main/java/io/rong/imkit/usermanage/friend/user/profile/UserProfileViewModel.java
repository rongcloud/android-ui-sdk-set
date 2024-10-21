package io.rong.imkit.usermanage.friend.user.profile;

import android.os.Bundle;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.model.UiUserDetail;
import io.rong.imkit.usermanage.handler.FriendInfoHandler;
import io.rong.imkit.usermanage.handler.UserProfileHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.listener.FriendEventListener;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.FriendRelationInfo;
import io.rong.imlib.model.FriendRelationType;
import io.rong.imlib.model.UserProfile;
import java.util.List;
import java.util.Map;

/**
 * 功能描述:
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class UserProfileViewModel extends BaseViewModel {

    private final MutableLiveData<UiUserDetail> mUserProfilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<ContactModel> mContactModelLiveData = new MutableLiveData<>();
    private final UserProfileHandler userProfileHandler;
    private final FriendInfoHandler friendInfoHandler;
    private UiUserDetail uiUserDetail;
    private String userId;

    protected boolean checkFriend = true;

    private final FriendEventListener listener =
            new FriendEventListener() {
                @Override
                public void onFriendAdd(
                        DirectionType directionType,
                        String userId,
                        String userName,
                        String portraitUri,
                        long operationTime) {
                    getUserProfile();
                }

                @Override
                public void onFriendDelete(
                        DirectionType directionType, List<String> userIds, long operationTime) {
                    getUserProfile();
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        String userId,
                        FriendApplicationType applicationType,
                        FriendApplicationStatus status,
                        DirectionType directionType,
                        long operationTime,
                        String extra) {
                    getUserProfile();
                }

                @Override
                public void onFriendCleared(long operationTime) {
                    getUserProfile();
                }

                @Override
                public void onFriendInfoChangedSync(
                        String userId,
                        String remark,
                        Map<String, String> extProfile,
                        long operationTime) {
                    getUserProfile();
                }
            };

    public UserProfileViewModel(Bundle args) {
        super(args);
        this.userId = args.getString(KitConstants.KEY_USER_ID);
        userProfileHandler = new UserProfileHandler();
        userProfileHandler.addDataChangeListener(
                UserProfileHandler.KEY_GET_USER_PROFILE,
                new OnDataChangeListener<UserProfile>() {
                    @Override
                    public void onDataChange(UserProfile profile) {
                        UserProfileViewModel.this.uiUserDetail =
                                new UiUserDetail(
                                        profile.getUserId(),
                                        profile.getName(),
                                        null,
                                        profile.getPortraitUri(),
                                        !checkFriend);
                        mUserProfilesLiveData.postValue(uiUserDetail);
                        mContactModelLiveData.postValue(
                                ContactModel.obtain(profile, ContactModel.ItemType.CONTENT));
                    }
                });
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_CHECK_FRIEND,
                new OnDataChangeListener<FriendRelationInfo>() {
                    @Override
                    public void onDataChange(FriendRelationInfo info) {
                        if (info.getRelationType() == FriendRelationType.BothWay
                                || info.getRelationType() == FriendRelationType.InMyFriendList) {
                            friendInfoHandler.getFriendInfo(info.getUserId());
                        } else {
                            userProfileHandler.getUserProfile(info.getUserId());
                        }
                    }
                });
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_GET_FRIEND,
                new OnDataChangeListener<FriendInfo>() {
                    @Override
                    public void onDataChange(FriendInfo info) {
                        UserProfileViewModel.this.uiUserDetail =
                                new UiUserDetail(
                                        info.getUserId(),
                                        info.getName(),
                                        info.getRemark(),
                                        info.getPortraitUri(),
                                        true);
                        mUserProfilesLiveData.postValue(uiUserDetail);
                        mContactModelLiveData.postValue(
                                ContactModel.obtain(info, ContactModel.ItemType.CONTENT));
                    }
                });
        IMCenter.getInstance().addFriendEventListener(listener);
    }

    public MutableLiveData<UiUserDetail> getUserProfilesLiveData() {
        return mUserProfilesLiveData;
    }

    public MutableLiveData<ContactModel> getContactModelLiveData() {
        return mContactModelLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        IMCenter.getInstance().removeFriendEventListener(listener);
        userProfileHandler.stop();
    }

    public UiUserDetail getUiUserDetail() {
        return uiUserDetail;
    }

    public void deleteFriend(OnDataChangeListener<Boolean> callback) {
        if (uiUserDetail != null) {
            friendInfoHandler.deleteFriend(uiUserDetail.getUserId(), callback);
        }
    }

    public void getUserProfile() {
        if (checkFriend) {
            friendInfoHandler.checkFriend(userId);
        } else {
            userProfileHandler.getUserProfile(userId);
        }
    }

    public void applyFriend(
            String remark, OnDataChangeListener<IRongCoreEnum.CoreErrorCode> callback) {
        if (uiUserDetail != null) {
            friendInfoHandler.applyFriend(uiUserDetail.getUserId(), remark, callback);
        }
    }
}
