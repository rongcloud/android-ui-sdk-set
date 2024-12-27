package io.rong.imkit.userinfo;

import android.os.Build;
import android.os.Looper;
import android.util.LruCache;
import android.util.Pair;
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
import io.rong.imlib.model.Message;
import io.rong.imlib.model.SubscribeEvent;
import io.rong.imlib.model.SubscribeEventRequest;
import io.rong.imlib.model.SubscribeInfoEvent;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.model.UserProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 功能描述: 用户信息管理类，用来获取用户信息。
 *
 * @author rongcloud
 * @since 5.10.5
 */
class UserManageHelper {
    public static final String TAG = "UserManageHelper";
    private static final int MAX_SIZE = 1000;
    private static final int MAX_RETRY_COUNT = 3;

    private final LruCache<String, ExtendedUserInfo> extendedUserInfoCache =
            new LruCache<>(MAX_SIZE);
    private final LruCache<String, GroupInfo> groupInfoCache = new LruCache<>(MAX_SIZE);
    private final LruCache<String, GroupMemberInfo> groupMemberInfoCache = new LruCache<>(MAX_SIZE);
    private final Map<String, UserInfo> userInfoCacheFromMessage = new ConcurrentHashMap<>();
    private final List<String> subscribeEventsErrorUserIds = new CopyOnWriteArrayList<>();
    private final List<RongUserInfoManager.UserDataObserver> mUserDataObservers =
            new CopyOnWriteArrayList<>();

    private final Map<String, Integer> getUserInfoErrorMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> getGroupInfoErrorMap = new ConcurrentHashMap<>();
    private final Map<Pair<String, String>, Integer> getGroupUserInfoErrorMap =
            new ConcurrentHashMap<>();

