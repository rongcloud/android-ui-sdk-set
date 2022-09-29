package io.rong.imkit.userinfo;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import io.rong.common.RLog;
import io.rong.common.utils.function.Action0;
import io.rong.common.utils.function.Action1;
import io.rong.common.utils.function.Func0;
import io.rong.common.utils.function.Func1;
import io.rong.common.utils.optional.Option;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RongUserInfoManager {
    private final String TAG = RongUserInfoManager.class.getSimpleName();
    private UserDataDelegate mUserDataDelegate;
    private CacheDataSource cacheDataSource;
    private DbDataSource dbDataSource;
    private UserInfo mCurrentUserInfo;
    private boolean mIsUserInfoAttached;
    private boolean isCacheUserInfo = true;
    private boolean isCacheGroupInfo = true;
    private boolean isCacheGroupMemberInfo = true;
    private List<UserDataObserver> mUserDataObservers;
    private Context context;

    private RongUserInfoManager() {
        mUserDataDelegate = new UserDataDelegate();
        mUserDataObservers = new ArrayList<>();
        cacheDataSource = new CacheDataSource();
        IMCenter.getInstance()
                .addOnReceiveMessageListener(
                        new RongIMClient.OnReceiveMessageWrapperListener() {
                            @Override
                            public boolean onReceived(
                                    Message message, int i, boolean b, boolean b1) {
                                if (message != null
                                        && message.getContent() != null
                                        && message.getContent().getUserInfo() != null
                                        && RongUserInfoManager.getInstance().getUserDatabase()
                                                != null) {
                                    final UserInfo userInfo = message.getContent().getUserInfo();
                                    UserInfo oldUserInfo = getUserInfo(userInfo.getUserId());
                                    // 如果新旧信息相同，则不刷新用户信息
                                    if (oldUserInfo != null
                                            && Objects.equals(
                                                    oldUserInfo.getName(), userInfo.getName())
                                            && Objects.equals(
                                                    oldUserInfo.getPortraitUri(),
                                                    userInfo.getPortraitUri())
                                            && Objects.equals(
                                                    oldUserInfo.getAlias(), userInfo.getAlias())
                                            && Objects.equals(
                                                    oldUserInfo.getExtra(), userInfo.getExtra())) {
                                        return false;
                                    }
                                    refreshUserInfoCache(userInfo);
                                }
                                return false;
                            }
                        });
    }

    public @Nullable UserDatabase getUserDatabase() {
        return dbDataSource == null ? null : dbDataSource.getDatabase();
    }

    public static RongUserInfoManager getInstance() {
        return SingleTonHolder.sInstance;
    }

    /**
     * 初始化并打开用户信息数据库
     *
     * @param context
     */
    public void initAndUpdateUserDataBase(Context context) {
        this.context = context;
        dbDataSource =
                new DbDataSource(
                        context,
                        RongIMClient.getInstance().getCurrentUserId(),
                        new RoomDatabase.Callback() {
                            @Override
                            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                super.onCreate(db);
                            }

                            @Override
                            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                super.onOpen(db);
                                if (RongConfigCenter.featureConfig().isPreLoadUserCache()) {
                                    preLoadUserCache();
                                }
                            }

                            @Override
                            public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                super.onDestructiveMigration(db);
                            }
                        });
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
        mUserDataDelegate.setUserInfoProvider(userInfoProvider);
        this.isCacheUserInfo = isCacheUserInfo;
    }

    public void setGroupInfoProvider(
            UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        mUserDataDelegate.setGroupInfoProvider(groupInfoProvider);
        this.isCacheGroupInfo = isCacheGroupInfo;
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
        mUserDataDelegate.setGroupUserInfoProvider(groupUserInfoProvider);
        isCacheGroupMemberInfo = isCacheGroupUserInfo;
    }

    public UserInfo getUserInfo(final String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        // 先拿一级缓存
        return Option.ofObj(cacheDataSource.getUserInfo(userId))
                .map(
                        new Func1<User, UserInfo>() {
                            @Override
                            public UserInfo call(User user) {
                                // 一级缓存有值，直接返回 userInfo
                                return transformUser(user);
                            }
                        })
                .orDefault(
                        new Func0<UserInfo>() {
                            @Override
                            public UserInfo call() {
                                // 一级缓存无值
                                if (isCacheUserInfo) {
                                    // 二级缓存
                                    getDbUserInfo(userId);
                                    return null;
                                } else {
                                    UserInfo userInfo = mUserDataDelegate.getUserInfo(userId);
                                    if (userInfo != null) {
                                        saveUserInfoCache(userInfo);
                                    }
                                    return userInfo;
                                }
                            }
                        });
    }

    private void getDbUserInfo(final String userId) {
        if (dbDataSource == null) {
            UserInfo userInfo = mUserDataDelegate.getUserInfo(userId);
            if (userInfo != null) {
                refreshUserInfoCache(userInfo);
            }
            return;
        }
        dbDataSource.getUserInfo(
                userId,
                new Consumer<User>() {
                    @Override
                    public void accept(User user) {
                        Option.ofObj(user)
                                .ifSome(
                                        new Action1<User>() {
                                            @Override
                                            public void call(User user) {
                                                // 缓存内存
                                                cacheDataSource.refreshUserInfo(user);
                                                notifyUserChange(transformUser(user));
                                            }
                                        })
                                .ifNone(
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                UserInfo userInfo =
                                                        mUserDataDelegate.getUserInfo(userId);
                                                if (userInfo != null) {
                                                    refreshUserInfoCache(userInfo);
                                                }
                                            }
                                        });
                    }
                });
    }

    private void notifyUserChange(final UserInfo userInfo) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            for (UserDataObserver item : mUserDataObservers) {
                item.onUserUpdate(userInfo);
            }
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyUserChange(userInfo);
                                }
                            });
        }
    }

    private UserInfo transformUser(User user) {
        int drawableId = R.drawable.rc_default_portrait;
        String url = user.portraitUrl;
        Uri portraitUri;
        if (!TextUtils.isEmpty(url)) {
            portraitUri = Uri.parse(url);
        } else if (context != null) {
            portraitUri = RongUtils.getUriFromDrawableRes(context, drawableId);
        } else if (IMCenter.getInstance().getContext() != null) {
            portraitUri =
                    RongUtils.getUriFromDrawableRes(
                            IMCenter.getInstance().getContext(), drawableId);
        } else {
            portraitUri = Uri.parse("");
        }
        UserInfo userInfo = new UserInfo(user.id, user.name == null ? "" : user.name, portraitUri);
        userInfo.setAlias(user.alias);
        userInfo.setExtra(user.extra);
        return userInfo;
    }

    public io.rong.imlib.model.Group getGroupInfo(final String groupId) {
        if (TextUtils.isEmpty(groupId)) {
            return null;
        }
        // 先拿一级缓存
        return Option.ofObj(cacheDataSource.getGroupInfo(groupId))
                .map(
                        new Func1<Group, io.rong.imlib.model.Group>() {
                            @Override
                            public io.rong.imlib.model.Group call(Group group) {
                                // 一级缓存有值，直接返回 userInfo
                                return transformGroup(group);
                            }
                        })
                .orDefault(
                        new Func0<io.rong.imlib.model.Group>() {
                            @Override
                            public io.rong.imlib.model.Group call() {
                                // 一级缓存无值
                                if (isCacheGroupInfo) {
                                    // 二级缓存
                                    getDbGroupInfo(groupId);
                                    return null;
                                } else {
                                    io.rong.imlib.model.Group groupInfo =
                                            mUserDataDelegate.getGroupInfo(groupId);
                                    if (groupInfo != null) {
                                        saveGroupInfoCache(groupInfo);
                                    }
                                    return groupInfo;
                                }
                            }
                        });
    }

    private io.rong.imlib.model.Group transformGroup(@NonNull Group group) {
        return new io.rong.imlib.model.Group(group.id, group.name, Uri.parse(group.portraitUrl));
    }

    private void getDbGroupInfo(final String groupId) {
        if (dbDataSource == null) {
            io.rong.imlib.model.Group groupInfo = mUserDataDelegate.getGroupInfo(groupId);
            if (groupInfo != null) {
                refreshGroupInfoCache(groupInfo);
            }
            return;
        }
        dbDataSource.getGroupInfo(
                groupId,
                new Consumer<Group>() {
                    @Override
                    public void accept(Group group) {
                        Option.ofObj(group)
                                .ifSome(
                                        new Action1<Group>() {
                                            @Override
                                            public void call(Group group) {
                                                // 缓存内存
                                                cacheDataSource.refreshGroupInfo(group);
                                                notifyGroupChange(transformGroup(group));
                                            }
                                        })
                                .ifNone(
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                io.rong.imlib.model.Group groupInfo =
                                                        mUserDataDelegate.getGroupInfo(groupId);
                                                if (groupInfo != null) {
                                                    refreshGroupInfoCache(groupInfo);
                                                }
                                            }
                                        });
                    }
                });
    }

    private void notifyGroupChange(final io.rong.imlib.model.Group group) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            for (UserDataObserver item : mUserDataObservers) {
                item.onGroupUpdate(group);
            }
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyGroupChange(group);
                                }
                            });
        }
    }

    public GroupUserInfo getGroupUserInfo(final String groupId, final String userId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(userId)) {
            return null;
        }
        // 先拿一级缓存
        return Option.ofObj(cacheDataSource.getGroupUserInfo(groupId, userId))
                .map(
                        new Func1<GroupMember, GroupUserInfo>() {
                            @Override
                            public GroupUserInfo call(GroupMember groupMember) {
                                // 一级缓存有值，直接返回 userInfo
                                return transformGroupMember(groupMember);
                            }
                        })
                .orDefault(
                        new Func0<GroupUserInfo>() {
                            @Override
                            public GroupUserInfo call() {
                                // 一级缓存无值
                                if (isCacheGroupMemberInfo) {
                                    // 二级缓存
                                    getDbGroupUserInfo(groupId, userId);
                                    return null;
                                } else {
                                    GroupUserInfo groupUserInfo =
                                            mUserDataDelegate.getGroupUserInfo(groupId, userId);
                                    if (groupUserInfo != null) {
                                        saveGroupUserInfoCache(groupUserInfo);
                                    }
                                    return groupUserInfo;
                                }
                            }
                        });
    }

    private void getDbGroupUserInfo(final String groupId, final String userId) {
        if (dbDataSource == null) {
            GroupUserInfo groupUserInfo = mUserDataDelegate.getGroupUserInfo(groupId, userId);
            if (groupUserInfo != null) {
                refreshGroupUserInfoCache(groupUserInfo);
            }
            return;
        }
        dbDataSource.getGroupUserInfo(
                groupId,
                userId,
                new Consumer<GroupMember>() {
                    @Override
                    public void accept(GroupMember groupMember) {
                        Option.ofObj(groupMember)
                                .ifSome(
                                        new Action1<GroupMember>() {
                                            @Override
                                            public void call(GroupMember member) {
                                                // 缓存内存
                                                cacheDataSource.refreshGroupUserInfo(member);
                                                notifyGroupMemberChange(
                                                        transformGroupMember(member));
                                            }
                                        })
                                .ifNone(
                                        new Action0() {
                                            @Override
                                            public void call() {
                                                GroupUserInfo groupUserInfo =
                                                        mUserDataDelegate.getGroupUserInfo(
                                                                groupId, userId);
                                                if (groupUserInfo != null) {
                                                    refreshGroupUserInfoCache(groupUserInfo);
                                                }
                                            }
                                        });
                    }
                });
    }

    private void notifyGroupMemberChange(final GroupUserInfo groupUserInfo) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            for (UserDataObserver item : mUserDataObservers) {
                item.onGroupUserInfoUpdate(groupUserInfo);
            }
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    notifyGroupMemberChange(groupUserInfo);
                                }
                            });
        }
    }

    private GroupUserInfo transformGroupMember(@NonNull GroupMember groupMember) {
        return new GroupUserInfo(groupMember.groupId, groupMember.userId, groupMember.memberName);
    }

    public UserInfo getCurrentUserInfo() {
        if (mCurrentUserInfo != null) {
            return mCurrentUserInfo;
        } else {
            return getUserInfo(RongIMClient.getInstance().getCurrentUserId());
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
        mCurrentUserInfo = userInfo;
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

    private void saveUserInfoCache(UserInfo userInfo) {
        if (userInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null user object.");
            return;
        }
        User user = new User(userInfo.getUserId(), userInfo.getName(), userInfo.getPortraitUri());
        user.extra = userInfo.getExtra();
        user.alias = userInfo.getAlias();
        cacheDataSource.refreshUserInfo(user);
        if (isCacheUserInfo && dbDataSource != null) {
            dbDataSource.refreshUserInfo(user);
        }
    }

    public void refreshUserInfoCache(UserInfo userInfo) {
        saveUserInfoCache(userInfo);
        notifyUserChange(userInfo);
    }

    public void refreshGroupInfoCache(io.rong.imlib.model.Group groupInfo) {
        saveGroupInfoCache(groupInfo);
        notifyGroupChange(groupInfo);
    }

    private void saveGroupInfoCache(io.rong.imlib.model.Group groupInfo) {
        if (groupInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null group object.");
            return;
        }
        RLog.d(TAG, "refresh Group info.");
        Group group =
                new Group(
                        groupInfo.getId(),
                        groupInfo.getName(),
                        groupInfo.getPortraitUri() == null
                                ? ""
                                : groupInfo.getPortraitUri().toString());
        cacheDataSource.refreshGroupInfo(group);
        if (isCacheGroupInfo && dbDataSource != null) {
            dbDataSource.refreshGroupInfo(group);
        }
    }

    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        saveGroupUserInfoCache(groupUserInfo);
        notifyGroupMemberChange(groupUserInfo);
    }

    private void saveGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        if (groupUserInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null groupUserInfo object.");
            return;
        }
        GroupMember groupMember =
                new GroupMember(
                        groupUserInfo.getGroupId(),
                        groupUserInfo.getUserId(),
                        groupUserInfo.getNickname());
        cacheDataSource.refreshGroupUserInfo(groupMember);
        if (isCacheGroupMemberInfo && dbDataSource != null) {
            dbDataSource.refreshGroupUserInfo(groupMember);
        }
    }

    public String getUserDisplayName(UserInfo userInfo) {
        if (userInfo == null) {
            return null;
        }

        return TextUtils.isEmpty(userInfo.getAlias()) ? userInfo.getName() : userInfo.getAlias();
    }

    public String getUserDisplayName(User user) {
        if (user == null) {
            return null;
        }

        return TextUtils.isEmpty(user.alias) ? user.name : user.alias;
    }

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

    /**
     * 增加数据变更监听器。 当用户信息、群信息或者群昵称信息发生变更时，会回调此监听器。
     *
     * @param observer 数据变更监听器。
     */
    public void addUserDataObserver(final UserDataObserver observer) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mUserDataObservers.add(observer);
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    addUserDataObserver(observer);
                                }
                            });
        }
    }

    /**
     * 移除数据变更监听器。
     *
     * @param observer 已设置的数据变更监听器。
     */
    public void removeUserDataObserver(final UserDataObserver observer) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mUserDataObservers.remove(observer);
        } else {
            ExecutorHelper.getInstance()
                    .mainThread()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    removeUserDataObserver(observer);
                                }
                            });
        }
    }

    private void preLoadUserCache() {
        if (dbDataSource == null) {
            return;
        }
        dbDataSource.getLimitUser(
                RongConfigCenter.featureConfig().getUserCacheMaxCount(),
                new Consumer<List<User>>() {
                    @Override
                    public void accept(List<User> users) {
                        for (User item : users) {
                            cacheDataSource.refreshUserInfo(item);
                        }
                    }
                });
        dbDataSource.getLimitGroup(
                RongConfigCenter.featureConfig().getGroupCacheMaxCount(),
                new Consumer<List<Group>>() {
                    @Override
                    public void accept(List<Group> groups) {
                        for (Group item : groups) {
                            cacheDataSource.refreshGroupInfo(item);
                        }
                    }
                });
        dbDataSource.getLimitGroupMember(
                RongConfigCenter.featureConfig().getGroupMemberCacheMaxCount(),
                new Consumer<List<GroupMember>>() {
                    @Override
                    public void accept(List<GroupMember> groupMembers) {
                        for (GroupMember item : groupMembers) {
                            cacheDataSource.refreshGroupUserInfo(item);
                        }
                    }
                });
    }

    /** 用户信息变更观察者，所有回调都在 ui 线程 */
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

    private static class SingleTonHolder {
        static RongUserInfoManager sInstance = new RongUserInfoManager();
    }
}
