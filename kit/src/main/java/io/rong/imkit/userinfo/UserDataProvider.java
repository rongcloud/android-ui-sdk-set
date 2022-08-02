package io.rong.imkit.userinfo;

import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

public class UserDataProvider {
    /**
     * 用户信息的提供者。
     * <p/>
     * 如果在聊天中遇到的聊天对象是没有登录过的用户（即没有通过融云服务器鉴权过的），IMCenter 是不知道用户信息的，IMCenter 将调用此
     * Provider 获取用户信息。
     */
    public interface UserInfoProvider {
        /**
         * 获取用户信息。
         *
         * @param userId 用户 Id。
         * @return 用户信息。
         */
        UserInfo getUserInfo(String userId);
    }

    /**
     * GroupUserInfo提供者。
     */
    public interface GroupUserInfoProvider {
        /**
         * 获取GroupUserInfo。
         *
         * @param groupId 群组id。
         * @param userId  用户id。
         * @return GroupUserInfo。
         */
        GroupUserInfo getGroupUserInfo(String groupId, String userId);
    }


    /**
     * 群组信息的提供者。
     * <p/>
     * IMCenter 本身不保存群组信息，如果在聊天中需要使用群组信息，IMCenter 将调用此 Provider 获取群组信息。
     */
    public interface GroupInfoProvider {
        /**
         * 获取群组信息。
         *
         * @param groupId 群组 Id.
         * @return 群组信息。
         */
        Group getGroupInfo(String groupId);
    }
}
