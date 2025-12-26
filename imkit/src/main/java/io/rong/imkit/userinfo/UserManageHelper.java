package io.rong.imkit.userinfo;

import static io.rong.imlib.model.SubscribeEvent.SubscribeType.FRIEND_USER_PROFILE;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.userinfo.model.ExtendedGroup;
import io.rong.imkit.userinfo.model.ExtendedGroupUserInfo;
import io.rong.imkit.userinfo.model.ExtendedUserInfo;
import io.rong.imkit.usermanage.interfaces.OnGroupAndUserEventListener;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.listener.FriendEventListener;
import io.rong.imlib.listener.GroupEventListener;
import io.rong.imlib.listener.OnSubscribeEventListener;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupInfoKeys;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupOperation;
import io.rong.imlib.model.GroupOperationType;
import io.rong.imlib.model.SubscribeInfoEvent;
import io.rong.imlib.model.UserProfile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 功能描述: 用户信息管理类，用来获取用户信息。
 *
 * @author rongcloud
 * @since 5.10.5
 */
class UserManageHelper {
    public static final String TAG = "UserManageHelper";
    private static final int MAX_CACHE_SIZE = 1000; // 本地缓存最大容量
    private static final int MAX_RETRY_COUNT = 6; // 请求最大重试次数
    private static final int MAX_BATCH_REQUEST_SIZE = 100; // 单次批量用户/群组请求最大数量

    private final LruCache<String, ExtendedUserInfo> extendedUserInfoCache =
            new LruCache<>(MAX_CACHE_SIZE); // 用户扩展信息缓存
    private final LruCache<String, GroupInfo> groupInfoCache =
            new LruCache<>(MAX_CACHE_SIZE); // 群组信息缓存
    private final LruCache<String, GroupMemberInfo> groupMemberInfoCache =
            new LruCache<>(MAX_CACHE_SIZE); // 群成员信息缓存
    private final List<RongUserInfoManager.UserDataObserver> mUserDataObservers =
            new CopyOnWriteArrayList<>(); // 观察者列表，用于分发用户/群组数据更新

    private ExtendedUserInfo currentExtendedUserInfo; // 当前登录用户的扩展信息
    private String currentUserId; // 当前登录用户 ID

    private final Set<String> pendingUserIds =
            Collections.synchronizedSet(new HashSet<>()); // 待批量拉取信息的用户 ID 集合
    private final Set<String> pendingGroupIds =
            Collections.synchronizedSet(new HashSet<>()); // 待批量拉取信息的群组 ID 集合
    private final Map<String, Set<String>> pendingGroupMemberRequests =
            Collections.synchronizedMap(new HashMap<>()); // 待批量拉取信息的群成员 ID 映射
    private final Set<String> executingGroupIds =
            Collections.synchronizedSet(new HashSet<>()); // 当前正在执行成员拉取的群组 ID 集合
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // 主线程 Handler
    private static final long BATCH_DELAY_MS = 0; // 批量请求延迟时间
    private static final long RETRY_REQUEST_DELAY_MS = 500; // 重试请求延迟时间
    private volatile boolean isUserBatchRunning = false; // 是否正在执行用户批量请求
    private volatile boolean isGroupBatchRunning = false; // 是否正在执行群组批量请求
    private volatile boolean isMyUserProfileRunning = false; // 是否正在请求当前用户资料
    private volatile boolean hasPendingMyUserProfileRequest = false; // 是否有挂起的当前用户资料请求
    private final Object myUserProfileLock = new Object(); // 当前用户资料请求锁

