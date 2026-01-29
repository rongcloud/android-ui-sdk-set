package io.rong.imkit.manager;

import static io.rong.imlib.IRongCoreEnum.CoreErrorCode.*;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.handler.AppSettingsHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.listener.FriendEventListener;
import io.rong.imlib.listener.OnSubscribeEventListener;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.PlatformOnlineStatus;
import io.rong.imlib.model.SubscribeEvent;
import io.rong.imlib.model.SubscribeEventRequest;
import io.rong.imlib.model.SubscribeInfoEvent;
import io.rong.imlib.model.SubscribeUserOnlineStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 在线状态管理类
 *
 * @since 5.32.0
 */
public class OnLineStatusManager {
    private static final String TAG = "OnLineStatusManager";
    // 默认订阅时间7天
    private static final int DEFAULT_SUBSCRIBE_ONLINE_TIME = 7 * 24 * 60 * 60;
    // 订阅有效期时间：6天。当触发订阅时候，如果当前时间减去已订阅且小于这个时间，则不会再次订阅
    private static final int EXPIRED_SUBSCRIBE_ONLINE_TIME = 6 * 24 * 60 * 60;
    // 默认最大批量查询数量
    private static final int DEFAULT_MAX_BATCH_SIZE = 100;
    // 默认最大查询已订阅事件的批量查询数
    private static final int MAX_QUERY_SUBSCRIBE_BATCH_SIZE = 100;
    // 订阅接口最大批量订阅数量
    private static final int MAX_SUBSCRIBE_EVENT_BATCH_SIZE = 20;
    // 订阅接口最大批量订阅数量，当使用 MAX_SUBSCRIBE_EVENT_BATCH_SIZE 超过订阅总数时，使用该参数查询。
    private static final int MIN_SUBSCRIBE_EVENT_BATCH_SIZE = 1;
    // 延迟执行任务时间
    private static final int DEFAULT_DELAY_TIME = 500;
    // 等待好友状态同步完成延时时间
    private static final int SUBSCRIPTION_SYNC_COMPLETED_DELAYED_TIME = 500;
    // 最大订阅数量
    private static final int DEFAULT_SUBSCRIBE_NUMBER = 1000;
    // 接口重试最大次数
    private static final int MAX_RETRY_COUNT = 10;
    // 订阅信息缓存
    private final Map<String, Long> mSubscribeInfoCache = new ConcurrentHashMap<>();
    // 好友信息缓存
    private final Map<String, String> mFriendUserIdCache = new ConcurrentHashMap<>();
    // 在线状态缓存
    private final Map<String, SubscribeUserOnlineStatus> mOnlineStatusCache =
            new ConcurrentHashMap<>();
    // 正在请求好友的的请求缓存
    private final Map<String, String> mGetFriendRequestCache = new ConcurrentHashMap<>();
    // 在线状态监听
    private final List<OnLineStatusListener> mOnLineStatusListeners = new CopyOnWriteArrayList<>();
    // 在线状态数据源接口。当订阅超限时会根据此接口来决定优先订阅列表
    private OnlineStatusDataSource mOnlineStatusDataSource;
    // OnlineStatusDataSource#onPriorityUserList 返回的数据
    private final List<String> mPriorityUserList = new CopyOnWriteArrayList<>();
    // Handler
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    // 是否同步完好友状态
    private volatile boolean mHasFriendOnlineSyncCompleted = false;
    // 好友事件监听
    private final FriendEventListener mFriendEventListener =
            new FriendEventListener() {
                @Override
                public void onFriendAdd(
                        DirectionType directionType,
                        String userId,
                        String name,
                        String portraitUri,
                        long operationTime) {
                    RLog.d(TAG, "onFriendAdd: " + userId + "," + operationTime);
                    if (TextUtils.isEmpty(userId)) {
                        return;
                    }
                    // 收到添加好友回调
                    // 1，取消订阅该用户在线状态
                    List<String> userList = new ArrayList<>();
                    userList.add(userId);
                    RLog.d(TAG, "onFriendAdd batchUnsubscribeOnlineStatusEvent");
                    batchUnsubscribeOnlineStatusEvent(userList, false, null);
                    // 2，添加到好友用户ID缓存中
                    mFriendUserIdCache.put(userId, userId);
                    // 3，获取在线状态, 会覆盖更新缓存。如果没开好友在线状态，则不调用getOnlineStatus获取在线状态。
                    RLog.d(TAG, "onFriendAdd OnlineStatus");
                    if (AppSettingsHandler.getInstance().isFriendOnlineStatusSubscribeEnable()) {
                        getOnlineStatus(userList, null);
                    }
                }

                @Override
                public void onFriendDelete(
                        DirectionType directionType, List<String> userIds, long operationTime) {
                    RLog.d(TAG, "onFriendDelete: " + userIds + "," + operationTime);
                    if (userIds.isEmpty()) {
                        return;
                    }
                    // 收到好友删除回调
                    // 1，移除好友删除的用户ID列表的好友信息缓存
                    for (String userId : userIds) {
                        mFriendUserIdCache.remove(userId);
                    }
                    // 2，订阅这些用户并查询在线状态
                    RLog.d(TAG, "onFriendDelete subscribeEvent");
                    batchSubscribeEvent(userIds, MAX_SUBSCRIBE_EVENT_BATCH_SIZE);
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        String userId,
                        FriendApplicationType applicationType,
                        FriendApplicationStatus applicationStatus,
                        DirectionType directionType,
                        long operationTime,
                        String extra) {
                    // do nothing
                }

                @Override
                public void onFriendCleared(long operationTime) {
                    RLog.d(TAG, "onFriendCleared: " + operationTime);
                    // 收到好友清理回调
                    // 1，获取所有好友ID
                    List<String> userIds = new ArrayList<>(mFriendUserIdCache.keySet());
                    // 2, 移除所有好友信息缓存
                    mFriendUserIdCache.clear();
                    // 3，订阅这些用户并查询在线状态，查询后更新到缓存中
                    RLog.d(TAG, "onFriendCleared batchSubscribeEvent");
                    batchSubscribeEvent(userIds, MAX_SUBSCRIBE_EVENT_BATCH_SIZE);
                }

                @Override
                public void onFriendInfoChangedSync(
                        String userId,
                        String remark,
                        Map<String, String> extProfile,
                        long operationTime) {
                    // do nothing
                }
            };
    // 订阅事件
    private final OnSubscribeEventListener mOnSubscribeEventListener =
            new OnSubscribeEventListener() {
                @Override
                public void onEventChange(List<SubscribeInfoEvent> subscribeEvents) {
                    RLog.d(TAG, "onEventChange: " + formatLogString(subscribeEvents));
                    // 处理在线状态订阅事件回调
                    onOnlineEventChange(subscribeEvents);
                }

                @Override
                public void onSubscriptionSyncCompleted(SubscribeEvent.SubscribeType type) {
                    RLog.d(TAG, "onSubscriptionSyncCompleted: " + type);
                    if (SubscribeEvent.SubscribeType.FRIEND_ONLINE_STATUS == type) {
                        mHasFriendOnlineSyncCompleted = true;
                    }
                }

                @Override
                public void onSubscriptionChangedOnOtherDevices(
                        List<SubscribeEvent> subscribeEvents) {
                    RLog.d(TAG, "onSubscriptionChangedOnOtherDevices: " + subscribeEvents.size());
                    // 多端同步订阅状态
                    onSubscriptionChanged(subscribeEvents);
                }

                /**
                 * 收到订阅事件回调
                 *
                 * <p>1,处理在线状态类型的订阅事件，更新到缓存中
                 */
                private void onOnlineEventChange(List<SubscribeInfoEvent> subscribeEvents) {
                    if (subscribeEvents.isEmpty()) {
                        return;
                    }
                    // 收到订阅事件监听，1,更新缓存，2,通知注册的Listener
                    List<String> changeUserList = new ArrayList<>();
                    for (SubscribeInfoEvent event : subscribeEvents) {
                        SubscribeEvent.SubscribeType type = event.getSubscribeType();
                        if (SubscribeEvent.SubscribeType.ONLINE_STATUS == type
                                || SubscribeEvent.SubscribeType.FRIEND_ONLINE_STATUS == type) {
                            changeUserList.add(event.getUserId());
                        }
                    }
                    if (!changeUserList.isEmpty()) {
                        RLog.d(TAG, "onEventChange getOnlineStatus: " + changeUserList);
                        getOnlineStatus(changeUserList, null);
                    }
                }

                /**
                 * 收到多端同步订阅状态，处理订阅和非订阅场景
                 *
                 * <p>1,找到 SUBSCRIBE 操作类型的 在线状态 类型事件的用户ID列表，拉取在线状态到缓存中
                 */
                private void onSubscriptionChanged(List<SubscribeEvent> subscribeEvents) {
                    if (subscribeEvents.isEmpty()) {
                        return;
                    }
                    RLog.d(TAG, "onSubscriptionChanged: " + subscribeEvents.size());
                    List<String> subscribeList = new ArrayList<>();
                    HashMap<String, SubscribeUserOnlineStatus> unsubscribeMap = new HashMap<>();
                    for (SubscribeEvent event : subscribeEvents) {
                        if (SubscribeEvent.SubscribeType.ONLINE_STATUS == event.getSubscribeType()
                                || SubscribeEvent.SubscribeType.FRIEND_ONLINE_STATUS
                                        == event.getSubscribeType()) {
                            String userId = event.getUserId();
                            if (event.getOperationType()
                                    == SubscribeEvent.OperationType.SUBSCRIBE) {
                                mSubscribeInfoCache.put(userId, event.getSubscribeTime());
                                subscribeList.add(userId);
                            } else {
                                mSubscribeInfoCache.remove(userId);
                                // 如果能够移除，则需要通知移除的为离线状态。
                                if (mOnlineStatusCache.remove(userId) != null) {
                                    unsubscribeMap.put(
                                            userId,
                                            new SubscribeUserOnlineStatus(userId, null, false));
                                }
                            }
                        }
                    }
                    if (!subscribeList.isEmpty()) {
                        RLog.d(TAG, "onSubscriptionChanged subscribe: " + subscribeList);
                        getOnlineStatus(subscribeList, null);
                    }
                    if (!unsubscribeMap.isEmpty()) {
                        RLog.d(
                                TAG,
                                "onSubscriptionChanged unsubscribe: " + unsubscribeMap.keySet());
                        notifyStatusChange(unsubscribeMap);
                    }
                }
            };
    // 连接监听
    private final RongIMClient.ConnectionStatusListener mConnectionStatusListener =
            status -> {
                // 主动退出登录，清空缓存
                if (status == RongIMClient.ConnectionStatusListener.ConnectionStatus.SIGN_OUT) {
                    onSignOut();
                }
            };

