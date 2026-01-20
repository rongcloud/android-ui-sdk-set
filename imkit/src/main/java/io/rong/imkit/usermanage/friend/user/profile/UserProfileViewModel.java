package io.rong.imkit.usermanage.friend.user.profile;

import android.os.Bundle;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.model.UiUserDetail;
import io.rong.imkit.usermanage.handler.FriendInfoHandler;
import io.rong.imkit.usermanage.handler.GroupInfoHandler;
import io.rong.imkit.usermanage.handler.UserProfileHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.listener.FriendEventListener;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.FriendRelationInfo;
import io.rong.imlib.model.FriendRelationType;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberInfoEditPermission;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.UserProfile;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户资料页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UserProfileViewModel extends BaseViewModel {

    private final MutableLiveData<UiUserDetail> mUserProfilesLiveData = new MutableLiveData<>();
    private final MutableLiveData<ContactModel> mContactModelLiveData = new MutableLiveData<>();
    private final MutableLiveData<GroupMemberInfo> myGroupMemberInfoLiveData =
            new MutableLiveData<>();
    private final MutableLiveData<GroupInfo> groupInfoLiveData = new MutableLiveData<>();

    private final UserProfileHandler userProfileHandler;
    private final FriendInfoHandler friendInfoHandler;
    private GroupInfoHandler groupInfoHandler;

    private final String userId;
    private boolean checkFriend = true;

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

    public UserProfileViewModel(Bundle arguments) {
        super(arguments);
        this.userId = arguments.getString(KitConstants.KEY_USER_ID);

        ConversationIdentifier conversationIdentifier =
                arguments.getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);

        if (conversationIdentifier != null) {
            groupInfoHandler = new GroupInfoHandler(conversationIdentifier);
            groupInfoHandler.addDataChangeListener(
                    GroupInfoHandler.KEY_GET_GROUP_MEMBERS,
                    new SafeDataHandler<List<GroupMemberInfo>>() {
                        @Override
                        public void onDataChange(List<GroupMemberInfo> groupMemberInfos) {
                            if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                                myGroupMemberInfoLiveData.postValue(groupMemberInfos.get(0));
                            }
                        }
                    });
            groupInfoHandler.addDataChangeListener(
                    GroupInfoHandler.KEY_GROUP_INFO,
                    new SafeDataHandler<GroupInfo>() {
                        @Override
                        public void onDataChange(GroupInfo groupInfo) {
                            groupInfoLiveData.postValue(groupInfo);
                            if (groupInfoHandler != null) {
                                groupInfoHandler.getGroupMembers(Arrays.asList(userId));
                            }
                        }
                    });
        }

        userProfileHandler = new UserProfileHandler();
        userProfileHandler.addDataChangeListener(
                UserProfileHandler.KEY_GET_USER_PROFILE,
                new OnDataChangeListener<UserProfile>() {
                    @Override
                    public void onDataChange(UserProfile profile) {
                        UiUserDetail uiUserDetail =
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
                        UiUserDetail uiUserDetail =
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

    public MutableLiveData<GroupMemberInfo> getMyGroupMemberInfoLiveData() {
        return myGroupMemberInfoLiveData;
    }

    boolean hasEditPermission() {
        GroupInfo groupInfo = groupInfoLiveData.getValue();
        if (groupInfo != null) {
            GroupMemberInfoEditPermission editPermission = groupInfo.getMemberInfoEditPermission();
            GroupMemberRole role = groupInfo.getRole();
            if (editPermission == GroupMemberInfoEditPermission.OwnerOrSelf
                    && role == GroupMemberRole.Owner) {
                return true;
            }
            if (editPermission == GroupMemberInfoEditPermission.OwnerOrManagerOrSelf
                    && (role == GroupMemberRole.Owner || role == GroupMemberRole.Manager)) {
                return true;
            }
        }
        return false;
    }

    public UiUserDetail getUiUserDetail() {
        return mUserProfilesLiveData.getValue();
    }

    public void deleteFriend(OnDataChangeListener<Boolean> callback) {
        if (getUiUserDetail() != null) {
            friendInfoHandler.deleteFriend(getUiUserDetail().getUserId(), callback);
        }
    }

    public void getUserProfile() {
        if (checkFriend) {
            friendInfoHandler.checkFriend(userId);
        } else {
            userProfileHandler.getUserProfile(userId);
        }

        if (groupInfoHandler != null) {
            groupInfoHandler.getGroupsInfo();
        }
    }

    public void applyFriend(
            String remark, OnDataChangeListener<IRongCoreEnum.CoreErrorCode> callback) {
        if (getUiUserDetail() != null) {
            friendInfoHandler.applyFriend(getUiUserDetail().getUserId(), remark, callback);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        IMCenter.getInstance().removeFriendEventListener(listener);
        userProfileHandler.stop();
        if (groupInfoHandler != null) {
            groupInfoHandler.stop();
        }
    }
}