    private ExtendedUserInfo currentExtendedUserInfo;
    private String currentUserId;

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
                                    userInfoCacheFromMessage.clear();
                                    currentUserId = userId;
                                    retryErrorData();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ConnectionErrorCode e) {}

                            @Override
                            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus code) {}
                        });

        // 定义并初始化订阅事件监听器
        OnSubscribeEventListener subscribeEventListener =
                new OnSubscribeEventListener() {
                    @Override
                    public void onEventChange(List<SubscribeInfoEvent> subscribeEvents) {
                        for (SubscribeInfoEvent subscribeInfoEvent : subscribeEvents) {
                            refreshUserInfoInner(subscribeInfoEvent.getUserId());
                        }
                    }

                    @Override
                    public void onSubscriptionSyncCompleted(SubscribeEvent.SubscribeType type) {
                        retryErrorData();
                    }
                };

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

        IMCenter.getInstance().addSubscribeEventListener(subscribeEventListener);
        IMCenter.getInstance().addGroupEventListener(groupEventListener);

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

        // 添加异步消息接收监听器
        IMCenter.getInstance()
                .addAsyncOnReceiveMessageListener(
                        new RongIMClient.OnReceiveMessageWrapperListener() {
                            @Override
                            public boolean onReceived(
                                    Message message, int i, boolean b, boolean b1) {
                                if (message != null
                                        && message.getContent() != null
                                        && message.getContent().getUserInfo() != null) {
                                    final UserInfo userInfo = message.getContent().getUserInfo();
                                    // 缓存用户信息
                                    if (userInfo != null) {
                                        userInfoCacheFromMessage.put(
                                                userInfo.getUserId(), userInfo);
                                    }
                                }
                                return false;
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

    private void refreshGroupMemberInfoInner(String groupId, GroupMemberInfo memberInfo) {
        groupMemberInfoCache.remove(generateGroupMemberKey(groupId, memberInfo.getUserId()));
        getGroupUserInfo(groupId, memberInfo.getUserId());
    }

    private void refreshGroupInfoInner(String groupId) {
        groupInfoCache.remove(groupId);
        getGroupInfo(groupId);
    }

    private void refreshUserInfoInner(String userId) {
        extendedUserInfoCache.remove(userId);
        getUserInfo(userId);
    }

    /**
     * 是否包含用户信息
     *
     * @param userId 用户 ID
     * @return 是否包含用户信息
     */
    boolean isContainFromMessage(String userId) {
        return userInfoCacheFromMessage.containsKey(userId);
    }

    /**
     * 从消息中获取用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserInfo getUserInfoFromMessage(String userId) {
        return userInfoCacheFromMessage.get(userId);
    }

    /**
     * 获取用户信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    ExtendedUserInfo getUserInfo(@NonNull final String userId) {
        ExtendedUserInfo cachedExtendedUserInfo = extendedUserInfoCache.get(userId);
        if (cachedExtendedUserInfo != null) {
            if (subscribeEventsErrorUserIds.contains(userId)) {
                subscribeToUserProfileEvents(userId);
            }
            return cachedExtendedUserInfo;
        }

        subscribeToUserProfileEvents(userId);

        String currentUserId = RongCoreClient.getInstance().getCurrentUserId();
        if (Objects.equals(currentUserId, userId)) {
            RongCoreClient.getInstance()
                    .getMyUserProfile(
                            new IRongCoreCallback.ResultCallback<UserProfile>() {
                                @Override
                                public void onSuccess(UserProfile userProfile) {
                                    if (userProfile != null) {
                                        ExtendedUserInfo extendedUserInfo =
                                                ExtendedUserInfo.obtain(userProfile);
                                        extendedUserInfoCache.put(userId, extendedUserInfo);
                                        getUserInfoErrorMap.remove(userId); // 成功后清除错误计数
                                        notifyUserChange(extendedUserInfo);
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    // 记录错误并根据错误计数决定是否重新获取
                                    RLog.e(
                                            TAG,
                                            "getUserInfo error for current user: " + e.toString());
                                    if (canRetry(userId, getUserInfoErrorMap)) {
                                        incrementErrorCount(userId, getUserInfoErrorMap);
                                        getUserInfo(userId); // 重新获取
                                    } else {
                                        RLog.e(
                                                TAG,
                                                "Max retry count reached for getUserInfo: "
                                                        + userId);
                                    }
                                }
                            });
        } else {
            RongCoreClient.getInstance()
                    .getUserProfiles(
                            Collections.singletonList(userId),
                            new IRongCoreCallback.ResultCallback<List<UserProfile>>() {
                                @Override
                                public void onSuccess(List<UserProfile> userProfiles) {
                                    if (userProfiles != null && !userProfiles.isEmpty()) {
                                        ExtendedUserInfo extendedUserInfo =
                                                ExtendedUserInfo.obtain(userProfiles.get(0));
                                        RongCoreClient.getInstance()
                                                .getFriendsInfo(
                                                        Collections.singletonList(userId),
                                                        new IRongCoreCallback.ResultCallback<
                                                                List<FriendInfo>>() {
                                                            @Override
                                                            public void onSuccess(
                                                                    List<FriendInfo> friendInfos) {
                                                                if (friendInfos != null
                                                                        && !friendInfos.isEmpty()) {
                                                                    extendedUserInfo.setAlias(
                                                                            friendInfos
                                                                                    .get(0)
                                                                                    .getRemark());
                                                                }
                                                                extendedUserInfoCache.put(
                                                                        userId, extendedUserInfo);
                                                                notifyUserChange(extendedUserInfo);
                                                                getUserInfoErrorMap.remove(userId);
                                                            }

                                                            @Override
                                                            public void onError(
                                                                    IRongCoreEnum.CoreErrorCode e) {
                                                                RLog.e(
                                                                        TAG,
                                                                        "getFriendsInfo error: "
                                                                                + e.toString());
                                                                if (canRetry(
                                                                        userId,
                                                                        getUserInfoErrorMap)) {
                                                                    incrementErrorCount(
                                                                            userId,
                                                                            getUserInfoErrorMap);
                                                                    getUserInfo(userId); // 重新获取
                                                                } else {
                                                                    RLog.e(
                                                                            TAG,
                                                                            "Max retry count reached for getFriendsInfo: "
                                                                                    + userId);
                                                                }
                                                            }
                                                        });
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    // 记录错误并根据错误计数决定是否重新获取
                                    RLog.e(TAG, "getUserInfo error: " + e.toString());
                                    if (canRetry(userId, getUserInfoErrorMap)) {
                                        incrementErrorCount(userId, getUserInfoErrorMap);
                                        getUserInfo(userId); // 重新获取
                                    } else {
                                        RLog.e(
                                                TAG,
                                                "Max retry count reached for getUserInfo: "
                                                        + userId);
                                    }
                                }
                            });
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
        GroupInfo cachedInfo = groupInfoCache.get(groupId);
        if (cachedInfo != null) {
            return cachedInfo;
        }

        RongCoreClient.getInstance()
                .getGroupsInfo(
                        Arrays.asList(groupId),
                        new IRongCoreCallback.ResultCallback<List<GroupInfo>>() {
                            @Override
                            public void onSuccess(List<GroupInfo> groupInfos) {
                                if (groupInfos != null && !groupInfos.isEmpty()) {
                                    GroupInfo groupInfo = groupInfos.get(0);
                                    groupInfoCache.put(groupId, groupInfo);
                                    notifyGroupChange(groupInfo);
                                    getGroupInfoErrorMap.remove(groupId);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                // 记录错误并根据错误计数决定是否重新获取
                                RLog.e(TAG, "getGroupInfo error: " + e.toString());
                                if (canRetry(groupId, getGroupInfoErrorMap)) {
                                    incrementErrorCount(groupId, getGroupInfoErrorMap);
                                    getGroupInfo(groupId); // 重新获取
                                } else {
                                    RLog.e(
                                            TAG,
                                            "Max retry count reached for getGroupInfo: " + groupId);
                                }
                            }
                        });

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

        RongCoreClient.getInstance()
                .getGroupMembers(
                        groupId,
                        Arrays.asList(userId),
                        new IRongCoreCallback.ResultCallback<List<GroupMemberInfo>>() {
                            @Override
                            public void onSuccess(List<GroupMemberInfo> groupMemberInfos) {
                                if (groupMemberInfos != null && !groupMemberInfos.isEmpty()) {
                                    GroupMemberInfo groupMemberInfo = groupMemberInfos.get(0);
                                    groupMemberInfoCache.put(key, groupMemberInfo);
                                    notifyGroupMemberChange(groupId, groupMemberInfo);
                                    getGroupUserInfoErrorMap.remove(Pair.create(groupId, userId));
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                // 记录错误并根据错误计数决定是否重新获取
                                RLog.e(TAG, "getGroupUserInfo error: " + e.toString());
                                Pair<String, String> pair = Pair.create(groupId, userId);
                                if (canRetry(pair, getGroupUserInfoErrorMap)) {
                                    incrementErrorCount(pair, getGroupUserInfoErrorMap);
                                    getGroupUserInfo(groupId, userId); // 重新获取
                                } else {
                                    RLog.e(
                                            TAG,
                                            "Max retry count reached for getGroupUserInfo: " + key);
                                }
                            }
                        });

        return null;
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
                        new IRongCoreCallback.UpdateUserProfileCallback() {
                            @Override
                            public void onSuccess() {
                                notifyUserChange(currentExtendedUserInfo);
                            }

                            @Override
                            public void onError(int errorCode, String errorData) {
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
                            new IRongCoreCallback.UpdateUserProfileCallback() {
                                @Override
                                public void onSuccess() {
                                    refreshUserInfoInner(extendedUserInfo.getUserId());
                                }

                                @Override
                                public void onError(int errorCode, String errorData) {
                                    // Handle error
                                }
                            });
        } else {
            RongCoreClient.getInstance()
                    .setFriendInfo(
                            extendedUserInfo.getUserProfile().getUserId(),
                            extendedUserInfo.getUserProfile().getName(),
                            extendedUserInfo.getUserProfile().getUserExtProfile(),
                            new IRongCoreCallback.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    refreshUserInfoInner(
                                            extendedUserInfo.getUserProfile().getUserId());
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
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
                        new IRongCoreCallback.OperationCallbackEx<String>() {
                            @Override
                            public void onSuccess() {
                                if (groupInfo != null) {
                                    refreshGroupInfoInner(groupInfo.getGroupId());
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, String errorData) {
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
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                refreshGroupMemberInfoInner(groupId, groupMemberInfo);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                // Handle error
                            }
                        });
    }

    private void subscribeToUserProfileEvents(@NonNull final String userId) {
        SubscribeEventRequest subscribeEventRequest =
                new SubscribeEventRequest(
                        SubscribeEvent.SubscribeType.USER_PROFILE,
                        Collections.singletonList(userId));
        RongCoreClient.getInstance()
                .subscribeEvent(
                        subscribeEventRequest,
                        new IRongCoreCallback.SubscribeEventCallback<List<String>>() {
                            @Override
                            public void onSuccess() {
                                subscribeEventsErrorUserIds.remove(userId);
                            }

                            @Override
                            public void onError(int errorCode, List<String> errorData) {
                                subscribeEventsErrorUserIds.add(userId);
                            }
                        });
    }

    private void notifyUserChange(ExtendedUserInfo extendedUserInfo) {
        runOnMainThread(
                () -> {
                    for (RongUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        observer.onUserUpdate(extendedUserInfo);
                    }
                });
    }

    private void notifyGroupChange(GroupInfo groupInfo) {
        runOnMainThread(
                () -> {
                    for (RongUserInfoManager.UserDataObserver observer : mUserDataObservers) {
                        observer.onGroupUpdate(ExtendedGroup.obtain(groupInfo));
                    }
                });
    }

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

    private void runOnMainThread(Runnable action) {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            action.run();
        } else {
            ExecutorHelper.getInstance().mainThread().execute(action);
        }
    }

    private void retryErrorData() {
        for (String groupId : getGroupInfoErrorMap.keySet()) {
            getGroupInfo(groupId);
        }
        for (String userId : getUserInfoErrorMap.keySet()) {
            getUserInfo(userId);
        }
        for (Pair<String, String> pair : getGroupUserInfoErrorMap.keySet()) {
            getGroupUserInfo(pair.first, pair.second);
        }
    }

    // 使用错误计数结构记录和限制重试次数
    private boolean canRetry(String key, Map<String, Integer> errorCountMap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return errorCountMap.getOrDefault(key, 0) < MAX_RETRY_COUNT;
        } else {
            return errorCountMap.containsKey(key) && errorCountMap.get(key) < MAX_RETRY_COUNT;
        }
    }

    private boolean canRetry(
            Pair<String, String> key, Map<Pair<String, String>, Integer> errorCountMap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return errorCountMap.getOrDefault(key, 0) < MAX_RETRY_COUNT;
        } else {
            return errorCountMap.containsKey(key) && errorCountMap.get(key) < MAX_RETRY_COUNT;
        }
    }

    private void incrementErrorCount(String key, Map<String, Integer> errorCountMap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            errorCountMap.put(key, errorCountMap.getOrDefault(key, 0) + 1);
        } else {
            errorCountMap.put(key, errorCountMap.containsKey(key) ? errorCountMap.get(key) + 1 : 1);
        }
    }

    private void incrementErrorCount(
            Pair<String, String> key, Map<Pair<String, String>, Integer> errorCountMap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            errorCountMap.put(key, errorCountMap.getOrDefault(key, 0) + 1);
        } else {
            errorCountMap.put(key, errorCountMap.containsKey(key) ? errorCountMap.get(key) + 1 : 1);
        }
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