    private OnLineStatusManager() {}

    private static class Holder {
        private static final OnLineStatusManager INSTANCE = new OnLineStatusManager();
    }

    public static OnLineStatusManager getInstance() {
        return OnLineStatusManager.Holder.INSTANCE;
    }

    /** 初始化 */
    public void init() {
        RLog.d(TAG, "init");
        IMCenter.getInstance().addFriendEventListener(mFriendEventListener);
        IMCenter.getInstance().addSubscribeEventListener(mOnSubscribeEventListener);
        IMCenter.getInstance().addConnectionStatusListener(mConnectionStatusListener);
    }

    /** 退出登录，清理缓存 */
    public void onSignOut() {
        RLog.d(TAG, "clearCache");
        mMainHandler.removeCallbacksAndMessages(null);
        mHasFriendOnlineSyncCompleted = false;
        mSubscribeInfoCache.clear();
        mFriendUserIdCache.clear();
        mOnlineStatusCache.clear();
        mGetFriendRequestCache.clear();
        mPriorityUserList.clear();
    }

    /** 添加在线状态变更监听接口 */
    public void addOnLineStatusListener(OnLineStatusListener listener) {
        if (listener != null && !mOnLineStatusListeners.contains(listener)) {
            RLog.d(TAG, "addOnLineStatusListener");
            mOnLineStatusListeners.add(listener);
        }
    }

