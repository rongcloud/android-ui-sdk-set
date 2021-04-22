package io.rong.imkit.userinfo;

import androidx.annotation.NonNull;
import androidx.lifecycle.MediatorLiveData;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.StringUtils;
import io.rong.imkit.widget.cache.RongCache;


public class LocalUserDataSource {
    private final String TAG = LocalUserDataSource.class.getSimpleName();
    private final int USER_CACHE_MAX_COUNT = 256;
    private final int GROUP_CACHE_MAX_COUNT = 128;
    private RongCache<String, User> mUserCache;
    private RongCache<String, GroupMember> mGroupMemberCache;
    private RongCache<String, Group> mGroupCache;
    private UserDatabase mDatabase;

    LocalUserDataSource(@NonNull UserDatabase database) {
        mDatabase = database;
        mUserCache = new RongCache<>(USER_CACHE_MAX_COUNT);
        mGroupMemberCache = new RongCache<>(USER_CACHE_MAX_COUNT);
        mGroupCache = new RongCache<>(GROUP_CACHE_MAX_COUNT);
    }

    User getUserInfo(final String userId, final MediatorLiveData<List<User>> users) {
        User user = mUserCache.get(userId);
        if (user == null && mDatabase != null) {
            user = mDatabase.getUserDao().getUser(userId);
            if (user != null) {
                mUserCache.put(userId, user);
                users.postValue(users.getValue());
            }
        }
        return user;
    }

    Group getGroupInfo(final String groupId) {
        Group group = mGroupCache.get(groupId);
        if (group == null && mDatabase != null) {
            group = mDatabase.getGroupDao().getGroup(groupId);
            if (group != null) {
                mGroupCache.put(groupId, group);
                RLog.d(TAG, "get group info from db");
            }
        }
        return group;
    }


    GroupMember getGroupUserInfo(final String groupId, final String userId, final MediatorLiveData<List<GroupMember>> groupMembers) {
        final String key = StringUtils.getKey(groupId, userId);
        GroupMember member = mGroupMemberCache.get(key);
        if (member == null) {
            member = mDatabase.getGroupMemberDao().getGroupMember(groupId, userId);
            if (member != null) {
                mGroupMemberCache.put(key, member);
            }
        }
        return member;
    }

    void refreshUserInfo(final User user) {
        if (user != null) {
            mUserCache.put(user.id, user);
            ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mDatabase.getUserDao().insertUser(user);
                    } catch (IllegalStateException e) {
                        RLog.e(TAG, e.getMessage());
                    }
                }
            });
        }
    }

    void refreshGroupUserInfo(final GroupMember groupMember) {
        if (groupMember != null) {
            String key = StringUtils.getKey(groupMember.groupId, groupMember.userId);
            mGroupMemberCache.put(key, groupMember);
            ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mDatabase.getGroupMemberDao().insertGroupMember(groupMember);
                    } catch (IllegalStateException e) {
                        RLog.e(TAG, e.getMessage());
                    }
                }
            });
        }
    }

    void refreshGroupInfo(final Group group) {
        if (group != null) {
            mGroupCache.put(group.id, group);
            ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        mDatabase.getGroupDao().insertGroup(group);
                    } catch (IllegalStateException e) {
                        RLog.e(TAG, e.getMessage());
                    }
                }
            });
        }
    }


}
