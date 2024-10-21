package io.rong.imkit.userinfo;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.userinfo.model.ExtendedGroup;
import io.rong.imkit.userinfo.model.ExtendedGroupUserInfo;
import io.rong.imkit.userinfo.model.ExtendedUserInfo;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.UserInfo;

/** @author rongcloud */
public class RongUserInfoManager {

    private static final String TAG = "RongUserInfoManager";
    private static final RongUserInfoManager sInstance = new RongUserInfoManager();

    private DataSourceType dataSourceType = DataSourceType.INFO_PROVIDER;
    private final UserInfoHelper userInfoHelper = new UserInfoHelper();
    private final UserManageHelper userManageHelper = new UserManageHelper();

    private boolean mIsUserInfoAttached;

    private RongUserInfoManager() {}

    public static RongUserInfoManager getInstance() {
        return sInstance;
    }

    /**
     * 切换用户信息数据源。
     *
     * @param dataSourceType 数据源类型。 默认为 {@link DataSourceType#INFO_PROVIDER}。
     */
    public void setDataSourceType(@NonNull DataSourceType dataSourceType) {
        if (dataSourceType == null) {
            return;
        }
        RLog.i(TAG, "setDataSourceType: " + dataSourceType.name());
        this.dataSourceType = dataSourceType;
    }

    /**
     * 获取用户信息数据源类型。
     *
     * @return 用户信息数据源类型。
     */
    @NonNull
    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    // 仅在 UserInfoHelper 中实现
    public void initAndUpdateUserDataBase(Context context) {
        userInfoHelper.initAndUpdateUserDataBase(context);
    }

    /**
     * 设置用户信息的提供者，供 UI 获取用户名称和头像信息。 各 ViewModel 会监听用户信息的变化，进行对应刷新。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。 在异步请求结果返回后，根据返回的结果调用 {@link
     * #refreshUserInfoCache(UserInfo)}} 刷新用户信息。
     *
     * @param userInfoProvider 用户信息提供者 {@link UserDataProvider.UserInfoProvider}。
     * @param isCacheUserInfo 设置是否由 IMKit 来缓存用户信息。<br>
     *     如果 App 提供的 UserInfoProvider。 每次都需要通过网络请求用户数据，而不是将用户数据缓存到本地，会影响用户信息的加载速度；<br>
     *     此时最好将本参数设置为 true，由 IMKit 来缓存用户信息。
     */
    public void setUserInfoProvider(
            UserDataProvider.UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        userInfoHelper.setUserInfoProvider(userInfoProvider, isCacheUserInfo);
    }

    /**
     * 设置群组信息提供者。
     *
     * @param groupInfoProvider 群组信息提供者。
     * @param isCacheGroupInfo 是否缓存群组信息。
     */
    public void setGroupInfoProvider(
            UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        userInfoHelper.setGroupInfoProvider(groupInfoProvider, isCacheGroupInfo);
    }

    public boolean isCacheUserOrGroupInfo() {
        return userInfoHelper.isCacheUserOrGroupInfo();
    }

    /**
     * 设置群成员提供者
     *
     * <p>可以使用此方法，修改群组中用户昵称
     *
     * <p>设置后，当 sdk 界面展示用户信息时，会回调 {@link
     * UserDataProvider.GroupUserInfoProvider#getGroupUserInfo(String, String)} 使用者只需要根据对应的 groupId,
     * userId 提供对应的用户信息 {@link GroupUserInfo}。 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupUserInfoCache(GroupUserInfo)} 刷新信息。
     *
     * @param groupUserInfoProvider 群组用户信息提供者。
     * @param isCacheGroupUserInfo 设置是否由 IMKit 来缓存 GroupUserInfo。<br>
     *     如果 App 提供的 GroupUserInfoProvider。 每次都需要通过网络请求数据，而不是将数据缓存到本地，会影响信息的加载速度；<br>
     *     此时最好将本参数设置为 true，由 IMKit 来缓存信息。
     */
    public void setGroupUserInfoProvider(
            UserDataProvider.GroupUserInfoProvider groupUserInfoProvider,
            boolean isCacheGroupUserInfo) {
        userInfoHelper.setGroupUserInfoProvider(groupUserInfoProvider, isCacheGroupUserInfo);
    }