    /** 移除在线状态变更监听接口 */
    public void removeOnLineStatusListener(OnLineStatusListener listener) {
        if (listener != null) {
            RLog.d(TAG, "removeOnLineStatusListener");
            mOnLineStatusListeners.remove(listener);
        }
    }

    /** 清除优先级用户列表缓存 */
    public void clearPriorityUserList() {
        mPriorityUserList.clear();
    }

    /** 设置在线状态订阅数据源接口 */
    public void setOnlineStatusDataSource(OnlineStatusDataSource dataSource) {
        RLog.d(TAG, "setOnlineStatusDataSource dataSource " + dataSource);
        mOnlineStatusDataSource = dataSource;
    }

    /** 移除在线状态订阅数据源接口 */
    public void removeOnlineStatusDataSource(OnlineStatusDataSource dataSource) {
        RLog.d(TAG, "removeOnlineStatusDataSource dataSource " + dataSource);
        if (dataSource == mOnlineStatusDataSource) {
            mOnlineStatusDataSource = null;
        }
    }

    /** 获取缓存的在线状态。 */
    public Map<String, SubscribeUserOnlineStatus> getUsersOnlineStatusCache() {
        return mOnlineStatusCache;
    }

    /**
     * 根据用户ID列表拉取在线状态（好友 + 非好友）
     *
     * <p>注意：
     *
     * <p>1，该接口的结果只会通过 addOnLineStatusListener 设置的 OnLineStatusListener 返回,页面销毁时要调用
     * removeOnLineStatusListener.
     *
     * <p>2，使用 getUserOnlineStatus 如果有缓存则直接返回，否则会拉取在线状态，通过 OnLineStatusListener 返回。
     *
     * <p>
     */
    public void fetchUsersOnlineStatus(String userId, boolean processSubscribedLimit) {
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        batchFetchUsersOnlineStatus(Collections.singletonList(userId), processSubscribedLimit, 0);
    }

    /**
     * 根据用户ID列表拉取在线状态（好友 + 非好友）
     *
     * <p>注意：
     *
     * <p>1，该接口的结果只会通过 addOnLineStatusListener 设置的 OnLineStatusListener 返回,页面销毁时要调用
     * removeOnLineStatusListener.
     *
     * <p>2，使用 getUserOnlineStatus 如果有缓存则直接返回，否则会拉取在线状态，通过 OnLineStatusListener 返回。
     *
     * <p>
     */
    public void fetchUsersOnlineStatus(List<String> userIdList) {
        batchFetchUsersOnlineStatus(userIdList, true, 0);
    }

    private void batchFetchUsersOnlineStatus(
            List<String> userIdList, boolean processSubscribedLimit, int retryCount) {
        if (userIdList == null || userIdList.isEmpty()) {
            return;
        }
        // 增加最大重试次数限制 20 次 (约10秒)
        int MAX_SYNC_WAIT_RETRY = 20;
        if (mHasFriendOnlineSyncCompleted && AppSettingsHandler.getInstance().hasInit()) {
            fetchUsersOnlineStatusImpl(userIdList, processSubscribedLimit);
        } else {
            if (retryCount < MAX_SYNC_WAIT_RETRY) {
                mMainHandler.postDelayed(
                        () ->
                                batchFetchUsersOnlineStatus(
                                        userIdList, processSubscribedLimit, retryCount + 1),
                        SUBSCRIPTION_SYNC_COMPLETED_DELAYED_TIME);
            } else {
                RLog.e(TAG, "fetchUsersOnlineStatus list timeout");
            }
        }
    }

    /**
     * 根据用户ID列表拉取在线状态（好友 + 非好友）
     *
     * @param userList 用户ID列表
     * @param processSubscribedLimit 是否处理订阅超限
     */
    private void fetchUsersOnlineStatusImpl(List<String> userList, boolean processSubscribedLimit) {
        RLog.d(
                TAG,
                "fetchOnlineStatus userList: "
                        + userList
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit);
        // 查询内存中保存的在线状态，分别放到缓存结果列表和待查询列表中
        HashMap<String, SubscribeUserOnlineStatus> cacheResult = new HashMap<>();
        List<String> waitQueryList = new ArrayList<>();
        for (String uid : userList) {
            SubscribeUserOnlineStatus status = mOnlineStatusCache.get(uid);
            if (status != null) {
                cacheResult.put(uid, status);
            } else {
                waitQueryList.add(uid);
            }
        }
        // 如果缓存列表有数据，通知注册的Listener
        notifyStatusChange(cacheResult);
        // 如果待查询列表为空，则直接返回
        if (waitQueryList.isEmpty()) {
            RLog.d(TAG, "fetchOnlineStatus waitQueryList isEmpty, userList:" + userList);
            return;
        }
        // 分页有序查询
        RLog.d(TAG, "fetchOnlineStatus waitQueryList: " + waitQueryList + ",cache:" + cacheResult);
        batchGetFriendsInfo(waitQueryList, processSubscribedLimit);
    }

    /**
     * 分页拉取好友信息
     *
     * @param uidList 用户ID列表
     */
    private void batchGetFriendsInfo(List<String> uidList, boolean processSubscribedLimit) {
        executeBatchTask(
                uidList,
                0,
                DEFAULT_MAX_BATCH_SIZE,
                new Task<String>() {
                    @Override
                    public void run(List<String> batchData, Runnable continueTask) {
                        getFriendsInfo(batchData, processSubscribedLimit, continueTask, 0);
                    }
                },
                null);
    }

