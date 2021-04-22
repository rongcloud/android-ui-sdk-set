package io.rong.imkit.userinfo;


import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.UserInfo;

public class UserDataDelegate {
    private UserDataProvider.UserInfoProvider mUserInfoProvider;
    private UserDataProvider.GroupInfoProvider mGroupInfoProvider;
    private UserDataProvider.GroupUserInfoProvider mGroupUserInfoProvider;

    UserDataDelegate() {
    }

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
        if (mUserInfoProvider != null) {
            return mUserInfoProvider.getUserInfo(userId);
        }
        return null;
    }

    public GroupMember getGroupUserInfo(String groupId, String userId) {
        if (mGroupUserInfoProvider != null) {
            GroupUserInfo groupUserInfo = mGroupUserInfoProvider.getGroupUserInfo(groupId, userId);
            if (groupUserInfo != null) {
                return new GroupMember(groupUserInfo.getGroupId(), groupUserInfo.getUserId(), groupUserInfo.getNickname());
            }
        }
        return null;
    }

    public Group getGroupInfo(String groupId) {
        if (mGroupInfoProvider != null) {
            io.rong.imlib.model.Group group = mGroupInfoProvider.getGroupInfo(groupId);
            if (group != null) {
                return new Group(group.getId(), group.getName(), group.getPortraitUri().toString());
            }
        }
        return null;
    }

}