    UserManageHelper() {

        // 添加连接状态监听器
        IMCenter.getInstance()
                .addConnectStatusListener(
                        new RongIMClient.ConnectCallback() {
                            @Override
                            public void onSuccess(String userId) {
                                // 如果当前用户发生变化，清空缓存
                                if (!Objects.equals(currentUserId, userId)) {
                                    RLog.i(
                                            TAG,
                                            "onSuccess: clear cache current:"
                                                    + userId
                                                    + " last:"
                                                    + currentUserId);
                                    extendedUserInfoCache.evictAll();
                                    groupInfoCache.evictAll();
                                    groupMemberInfoCache.evictAll();
                                    currentUserId = userId;
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ConnectionErrorCode e) {}

                            @Override
                            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus code) {}
                        });

        // 定义并初始化群组事件监听器
        GroupEventListener groupEventListener =
                new GroupEventListener() {
                    @Override
                    public void onGroupOperation(
                            String groupId,
                            GroupMemberInfo operatorInfo,
                            GroupInfo groupInfo,
                            GroupOperation operation,
                            List<GroupMemberInfo> memberInfos,
                            long operationTime) {
                        // 处理群组操作
                        refreshGroupInfoInner(groupId);
                    }

                    @Override
                    public void onGroupInfoChanged(
                            GroupMemberInfo operatorInfo,
                            GroupInfo groupInfo,
                            List<GroupInfoKeys> updateKeys,
                            long operationTime) {
                        if (groupInfo != null) {
                            refreshGroupInfoInner(groupInfo.getGroupId());
                        }
                    }

                    @Override
                    public void onGroupMemberInfoChanged(
                            String groupId,
                            GroupMemberInfo operatorInfo,
                            GroupMemberInfo memberInfo,
                            long operationTime) {
                        if (memberInfo != null) {
                            refreshGroupMemberInfoInner(groupId, memberInfo);
                        }
                    }

                    @Override
                    public void onGroupApplicationEvent(GroupApplicationInfo info) {
                        // 处理群组申请事件
                    }

                    @Override
                    public void onGroupRemarkChangedSync(
                            String groupId,
                            GroupOperationType operationType,
                            String groupRemark,
                            long operationTime) {
                        // 处理群组备注变更事件
                        refreshGroupInfoInner(groupId);
                    }

                    @Override
                    public void onGroupFollowsChangedSync(
                            String groupId,
                            GroupOperationType operationType,
                            List<String> userIds,
                            long operationTime) {
                        // 处理群组关注变更事件
                    }
                };

        IMCenter.getInstance().addGroupEventListener(groupEventListener);

        IMCenter.getInstance()
                .addSubscribeEventListener(
                        new OnSubscribeEventListener() {
                            @Override
                            public void onEventChange(List<SubscribeInfoEvent> subscribeEvents) {
                                Log.e(TAG, "onEventChange: " + subscribeEvents);
                                if (subscribeEvents == null || subscribeEvents.isEmpty()) {
                                    return;
                                }
                                for (SubscribeInfoEvent event : subscribeEvents) {
                                    if (event.getSubscribeType() == FRIEND_USER_PROFILE
                                            && event.getUserProfile() != null) {
                                        UserProfile profile = event.getUserProfile();
                                        ExtendedUserInfo info =
                                                extendedUserInfoCache.get(profile.getUserId());
                                        if (info != null) {
                                            info.setName(profile.getName());
                                            info.setPortraitUri(
                                                    Uri.parse(profile.getPortraitUri()));
                                            info.getFriendInfo().setUserId(profile.getUserId());
                                            info.getFriendInfo().setName(profile.getName());
                                            info.getFriendInfo()
                                                    .setPortraitUri(profile.getPortraitUri());
                                        } else {
                                            FriendInfo friendInfo = new FriendInfo();
                                            friendInfo.setUserId(profile.getUserId());
                                            friendInfo.setName(profile.getName());
                                            friendInfo.setPortraitUri(profile.getPortraitUri());
                                            info = ExtendedUserInfo.obtain(friendInfo);
                                            extendedUserInfoCache.put(profile.getUserId(), info);
                                        }
                                        notifyUserChange(info);
                                    }
                                }
                            }
                        });
        IMCenter.getInstance()
                .addFriendEventListener(
                        new FriendEventListener() {
                            @Override
                            public void onFriendAdd(
                                    DirectionType directionType,
                                    String userId,
                                    String userName,
                                    String portraitUri,
                                    long operationTime) {
                                refreshUserInfoInner(userId);
                            }

                            @Override
                            public void onFriendDelete(
                                    DirectionType directionType,
                                    List<String> userIds,
                                    long operationTime) {
                                if (userIds != null) {
                                    for (String userId : userIds) {
                                        refreshUserInfoInner(userId);
                                    }
                                }
                            }

                            @Override
                            public void onFriendApplicationStatusChanged(
                                    String userId,
                                    FriendApplicationType applicationType,
                                    FriendApplicationStatus status,
                                    DirectionType directionType,
                                    long operationTime,
                                    String extra) {}

                            @Override
                            public void onFriendCleared(long operationTime) {}

                            @Override
                            public void onFriendInfoChangedSync(
                                    String userId,
                                    String remark,
                                    Map<String, String> extProfile,
                                    long operationTime) {
                                // 遍历 userProfileCache ,找到 对应的 userId ,更新 remark
                                refreshUserInfoInner(userId);
                            }
                        });

        IMCenter.getInstance()
                .addOnGroupAndUserEventListener(
                        new OnGroupAndUserEventListener() {
                            @Override
                            public void updateGroupInfo(
                                    GroupInfo groupInfo, IRongCoreEnum.CoreErrorCode errorCode) {
                                if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS
                                        && groupInfo != null) {
                                    refreshGroupInfoInner(groupInfo.getGroupId());
                                }
                            }

                            @Override
                            public void setGroupMemberInfo(
                                    String groupId,
                                    String userId,
                                    String nickname,
                                    String extra,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                    refreshGroupMemberInfoInner(
                                            groupId, new GroupMemberInfo(userId, nickname));
                                }
                            }

                            @Override
                            public void setGroupRemark(
                                    String groupId,
                                    String remark,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                    refreshGroupInfoInner(groupId);
                                }
                            }

