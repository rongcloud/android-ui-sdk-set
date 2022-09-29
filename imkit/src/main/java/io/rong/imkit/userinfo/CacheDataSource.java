package io.rong.imkit.userinfo;

import androidx.annotation.NonNull;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.StringUtils;
import io.rong.imkit.widget.cache.RongCache;

public class CacheDataSource {
    private final String TAG = CacheDataSource.class.getSimpleName();
    private RongCache<String, User> mUserCache;
    private RongCache<String, GroupMember> mGroupMemberCache;
    private RongCache<String, Group> mGroupCache;

    CacheDataSource() {
        mUserCache = new RongCache<>(RongConfigCenter.featureConfig().getUserCacheMaxCount());
        mGroupMemberCache =
                new RongCache<>(RongConfigCenter.featureConfig().getGroupMemberCacheMaxCount());
        mGroupCache = new RongCache<>(RongConfigCenter.featureConfig().getGroupCacheMaxCount());
    }

    User getUserInfo(final String userId) {
        synchronized (mUserCache) {
            return mUserCache.get(userId);
        }
    }

    Group getGroupInfo(final String groupId) {
        synchronized (mGroupCache) {
            return mGroupCache.get(groupId);
        }
    }

    GroupMember getGroupUserInfo(final String groupId, final String userId) {
        synchronized (mGroupMemberCache) {
            final String key = StringUtils.getKey(groupId, userId);
            return mGroupMemberCache.get(key);
        }
    }

    void refreshUserInfo(@NonNull final User user) {
        synchronized (mUserCache) {
            mUserCache.put(user.id, user);
        }
    }

    void refreshGroupUserInfo(@NonNull final GroupMember groupMember) {
        synchronized (mGroupMemberCache) {
            String key = StringUtils.getKey(groupMember.groupId, groupMember.userId);
            mGroupMemberCache.put(key, groupMember);
        }
    }

    void refreshGroupInfo(@NonNull final Group group) {
        synchronized (mGroupCache) {
            mGroupCache.put(group.id, group);
        }
    }
}
