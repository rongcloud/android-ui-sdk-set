package io.rong.imkit.userinfo;

import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

public class UserDataProvider {
    /**
     * /~chinese 用户信息的提供者。
     *
     * <p>如果在聊天中遇到的聊天对象是没有登录过的用户（即没有通过融云服务器鉴权过的），IMCenter 是不知道用户信息的，IMCenter 将调用此 Provider 获取用户信息。
     */

    /**
     * /~english The provider of user information. If the chat object encountered in the chat is a
     * user who has not logged in (that is, the), IMCenter that has not been authenticated through
     * the RongCloud cloud server does not know the user information, IMCenter will call this
     * Provider to obtain the user information.
     */
    public interface UserInfoProvider {
        /**
         * /~chinese 获取用户信息。
         *
         * @param userId 用户 Id。
         * @return 用户信息。
         */

        /**
         * /~english Get user information.
         *
         * @param userId User id
         * @return User information.
         */
        UserInfo getUserInfo(String userId);
    }

    /** /~chinese GroupUserInfo提供者。 */

    /** /~english GroupUserInfo provider. */
    public interface GroupUserInfoProvider {
        /**
         * /~chinese 获取GroupUserInfo。
         *
         * @param groupId 群组id。
         * @param userId 用户id。
         * @return GroupUserInfo。
         */

        /**
         * /~english Get the GroupUserInfo.
         *
         * @param groupId Group id
         * @param userId User id
         * @return GroupUserInfo
         */
        GroupUserInfo getGroupUserInfo(String groupId, String userId);
    }

    /**
     * /~chinese 群组信息的提供者。
     *
     * <p>IMCenter 本身不保存群组信息，如果在聊天中需要使用群组信息，IMCenter 将调用此 Provider 获取群组信息。
     */

    /**
     * /~english The provider of group information. IMCenter itself does not save the group
     * information. If you shall use the group information in the chat, IMCenter will call this
     * Provider to get the group information.
     */
    public interface GroupInfoProvider {
        /**
         * /~chinese 获取群组信息。
         *
         * @param groupId 群组 Id.
         * @return 群组信息。
         */

        /**
         * /~english Get group information.
         *
         * @param groupId Group Id
         * @return Group information
         */
        Group getGroupInfo(String groupId);
    }
}