    /**
     * 获取用户信息。
     *
     * @param userId 用户 Id。
     * @return 用户信息。
     */
    public UserInfo getUserInfo(final String userId) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getUserInfo(userId);
        } else {
            if (userManageHelper.isContainFromMessage(userId)) {
                return userManageHelper.getUserInfoFromMessage(userId);
            }
            return userManageHelper.getUserInfo(userId);
        }
    }

    /**
     * 获取群组信息。
     *
     * @param groupId 群组 Id。
     * @return 群组信息。
     */
    public io.rong.imlib.model.Group getGroupInfo(final String groupId) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getGroupInfo(groupId);
        } else {
            return ExtendedGroup.obtain(userManageHelper.getGroupInfo(groupId));
        }
    }

    /**
     * 获取群组用户信息。
     *
     * @param groupId 群组 Id。
     * @param userId 用户 Id。
     * @return 群组用户信息。
     */
    public GroupUserInfo getGroupUserInfo(final String groupId, final String userId) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getGroupUserInfo(groupId, userId);
        } else {
            ExtendedGroupUserInfo extendedGroupUserInfo =
                    ExtendedGroupUserInfo.obtain(
                            userManageHelper.getGroupUserInfo(groupId, userId));
            extendedGroupUserInfo.setGroupId(groupId);
            return extendedGroupUserInfo;
        }
    }

    /**
     * 获取当前用户信息。
     *
     * @return 当前用户信息。
     */
    public UserInfo getCurrentUserInfo() {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            return userInfoHelper.getCurrentUserInfo();
        } else {
            return userManageHelper.getCurrentUserInfo();
        }
    }

    /**
     * 设置当前用户信息。 如果开发者没有实现用户信息提供者，而是使用消息携带用户信息，需要使用这个方法设置当前用户的信息， 然后在{@link
     * IMCenter#init(Application, String, boolean)}之后调用{@link #setMessageAttachedUserInfo(boolean)}，
     * 这样可以在每条消息中携带当前用户的信息，IMKit会在接收到消息的时候取出用户信息并刷新到界面上。
     *
     * @param userInfo 当前用户信息。
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.setCurrentUserInfo(userInfo);
        } else {
            userManageHelper.setCurrentUserInfo((ExtendedUserInfo) userInfo);
        }
    }

    /**
     * 设置消息体内是否携带用户信息。
     *
     * @param state 是否携带用户信息，true 携带，false 不携带。
     */
    public void setMessageAttachedUserInfo(boolean state) {
        mIsUserInfoAttached = state;
    }

    /**
     * 获取当前用户关于消息体内是否携带用户信息的配置
     *
     * @return 是否携带用户信息
     */
    public boolean getUserInfoAttachedState() {
        return mIsUserInfoAttached;
    }

    /**
     * 添加用户信息观察者。
     *
     * @param observer 用户信息观察者。
     */
    public void addUserDataObserver(UserDataObserver observer) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.addUserDataObserver(observer);
        } else {
            userManageHelper.addUserDataObserver(observer);
        }
    }

    /**
     * 移除用户信息观察者。
     *
     * @param observer 用户信息观察者。
     */
    public void removeUserDataObserver(UserDataObserver observer) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.removeUserDataObserver(observer);
        } else {
            userManageHelper.removeUserDataObserver(observer);
        }
    }

    /**
     * 刷新用户信息缓存。
     *
     * @param userInfo 用户信息。
     */
    public void refreshUserInfoCache(UserInfo userInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.refreshUserInfoCache(userInfo);
        } else {
            userManageHelper.refreshUserInfoCache((ExtendedUserInfo) userInfo);
        }
    }

    /**
     * 刷新群组信息缓存。
     *
     * @param groupInfo 群组信息。
     */
    public void refreshGroupInfoCache(io.rong.imlib.model.Group groupInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.refreshGroupInfoCache(groupInfo);
        } else {
            userManageHelper.refreshGroupInfoCache(((ExtendedGroup) groupInfo).toGroupInfo());
        }
    }

    /**
     * 刷新群组用户信息缓存。
     *
     * @param groupUserInfo 群组用户信息。
     */
    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        if (dataSourceType == DataSourceType.INFO_PROVIDER) {
            userInfoHelper.refreshGroupUserInfoCache(groupUserInfo);
        } else {
            userManageHelper.refreshGroupUserInfoCache(
                    groupUserInfo.getGroupId(),
                    ((ExtendedGroupUserInfo) groupUserInfo).toGroupMemberInfo());
        }
    }

    /**
     * 获取用户显示名称。
     *
     * @param userInfo 用户信息。
     * @return 用户显示名称。
     */
    public String getUserDisplayName(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }

        return TextUtils.isEmpty(userInfo.getAlias()) ? userInfo.getName() : userInfo.getAlias();
    }

    /**
     * 获取用户显示名称。
     *
     * @param user 用户信息。
     * @return 用户显示名称。
     */
    public String getUserDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return TextUtils.isEmpty(user.alias) ? user.name : user.alias;
    }

    /**
     * 获取用户显示名称。
     *
     * @param userInfo 用户信息。
     * @param groupMemberName 群组成员名称。
     * @return 用户显示名称。
     */
    public String getUserDisplayName(UserInfo userInfo, String groupMemberName) {
        if (userInfo == null) {
            return groupMemberName == null ? "" : groupMemberName;
        }
        if (!TextUtils.isEmpty(userInfo.getAlias())) {
            return userInfo.getAlias();
        } else if (!TextUtils.isEmpty(groupMemberName)) {
            return groupMemberName;
        } else {
            return userInfo.getName();
        }
    }

    public interface UserDataObserver {
        /**
         * 用户信息发生变更时的回调方法。
         *
         * @param info 变更后的用户信息。
         */
        void onUserUpdate(UserInfo info);

        /**
         * 群组信息发生变更时的回调方法。
         *
         * @param group 变更后的群信息。
         */
        void onGroupUpdate(io.rong.imlib.model.Group group);

        /**
         * 群昵称信息发生变更时的回调方法。
         *
         * @param groupUserInfo 变更后的群昵称信息。
         */
        void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo);
    }

    /** 数据源类型。 */
    public enum DataSourceType {
        /** 用户信息提供者 */
        INFO_PROVIDER,
        /** 用户管理 */
        INFO_MANAGEMENT
    }
}
