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
                                    // ???????????????????????????????????????????????????
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
     * ???????????????????????????????????????
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
     * ???????????????????????????????????? UI ???????????????????????????????????? ??? ViewModel ??????????????????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????? null ????????? ???????????????????????????????????????????????????????????? {@link
     * #refreshUserInfoCache(UserInfo)}} ?????????????????????
     *
     * @param userInfoProvider ????????????????????? {@link UserDataProvider.UserInfoProvider}???
     * @param isCacheUserInfo ??????????????? IMKit ????????????????????????<br>
     *     ?????? App ????????? UserInfoProvider??? ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????<br>
     *     ????????????????????????????????? true?????? IMKit ????????????????????????
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
     * ????????????????????????
     *
     * <p>???????????????????????????????????????????????????
     *
     * <p>??????????????? sdk ??????????????????????????????????????? {@link
     * UserDataProvider.GroupUserInfoProvider#getGroupUserInfo(String, String)} ????????????????????????????????? groupId,
     * userId ??????????????????????????? {@link GroupUserInfo}??? ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????? null ?????????
     * ???????????????????????????????????????????????????????????? {@link #refreshGroupUserInfoCache(GroupUserInfo)} ???????????????
     *
     * @param groupUserInfoProvider ??????????????????????????????
     * @param isCacheGroupUserInfo ??????????????? IMKit ????????? GroupUserInfo???<br>
     *     ?????? App ????????? GroupUserInfoProvider??? ???????????????????????????????????????????????????????????????????????????????????????????????????????????????<br>
     *     ????????????????????????????????? true?????? IMKit ??????????????????
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
        // ??????????????????
        return Option.ofObj(cacheDataSource.getUserInfo(userId))
                .map(
                        new Func1<User, UserInfo>() {
                            @Override
                            public UserInfo call(User user) {
                                // ????????????????????????????????? userInfo
                                return transformUser(user);
                            }
                        })
                .orDefault(
                        new Func0<UserInfo>() {
                            @Override
                            public UserInfo call() {
                                // ??????????????????
                                if (isCacheUserInfo) {
                                    // ????????????
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
                                                // ????????????
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
        // ??????????????????
        return Option.ofObj(cacheDataSource.getGroupInfo(groupId))
                .map(
                        new Func1<Group, io.rong.imlib.model.Group>() {
                            @Override
                            public io.rong.imlib.model.Group call(Group group) {
                                // ????????????????????????????????? userInfo
                                return transformGroup(group);
                            }
                        })
                .orDefault(
                        new Func0<io.rong.imlib.model.Group>() {
                            @Override
                            public io.rong.imlib.model.Group call() {
                                // ??????????????????
                                if (isCacheGroupInfo) {
                                    // ????????????
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
                                                // ????????????
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
        // ??????????????????
        return Option.ofObj(cacheDataSource.getGroupUserInfo(groupId, userId))
                .map(
                        new Func1<GroupMember, GroupUserInfo>() {
                            @Override
                            public GroupUserInfo call(GroupMember groupMember) {
                                // ????????????????????????????????? userInfo
                                return transformGroupMember(groupMember);
                            }
                        })
                .orDefault(
                        new Func0<GroupUserInfo>() {
                            @Override
                            public GroupUserInfo call() {
                                // ??????????????????
                                if (isCacheGroupMemberInfo) {
                                    // ????????????
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
                                                // ????????????
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
     * ??????????????????????????? ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????? ?????????{@link
     * IMCenter#init(Application, String, boolean)}????????????{@link #setMessageAttachedUserInfo(boolean)}???
     * ????????????????????????????????????????????????????????????IMKit????????????????????????????????????????????????????????????????????????
     *
     * @param userInfo ?????????????????????
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        mCurrentUserInfo = userInfo;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param state ???????????????????????????true ?????????false ????????????
     */
    public void setMessageAttachedUserInfo(boolean state) {
        mIsUserInfoAttached = state;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     *
     * @return ????????????????????????
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
     * ?????????????????????????????? ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param observer ????????????????????????
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
     * ??????????????????????????????
     *
     * @param observer ????????????????????????????????????
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

    /** ???????????????????????????????????????????????? ui ?????? */
    public interface UserDataObserver {
        /**
         * ?????????????????????????????????????????????
         *
         * @param info ???????????????????????????
         */
        void onUserUpdate(UserInfo info);

        /**
         * ?????????????????????????????????????????????
         *
         * @param group ????????????????????????
         */
        void onGroupUpdate(io.rong.imlib.model.Group group);

        /**
         * ????????????????????????????????????????????????
         *
         * @param groupUserInfo ??????????????????????????????
         */
        void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo);
    }

    private static class SingleTonHolder {
        static RongUserInfoManager sInstance = new RongUserInfoManager();
    }
}
