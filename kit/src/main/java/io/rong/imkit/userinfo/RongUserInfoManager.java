package io.rong.imkit.userinfo;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public class RongUserInfoManager {
    private final String TAG = RongUserInfoManager.class.getSimpleName();
    private UserDataDelegate mUserDataDelegate;
    private LocalUserDataSource mLocalDataSource;
    private UserDatabase mDatabase;
    private UserInfo mCurrentUserInfo;
    private boolean mIsUserInfoAttached;
    private boolean mIsCacheUserInfo = true;
    private boolean mIsCacheGroupInfo = true;
    private boolean mIsCacheGroupMemberInfo = true;
    private MediatorLiveData<List<User>> mAllUsers;
    private MediatorLiveData<List<Group>> mAllGroups;
    private MediatorLiveData<List<GroupMember>> mAllGroupMembers;
    private List<UserDataObserver> mUserDataObservers;

    private static class SingleTonHolder {
        static RongUserInfoManager sInstance = new RongUserInfoManager();
    }

    public static RongUserInfoManager getInstance() {
        return SingleTonHolder.sInstance;
    }

    private RongUserInfoManager() {
        mUserDataDelegate = new UserDataDelegate();
        mUserDataObservers = new ArrayList<>();
        mAllUsers = new MediatorLiveData<>();
        mAllGroups = new MediatorLiveData<>();
        mAllGroupMembers = new MediatorLiveData<>();
        IMCenter.getInstance().addOnReceiveMessageListener(new RongIMClient.OnReceiveMessageWrapperListener() {
            @Override
            public boolean onReceived(Message message, int i, boolean b, boolean b1) {
                if (message != null && message.getContent() != null && message.getContent().getUserInfo() != null
                        && RongUserInfoManager.getInstance().getUserDatabase() != null) {
                    final UserInfo userInfo = message.getContent().getUserInfo();
                    ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            RongUserInfoManager.getInstance().getUserDatabase().getUserDao().insertUser(new User(userInfo));
                        }
                    });
                }
                return false;
            }
        });
        mAllUsers.addSource(UserDatabase.getDatabaseCreated(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
                if (value) {
                    mAllUsers.addSource(mDatabase.getUserDao().getAllUsers(), new Observer<List<User>>() {
                        @Override
                        public void onChanged(List<User> users) {
                            mAllUsers.postValue(users);
                        }
                    });
                } else {
                    mAllUsers.removeSource(UserDatabase.getDatabaseCreated());
                }
            }
        });
        mAllGroups.addSource(UserDatabase.getDatabaseCreated(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
                if (value) {
                    mAllGroups.addSource(mDatabase.getGroupDao().getAllGroups(), new Observer<List<Group>>() {
                        @Override
                        public void onChanged(List<Group> groups) {
                            mAllGroups.postValue(groups);
                        }
                    });
                }
            }
        });
        mAllGroupMembers.addSource(UserDatabase.getDatabaseCreated(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
                if (value) {
                    mAllGroupMembers.addSource(mDatabase.getGroupMemberDao().getAllGroupMembers(), new Observer<List<GroupMember>>() {
                        @Override
                        public void onChanged(List<GroupMember> groupMembers) {
                            mAllGroupMembers.postValue(groupMembers);
                        }
                    });
                }
            }
        });
    }

    /**
     * 初始化并打开用户信息数据库
     *
     * @param context
     */
    public void initAndUpdateUserDataBase(Context context) {
        mDatabase = UserDatabase.openDb(context, RongIMClient.getInstance().getCurrentUserId());
        if (mDatabase != null) {
            mLocalDataSource = new LocalUserDataSource(mDatabase);
        }
    }

    /**
     * <p>
     * 设置用户信息的提供者，供 UI 获取用户名称和头像信息。
     * 各 ViewModel 会监听用户信息的变化，进行对应刷新。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshUserInfoCache(UserInfo)}}  刷新用户信息。
     * </p>
     *
     * @param userInfoProvider 用户信息提供者 {@link UserDataProvider.UserInfoProvider}。
     * @param isCacheUserInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                         如果 App 提供的 UserInfoProvider。
     *                         每次都需要通过网络请求用户数据，而不是将用户数据缓存到本地，会影响用户信息的加载速度；<br>
     *                         此时最好将本参数设置为 true，由 IMKit 来缓存用户信息。
     */
    public void setUserInfoProvider(UserDataProvider.UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        mUserDataDelegate.setUserInfoProvider(userInfoProvider);
        mIsCacheUserInfo = isCacheUserInfo;
    }

    public void setGroupInfoProvider(UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        mUserDataDelegate.setGroupInfoProvider(groupInfoProvider);
        mIsCacheGroupInfo = isCacheGroupInfo;
    }

    /**
     * <p>设置群成员提供者</p>
     * <p>可以使用此方法，修改群组中用户昵称</p>
     * <p>设置后，当 sdk 界面展示用户信息时，会回调 {@link UserDataProvider.GroupUserInfoProvider#getGroupUserInfo(String, String)}
     * 使用者只需要根据对应的 groupId, userId 提供对应的用户信息 {@link GroupUserInfo}。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupUserInfoCache(GroupUserInfo)} 刷新信息。</p>
     *
     * @param groupUserInfoProvider 群组用户信息提供者。
     * @param isCacheGroupUserInfo  设置是否由 IMKit 来缓存 GroupUserInfo。<br>
     *                         如果 App 提供的 GroupUserInfoProvider。
     *                         每次都需要通过网络请求数据，而不是将数据缓存到本地，会影响信息的加载速度；<br>
     *                         此时最好将本参数设置为 true，由 IMKit 来缓存信息。
     */
    public void setGroupUserInfoProvider(UserDataProvider.GroupUserInfoProvider groupUserInfoProvider, boolean isCacheGroupUserInfo) {
        mUserDataDelegate.setGroupUserInfoProvider(groupUserInfoProvider);
        mIsCacheGroupMemberInfo = isCacheGroupUserInfo;
    }

    public UserDatabase getUserDatabase() {
        return mDatabase;
    }

    public LiveData<List<User>> getAllUsersLiveData() {
        return mAllUsers;
    }

    public LiveData<List<Group>> getAllGroupsLiveData() {
        return mAllGroups;
    }

    public LiveData<List<GroupMember>> getAllGroupMembersLiveData() {
        return mAllGroupMembers;
    }

    public UserInfo getUserInfo(String userId) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        User user = null;
        if (mIsCacheUserInfo && mLocalDataSource != null) {
            user = mLocalDataSource.getUserInfo(userId, mAllUsers);
        }
        if (user == null && mUserDataDelegate != null) {
            UserInfo userInfo = mUserDataDelegate.getUserInfo(userId);
            if (userInfo != null) {
                user = new User(userInfo.getUserId(), userInfo.getName() == null ? "" : userInfo.getName(),
                        userInfo.getPortraitUri());
            }
            if (mIsCacheUserInfo && user != null && mLocalDataSource != null) {
                mLocalDataSource.refreshUserInfo(user);
            }
        }
        if (user != null) {
            UserInfo userInfo = new UserInfo(user.id, user.name == null ? "" : user.name, Uri.parse(user.portraitUrl));
            userInfo.setExtra(user.extra);
            return userInfo;
        } else {
            return null;
        }
    }

    public LiveData<User> getUserInfoLiveData(final String userId) {
        final MediatorLiveData userLiveData = new MediatorLiveData<>();
        userLiveData.addSource(UserDatabase.getDatabaseCreated(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
                if (value) {
                    userLiveData.addSource(mDatabase.getUserDao().getUserLiveData(userId), new Observer<User>() {
                        @Override
                        public void onChanged(User user) {
                            userLiveData.postValue(user);
                        }
                    });
                } else {
                    userLiveData.removeSource(UserDatabase.getDatabaseCreated());
                }
            }
        });
        return userLiveData;
    }

    public io.rong.imlib.model.Group getGroupInfo(final String groupId) {
        if (TextUtils.isEmpty(groupId)) {
            return null;
        }
        Group group = null;
        if (mIsCacheGroupInfo && mLocalDataSource != null) {
            group = mLocalDataSource.getGroupInfo(groupId);
        }
        if (group == null && mUserDataDelegate != null) {
            group = mUserDataDelegate.getGroupInfo(groupId);
            if (mIsCacheGroupInfo && group != null && mLocalDataSource != null) {
                mLocalDataSource.refreshGroupInfo(group);
            }
        }
        if (group != null) {
            RLog.d(TAG, "get group info synchronize. name:" + group.name);
            return new io.rong.imlib.model.Group(group.id, group.name, Uri.parse(group.portraitUrl));
        } else {
            RLog.d(TAG, "get group info null");
            return null;
        }
    }

    public GroupUserInfo getGroupUserInfo(final String groupId, final String userId) {
        if (TextUtils.isEmpty(groupId) || TextUtils.isEmpty(userId)) {
            return null;
        }
        GroupMember groupMember = null;
        if (mIsCacheGroupMemberInfo && mLocalDataSource != null) {
            groupMember = mLocalDataSource.getGroupUserInfo(groupId, userId, mAllGroupMembers);
        }
        if (groupMember == null && mUserDataDelegate != null) {
            groupMember = mUserDataDelegate.getGroupUserInfo(groupId, userId);
            if (mIsCacheGroupMemberInfo && groupMember != null && mLocalDataSource != null) {
                mLocalDataSource.refreshGroupUserInfo(groupMember);
            }
        }

        if (groupMember != null) {
            return new GroupUserInfo(groupId, userId, groupMember.memberName);
        } else {
            return null;
        }
    }

    /**
     * 设置当前用户信息。
     * 如果开发者没有实现用户信息提供者，而是使用消息携带用户信息，需要使用这个方法设置当前用户的信息，
     * 然后在{@link IMCenter#init(Application, String, boolean)}之后调用{@link #setMessageAttachedUserInfo(boolean)}，
     * 这样可以在每条消息中携带当前用户的信息，IMKit会在接收到消息的时候取出用户信息并刷新到界面上。
     *
     * @param userInfo 当前用户信息。
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        mCurrentUserInfo = userInfo;
    }

    public UserInfo getCurrentUserInfo() {
        if (mCurrentUserInfo != null) {
            return mCurrentUserInfo;
        } else {
            return getUserInfo(RongIMClient.getInstance().getCurrentUserId());
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

    public void refreshUserInfoCache(UserInfo userInfo) {
        if (userInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null user object.");
            return;
        }
        User user = new User(userInfo.getUserId(), userInfo.getName(), userInfo.getPortraitUri());
        user.extra = userInfo.getExtra();

        if (mIsCacheUserInfo && mLocalDataSource != null) {
            mLocalDataSource.refreshUserInfo(user);
        } else {
            List<User> userList = mAllUsers.getValue();
            if (userList == null) {
                userList = new ArrayList<>();
            }
            for (User temp : userList) {
                if (temp.id.equals(user.id)) {
                    userList.remove(temp);
                    userList.add(user);
                    break;
                }
            }
            mAllUsers.postValue(userList);
        }
        for (UserDataObserver observer : mUserDataObservers) {
            observer.onUserUpdate(userInfo);
        }
    }

    public void refreshGroupInfoCache(io.rong.imlib.model.Group groupInfo) {
        if (groupInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null group object.");
            return;
        }
        RLog.d(TAG, "refresh Group info.");
        Group group = new Group(groupInfo.getId(), groupInfo.getName(), groupInfo.getPortraitUri() == null ? "" : groupInfo.getPortraitUri().toString());

        if (mIsCacheGroupInfo && mLocalDataSource != null) {
            mLocalDataSource.refreshGroupInfo(group);
        } else {
            List<Group> groupList = mAllGroups.getValue();
            if (groupList == null) {
                groupList = new ArrayList<>();
            }
            for (Group temp : groupList) {
                if (temp.id.equals(groupInfo.getId())) {
                    groupList.remove(temp);
                    groupList.add(group);
                    break;
                }
            }
            mAllGroups.postValue(groupList);
        }
        for (UserDataObserver observer : mUserDataObservers) {
            observer.onGroupUpdate(groupInfo);
        }
    }

    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        if (groupUserInfo == null) {
            RLog.e(TAG, "Invalid to refresh a null groupUserInfo object.");
            return;
        }
        GroupMember groupMember = new GroupMember(groupUserInfo.getGroupId(), groupUserInfo.getUserId(), groupUserInfo.getNickname());
        if (mIsCacheGroupMemberInfo && mLocalDataSource != null) {
            mLocalDataSource.refreshGroupUserInfo(groupMember);
        } else {
            List<GroupMember> groupMemberList = mAllGroupMembers.getValue();
            if (groupMemberList == null) {
                groupMemberList = new ArrayList<>();
            }
            for (GroupMember temp : groupMemberList) {
                if (temp.groupId.equals(groupMember.groupId) && temp.userId.equals(groupMember.userId)) {
                    groupMemberList.remove(temp);
                    groupMemberList.add(groupMember);
                    break;
                }
            }
            mAllGroupMembers.postValue(groupMemberList);
        }
        for (UserDataObserver observer : mUserDataObservers) {
            observer.onGroupUserInfoUpdate(groupUserInfo);
        }
    }

    public void addUserDataObserver(UserDataObserver observer) {
        mUserDataObservers.add(observer);
    }

    public void removeUserDataObserver(UserDataObserver observer) {
        mUserDataObservers.remove(observer);
    }

    public interface UserDataObserver {
        void onUserUpdate(UserInfo info);

        void onGroupUpdate(io.rong.imlib.model.Group group);

        void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo);
    }
}
