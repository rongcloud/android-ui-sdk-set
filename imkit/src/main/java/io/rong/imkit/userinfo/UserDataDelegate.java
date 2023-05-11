package io.rong.imkit.userinfo;

import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.UserInfo;

public class UserDataDelegate {
    private UserDataProvider.UserInfoProvider mUserInfoProvider;
    private UserDataProvider.GroupInfoProvider mGroupInfoProvider;
    private UserDataProvider.GroupUserInfoProvider mGroupUserInfoProvider;

    UserDataDelegate() {}

    public void setUserInfoProvider(UserDataProvider.UserInfoProvider provider) {
        mUserInfoProvider = provider;
    }

    public void setGroupInfoProvider(UserDataProvider.GroupInfoProvider provider) {
        mGroupInfoProvider = provider;
    }

    public void setGroupUserInfoProvider(UserDataProvider.GroupUserInfoProvider provider) {
        mGroupUserInfoProvider = provider;
    }

    public UserInfo getUserInfo(String userId) {
        return mUserInfoProvider != null ? mUserInfoProvider.getUserInfo(userId) : null;
    }

    public GroupUserInfo getGroupUserInfo(String groupId, String userId) {
        return mGroupUserInfoProvider != null
                ? mGroupUserInfoProvider.getGroupUserInfo(groupId, userId)
                : null;
    }

    public io.rong.imlib.model.Group getGroupInfo(String groupId) {
        return mGroupInfoProvider != null ? mGroupInfoProvider.getGroupInfo(groupId) : null;
    }
}