    /** 拉取好友信息 */
    private void getFriendsInfo(
            List<String> uidList,
            boolean processSubscribedLimit,
            Runnable continueTask,
            int retryCount) {
        String uidListTag = uidList.toString();
        RLog.d(
                TAG,
                "getFriendsInfo uidList: "
                        + uidListTag
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit
                        + ", retryCount:"
                        + retryCount);
        // 信息托管关闭(好友在线状态必然关闭，无需判断好友在线状态开关)，走订阅在线状态逻辑
        // 如果信息托管开启但好友在线状态关闭，那好友会获取不到状态；非好友能否获取在线状态则依赖是否开启在线状态订
        if (!AppSettingsHandler.getInstance().isUserProfileEnabled()) {
            RLog.d(TAG, "fetchOnlineStatus userProfile or friendOnlineStatus closed");
            mGetFriendRequestCache.remove(uidListTag);
            batchSubscribeEvent(
                    uidList,
                    MAX_SUBSCRIBE_EVENT_BATCH_SIZE,
                    processSubscribedLimit,
                    new Runnable() {
                        @Override
                        public void run() {
                            if (continueTask != null) {
                                continueTask.run();
                            }
                        }
                    });
            return;
        }
        if (mGetFriendRequestCache.containsKey(uidListTag)) {
            RLog.d(TAG, "fetchOnlineStatus mRequestCache contains cache， tag:" + uidListTag);
            if (continueTask != null) {
                continueTask.run();
            }
            return;
        }
        mGetFriendRequestCache.put(uidListTag, "");
        RongCoreClient.getInstance()
                .getFriendsInfo(
                        uidList,
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {

                            @Override
                            public void onSuccess(List<FriendInfo> friendInfos) {
                                RLog.d(TAG, "getFriendsInfo onSuccess: " + friendInfos);
                                HashSet<String> set = new HashSet<>();
                                if (!friendInfos.isEmpty()) {
                                    for (FriendInfo friendInfo : friendInfos) {
                                        set.add(friendInfo.getUserId());
                                        // 添加好友ID到好友信息缓存中
                                        mFriendUserIdCache.put(
                                                friendInfo.getUserId(), friendInfo.getUserId());
                                    }
                                }
                                // 根据查询到的好友信息，把 uidList 用户列表拆分成好友列表和非好友列表
                                List<String> friendUidList = new ArrayList<>();
                                List<String> notFriendUidList = new ArrayList<>();
                                for (String uid : uidList) {
                                    if (set.contains(uid)) {
                                        friendUidList.add(uid);
                                    } else {
                                        notFriendUidList.add(uid);
                                    }
                                }
                                // 用户是好友，直接查询在线状态
                                if (!friendUidList.isEmpty()) {
                                    RLog.d(TAG, "getFriendsInfo OnlineStatus");
                                    // 如果没开好友在线状态，这里则不会获取好友的在线状态
                                    if (AppSettingsHandler.getInstance()
                                            .isFriendOnlineStatusSubscribeEnable()) {
                                        getOnlineStatus(friendUidList, null);
                                    }
                                }
                                // 用户非好友，订阅之后，再查询在线状态
                                if (!notFriendUidList.isEmpty()) {
                                    RLog.d(TAG, "getFriendsInfo subscribeEvent");
                                    // 查询这些用户的订阅信息：订阅的用户直接查询，没有订阅的用户先订阅再查询
                                    batchSubscribeEvent(
                                            notFriendUidList,
                                            MAX_SUBSCRIBE_EVENT_BATCH_SIZE,
                                            processSubscribedLimit,
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (continueTask != null) {
                                                        continueTask.run();
                                                    }
                                                }
                                            });
                                } else {
                                    RLog.d(
                                            TAG,
                                            "getFriendsInfo subscribeEvent: finish notFriendUidList");
                                    if (continueTask != null) {
                                        continueTask.run();
                                    }
                                }
                                mGetFriendRequestCache.remove(uidListTag);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                RLog.e(
                                        TAG,
                                        "getFriendsInfo onError "
                                                + e
                                                + ", retryCount:"
                                                + retryCount);
                                mGetFriendRequestCache.remove(uidListTag);
                                if (isNeedRetry(e.getValue(), retryCount)) {
                                    postDelay(
                                            () ->
                                                    getFriendsInfo(
                                                            uidList,
                                                            processSubscribedLimit,
                                                            continueTask,
                                                            retryCount + 1));
                                } else {
                                    if (continueTask != null) {
                                        continueTask.run();
                                    }
                                }
                            }
                        });
    }

    /** 分页订阅在线状态(非好友) */
    private void batchSubscribeEvent(
            List<String> uidList,
            int batchSize,
            boolean processSubscribedLimit,
            Runnable onAllCompleted) {
        RLog.d(
                TAG,
                "batchSubscribeEvent uidSize:"
                        + uidList.size()
                        + ",batchSize:"
                        + batchSize
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit
                        + ",list:"
                        + uidList);
        executeBatchTask(
                uidList,
                0,
                batchSize,
                new Task<String>() {
                    @Override
                    public void run(List<String> batchData, Runnable continueTask) {
                        subscribeEvent(
                                batchData, batchSize, processSubscribedLimit, continueTask, 0);
                    }
                },
                onAllCompleted);
    }

    private void batchSubscribeEvent(List<String> uidList, int batchSize) {
        batchSubscribeEvent(uidList, batchSize, true, null);
    }

    /**
     * 订阅在线状态(非好友)。
     *
     * <p>注意：
     *
     * <p>1，上游调用此方法需要保证 uidList 均不是好友。
     *
     * <p>2，需要保证订阅顺序，否则不好判断超过订阅上限临界值。不能直接使用For循环调用，而是串行调用。
     *
     * @param uidList 用户ID列表
     */
    private void subscribeEvent(
            List<String> uidList,
            int batchSize,
            boolean processSubscribedLimit,
            Runnable continueTask,
            int retryCount) {
        if (!AppSettingsHandler.getInstance().isOnlineStatusSubscribeEnable()) {
            if (continueTask != null) {
                continueTask.run();
            }
            return;
        }
        RLog.d(
                TAG,
                "subscribeEvent uidList:"
                        + uidList
                        + ",batchSize:"
                        + batchSize
                        + ",processSubscribedLimit:"
                        + processSubscribedLimit
                        + ", retryCount:"
                        + retryCount);
        SubscribeEventRequest request =
                new SubscribeEventRequest(
                        SubscribeEvent.SubscribeType.ONLINE_STATUS,
                        DEFAULT_SUBSCRIBE_ONLINE_TIME,
                        uidList);
        RongCoreClient.getInstance()
                .subscribeEvent(
                        request,
                        new IRongCoreCallback.SubscribeEventCallback<List<String>>() {
                            @Override
                            public void onSuccess() {
                                RLog.d(TAG, "subscribeEvent onSuccess uidList:" + uidList);
                                // 订阅信息放缓存中
                                for (String uid : uidList) {
                                    mSubscribeInfoCache.put(uid, -1L);
                                }
                                // 订阅成功后，更新订阅信息
                                querySubscribeEventByIds(uidList, 0);
                                // 订阅成功后，查询在线状态
                                RLog.d(TAG, "subscribeEvent OnlineStatus");
                                getOnlineStatus(uidList, null);
                                if (continueTask != null) {
                                    continueTask.run();
                                }
                            }

                            @Override
                            public void onError(int errorCode, List<String> userIdList) {
                                RLog.d(
                                        TAG,
                                        "subscribeEvent onError uidList:"
                                                + uidList
                                                + ",errorList:"
                                                + userIdList
                                                + ","
                                                + errorCode
                                                + ", retryCount:"
                                                + retryCount);
                                // 限频重试
                                if (isNeedRetry(errorCode, retryCount)) {
                                    postDelay(
                                            () ->
                                                    subscribeEvent(
                                                            uidList,
                                                            batchSize,
                                                            processSubscribedLimit,
                                                            continueTask,
                                                            retryCount + 1));
                                    return;
                                }
                                // 用户被订阅量达到上限
                                if (errorCode
                                        == RC_BESUBSCRIBED_USERIDS_COUNT_EXCEED_LIMIT.getValue()) {
                                    // 剔除掉被用户不能订阅的用户ID，重新订阅
                                    List<String> excludeExceedUidList = new ArrayList<>(uidList);
                                    for (String uidExceed : userIdList) {
                                        excludeExceedUidList.remove(uidExceed);
                                    }
                                    // 被订阅超限的用户查询下在线状态
                                    getOnlineStatus(
                                            userIdList,
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    RLog.d(
                                                            TAG,
                                                            "beSubscribedUserExceedLimit subscribeEvent");
                                                    batchSubscribeEvent(
                                                            excludeExceedUidList,
                                                            MAX_SUBSCRIBE_EVENT_BATCH_SIZE,
                                                            processSubscribedLimit,
                                                            () -> {
                                                                if (continueTask != null) {
                                                                    continueTask.run();
                                                                }
                                                            });
                                                }
                                            });
                                    return;
                                }
                                // 订阅用户数达到上限
                                if (errorCode == RC_SUBSCRIBED_USERIDS_EXCEED_LIMIT.getValue()
                                        && processSubscribedLimit) {
                                    // 获取全部订阅用户，获取完毕后回调给会话列表获取优先淘汰列表优先级列表，按需淘汰
                                    RLog.d(
                                            TAG,
                                            "SubscribedUserIdsExceedLimit currentSubscribeCache:"
                                                    + mSubscribeInfoCache
                                                    + " , "
                                                    + uidList);
                                    post(() -> processSubscribedUsersExceedLimit(batchSize));
                                    return;
                                }
                                if (continueTask != null) {
                                    continueTask.run();
                                }
                            }
                        });
    }

    /** 处理当前登录用户订阅时遇到订阅用户数达到上限的逻辑 */
    private void processSubscribedUsersExceedLimit(int batchSize) {
        RLog.d(TAG, "SubscribedUserIdsExceedLimit queryAllSubscribeEvent " + batchSize);
        queryAllSubscribeEvent(
                new Runnable() {
                    @Override
                    public void run() {
                        OnlineStatusDataSource dataSource = mOnlineStatusDataSource;
                        if (dataSource == null) {
                            RLog.d(TAG, "SubscribedUserIdsExceedLimit DataSource null");
                            // 数据源置空，说明不再需要维护优先级，清空缓存
                            mPriorityUserList.clear();
                            return;
                        }
                        // 获取当前按优先级排序的用户ID列表，优先淘汰队尾数据。
                        List<String> priorityList = dataSource.onPriorityUserList();

                        // 如果 priorityList 为 null 或 Empty，视为数据源刷新中或暂时不可用
                        // 使用旧缓存兜底，防止列表刷新导致的订阅闪烁
                        if (priorityList == null || priorityList.isEmpty()) {
                            RLog.d(
                                    TAG,
                                    "SubscribedUserIdsExceedLimit priorityList null/empty, use cache");
                            if (!mPriorityUserList.isEmpty()) {
                                priorityList = new ArrayList<>(mPriorityUserList);
                            } else {
                                // 缓存也为空，确实无数据
                                return;
                            }
                        } else {
                            // 有效数据，更新缓存
                            mPriorityUserList.clear();
                            mPriorityUserList.addAll(priorityList);
                        }

                        if (priorityList.isEmpty()) {
                            RLog.d(TAG, "SubscribedUserIdsExceedLimit priorityList isEmpty");
                            return;
                        }
                        // 筛选出不在好友缓存中的用户ID列表
                        List<String> notFriendList = new ArrayList<>();
                        for (String uid : priorityList) {
                            // 判断是非好友
                            if (!mFriendUserIdCache.containsKey(uid)) {
                                notFriendList.add(uid);
                            }
                        }
                        // 如果 notFriendList 数量大于订阅数量上限，则截取 maxSubscribeNumber 长度的列表。
                        int subIndex = Math.min(notFriendList.size(), DEFAULT_SUBSCRIBE_NUMBER);
                        Set<String> subNotFriendSet =
                                new HashSet<>(new ArrayList<>(notFriendList.subList(0, subIndex)));
                        RLog.d(
                                TAG,
                                "processSubscribedUserIdsExceedLimit notFriendList:"
                                        + notFriendList
                                        + ",subList:"
                                        + subNotFriendSet);
                        RLog.d(
                                TAG,
                                "processSubscribedUserIdsExceedLimit mSubscribeInfoCache:"
                                        + mSubscribeInfoCache);
                        // 需要取消订阅的用户列表
                        List<String> needUnsubscribeList = new ArrayList<>();
                        // 不需要再次订阅但需要查询在线状态的用户列表
                        List<String> subNotFriendNotifyList = new ArrayList<>();
                        long time = System.currentTimeMillis();
                        for (Map.Entry<String, Long> entry : mSubscribeInfoCache.entrySet()) {
                            String uid = entry.getKey();
                            // 没在 subNotFriendList 中的用户列表，需要解除订阅
                            if (!subNotFriendSet.contains(uid)) {
                                needUnsubscribeList.add(uid);
                            } else {
                                // 在 subNotFriendList 中，说明有订阅过。
                                // event为空，或者当前时间减去订阅时间小于临界时间，不需要再订阅，subNotFriendList
                                // 移除该ID，subNotFriendNotifyList 添加该ID
                                Long subscribeTime = entry.getValue();
                                if (subscribeTime < 0
                                        || time - subscribeTime
                                                < EXPIRED_SUBSCRIBE_ONLINE_TIME * 1000) {
                                    RLog.d(TAG, "SubscribedUserIdsExceedLimit expired " + uid);
                                    subNotFriendSet.remove(uid);
                                    subNotFriendNotifyList.add(uid);
                                }
                            }
                        }
                        List<String> subNotFriendList = new ArrayList<>(subNotFriendSet);
                        // 如果没有过期，不需要重新订阅，需要查询
                        if (!subNotFriendNotifyList.isEmpty()) {
                            getOnlineStatus(subNotFriendNotifyList, null);
                            RLog.d(
                                    TAG,
                                    "SubscribedUserIdsExceedLimit subNotFriendNotifyList "
                                            + subNotFriendNotifyList);
                        }
                        // 如果超限了，并且没有可以取消订阅的，则不再继续处理
                        if (needUnsubscribeList.isEmpty()
                                && batchSize == MIN_SUBSCRIBE_EVENT_BATCH_SIZE) {
                            RLog.d(TAG, "SubscribedUserIdsExceedLimit needUnsubscribeList isEmpty");
                            return;
                        }
                        // 取消订阅 needUnsubscribeList ，成功后订阅 needSubscribeList
                        RLog.d(
                                TAG,
                                "SubscribedUserIdsExceedLimit batchUnsubscribeOnlineStatusEvent "
                                        + needUnsubscribeList
                                        + ",subNotFriendList:"
                                        + subNotFriendList);
                        batchUnsubscribeOnlineStatusEvent(
                                needUnsubscribeList,
                                true,
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        // 取消成功后，重新订阅 uidList
                                        RLog.d(
                                                TAG,
                                                "batchUnsubscribeOnlineStatusEvent finish batchSubscribeEvent "
                                                        + subNotFriendList);
                                        batchSubscribeEvent(
                                                subNotFriendList, MIN_SUBSCRIBE_EVENT_BATCH_SIZE);
                                    }
                                });
                    }
                });
    }

    /** 查询全部订阅信息 */
    private void queryAllSubscribeEvent(Runnable onCompleted) {
        querySubscribeEvent(0, onCompleted, 0);
    }

    private void querySubscribeEvent(int startIndex, Runnable onCompleted, int retryCount) {
        // 查询类型
        RLog.d(
                TAG,
                "queryAllSubscribeEvent startIndex:" + startIndex + ", retryCount:" + retryCount);
        RongCoreClient.getInstance()
                .querySubscribeEvent(
                        new SubscribeEventRequest(SubscribeEvent.SubscribeType.ONLINE_STATUS),
                        MAX_QUERY_SUBSCRIBE_BATCH_SIZE,
                        startIndex,
                        new IRongCoreCallback.ResultCallback<List<SubscribeInfoEvent>>() {
                            @Override
                            public void onSuccess(List<SubscribeInfoEvent> subscribeInfoEvents) {
                                RLog.d(
                                        TAG,
                                        "queryAllSubscribeEvent "
                                                + subscribeInfoEvents.size()
                                                + ","
                                                + subscribeInfoEvents);
                                // 查询成功，返回查询用户信息。
                                if (!subscribeInfoEvents.isEmpty()) {
                                    for (SubscribeInfoEvent event : subscribeInfoEvents) {
                                        mSubscribeInfoCache.put(
                                                event.getUserId(), event.getSubscribeTime());
                                    }
                                    RLog.d(
                                            TAG,
                                            "queryAllSubscribeEvent mSubscribeInfoCache "
                                                    + mSubscribeInfoCache.size()
                                                    + ","
                                                    + mSubscribeInfoCache);
                                    querySubscribeEvent(
                                            startIndex + subscribeInfoEvents.size(),
                                            onCompleted,
                                            0);
                                } else {
                                    RLog.d(TAG, "querySubscribeEvent onSuccess onCompleted");
                                    if (onCompleted != null) {
                                        onCompleted.run();
                                    }
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                RLog.d(
                                        TAG,
                                        "querySubscribeEvent onError "
                                                + e
                                                + ", retryCount:"
                                                + retryCount);
                                if (isNeedRetry(e.getValue(), retryCount)) {
                                    postDelay(
                                            () ->
                                                    querySubscribeEvent(
                                                            startIndex,
                                                            onCompleted,
                                                            retryCount + 1));
                                } else {
                                    RLog.d(TAG, "querySubscribeEvent onError onCompleted");
                                    if (onCompleted != null) {
                                        onCompleted.run();
                                    }
                                }
                            }
                        });
    }

    /** 根据用户ID查询订阅信息 */
    private void querySubscribeEventByIds(List<String> uidList, int retryCount) {
        RLog.d(TAG, "querySubscribeEventByIds uidList:" + uidList + ", retryCount:" + retryCount);
        // 查询类型
        SubscribeEvent.SubscribeType type = SubscribeEvent.SubscribeType.ONLINE_STATUS;
        RongCoreClient.getInstance()
                .querySubscribeEvent(
                        new SubscribeEventRequest(type, uidList),
                        new IRongCoreCallback.ResultCallback<List<SubscribeInfoEvent>>() {
                            @Override
                            public void onSuccess(List<SubscribeInfoEvent> subscribeInfoEvents) {
                                RLog.d(
                                        TAG,
                                        "querySubscribeEventByIds "
                                                + subscribeInfoEvents.size()
                                                + ","
                                                + subscribeInfoEvents);
                                // 查询成功，返回查询用户信息。
                                if (!subscribeInfoEvents.isEmpty()) {
                                    for (SubscribeInfoEvent event : subscribeInfoEvents) {
                                        mSubscribeInfoCache.put(
                                                event.getUserId(), event.getSubscribeTime());
                                    }
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                RLog.d(
                                        TAG,
                                        "querySubscribeEventByIds onError "
                                                + e
                                                + ", retryCount:"
                                                + retryCount);
                                if (isNeedRetry(e.getValue(), retryCount)) {
                                    postDelay(
                                            () ->
                                                    querySubscribeEventByIds(
                                                            uidList, retryCount + 1));
                                }
                            }
                        });
    }

    /**
     * 批量取消订阅用户在线状态
     *
     * @param uidList 用户ID列表
     * @param onCompleted 完成回调
     */
    private void batchUnsubscribeOnlineStatusEvent(
            List<String> uidList, boolean removeOnlineStatusCache, Runnable onCompleted) {
        RLog.d(
                TAG,
                "batchUnsubscribeOnlineStatusEvent: "
                        + uidList
                        + ",removeOnlineStatusCache:"
                        + removeOnlineStatusCache);
        executeBatchTask(
                uidList,
                0,
                DEFAULT_MAX_BATCH_SIZE,
                (uidList1, continueTask) ->
                        unsubscribeOnlineStatusEvent(
                                uidList1, removeOnlineStatusCache, continueTask, 0),
                onCompleted);
    }

    /** 取消订阅用户在线状态 */
    private void unsubscribeOnlineStatusEvent(
            List<String> uidList,
            boolean removeOnlineStatusCache,
            Runnable continueTask,
            int retryCount) {
        RLog.d(
                TAG,
                "unsubscribeOnlineStatusEvent: "
                        + uidList
                        + ",removeOnlineStatusCache:"
                        + removeOnlineStatusCache
                        + ", retryCount:"
                        + retryCount);
        SubscribeEventRequest request =
                new SubscribeEventRequest(SubscribeEvent.SubscribeType.ONLINE_STATUS, uidList);
        IRongCoreCallback.SubscribeEventCallback<List<String>> callback =
                new IRongCoreCallback.SubscribeEventCallback<List<String>>() {

                    @Override
                    public void onSuccess() {
                        RLog.d(TAG, "unsubscribeUsersOnlineStatus: onSuccess");
                        // 取消订阅成功，移除订阅信息
                        for (String uid : uidList) {
                            mSubscribeInfoCache.remove(uid);
                        }
                        // 如果不是好友，移除在线状态信息
                        if (removeOnlineStatusCache) {
                            HashMap<String, SubscribeUserOnlineStatus> unsubscribeMap =
                                    new HashMap<>();
                            for (String uid : uidList) {
                                // 如果能够移除，则需要通知移除的为离线状态。
                                if (mOnlineStatusCache.remove(uid) != null) {
                                    unsubscribeMap.put(
                                            uid, new SubscribeUserOnlineStatus(uid, null, false));
                                }
                            }
                            // 通知取消订阅的这些用户状态为离线
                            RLog.d(
                                    TAG,
                                    "unsubscribeUsersOnlineStatus unsubscribeMap "
                                            + unsubscribeMap);
                            notifyStatusChange(unsubscribeMap);
                        }
                        if (continueTask != null) {
                            continueTask.run();
                        }
                    }

                    @Override
                    public void onError(int e, List<String> errorData) {
                        RLog.e(
                                TAG,
                                "unsubscribeUsersOnlineStatus: onError "
                                        + e
                                        + ", retryCount:"
                                        + retryCount);
                        if (isNeedRetry(e, retryCount)) {
                            postDelay(
                                    () ->
                                            unsubscribeOnlineStatusEvent(
                                                    uidList,
                                                    removeOnlineStatusCache,
                                                    continueTask,
                                                    retryCount + 1));
                        } else {
                            if (continueTask != null) {
                                continueTask.run();
                            }
                        }
                    }
                };
        RongCoreClient.getInstance().unSubscribeEvent(request, callback);
    }

    /** 获取在线状态 */
    private void getOnlineStatus(List<String> uidList, Runnable onCompleted) {
        getOnlineStatus(uidList, onCompleted, 0);
    }

    private void getOnlineStatus(List<String> uidList, Runnable onCompleted, int retryCount) {
        RLog.d(TAG, "getOnlineStatus " + uidList + ", retryCount:" + retryCount);
        RongCoreClient.getInstance()
                .getSubscribeUsersOnlineStatus(
                        uidList,
                        new IRongCoreCallback.ResultCallback<List<SubscribeUserOnlineStatus>>() {
                            @Override
                            public void onSuccess(List<SubscribeUserOnlineStatus> result) {
                                RLog.d(TAG, "getOnlineStatus onSuccess " + result.size());
                                if (onCompleted != null) {
                                    onCompleted.run();
                                }
                                if (result.isEmpty()) {
                                    return;
                                }
                                RLog.d(TAG, "getOnlineStatus onSuccess " + formatLogString(result));

                                // 放到在线状态缓存中
                                for (SubscribeUserOnlineStatus status : result) {
                                    mOnlineStatusCache.put(status.getUserId(), status);
                                }
                                // 通知注册的Listener
                                notifyStatusChange(result);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                RLog.e(
                                        TAG,
                                        "getOnlineStatus: onError "
                                                + e
                                                + ", retryCount:"
                                                + retryCount);
                                if (isNeedRetry(e.getValue(), retryCount)) {
                                    postDelay(
                                            () ->
                                                    getOnlineStatus(
                                                            uidList, onCompleted, retryCount + 1));
                                } else {
                                    if (onCompleted != null) {
                                        onCompleted.run();
                                    }
                                }
                            }
                        });
    }

    private <T> String formatLogString(List<T> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(" " + data.size());
        for (T t : data) {
            if (t instanceof SubscribeUserOnlineStatus) {
                SubscribeUserOnlineStatus status = (SubscribeUserOnlineStatus) t;
                sb.append(" [")
                        .append(status.getUserId())
                        .append(",online:")
                        .append(status.isOnline());
                List<PlatformOnlineStatus> details = status.getDetails();
                sb.append(",platforms:");
                if (details != null && !details.isEmpty()) {
                    for (PlatformOnlineStatus detail : details) {
                        if (detail.isOnline()) {
                            sb.append(detail.getPlatform().name()).append(" ");
                        }
                    }
                }
                sb.append("]");
            } else if (t instanceof SubscribeInfoEvent) {
                SubscribeInfoEvent event = (SubscribeInfoEvent) t;
                if (SubscribeEvent.SubscribeType.ONLINE_STATUS == event.getSubscribeType()
                        || SubscribeEvent.SubscribeType.FRIEND_ONLINE_STATUS
                                == event.getSubscribeType()) {
                    sb.append(" [").append(event.getUserId());
                    List<SubscribeInfoEvent.SubscribeEventDetail> details = event.getDetails();
                    boolean isOnline = false;
                    sb.append(",platforms:");
                    if (details != null && !details.isEmpty()) {
                        for (SubscribeInfoEvent.SubscribeEventDetail detail : details) {
                            if (detail.getEventValue() == 1) {
                                isOnline = true;
                                sb.append(detail.getPlatform().name()).append(" ");
                            }
                        }
                    }
                    sb.append(",online:").append(isOnline);
                    sb.append("]");
                }
            }
        }
        return sb.toString();
    }

    private String formatLogString(Map<String, SubscribeUserOnlineStatus> data) {
        if (data == null || data.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, SubscribeUserOnlineStatus> entry : data.entrySet()) {
            sb.append("[")
                    .append(entry.getKey())
                    .append(",online:")
                    .append(entry.getValue().isOnline())
                    .append("]");
        }
        return sb.toString();
    }

    /**
     * 通知注册的 OnLineStatusListener
     *
     * @param onlineResult SubscribeUserOnlineStatus列表
     */
    private void notifyStatusChange(List<SubscribeUserOnlineStatus> onlineResult) {
        if (onlineResult == null || onlineResult.isEmpty()) {
            return;
        }
        HashMap<String, SubscribeUserOnlineStatus> result = new HashMap<>();
        for (SubscribeUserOnlineStatus status : onlineResult) {
            result.put(status.getUserId(), status);
        }
        notifyStatusChange(result);
    }

    /**
     * 通知注册的 OnLineStatusListener
     *
     * @param onlineResult SubscribeUserOnlineStatus Map
     */
    private void notifyStatusChange(Map<String, SubscribeUserOnlineStatus> onlineResult) {
        if (onlineResult == null || onlineResult.isEmpty()) {
            return;
        }
        // 主线程有序回调
        RLog.d(TAG, "notifyStatusChange notify data: " + formatLogString(onlineResult));
        post(
                () -> {
                    for (OnLineStatusListener listener : mOnLineStatusListeners) {
                        listener.onOnlineStatusUpdate(onlineResult);
                    }
                });
    }

    /**
     * 放到队列中执行
     *
     * @param runnable 执行任务
     */
    private void post(Runnable runnable) {
        mMainHandler.post(runnable);
    }

    /**
     * 延迟执行任务
     *
     * @param runnable 延迟任务
     */
    private void postDelay(Runnable runnable) {
        mMainHandler.postDelayed(runnable, DEFAULT_DELAY_TIME);
    }

    /**
     * 批量处理器接口
     *
     * @param <T> 数据类型
     */
    private interface Task<T> {
        /**
         * 处理一个批次的数据
         *
         * @param batchData 当前批次的数据列表
         * @param continueTask 调用run，继续执行下一次批次任务
         */
        void run(List<T> batchData, Runnable continueTask);
    }

    /** 判断是否需要重试 */
    private boolean isNeedRetry(int code, int retryCount) {
        return (code == RC_REQUEST_OVERFREQUENCY.getValue()
                        || code == RC_MSG_RESP_TIMEOUT.getValue())
                && retryCount < MAX_RETRY_COUNT;
    }

    /**
     * 通用批量处理方法
     *
     * @param list 待处理的完整列表
     * @param startIndex 起始索引，从头开始传0
     * @param task 批量处理器
     * @param onAllCompleted 所有批次完成后的回调，不需要可传null
     * @param <T> 数据类型
     */
    private <T> void executeBatchTask(
            List<T> list, int startIndex, int batchSize, Task<T> task, Runnable onAllCompleted) {
        if (list == null || list.isEmpty() || startIndex >= list.size()) {
            if (onAllCompleted != null) {
                onAllCompleted.run();
            }
            return;
        }

        int endIndex = Math.min(startIndex + batchSize, list.size());
        List<T> batch = list.subList(startIndex, endIndex);

        task.run(
                batch,
                () -> {
                    // 递归处理下一批次
                    executeBatchTask(list, endIndex, batchSize, task, onAllCompleted);
                });
    }
}