                            @Override
                            public void updateMyUserProfile(
                                    UserProfile userProfile,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS
                                        && userProfile != null) {
                                    refreshUserInfoInner(userProfile.getUserId());
                                }
                            }

                            @Override
                            public void setFriendInfo(
                                    String userId,
                                    String remark,
                                    Map<String, String> extProfile,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                    refreshUserInfoInner(userId);
                                }
                            }
                        });
    }

    /* 刷新单个群组成员缓存并重新拉取数据 */
    private void refreshGroupMemberInfoInner(String groupId, GroupMemberInfo memberInfo) {
        groupMemberInfoCache.remove(generateGroupMemberKey(groupId, memberInfo.getUserId()));
        getGroupUserInfo(groupId, memberInfo.getUserId());
    }

    /* 刷新单个群组信息缓存并重新拉取数据 */
    private void refreshGroupInfoInner(String groupId) {
        if (groupId == null) {
            RLog.e(TAG, "refreshGroupInfoInner: groupId is null");
            return;
        }
        groupInfoCache.remove(groupId);
        getGroupInfo(groupId);
    }

    /* 刷新单个用户信息缓存并重新拉取数据 */
    private void refreshUserInfoInner(String userId) {
        if (userId == null) {
            RLog.e(TAG, "refreshUserInfoInner: userId is null");
            return;
        }
        extendedUserInfoCache.remove(userId);
        getUserInfo(userId);
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    ExtendedUserInfo getUserInfo(final String userId) {
        if (userId == null) {
            return null;
        }
        ExtendedUserInfo cachedExtendedUserInfo = extendedUserInfoCache.get(userId);
        if (cachedExtendedUserInfo != null) {
            return cachedExtendedUserInfo;
        }

        String currentUserId = RongCoreClient.getInstance().getCurrentUserId();
        if (Objects.equals(currentUserId, userId)) {
            synchronized (myUserProfileLock) {
                if (isMyUserProfileRunning) {
                    hasPendingMyUserProfileRequest = true;
                } else {
                    isMyUserProfileRunning = true;
                    mainHandler.post(myUserProfileRequestRunnable);
                }
            }
        } else {
            synchronized (pendingUserIds) {
                pendingUserIds.add(userId);
                if (!isUserBatchRunning) {
                    isUserBatchRunning = true;
                    mainHandler.postDelayed(batchUserProfileRequestRunnable, BATCH_DELAY_MS);
                }
            }
        }
        return null;
    }

    /**
     * 获取群组信息
     *
     * @param groupId 群组 ID
     * @return 群组信息
     */
    GroupInfo getGroupInfo(@NonNull final String groupId) {
        if (groupId == null) {
            RLog.e(TAG, "getGroupInfo: groupId is null");
            return null;
        }
        GroupInfo cachedInfo = groupInfoCache.get(groupId);
        if (cachedInfo != null) {
            return cachedInfo;
        }

        synchronized (pendingGroupIds) {
            pendingGroupIds.add(groupId);
            if (!isGroupBatchRunning) {
                isGroupBatchRunning = true;
                mainHandler.postDelayed(batchGroupInfoRequestRunnable, BATCH_DELAY_MS);
            }
        }
        return null;
    }

    /**
     * 获取群组成员信息
     *
     * @param groupId 群组 ID
     * @param userId 用户 ID
     * @return 群组成员信息
     */
    GroupMemberInfo getGroupUserInfo(final String groupId, final String userId) {
        String key = generateGroupMemberKey(groupId, userId);
        GroupMemberInfo cachedMemberInfo = groupMemberInfoCache.get(key);
        if (cachedMemberInfo != null) {
            return cachedMemberInfo;
        }

        synchronized (pendingGroupMemberRequests) {
            Set<String> userIds = pendingGroupMemberRequests.get(groupId);
            if (userIds == null) {
                userIds = new HashSet<>();
                pendingGroupMemberRequests.put(groupId, userIds);
            }
            userIds.add(userId);
            // 只有当该群组是第一次添加请求，且没有在执行中时，才触发延迟任务？
            // 简单起见，只要有添加就触发延迟任务，Runnable 内部会检查状态
            mainHandler.postDelayed(batchGroupMemberInfoRunnable, BATCH_DELAY_MS);
        }

        return null;
    }

    void loadUserInfos(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> requestIds = new ArrayList<>();
        List<ExtendedUserInfo> cacheList = new ArrayList<>();
        for (String id : userIds) {
            ExtendedUserInfo cachedExtendedUserInfo = extendedUserInfoCache.get(id);
            if (cachedExtendedUserInfo != null) {
                cacheList.add(cachedExtendedUserInfo);
            } else {
                requestIds.add(id);
            }
        }
        // 不需要 notify
        RLog.d(TAG, "loadUserInfos: " + cacheList.size());
        if (requestIds.isEmpty()) {
            return;
        }
        synchronized (pendingUserIds) {
            pendingUserIds.addAll(requestIds);
            if (!isUserBatchRunning) {
                isUserBatchRunning = true;
                mainHandler.postDelayed(batchUserProfileRequestRunnable, BATCH_DELAY_MS);
            }
        }
    }

    void loadGroupInfos(List<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        List<String> requestIds = new ArrayList<>();
        List<GroupInfo> cacheList = new ArrayList<>();
        for (String id : groupIds) {
            GroupInfo cachedInfo = groupInfoCache.get(id);
            if (cachedInfo != null) {
                cacheList.add(cachedInfo);
            } else {
                requestIds.add(id);
            }
        }
        // 不需要 notify
        RLog.d(TAG, "loadGroupInfos: " + cacheList.size());
        if (requestIds.isEmpty()) {
            return;
        }
        synchronized (pendingGroupIds) {
            pendingGroupIds.addAll(requestIds);
            if (!isGroupBatchRunning) {
                isGroupBatchRunning = true;
                mainHandler.postDelayed(batchGroupInfoRequestRunnable, BATCH_DELAY_MS);
            }
        }
    }

    void loadGroupUserInfos(List<String> groupIds, List<String> userIds) {
        if (groupIds == null || groupIds.isEmpty() || userIds == null || userIds.isEmpty()) {
            return;
        }
        List<String> requestGroupIds = new ArrayList<>();
        List<String> requestUserIds = new ArrayList<>();
        List<GroupMemberInfo> cacheList = new ArrayList<>();
        for (int i = 0; i < groupIds.size(); i++) {
            String groupId = groupIds.get(i);
            String userId = userIds.get(i);
            String key = generateGroupMemberKey(groupId, userId);
            GroupMemberInfo cachedMemberInfo = groupMemberInfoCache.get(key);
            if (cachedMemberInfo != null) {
                cacheList.add(cachedMemberInfo);
            } else {
                requestGroupIds.add(groupId);
                requestUserIds.add(userId);
            }
        }
        // 不需要 notify
        RLog.d(TAG, "loadGroupUserInfos: " + cacheList.size());
        if (requestGroupIds.isEmpty() || requestUserIds.isEmpty()) {
            return;
        }
        synchronized (pendingGroupMemberRequests) {
            for (int i = 0; i < requestGroupIds.size(); i++) {
                String groupId = requestGroupIds.get(i);
                Set<String> userIdSet = pendingGroupMemberRequests.get(groupId);
                if (userIdSet == null) {
                    userIdSet = new HashSet<>();
                    pendingGroupMemberRequests.put(groupId, userIdSet);
                }
                userIdSet.add(requestUserIds.get(i));
            }
            // 只有当该群组是第一次添加请求，且没有在执行中时，才触发延迟任务？
            // 简单起见，只要有添加就触发延迟任务，Runnable 内部会检查状态
            mainHandler.postDelayed(batchGroupMemberInfoRunnable, BATCH_DELAY_MS);
        }
    }

    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    ExtendedUserInfo getCurrentUserInfo() {
        String currentUserId = RongCoreClient.getInstance().getCurrentUserId();
        return getUserInfo(currentUserId);
    }

    /**
     * 设置当前用户信息
     *
     * @param userProfile 用户信息
     */
    void setCurrentUserInfo(@NonNull ExtendedUserInfo userProfile) {
        this.currentExtendedUserInfo = userProfile;
        RongCoreClient.getInstance()
                .updateMyUserProfile(
                        userProfile.getUserProfile(),
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyUserChange(currentExtendedUserInfo);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode code, List<String> errorKeys) {
                                // Handle error
                            }
                        });
    }

    /**
     * 添加用户信息观察者
     *
     * @param observer 用户信息观察者
     */
    void addUserDataObserver(@NonNull RongUserInfoManager.UserDataObserver observer) {
        mUserDataObservers.add(observer);
    }

    /**
     * 移除用户信息观察者
     *
     * @param observer 用户信息观察者
     */
    void removeUserDataObserver(@NonNull RongUserInfoManager.UserDataObserver observer) {
        mUserDataObservers.remove(observer);
    }

    /**
     * 刷新用户信息缓存
     *
     * @param extendedUserInfo 用户信息
     */
    void refreshUserInfoCache(@NonNull ExtendedUserInfo extendedUserInfo) {
        String currentUserId = RongCoreClient.getInstance().getCurrentUserId();
        if (Objects.equals(currentUserId, extendedUserInfo.getUserId())) {
            RongCoreClient.getInstance()
                    .updateMyUserProfile(
                            extendedUserInfo.getUserProfile(),
                            new IRongCoreCallback.ExamineOperationCallback() {
                                @Override
                                public void onSuccess() {
                                    refreshUserInfoInner(extendedUserInfo.getUserId());
                                }

                                @Override
                                public void onError(
                                        IRongCoreEnum.CoreErrorCode code, List<String> errorKeys) {}
                            });
        } else {
            String remark = "";
            HashMap<String, String> extendedUserInfoMap = new HashMap<>();
            if (!TextUtils.isEmpty(extendedUserInfo.getUserProfile().getUserId())) {
                extendedUserInfoMap = extendedUserInfo.getUserProfile().getUserExtProfile();
                remark = extendedUserInfo.getUserProfile().getName();
            } else if (!TextUtils.isEmpty(extendedUserInfo.getFriendInfo().getUserId())) {
                extendedUserInfoMap = extendedUserInfo.getFriendInfo().getExtProfile();
                remark = extendedUserInfo.getFriendInfo().getRemark();
            }
            RongCoreClient.getInstance()
                    .setFriendInfo(
                            extendedUserInfo.getUserId(),
                            remark,
                            extendedUserInfoMap,
                            new IRongCoreCallback.ExamineOperationCallback() {
                                @Override
                                public void onSuccess() {
                                    refreshUserInfoInner(extendedUserInfo.getUserId());
                                }

                                @Override
                                public void onError(
                                        IRongCoreEnum.CoreErrorCode code, List<String> errorKeys) {
                                    // Handle error
                                }
                            });
        }
    }

    /**
     * 刷新群组信息缓存
     *
     * @param groupInfo 群组信息
     */
    void refreshGroupInfoCache(GroupInfo groupInfo) {
        RongCoreClient.getInstance()
                .updateGroupInfo(
                        groupInfo,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (groupInfo != null) {
                                    refreshGroupInfoInner(groupInfo.getGroupId());
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                    List<String> errorKeys) {
                                if (groupInfo != null) {
                                    refreshGroupInfoInner(groupInfo.getGroupId());
                                }
                            }
                        });
    }

    /**
     * 刷新群组成员信息缓存
     *
     * @param groupId 群组 ID
     * @param groupMemberInfo 群组成员信息
     */
    void refreshGroupUserInfoCache(String groupId, GroupMemberInfo groupMemberInfo) {
        RongCoreClient.getInstance()
                .setGroupMemberInfo(
                        groupId,
                        groupMemberInfo.getUserId(),
                        groupMemberInfo.getNickname(),
                        groupMemberInfo.getExtra(),
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                refreshGroupMemberInfoInner(groupId, groupMemberInfo);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                    List<String> errorKeys) {
                                // Handle error
                            }
                        });
    }

    /* 通知所有观察者，用户信息发生了变化 */
    private void notifyUserChange(ExtendedUserInfo extendedUserInfo) {
        runOnMainThread(
                () -> {
                    for (RongUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        observer.onUserUpdate(extendedUserInfo);
                    }
                });
    }

    /* 通知所有观察者，群组信息发生了变化 */
    private void notifyGroupChange(GroupInfo groupInfo) {
        runOnMainThread(
                () -> {
                    for (RongUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        observer.onGroupUpdate(ExtendedGroup.obtain(groupInfo));
                    }
                });
    }

    /* 通知所有观察者，某个群成员信息发生了变化 */
    private void notifyGroupMemberChange(String groupId, GroupMemberInfo groupMemberInfo) {
        runOnMainThread(
                () -> {
                    for (RongUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        ExtendedGroupUserInfo extendedGroupUserInfo =
                                ExtendedGroupUserInfo.obtain(groupMemberInfo);
                        extendedGroupUserInfo.setGroupId(groupId);
                        observer.onGroupUserInfoUpdate(extendedGroupUserInfo);
                    }
                });
    }

    // 用户资料拉取任务（按队列合并触发请求）
    private final Runnable batchUserProfileRequestRunnable =
            new Runnable() {
                @Override
                public void run() {
                    final List<String> userIds;
                    synchronized (pendingUserIds) {
                        if (pendingUserIds.size() > MAX_BATCH_REQUEST_SIZE) {
                            userIds = new ArrayList<>(MAX_BATCH_REQUEST_SIZE);
                            Iterator<String> iterator = pendingUserIds.iterator();
                            for (int i = 0; i < MAX_BATCH_REQUEST_SIZE; i++) {
                                String id = iterator.next();
                                userIds.add(id);
                                iterator.remove();
                            }
                        } else {
                            userIds = new ArrayList<>(pendingUserIds);
                            pendingUserIds.clear();
                        }
                    }
                    // 优先请求好友信息，请求不到再请求UserProfile
                    requestFriendsInfoWithRetry(
                            userIds,
                            0,
                            new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                                @Override
                                public void onSuccess(List<FriendInfo> friendInfos) {
                                    final Map<String, ExtendedUserInfo> userMap = new HashMap<>();
                                    List<String> successIds = new ArrayList<>();
                                    List<String> needRequestIds = new ArrayList<>(userIds);

                                    if (friendInfos != null && !friendInfos.isEmpty()) {
                                        for (FriendInfo friend : friendInfos) {
                                            needRequestIds.remove(friend.getUserId());
                                            String uid = friend.getUserId();
                                            userMap.put(uid, ExtendedUserInfo.obtain(friend));
                                            successIds.add(uid);
                                        }
                                    }
                                    updateCacheAndNotify(userMap.values());
                                    finishUserBatch(successIds);
                                    // 全部都是好友，则不用再请求UserProfile了
                                    if (!needRequestIds.isEmpty()) {
                                        requestUserProfiles(needRequestIds);
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    // 请求好友失败，请求UserProfile
                                    requestUserProfiles(userIds);
                                }
                            });
                }
            };

    /* 带重试请求用户资料，带通知逻辑 */
    private void requestUserProfiles(List<String> userIds) {
        Map<String, ExtendedUserInfo> userMap = new HashMap<>();
        List<String> successIds = new ArrayList<>();
        requestUserProfilesWithRetry(
                userIds,
                0,
                new IRongCoreCallback.ResultCallback<List<UserProfile>>() {
                    @Override
                    public void onSuccess(List<UserProfile> userProfiles) {
                        if (userProfiles != null && !userProfiles.isEmpty()) {
                            for (UserProfile profile : userProfiles) {
                                String uid = profile.getUserId();
                                userMap.put(uid, ExtendedUserInfo.obtain(profile));
                                successIds.add(uid);
                            }
                        }
                        updateCacheAndNotify(userMap.values());
                        finishUserBatch(successIds);
                    }

                    @Override
                    public void onError(IRongCoreEnum.CoreErrorCode e) {
                        // 最终失败，结束本批次
                        updateCacheAndNotify(userMap.values());
                        finishUserBatch(successIds);
                    }
                });
    }

    /* 请求用户资料，带重试逻辑 */
    private void requestUserProfilesWithRetry(
            final List<String> userIds,
            final int retryCount,
            final IRongCoreCallback.ResultCallback<List<UserProfile>> callback) {
        RongCoreClient.getInstance()
                .getUserProfiles(
                        userIds,
                        new IRongCoreCallback.ResultCallback<List<UserProfile>>() {
                            @Override
                            public void onSuccess(List<UserProfile> userProfiles) {
                                callback.onSuccess(userProfiles);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (shouldRetry(e, retryCount)) {
                                    postRetryDelayed(
                                            () ->
                                                    requestUserProfilesWithRetry(
                                                            userIds, retryCount + 1, callback));
                                } else {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    /* 请求好友信息，带重试逻辑 */
    private void requestFriendsInfoWithRetry(
            final List<String> userIds,
            final int retryCount,
            final IRongCoreCallback.ResultCallback<List<FriendInfo>> callback) {
        RongCoreClient.getInstance()
                .getFriendsInfo(
                        userIds,
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                            @Override
                            public void onSuccess(List<FriendInfo> friendInfos) {
                                callback.onSuccess(friendInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (shouldRetry(e, retryCount)) {
                                    postRetryDelayed(
                                            () ->
                                                    requestFriendsInfoWithRetry(
                                                            userIds, retryCount + 1, callback));
                                } else {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    // 群组信息拉取任务（按队列合并触发请求）
    private final Runnable batchGroupInfoRequestRunnable =
            new Runnable() {
                @Override
                public void run() {
                    final List<String> groupIds;
                    synchronized (pendingGroupIds) {
                        if (pendingGroupIds.size() > MAX_BATCH_REQUEST_SIZE) {
                            groupIds = new ArrayList<>(MAX_BATCH_REQUEST_SIZE);
                            Iterator<String> iterator = pendingGroupIds.iterator();
                            for (int i = 0; i < MAX_BATCH_REQUEST_SIZE; i++) {
                                String id = iterator.next();
                                groupIds.add(id);
                                iterator.remove();
                            }
                        } else {
                            groupIds = new ArrayList<>(pendingGroupIds);
                            pendingGroupIds.clear();
                        }
                    }

                    requestGroupsInfoWithRetry(
                            groupIds,
                            0,
                            new IRongCoreCallback.ResultCallback<List<GroupInfo>>() {
                                @Override
                                public void onSuccess(List<GroupInfo> groupInfos) {
                                    final List<String> successIds = new ArrayList<>();
                                    if (groupInfos != null) {
                                        for (GroupInfo info : groupInfos) {
                                            if (info != null) {
                                                groupInfoCache.put(info.getGroupId(), info);
                                                notifyGroupChange(info);
                                                successIds.add(info.getGroupId());
                                            }
                                        }
                                    }
                                    finishGroupBatch(successIds);
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    finishGroupBatch(new ArrayList<>());
                                }
                            });
                }
            };

    /* 请求群组信息，带重试逻辑 */
    private void requestGroupsInfoWithRetry(
            final List<String> groupIds,
            final int retryCount,
            final IRongCoreCallback.ResultCallback<List<GroupInfo>> callback) {
        RongCoreClient.getInstance()
                .getGroupsInfo(
                        groupIds,
                        new IRongCoreCallback.ResultCallback<List<GroupInfo>>() {
                            @Override
                            public void onSuccess(List<GroupInfo> groupInfos) {
                                callback.onSuccess(groupInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (shouldRetry(e, retryCount)) {
                                    postRetryDelayed(
                                            () ->
                                                    requestGroupsInfoWithRetry(
                                                            groupIds, retryCount + 1, callback));
                                } else {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    /* 结束一批用户信息拉取，清理状态并触发下一批 */
    private void finishUserBatch(List<String> successIds) {
        synchronized (pendingUserIds) {
            // 移除已成功获取的ID，防止重复请求
            if (successIds != null && !successIds.isEmpty()) {
                for (String successId : successIds) {
                    pendingUserIds.remove(successId);
                }
            }
            // 如果还有待处理的请求，触发下一批
            if (!pendingUserIds.isEmpty()) {
                mainHandler.postDelayed(batchUserProfileRequestRunnable, 0);
            } else {
                // 重置标记位为false
                isUserBatchRunning = false;
            }
        }
    }

    /* 结束一批群组信息拉取，清理状态并触发下一批 */
    private void finishGroupBatch(List<String> successIds) {
        synchronized (pendingGroupIds) {
            // 移除已成功获取的ID，防止重复请求
            if (successIds != null && !successIds.isEmpty()) {
                for (String successId : successIds) {
                    pendingGroupIds.remove(successId);
                }
            }
            // 如果还有待处理的请求，触发下一批
            if (!pendingGroupIds.isEmpty()) {
                mainHandler.post(batchGroupInfoRequestRunnable);
            } else {
                // 重置标记位为false
                isGroupBatchRunning = false;
            }
        }
    }

    // 群成员信息拉取任务（按队列合并触发请求）
    private final Runnable batchGroupMemberInfoRunnable =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (pendingGroupMemberRequests) {
                        Iterator<Map.Entry<String, Set<String>>> iterator =
                                pendingGroupMemberRequests.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<String, Set<String>> entry = iterator.next();
                            String groupId = entry.getKey();
                            // 如果该群组正在执行请求，则跳过
                            if (executingGroupIds.contains(groupId)) {
                                continue;
                            }

                            Set<String> pendingIds = entry.getValue();
                            if (pendingIds == null || pendingIds.isEmpty()) {
                                iterator.remove();
                                continue;
                            }

                            // 标记该群组正在执行
                            executingGroupIds.add(groupId);

                            // 取出最多 MAX_GROUP_MEMBER_BATCH_SIZE 个 ID
                            List<String> batchIds = new ArrayList<>();
                            Iterator<String> idIterator = pendingIds.iterator();
                            int count = 0;
                            while (idIterator.hasNext() && count < MAX_BATCH_REQUEST_SIZE) {
                                batchIds.add(idIterator.next());
                                idIterator.remove();
                                count++;
                            }

                            // 如果取完后集合为空，移除该 Key
                            if (pendingIds.isEmpty()) {
                                iterator.remove();
                            }

                            requestGroupMembersWithRetry(
                                    groupId,
                                    batchIds,
                                    0,
                                    new IRongCoreCallback.ResultCallback<List<GroupMemberInfo>>() {
                                        @Override
                                        public void onSuccess(
                                                List<GroupMemberInfo> groupMemberInfos) {
                                            if (groupMemberInfos != null) {
                                                for (GroupMemberInfo info : groupMemberInfos) {
                                                    String key =
                                                            generateGroupMemberKey(
                                                                    groupId, info.getUserId());
                                                    groupMemberInfoCache.put(key, info);
                                                    notifyGroupMemberChange(groupId, info);
                                                }
                                            }
                                            finishGroupMemberBatch(groupId);
                                        }

                                        @Override
                                        public void onError(IRongCoreEnum.CoreErrorCode e) {
                                            finishGroupMemberBatch(groupId);
                                        }
                                    });
                        }
                    }
                }
            };

    /* 结束某个群的成员拉取批次，必要时继续下一批 */
    private void finishGroupMemberBatch(String groupId) {
        synchronized (pendingGroupMemberRequests) {
            // 从 executingGroupIds 中移除，允许该群组处理下一批
            executingGroupIds.remove(groupId);

            // 检查是否还有待处理的任务（任何群组）
            if (!pendingGroupMemberRequests.isEmpty()) {
                mainHandler.post(batchGroupMemberInfoRunnable);
            }
        }
    }

    /* 更新用户缓存并通知观察者 */
    private void updateCacheAndNotify(Collection<ExtendedUserInfo> userInfos) {
        if (userInfos.isEmpty()) {
            return;
        }
        for (ExtendedUserInfo info : userInfos) {
            extendedUserInfoCache.put(info.getUserId(), info);
            notifyUserChange(info);
        }
    }

    /* 保证在主线程执行指定任务 */
    private void runOnMainThread(Runnable action) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            action.run();
        } else {
            ExecutorHelper.getInstance().mainThread().execute(action);
        }
    }

    /* 当前用户资料请求结束后的收尾逻辑 */
    private void finishMyProfileRequest() {
        synchronized (myUserProfileLock) {
            if (hasPendingMyUserProfileRequest) {
                hasPendingMyUserProfileRequest = false;
                mainHandler.post(myUserProfileRequestRunnable);
            } else {
                isMyUserProfileRunning = false;
            }
        }
    }

    /* 执行当前用户资料的实际获取 */
    private final Runnable myUserProfileRequestRunnable =
            new Runnable() {
                @Override
                public void run() {
                    requestMyUserProfileWithRetry(
                            0,
                            new IRongCoreCallback.ResultCallback<UserProfile>() {
                                @Override
                                public void onSuccess(UserProfile userProfile) {
                                    if (userProfile != null) {
                                        ExtendedUserInfo extendedUserInfo =
                                                ExtendedUserInfo.obtain(userProfile);
                                        extendedUserInfoCache.put(
                                                userProfile.getUserId(), extendedUserInfo);
                                        notifyUserChange(extendedUserInfo);
                                    }
                                    finishMyProfileRequest();
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    finishMyProfileRequest();
                                }
                            });
                }
            };

    /* 获取当前用户资料，带重试逻辑 */
    private void requestMyUserProfileWithRetry(
            final int retryCount, final IRongCoreCallback.ResultCallback<UserProfile> callback) {
        RongCoreClient.getInstance()
                .getMyUserProfile(
                        new IRongCoreCallback.ResultCallback<UserProfile>() {
                            @Override
                            public void onSuccess(UserProfile userProfile) {
                                callback.onSuccess(userProfile);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (shouldRetry(e, retryCount)) {
                                    postRetryDelayed(
                                            () ->
                                                    requestMyUserProfileWithRetry(
                                                            retryCount + 1, callback));
                                } else {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    /* 请求指定群里多个成员信息列表，带重试逻辑 */
    private void requestGroupMembersWithRetry(
            final String groupId,
            final List<String> userIds,
            final int retryCount,
            final IRongCoreCallback.ResultCallback<List<GroupMemberInfo>> callback) {
        RongCoreClient.getInstance()
                .getGroupMembers(
                        groupId,
                        userIds,
                        new IRongCoreCallback.ResultCallback<List<GroupMemberInfo>>() {
                            @Override
                            public void onSuccess(List<GroupMemberInfo> groupMemberInfos) {
                                callback.onSuccess(groupMemberInfos);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (shouldRetry(e, retryCount)) {
                                    postRetryDelayed(
                                            () ->
                                                    requestGroupMembersWithRetry(
                                                            groupId,
                                                            userIds,
                                                            retryCount + 1,
                                                            callback));
                                } else {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    /* 判断当前错误是否需要进行重试 */
    private boolean shouldRetry(IRongCoreEnum.CoreErrorCode errorCode, int retryCount) {
        return (errorCode == IRongCoreEnum.CoreErrorCode.NET_DATA_IS_SYNCHRONIZING
                        || errorCode == IRongCoreEnum.CoreErrorCode.RC_REQUEST_OVERFREQUENCY)
                && retryCount < MAX_RETRY_COUNT;
    }

    /* 统一的重试延迟调度 */
    private void postRetryDelayed(Runnable runnable) {
        mainHandler.postDelayed(runnable, RETRY_REQUEST_DELAY_MS);
    }

    /**
     * 生成群组成员缓存的 key
     *
     * @param groupId 群组 ID
     * @param userId 用户 ID
     * @return 缓存 key
     */
    private String generateGroupMemberKey(String groupId, String userId) {
        return String.format("%s_%s", groupId, userId);
    }
}
