package io.rong.imkit;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.config.ConversationClickListener;
import io.rong.imkit.config.ConversationListBehaviorListener;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.event.actionevent.ClearEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.event.actionevent.InsertEvent;
import io.rong.imkit.event.actionevent.MessageEventListener;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.event.actionevent.RefreshEvent;
import io.rong.imkit.event.actionevent.SendEvent;
import io.rong.imkit.event.actionevent.SendMediaEvent;
import io.rong.imkit.feature.forward.CombineMessage;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import io.rong.imkit.notification.MessageNotificationHelper;
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.usermanage.interfaces.OnGroupAndUserEventListener;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.language.RongConfigurationManager;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.ExecutorFactory;
import io.rong.imlib.listener.FriendEventListener;
import io.rong.imlib.listener.GroupEventListener;
import io.rong.imlib.listener.OnReceiveMessageWrapperListener;
import io.rong.imlib.listener.OnSubscribeEventListener;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.imlib.model.ConnectOption;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupInfoKeys;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupOperation;
import io.rong.imlib.model.GroupOperationType;
import io.rong.imlib.model.InitOption;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReceivedProfile;
import io.rong.imlib.model.SendMessageOption;
import io.rong.imlib.model.SubscribeEvent;
import io.rong.imlib.model.SubscribeInfoEvent;
import io.rong.imlib.model.UltraGroupChannelChangeTypeInfo;
import io.rong.imlib.model.UltraGroupChannelDisbandedInfo;
import io.rong.imlib.model.UltraGroupChannelUserKickedInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.model.UserProfile;
import io.rong.imlib.typingmessage.TypingStatus;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.SightMessage;
import io.rong.message.TextMessage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class IMCenter {
    private static final String TAG = IMCenter.class.getSimpleName();
    private Context mContext;
    private MessageInterceptor mMessageInterceptor;
    private List<RongIMClient.ConnectionStatusListener> mConnectionStatusObserverList =
            new CopyOnWriteArrayList<>();
    private List<IRongCoreListener.OnReceiveMessageWrapperListener> mOnReceiveMessageObserverList =
            new CopyOnWriteArrayList<>();
    private List<IRongCoreListener.OnReceiveMessageWrapperListener>
            mAsyncOnReceiveMessageObserverList = new CopyOnWriteArrayList<>();
    private List<RongIMClient.ConversationStatusListener> mConversationStatusObserverList =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.ReadReceiptListener> mReadReceiptObserverList =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.OnRecallMessageListener> mOnRecallMessageObserverList =
            new CopyOnWriteArrayList<>();

    private final List<MessageEventListener> mMessageEventListeners = new CopyOnWriteArrayList<>();
    private final List<OnGroupAndUserEventListener> onGroupAndUserEventListeners =
            new CopyOnWriteArrayList<>();
    private Map<String, IRongCallback.IDownloadMediaFileCallback> mMediaListeners =
            new ConcurrentHashMap<>();
    private List<ConversationEventListener> mConversationEventListener =
            new CopyOnWriteArrayList<>();

    private List<RongIMClient.ConnectCallback> mConnectStatusListener =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.SyncConversationReadStatusListener>
            mSyncConversationReadStatusListeners = new CopyOnWriteArrayList<>();
    private List<RongIMClient.TypingStatusListener> mTypingStatusListeners =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.ResultCallback<Message>> mCancelSendMediaMessageListeners =
            new CopyOnWriteArrayList<>();
    private final List<GroupEventListener> mGroupEventListeners = new CopyOnWriteArrayList<>();
    private final List<OnSubscribeEventListener> mSubscribeEventListeners =
            new CopyOnWriteArrayList<>();
    private final List<FriendEventListener> friendEventListeners = new CopyOnWriteArrayList<>();
    private static KitFragmentFactory kitFragmentFactory = new KitFragmentFactory();

    private static final String EMOJI_TTF_FILE_NAME = "NotoColorEmojiCompat.ttf";

    /** 连接状态变化的监听器。 */
    private IRongCoreListener.ConnectionStatusListener mConnectionStatusListener =
            new IRongCoreListener.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus connectionStatus) {
                    for (RongIMClient.ConnectionStatusListener listener :
                            mConnectionStatusObserverList) {
                        listener.onChanged(
                                RongIMClient.ConnectionStatusListener.ConnectionStatus.valueOf(
                                        connectionStatus.getValue()));
                    }
                }
            };

    /** 接受消息的事件监听器。 */
    private OnReceiveMessageWrapperListener mOnReceiveMessageListener =
            new OnReceiveMessageWrapperListener() {
                @Override
                public void onReceivedMessage(Message message, ReceivedProfile profile) {
                    if (mMessageInterceptor != null
                            && mMessageInterceptor.interceptReceivedMessage(
                                    message,
                                    profile.getLeft(),
                                    profile.hasPackage(),
                                    profile.isOffline())) {
                        RLog.d(TAG, "message has been intercepted.");
                        return;
                    }

                    dispatchOnReceiveMessageObserver(message, profile);

                    for (IRongCoreListener.OnReceiveMessageWrapperListener observer :
                            mAsyncOnReceiveMessageObserverList) {
                        observer.onReceived(
                                message,
                                profile.getLeft(),
                                profile.hasPackage(),
                                profile.isOffline());
                    }
                }

                @Override
                public void onOfflineMessageSyncCompleted() {
                    super.onOfflineMessageSyncCompleted();
                    dispatchOnOfflineMessageSyncCompleted();
                }
            };

    private void dispatchOnReceiveMessageObserver(Message message, ReceivedProfile profile) {
        if (mOnReceiveMessageObserverList.isEmpty()) {
            return;
        }
        if (!ExecutorFactory.isMainThread()) {
            ExecutorFactory.getInstance()
                    .getMainHandler()
                    .post(() -> dispatchOnReceiveMessageObserver(message, profile));
            return;
        }

        for (IRongCoreListener.OnReceiveMessageWrapperListener observer :
                mOnReceiveMessageObserverList) {
            observer.onReceived(
                    message, profile.getLeft(), profile.hasPackage(), profile.isOffline());
        }
    }

    private void dispatchOnOfflineMessageSyncCompleted() {
        if (mOnReceiveMessageObserverList.isEmpty()) {
            return;
        }
        if (!ExecutorFactory.isMainThread()) {
            ExecutorFactory.getInstance()
                    .getMainHandler()
                    .post(() -> dispatchOnOfflineMessageSyncCompleted());
            return;
        }
        for (IRongCoreListener.OnReceiveMessageWrapperListener observer :
                mOnReceiveMessageObserverList) {
            observer.onOfflineMessageSyncCompleted();
        }
    }

    /** 会话状态监听器。当会话的置顶或者免打扰状态更改时，会回调此方法。 */
    private RongIMClient.ConversationStatusListener mConversationStatusListener =
            new RongIMClient.ConversationStatusListener() {
                @Override
                public void onStatusChanged(ConversationStatus[] conversationStatuses) {
                    for (RongIMClient.ConversationStatusListener listener :
                            mConversationStatusObserverList) {
                        listener.onStatusChanged(conversationStatuses);
                    }
                }
            };

    /** 消息被撤回时的监听器。 */
    private RongIMClient.OnRecallMessageListener mOnRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    for (RongIMClient.OnRecallMessageListener listener :
                            mOnRecallMessageObserverList) {
                        listener.onMessageRecalled(message, recallNotificationMessage);
                    }
                    return false;
                }
            };

    /** 消息回执监听器 */
    private RongIMClient.ReadReceiptListener mReadReceiptListener =
            new RongIMClient.ReadReceiptListener() {
                /**
                 * 单聊中收到消息回执的回调。
                 *
                 * @param message 封装了 {@link ReadReceiptMessage}
                 */
                @Override
                public void onReadReceiptReceived(final Message message) {
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            for (RongIMClient.ReadReceiptListener listener :
                                                    mReadReceiptObserverList) {
                                                listener.onReadReceiptReceived(message);
                                            }
                                        }
                                    });
                }

                /**
                 * 群组和讨论组中，某人发起了回执请求，会话中其余人会收到该请求，并回调此方法。
                 *
                 * <p>接收方需要在合适的时机（读取了消息之后）调用 {@link
                 * RongIMClient#sendReadReceiptResponse(Conversation.ConversationType, String, List,
                 * RongIMClient.OperationCallback)} 回复响应。
                 *
                 * @param conversationType 会话类型
                 * @param targetId 会话目标 id
                 * @param messageUId 请求已读回执的消息 uId
                 */
                @Override
                public void onMessageReceiptRequest(
                        final Conversation.ConversationType conversationType,
                        final String targetId,
                        final String messageUId) {
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            for (RongIMClient.ReadReceiptListener listener :
                                                    mReadReceiptObserverList) {
                                                listener.onMessageReceiptRequest(
                                                        conversationType, targetId, messageUId);
                                            }
                                        }
                                    });
                }

                /**
                 * 在群组和讨论组中发起了回执请求的用户，当收到接收方的响应时，会回调此方法。
                 *
                 * @param conversationType 会话类型
                 * @param targetId 会话 id
                 * @param messageUId 收到回执响应的消息的 uId
                 * @param respondUserIdList 会话中响应了此消息的用户列表。其中 key： 用户 id ； value： 响应时间。
                 */
                @Override
                public void onMessageReceiptResponse(
                        final Conversation.ConversationType conversationType,
                        final String targetId,
                        final String messageUId,
                        final HashMap<String, Long> respondUserIdList) {
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            for (RongIMClient.ReadReceiptListener listener :
                                                    mReadReceiptObserverList) {
                                                listener.onMessageReceiptResponse(
                                                        conversationType,
                                                        targetId,
                                                        messageUId,
                                                        respondUserIdList);
                                            }
                                        }
                                    });
                }
            };

    /** 阅后即焚事件监听器。 */
    private RongIMClient.OnReceiveDestructionMessageListener mOnReceiveDestructMessageListener =
            new RongIMClient.OnReceiveDestructionMessageListener() {
                @Override
                public void onReceive(Message message) {
                    for (MessageEventListener item : mMessageEventListeners) {
                        item.onDeleteMessage(
                                new DeleteEvent(
                                        message.getConversationType(),
                                        message.getTargetId(),
                                        new int[] {message.getMessageId()}));
                    }
                    if (MessageItemLongClickActionManager.getInstance().getLongClickDialog()
                            != null) {
                        MessageItemLongClickActionManager.getInstance()
                                .getLongClickDialog()
                                .dismiss();
                    }
                }
            };

    private RongIMClient.SyncConversationReadStatusListener mSyncConversationReadStatusListener =
            new RongIMClient.SyncConversationReadStatusListener() {
                @Override
                public void onSyncConversationReadStatus(
                        Conversation.ConversationType type, String targetId) {
                    for (RongIMClient.SyncConversationReadStatusListener item :
                            mSyncConversationReadStatusListeners) {
                        item.onSyncConversationReadStatus(type, targetId);
                    }
                }
            };
    private RongIMClient.TypingStatusListener mTypingStatusListener =
            new RongIMClient.TypingStatusListener() {
                @Override
                public void onTypingStatusChanged(
                        Conversation.ConversationType type,
                        String targetId,
                        Collection<TypingStatus> typingStatusSet) {
                    for (RongIMClient.TypingStatusListener item : mTypingStatusListeners) {
                        item.onTypingStatusChanged(type, targetId, typingStatusSet);
                    }
                }
            };

    private IRongCoreListener.UltraGroupChannelListener mUltraGroupChannelListener =
            new IRongCoreListener.UltraGroupChannelListener() {
                @Override
                public void ultraGroupChannelUserDidKicked(
                        List<UltraGroupChannelUserKickedInfo> infoList) {
                    RLog.d(TAG, "ultraGroupChannelUserDidKicked: " + infoList.size());
                    StringBuilder stringBuilder = new StringBuilder();
                    for (UltraGroupChannelUserKickedInfo info : infoList) {
                        for (ConversationEventListener conversationEventListener :
                                mConversationEventListener) {
                            conversationEventListener.onChannelKicked(
                                    info.getChangeInfo().getTargetId(),
                                    info.getChangeInfo().getChannelId(),
                                    info.getUserId());
                        }
                    }
                }

                @Override
                public void ultraGroupChannelTypeDidChanged(
                        List<UltraGroupChannelChangeTypeInfo> infoList) {
                    RLog.d(TAG, "ultraGroupChannelTypeDidChanged: " + infoList.size());
                    for (UltraGroupChannelChangeTypeInfo ultraGroupChannelChangeTypeInfo :
                            infoList) {
                        for (ConversationEventListener conversationEventListener :
                                mConversationEventListener) {
                            conversationEventListener.onChannelChange(
                                    ultraGroupChannelChangeTypeInfo.getChangeInfo().getTargetId(),
                                    ultraGroupChannelChangeTypeInfo.getChangeInfo().getChannelId(),
                                    ultraGroupChannelChangeTypeInfo.getChangeType()
                                                    == IRongCoreEnum.UltraGroupChannelChangeType
                                                            .ULTRA_GROUP_CHANNEL_CHANGE_TYPE_PRIVATE_TO_PUBLIC
                                            ? IRongCoreEnum.UltraGroupChannelType
                                                    .ULTRA_GROUP_CHANNEL_TYPE_PUBLIC
                                            : IRongCoreEnum.UltraGroupChannelType
                                                    .ULTRA_GROUP_CHANNEL_TYPE_PRIVATE);
                        }
                    }
                }

                @Override
                public void ultraGroupChannelDidDisbanded(
                        List<UltraGroupChannelDisbandedInfo> infoList) {
                    RLog.d(TAG, "ultraGroupChannelDidDisbanded: " + infoList.size());
                    for (UltraGroupChannelDisbandedInfo ultraGroupChannelDisbandedInfo : infoList) {
                        for (ConversationEventListener listener : mConversationEventListener) {
                            listener.onChannelDelete(
                                    ultraGroupChannelDisbandedInfo.getChangeInfo().getTargetId(),
                                    ultraGroupChannelDisbandedInfo.getChangeInfo().getChannelId());
                        }
                    }
                }
            };

    private GroupEventListener mGroupEventListener =
            new GroupEventListener() {
                @Override
                public void onGroupOperation(
                        String groupId,
                        GroupMemberInfo operatorInfo,
                        GroupInfo groupInfo,
                        GroupOperation operation,
                        List<GroupMemberInfo> memberInfos,
                        long operationTime) {
                    for (GroupEventListener listener : mGroupEventListeners) {
                        listener.onGroupOperation(
                                groupId,
                                operatorInfo,
                                groupInfo,
                                operation,
                                memberInfos,
                                operationTime);
                    }
                }

                @Override
                public void onGroupInfoChanged(
                        GroupMemberInfo operatorInfo,
                        GroupInfo groupInfo,
                        List<GroupInfoKeys> updateKeys,
                        long operationTime) {
                    for (GroupEventListener listener : mGroupEventListeners) {
                        listener.onGroupInfoChanged(
                                operatorInfo, groupInfo, updateKeys, operationTime);
                    }
                }

                @Override
                public void onGroupMemberInfoChanged(
                        String groupId,
                        GroupMemberInfo operatorInfo,
                        GroupMemberInfo memberInfo,
                        long operationTime) {
                    for (GroupEventListener listener : mGroupEventListeners) {
                        listener.onGroupMemberInfoChanged(
                                groupId, operatorInfo, memberInfo, operationTime);
                    }
                }

                @Override
                public void onGroupApplicationEvent(GroupApplicationInfo info) {
                    for (GroupEventListener listener : mGroupEventListeners) {
                        listener.onGroupApplicationEvent(info);
                    }
                }

                @Override
                public void onGroupRemarkChangedSync(
                        String groupId,
                        GroupOperationType operationType,
                        String groupRemark,
                        long operationTime) {
                    for (GroupEventListener listener : mGroupEventListeners) {
                        listener.onGroupRemarkChangedSync(
                                groupId, operationType, groupRemark, operationTime);
                    }
                }

                @Override
                public void onGroupFollowsChangedSync(
                        String groupId,
                        GroupOperationType operationType,
                        List<String> userIds,
                        long operationTime) {
                    for (GroupEventListener listener : mGroupEventListeners) {
                        listener.onGroupFollowsChangedSync(
                                groupId, operationType, userIds, operationTime);
                    }
                }
            };

    private OnSubscribeEventListener mOnSubscribeEventListener =
            new OnSubscribeEventListener() {
                @Override
                public void onEventChange(List<SubscribeInfoEvent> subscribeEvents) {
                    for (OnSubscribeEventListener listener : mSubscribeEventListeners) {
                        listener.onEventChange(subscribeEvents);
                    }
                }

                @Override
                public void onSubscriptionSyncCompleted(SubscribeEvent.SubscribeType type) {
                    for (OnSubscribeEventListener listener : mSubscribeEventListeners) {
                        listener.onSubscriptionSyncCompleted(type);
                    }
                }

                @Override
                public void onSubscriptionChangedOnOtherDevices(
                        List<SubscribeEvent> subscribeEvents) {
                    for (OnSubscribeEventListener listener : mSubscribeEventListeners) {
                        listener.onSubscriptionChangedOnOtherDevices(subscribeEvents);
                    }
                }
            };
    private FriendEventListener mFriendEventListener =
            new FriendEventListener() {
                @Override
                public void onFriendAdd(
                        DirectionType directionType,
                        String userId,
                        String userName,
                        String portraitUri,
                        long operationTime) {
                    for (FriendEventListener listener : friendEventListeners) {
                        listener.onFriendAdd(
                                directionType, userId, userName, portraitUri, operationTime);
                    }
                }

                @Override
                public void onFriendDelete(
                        DirectionType directionType, List<String> userIds, long operationTime) {
                    for (FriendEventListener listener : friendEventListeners) {
                        listener.onFriendDelete(directionType, userIds, operationTime);
                    }
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        String userId,
                        FriendApplicationType applicationType,
                        FriendApplicationStatus status,
                        DirectionType directionType,
                        long operationTime,
                        String extra) {
                    for (FriendEventListener listener : friendEventListeners) {
                        listener.onFriendApplicationStatusChanged(
                                userId,
                                applicationType,
                                status,
                                directionType,
                                operationTime,
                                extra);
                    }
                }

                @Override
                public void onFriendCleared(long operationTime) {
                    for (FriendEventListener listener : friendEventListeners) {
                        listener.onFriendCleared(operationTime);
                    }
                }

                @Override
                public void onFriendInfoChangedSync(
                        String userId,
                        String remark,
                        Map<String, String> extProfile,
                        long operationTime) {
                    for (FriendEventListener listener : friendEventListeners) {
                        listener.onFriendInfoChangedSync(userId, remark, extProfile, operationTime);
                    }
                }
            };

    private IMCenter() {
        // default implementation ignored
    }

    public static IMCenter getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param application 应用上下文。
     * @param appKey 在融云开发者后台注册的应用 AppKey。
     * @param isEnablePush 是否使用推送功能
     * @deprecated 已废弃。 请使用{@link #init(Application, String, InitOption)}
     */
    public static void init(Application application, String appKey, boolean isEnablePush) {
        init(application, appKey, isEnablePush, null);
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param application 应用上下文。
     * @param appKey 在融云开发者后台注册的应用 AppKey。
     * @param option 初始化所需要的配置信息，详情可参考 {@link InitOption}
     * @since 5.4.1
     */
    public static void init(Application application, String appKey, InitOption option) {
        if (application == null) {
            RLog.e(TAG, "init error: application is null");
            return;
        }
        if (TextUtils.isEmpty(appKey)) {
            RLog.e(TAG, "init error: appKey is null");
            return;
        }
        if (option == null) {
            RLog.i(TAG, "init warn: option is null");
            option = new InitOption.Builder().build();
        }
        //        initEmojiConfig(application);
        SingletonHolder.sInstance.mContext = application.getApplicationContext();
        RongConfigCenter.syncFromXml(application);
        RongCoreClient.init(application.getApplicationContext(), appKey, option);
        // RongCoreClientImpl#initSDK有处理isMainProcess逻辑，所以RongCoreClient.init不需要判断进程
        if (option.isMainProcess() == null) {
            String current = io.rong.common.SystemUtils.getCurrentProcessName(application);
            String mainProcessName = application.getPackageName();
            if (!mainProcessName.equals(current)) {
                RLog.w(TAG, "Init. Current process : " + current);
                return;
            }
        } else if (Boolean.FALSE.equals(option.isMainProcess())) {
            RLog.w(TAG, "Init. isMainProcess : Boolean.FALSE");
            return;
        }
        RongExtensionManager.getInstance().init(application.getApplicationContext(), appKey);
        HQVoiceMsgDownloadManager.getInstance().init(application);
        RongNotificationManager.getInstance().init(application);
        RongConfigurationManager.init(application);
        RongCoreClient.addOnReceiveMessageListener(
                SingletonHolder.sInstance.mOnReceiveMessageListener);
        RongCoreClient.addConnectionStatusListener(
                SingletonHolder.sInstance.mConnectionStatusListener);
        RongIMClient.setOnRecallMessageListener(SingletonHolder.sInstance.mOnRecallMessageListener);
        RongIMClient.getInstance()
                .setConversationStatusListener(
                        SingletonHolder.sInstance.mConversationStatusListener);
        RongIMClient.setReadReceiptListener(SingletonHolder.sInstance.mReadReceiptListener);
        RongIMClient.getInstance()
                .setOnReceiveDestructionMessageListener(
                        SingletonHolder.sInstance.mOnReceiveDestructMessageListener);
        RongIMClient.getInstance()
                .setSyncConversationReadStatusListener(
                        SingletonHolder.sInstance.mSyncConversationReadStatusListener);
        RongIMClient.setTypingStatusListener(SingletonHolder.sInstance.mTypingStatusListener);
        ChannelClient.getInstance()
                .setUltraGroupChannelListener(SingletonHolder.sInstance.mUltraGroupChannelListener);
        RongCoreClient.getInstance()
                .setGroupEventListener(SingletonHolder.sInstance.mGroupEventListener);
        RongCoreClient.getInstance()
                .addSubscribeEventListener(SingletonHolder.sInstance.mOnSubscribeEventListener);
        RongCoreClient.getInstance()
                .setFriendEventListener(SingletonHolder.sInstance.mFriendEventListener);
        MessageNotificationHelper.setPushNotifyLevelListener();
        RongIMClient.registerMessageType(CombineMessage.class);
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param application 应用上下文。
     * @param appKey 在融云开发者后台注册的应用 AppKey。
     * @param isEnablePush 是否使用推送功能
     * @param isMainProcess 是否为主进程。如果为null，则代表由SDK判断进程
     */
    public static void init(
            Application application, String appKey, boolean isEnablePush, Boolean isMainProcess) {
        init(
                application,
                appKey,
                new InitOption.Builder()
                        .enablePush(isEnablePush)
                        .setMainProcess(isMainProcess)
                        .build());
    }

    /**
     * 设置创建生成的Fragment的工厂
     *
     * @param kitFragmentFactory KitFragmentFactory
     * @since 5.10.4
     */
    public static void setKitFragmentFactory(@NonNull KitFragmentFactory kitFragmentFactory) {
        IMCenter.kitFragmentFactory = kitFragmentFactory;
    }

    /**
     * 获取创建生成的Fragment的工厂
     *
     * @return KitFragmentFactory
     * @since 5.10.4
     */
    public static KitFragmentFactory getKitFragmentFactory() {
        return kitFragmentFactory;
    }

    /**
     * 设置会话界面操作的监听器。
     *
     * @param listener 会话界面操作的监听器。
     */
    public static void setConversationClickListener(ConversationClickListener listener) {
        RongConfigCenter.conversationConfig().setConversationClickListener(listener);
    }

    /**
     * 设置会话列表界面操作的监听器。
     *
     * @param listener 会话列表界面操作的监听器。
     */
    public static void setConversationListBehaviorListener(
            ConversationListBehaviorListener listener) {
        RongConfigCenter.conversationListConfig().setBehaviorListener(listener);
    }

    /**
     * 连接服务器，在整个应用程序全局，只需要调用一次。
     *
     * <p>调用该接口，SDK 会在 timeLimit 秒内尝试重连，直到出现下面三种情况之一： 第一、连接成功，回调 onSuccess(userId)。 第二、超时，回调
     * onError(RC_CONNECT_TIMEOUT)，并不再重连。 第三、出现 SDK 无法处理的错误，回调 onError(errorCode)（如 token 非法），并不再重连。
     * 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     *
     * @param token 从服务端获取的 <a href="http://docs.rongcloud.cn/android#token">用户身份令牌（ Token）</a>。
     * @param timeLimit 连接超时时间，单位：秒。{@code timeLimit <= 0}，则 IM 将一直连接，直到连接成功或者无法连接（如 {@code token}
     *     非法） {@code timeLimit > 0} ,则 IM 将最多连接 timeLimit 秒： 如果在 timeLimit
     *     秒内连接成功，后面再发生了网络变化或前后台切换，SDK 会自动重连； 如果在 timeLimit 秒无法连接成功则不再进行重连，通过 onError
     *     告知连接超时，您需要再自行调用 connect 接口
     * @param connectCallback 连接服务器的回调扩展类，新增打开数据库的回调，用户可以在此回调中执行拉取会话列表操作。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void connect(
            String token, int timeLimit, final RongIMClient.ConnectCallback connectCallback) {
        ConnectOption connectOption = ConnectOption.obtain(token, timeLimit);
        connect(connectOption, connectCallback);
    }

    /**
     * 返回 SDK 是否已经初始化。
     *
     * @return true 代表已经初始化；false 未初始化。
     */
    public boolean isInitialized() {
        return mContext != null;
    }

    private void connect(ConnectOption option, final RongIMClient.ConnectCallback connectCallback) {
        RongIMClient.connect(
                option.getToken(),
                option.getTimeLimit(),
                new RongIMClient.ConnectCallback() {
                    @Override
                    public void onSuccess(String s) {
                        if (connectCallback != null) {
                            connectCallback.onSuccess(s);
                        }
                        if (!TextUtils.isEmpty(s)) {
                            RongNotificationManager.getInstance().getNotificationQuietHours(null);
                            MessageNotificationHelper.clearCache();
                            RongUserInfoManager.getInstance()
                                    .initAndUpdateUserDataBase(SingletonHolder.sInstance.mContext);
                            for (RongIMClient.ConnectCallback callback : mConnectStatusListener) {
                                callback.onSuccess(s);
                            }
                        } else {
                            RLog.e(TAG, "IM connect success but userId is empty.");
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {
                        if (connectCallback != null) {
                            connectCallback.onError(connectionErrorCode);
                        }
                        for (RongIMClient.ConnectCallback callback : mConnectStatusListener) {
                            callback.onError(connectionErrorCode);
                        }
                    }

                    @Override
                    public void onDatabaseOpened(
                            RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                        if (connectCallback != null) {
                            connectCallback.onDatabaseOpened(databaseOpenStatus);
                        }
                        if (databaseOpenStatus
                                        == RongIMClient.DatabaseOpenStatus.DATABASE_OPEN_SUCCESS
                                && RongUserInfoManager.getInstance().isCacheUserOrGroupInfo()) {
                            RongUserInfoManager.getInstance()
                                    .initAndUpdateUserDataBase(SingletonHolder.sInstance.mContext);
                        }
                        for (RongIMClient.ConnectCallback callback : mConnectStatusListener) {
                            callback.onDatabaseOpened(databaseOpenStatus);
                        }
                    }
                });
    }

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调方式获取清空是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback 清空是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void clearMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .clearMessages(
                        conversationType,
                        targetId,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (bool) {
                                    for (MessageEventListener listener : mMessageEventListeners) {
                                        listener.onClearMessages(
                                                new ClearEvent(conversationType, targetId));
                                    }
                                    if (callback != null) callback.onSuccess(bool);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        });
    }

    public void connect(String token, final RongIMClient.ConnectCallback connectCallback) {
        connect(token, -1, connectCallback);
    }

    /**
     * 撤回消息
     *
     * @param message 将被撤回的消息
     * @param pushContent 被撤回时，通知栏显示的信息
     * @param callback 成功回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void recallMessage(
            final Message message, String pushContent, final RongIMClient.ResultCallback callback) {
        RongIMClient.getInstance()
                .recallMessage(
                        message,
                        pushContent,
                        new RongIMClient.ResultCallback<RecallNotificationMessage>() {
                            @Override
                            public void onSuccess(
                                    RecallNotificationMessage recallNotificationMessage) {
                                if (callback != null) {
                                    callback.onSuccess(recallNotificationMessage);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onRecallEvent(
                                            new RecallEvent(
                                                    message.getConversationType(),
                                                    message.getTargetId(),
                                                    message.getMessageId(),
                                                    recallNotificationMessage));
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                RLog.d(TAG, "recallMessage errorCode = " + errorCode.getValue());
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                            }
                        });
    }

    public RongIMClient.ConnectionStatusListener.ConnectionStatus getCurrentConnectionStatus() {
        return RongIMClient.getInstance().getCurrentConnectionStatus();
    }

    /**
     * 根据会话类型，发送消息。
     *
     * <p>通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条</strong>。
     *
     * @param type 会话类型。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param content 消息内容，例如 {@link TextMessage}, {@link ImageMessage}。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 SDK
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调。参考 {@link IRongCallback.ISendMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendMessage(
            final Conversation.ConversationType type,
            final String targetId,
            final MessageContent content,
            final String pushContent,
            final String pushData,
            final IRongCallback.ISendMessageCallback callback) {
        Message message = Message.obtain(targetId, type, content);
        sendMessage(message, pushContent, pushData, callback);
    }

    /**
     * 发送消息。 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。
     *
     * @param message 将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendMessage(
            Message message,
            String pushContent,
            String pushData,
            IRongCallback.ISendMessageCallback callback) {
        sendMessage(message, pushContent, pushData, null, callback);
    }

    /**
     * 发送消息。 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。
     *
     * @param message 将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendMessage(
            Message message,
            String pushContent,
            final String pushData,
            SendMessageOption option,
            final IRongCallback.ISendMessageCallback callback) {
        message.setNeedReceipt(true);
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance()
                .sendMessage(
                        message,
                        pushContent,
                        pushData,
                        option,
                        new IRongCallback.ISendMessageCallback() {
                            @Override
                            public void onAttached(Message message) {
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.ATTACH, message));
                                }
                                if (callback != null) callback.onAttached(message);
                            }

                            @Override
                            public void onSuccess(Message message) {
                                filterSentMessage(message, null, null);
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                                if (mMessageInterceptor != null
                                        && mMessageInterceptor.interceptOnSentMessage(message)) {
                                    RLog.d(TAG, "message has been intercepted.");
                                    return;
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.SUCCESS, message));
                                }
                            }

                            @Override
                            public void onError(
                                    final Message message, final RongIMClient.ErrorCode errorCode) {
                                filterSentMessage(
                                        message,
                                        errorCode,
                                        new FilterSentListener() {
                                            @Override
                                            public void onComplete() {
                                                for (MessageEventListener item :
                                                        mMessageEventListeners) {
                                                    item.onSendMessage(
                                                            new SendEvent(
                                                                    SendEvent.ERROR,
                                                                    message,
                                                                    errorCode));
                                                }
                                            }
                                        });
                                if (callback != null) callback.onError(message, errorCode);
                            }
                        });
    }

    /**
     * 发送已读回执，该方法会触发刷新消息未读数
     *
     * <p>通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条</strong>。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param timestamp 时间戳
     * @param callback 发送消息的回调。参考 {@link IRongCallback.ISendMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendReadReceiptMessage(
            final Conversation.ConversationType conversationType,
            final String targetId,
            long timestamp,
            final IRongCallback.ISendMessageCallback callback) {
        RongIMClient.getInstance()
                .sendReadReceiptMessage(
                        conversationType,
                        targetId,
                        timestamp,
                        new IRongCallback.ISendMessageCallback() {
                            @Override
                            public void onAttached(Message message) {
                                if (callback != null) {
                                    callback.onAttached(message);
                                }
                            }

                            @Override
                            public void onSuccess(Message message) {
                                filterSentMessage(message, null, null);
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(
                                            ConversationIdentifier.obtain(
                                                    conversationType,
                                                    targetId,
                                                    message.getChannelId()));
                                }
                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                                filterSentMessage(message, errorCode, null);
                                if (callback != null) {
                                    callback.onError(message, errorCode);
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(
                                            ConversationIdentifier.obtain(
                                                    conversationType,
                                                    targetId,
                                                    message != null ? message.getChannelId() : ""));
                                }
                            }
                        });
    }

    /**
     * 同步会话阅读状态。
     *
     * @param type 会话类型
     * @param targetId 会话 id
     * @param timestamp 会话中已读的最后一条消息的发送时间戳 {@link Message#getSentTime()}
     * @param callback 回调函数。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void syncConversationReadStatus(
            final Conversation.ConversationType type,
            final String targetId,
            long timestamp,
            final RongIMClient.OperationCallback callback) {
        syncConversationReadStatus(
                ConversationIdentifier.obtain(type, targetId, ""), timestamp, callback);
    }

    /**
     * 同步会话阅读状态。
     *
     * @param conversationIdentifier 会话标识
     * @param timestamp 会话中已读的最后一条消息的发送时间戳 {@link Message#getSentTime()}
     * @param callback 回调函数。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void syncConversationReadStatus(
            final ConversationIdentifier conversationIdentifier,
            long timestamp,
            final RongIMClient.OperationCallback callback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "syncConversationReadStatus conversationIdentifier is null");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        ChannelClient.getInstance()
                .syncConversationReadStatus(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        timestamp,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(conversationIdentifier);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                if (callback != null) {
                                    callback.onError(
                                            RongIMClient.ErrorCode.valueOf(coreErrorCode.code));
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(conversationIdentifier);
                                }
                            }
                        });
    }

    /**
     * 发送消息。 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。
     *
     * @param message 将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调，参考 {@link IRongCallback.ISendMediaMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendMediaMessage(
            Message message,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMediaMessageCallback callback) {
        sendMediaMessage(message, pushContent, pushData, new SendMessageOption(), callback);
    }

    /**
     * 发送消息。 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。
     *
     * @param message 将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调，参考 {@link IRongCallback.ISendMediaMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     * @param option 发送消息附加选项，目前仅支持设置 isVoIPPush，如果对端设备是 iOS，设置 isVoIPPush 为 True，会走 VoIP 通道推送 Push。
     * @since 5.4.4
     */
    public void sendMediaMessage(
            Message message,
            String pushContent,
            final String pushData,
            final SendMessageOption option,
            final IRongCallback.ISendMediaMessageCallback callback) {
        message.setNeedReceipt(true);
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance()
                .sendMediaMessage(
                        message,
                        pushContent,
                        pushData,
                        option,
                        new IRongCallback.ISendMediaMessageCallback() {
                            @Override
                            public void onProgress(Message message, int i) {
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(
                                                    SendMediaEvent.PROGRESS, message, i));
                                }
                                if (callback != null) callback.onProgress(message, i);
                            }

                            @Override
                            public void onCanceled(Message message) {
                                filterSentMessage(message, null, null);
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(SendMediaEvent.CANCEL, message));
                                }
                                if (callback != null) callback.onCanceled(message);
                            }

                            @Override
                            public void onAttached(Message message) {
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(SendMediaEvent.ATTACH, message));
                                }
                                if (callback != null) callback.onAttached(message);
                            }

                            @Override
                            public void onSuccess(Message message) {
                                filterSentMessage(message, null, null);
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                                if (mMessageInterceptor != null
                                        && mMessageInterceptor.interceptOnSentMessage(message)) {
                                    RLog.d(TAG, "message has been intercepted.");
                                    return;
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(SendMediaEvent.SUCCESS, message));
                                }
                            }

                            @Override
                            public void onError(
                                    final Message message, final RongIMClient.ErrorCode errorCode) {
                                filterSentMessage(
                                        message,
                                        errorCode,
                                        new FilterSentListener() {
                                            @Override
                                            public void onComplete() {
                                                for (MessageEventListener item :
                                                        mMessageEventListeners) {
                                                    item.onSendMediaMessage(
                                                            new SendMediaEvent(
                                                                    SendMediaEvent.ERROR,
                                                                    message,
                                                                    errorCode));
                                                }
                                            }
                                        });
                                if (callback != null) callback.onError(message, errorCode);
                            }
                        });
    }

    public void sendMediaMessage(
            Message message,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMediaMessageCallbackWithUploader callback) {
        sendMediaMessage(message, pushContent, pushData, new SendMessageOption(), callback);
    }

    public void sendMediaMessage(
            Message message,
            String pushContent,
            final String pushData,
            final SendMessageOption option,
            final IRongCallback.ISendMediaMessageCallbackWithUploader callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        IRongCallback.ISendMediaMessageCallbackWithUploader sendMediaMessageCallbackWithUploader =
                new IRongCallback.ISendMediaMessageCallbackWithUploader() {
                    @Override
                    public void onAttached(
                            Message message, IRongCallback.MediaMessageUploader uploader) {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(
                                    new SendMediaEvent(SendMediaEvent.ATTACH, message));
                        }
                        if (callback != null) callback.onAttached(message, uploader);
                    }

                    @Override
                    public void onError(
                            final Message message, final RongIMClient.ErrorCode errorCode) {
                        filterSentMessage(
                                message,
                                errorCode,
                                new FilterSentListener() {
                                    @Override
                                    public void onComplete() {
                                        for (MessageEventListener item : mMessageEventListeners) {
                                            item.onSendMediaMessage(
                                                    new SendMediaEvent(
                                                            SendMediaEvent.ERROR,
                                                            message,
                                                            errorCode));
                                        }
                                    }
                                });
                        if (callback != null) callback.onError(message, errorCode);
                    }

                    @Override
                    public void onProgress(Message message, int progress) {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(
                                    new SendMediaEvent(SendMediaEvent.PROGRESS, message, progress));
                        }
                        if (callback != null) {
                            callback.onProgress(message, progress);
                        }
                    }

                    @Override
                    public void onSuccess(Message message) {
                        filterSentMessage(message, null, null);
                        if (callback != null) {
                            callback.onSuccess(message);
                        }
                        if (mMessageInterceptor != null
                                && mMessageInterceptor.interceptOnSentMessage(message)) {
                            RLog.d(TAG, "message has been intercepted.");
                            return;
                        }
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(
                                    new SendMediaEvent(SendMediaEvent.SUCCESS, message));
                        }
                    }

                    @Override
                    public void onCanceled(Message message) {
                        filterSentMessage(message, null, null);
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(
                                    new SendMediaEvent(SendMediaEvent.CANCEL, message));
                        }
                        if (callback != null) callback.onCanceled(message);
                    }
                };

        RongIMClient.getInstance()
                .sendMediaMessage(
                        message,
                        pushContent,
                        pushData,
                        option,
                        sendMediaMessageCallbackWithUploader);
    }

    /**
     * 发送定向消息。向会话中特定的某些用户发送消息，会话中其他用户将不会收到此消息。 通过 {@link IRongCallback.ISendMessageCallback}
     * 中的方法回调发送的消息状态及消息体。 此方法只能发送非多媒体消息，多媒体消息如{@link ImageMessage} {@link FileMessage} ，或者继承自{@link
     * MediaMessageContent}的消息须调用 {@link #sendDirectionalMediaMessage(Message, String[], String,
     * String, IRongCallback.ISendMediaMessageCallback)}。
     *
     * <p>从 5.6.9 版本开始，支持超级群会话类型。
     *
     * @param type 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是群组 Id 、超级群 Id。
     * @param content 消息内容，例如 {@link TextMessage}
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param userIds 会话中将会接收到此消息的用户列表。
     * @param callback 发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendDirectionalMessage(
            Conversation.ConversationType type,
            String targetId,
            MessageContent content,
            final String[] userIds,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMessageCallback callback) {
        Message message = Message.obtain(targetId, type, content);
        sendDirectionalMessage(message, userIds, pushContent, pushData, callback);
    }

    public void sendDirectionalMessage(
            Message message,
            final String[] userIds,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMessageCallback callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        ChannelClient.getInstance()
                .sendDirectionalMessage(
                        message,
                        userIds,
                        pushContent,
                        pushData,
                        null,
                        new IRongCoreCallback.ISendMessageCallback() {
                            @Override
                            public void onAttached(Message message) {
                                if (callback != null) {
                                    callback.onAttached(message);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.ATTACH, message));
                                }
                            }

                            @Override
                            public void onSuccess(Message message) {
                                filterSentMessage(message, null, null);
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                                if (mMessageInterceptor != null
                                        && mMessageInterceptor.interceptOnSentMessage(message)) {
                                    RLog.d(TAG, "message has been intercepted.");
                                    return;
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.SUCCESS, message));
                                }
                            }

                            @Override
                            public void onError(Message message, IRongCoreEnum.CoreErrorCode e) {
                                filterSentMessage(
                                        message,
                                        RongIMClient.ErrorCode.valueOf(e.code),
                                        new FilterSentListener() {
                                            @Override
                                            public void onComplete() {
                                                for (MessageEventListener item :
                                                        mMessageEventListeners) {
                                                    item.onSendMessage(
                                                            new SendEvent(
                                                                    SendEvent.ERROR, message));
                                                }
                                            }
                                        });
                                if (callback != null) {
                                    callback.onError(
                                            message, RongIMClient.ErrorCode.valueOf(e.code));
                                }
                            }
                        });
    }

    /**
     * 发送定向多媒体消息 向会话中特定的某些用户发送消息，会话中其他用户将不会收到此消息。
     *
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为多媒体消息，如 {@link ImageMessage} {@link
     * FileMessage}
     *
     * <p>或者其他继承自 {@link MediaMessageContent} 的消息
     *
     * <p>从 5.6.9 版本开始，支持超级群会话类型。
     *
     * @param message 发送消息的实体。
     * @param userIds 定向接收者 id 数组
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 发送文件消息时，此字段必须填写，否则会收不到 push 推送。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调 {@link RongIMClient.SendMediaMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendDirectionalMediaMessage(
            Message message,
            String[] userIds,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMediaMessageCallback callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance()
                .sendDirectionalMediaMessage(
                        message,
                        userIds,
                        pushContent,
                        pushData,
                        new IRongCallback.ISendMediaMessageCallback() {
                            @Override
                            public void onProgress(Message message, int i) {
                                if (callback != null) {
                                    callback.onProgress(message, i);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(
                                                    SendMediaEvent.PROGRESS, message, i));
                                }
                            }

                            @Override
                            public void onCanceled(Message message) {
                                filterSentMessage(message, null, null);
                                if (callback != null) {
                                    callback.onCanceled(message);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(SendMediaEvent.CANCEL, message));
                                }
                            }

                            @Override
                            public void onAttached(Message message) {
                                if (callback != null) {
                                    callback.onAttached(message);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(SendMediaEvent.ATTACH, message));
                                }
                            }

                            @Override
                            public void onSuccess(Message message) {
                                filterSentMessage(message, null, null);
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                                if (mMessageInterceptor != null
                                        && mMessageInterceptor.interceptOnSentMessage(message)) {
                                    RLog.d(TAG, "message has been intercepted.");
                                    return;
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMediaMessage(
                                            new SendMediaEvent(SendMediaEvent.SUCCESS, message));
                                }
                            }

                            @Override
                            public void onError(
                                    final Message message, final RongIMClient.ErrorCode errorCode) {
                                filterSentMessage(
                                        message,
                                        errorCode,
                                        new FilterSentListener() {
                                            @Override
                                            public void onComplete() {
                                                for (MessageEventListener item :
                                                        mMessageEventListeners) {
                                                    item.onSendMediaMessage(
                                                            new SendMediaEvent(
                                                                    SendMediaEvent.ERROR,
                                                                    message,
                                                                    errorCode));
                                                }
                                            }
                                        });
                                if (callback != null) {
                                    callback.onError(message, errorCode);
                                }
                            }
                        });
    }

    /**
     * 删除指定时间戳之前的消息，可选择是否同时删除服务器端消息
     *
     * <p>此方法可从服务器端清除历史消息，<Strong>但是必须先开通历史消息云存储功能。</Strong> <br>
     * 根据会话类型和会话 id 清除某一会话指定时间戳之前的本地数据库消息（服务端历史消息）， 清除成功后只能从本地数据库（服务端）获取到该时间戳之后的历史消息。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 id。
     * @param recordTime 清除消息截止时间戳，{@code 0 <= recordTime <= }当前会话最后一条消息的 sentTime,0
     *     清除所有消息，其他值清除小于等于 recordTime 的消息。
     * @param cleanRemote 是否删除服务器端消息
     * @param callback 清除消息的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void cleanHistoryMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final long recordTime,
            final boolean cleanRemote,
            final RongIMClient.OperationCallback callback) {
        cleanHistoryMessages(
                ConversationIdentifier.obtain(conversationType, targetId, ""),
                recordTime,
                cleanRemote,
                callback);
    }

    /**
     * 删除指定时间戳之前的消息，可选择是否同时删除服务器端消息
     *
     * <p>此方法可从服务器端清除历史消息，<Strong>但是必须先开通历史消息云存储功能。</Strong> <br>
     * 根据会话类型和会话 id 清除某一会话指定时间戳之前的本地数据库消息（服务端历史消息）， 清除成功后只能从本地数据库（服务端）获取到该时间戳之后的历史消息。
     *
     * @param conversationIdentifier 会话标识。
     * @param recordTime 清除消息截止时间戳，{@code 0 <= recordTime <= }当前会话最后一条消息的 sentTime,0
     *     清除所有消息，其他值清除小于等于 recordTime 的消息。
     * @param cleanRemote 是否删除服务器端消息
     * @param callback 清除消息的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void cleanHistoryMessages(
            final ConversationIdentifier conversationIdentifier,
            final long recordTime,
            final boolean cleanRemote,
            final RongIMClient.OperationCallback callback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "cleanHistoryMessages conversationIdentifier is null");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        ChannelClient.getInstance()
                .cleanHistoryMessages(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        recordTime,
                        cleanRemote,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) callback.onSuccess();
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedMessage(conversationIdentifier);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (callback != null)
                                    callback.onError(RongIMClient.ErrorCode.valueOf(e.code));
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onOperationFailed(
                                            RongIMClient.ErrorCode.valueOf(e.code));
                                }
                            }
                        });
    }

    /**
     * 清除某会话的消息未读状态
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback 清除是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void clearMessagesUnreadStatus(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final RongIMClient.ResultCallback<Boolean> callback) {
        clearMessagesUnreadStatus(
                ConversationIdentifier.obtain(conversationType, targetId, ""), callback);
    }

    /**
     * 清除某会话的消息未读状态
     *
     * @param conversationIdentifier 会话标识。
     * @param callback 清除是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void clearMessagesUnreadStatus(
            final ConversationIdentifier conversationIdentifier,
            final RongIMClient.ResultCallback<Boolean> callback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "clearMessagesUnreadStatus conversationIdentifier is null");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        ChannelClient.getInstance()
                .clearMessagesUnreadStatus(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        new IRongCoreCallback.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (callback != null) callback.onSuccess(bool);
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(conversationIdentifier);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (callback != null)
                                    callback.onError(RongIMClient.ErrorCode.valueOf(e.code));
                            }
                        });
    }

    /**
     * 清除某一会话的文字消息草稿，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 是否清除成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void clearTextMessageDraft(
            Conversation.ConversationType conversationType,
            String targetId,
            RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 保存文字消息草稿，回调方式获取保存是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content 草稿的文字内容。
     * @param callback 是否保存成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void saveTextMessageDraft(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final String content,
            final RongIMClient.ResultCallback<Boolean> callback) {
        saveTextMessageDraft(
                ConversationIdentifier.obtain(conversationType, targetId, ""), content, callback);
    }

    /**
     * 保存文字消息草稿，回调方式获取保存是否成功。
     *
     * @param conversationIdentifier 会话标识。
     * @param content 草稿的文字内容。
     * @param callback 是否保存成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void saveTextMessageDraft(
            final ConversationIdentifier conversationIdentifier,
            final String content,
            final RongIMClient.ResultCallback<Boolean> callback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "saveTextMessageDraft conversationIdentifier is null");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        ChannelClient.getInstance()
                .saveTextMessageDraft(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        content,
                        new IRongCoreCallback.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean value) {
                                if (callback != null) {
                                    callback.onSuccess(value);
                                }
                                if (value) {
                                    for (ConversationEventListener listener :
                                            mConversationEventListener) {
                                        listener.onSaveDraft(conversationIdentifier, content);
                                    }
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (callback != null) {
                                    callback.onError(RongIMClient.ErrorCode.valueOf(e.code));
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onOperationFailed(
                                            RongIMClient.ErrorCode.valueOf(e.code));
                                }
                            }
                        });
    }

    /**
     * 从会话列表中移除某一会话，但是不删除会话内的消息。
     *
     * <p>如果此会话中有新的消息，该会话将重新在会话列表中显示，并显示最近的历史消息。
     *
     * @param type 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 移除会话是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void removeConversation(
            final Conversation.ConversationType type,
            final String targetId,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .removeConversation(
                        type,
                        targetId,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (callback != null) {
                                    callback.onSuccess(bool);
                                }
                                if (bool) {
                                    for (ConversationEventListener listener :
                                            mConversationEventListener) {
                                        listener.onConversationRemoved(type, targetId);
                                    }
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        });
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param conversationIdentifier 会话标识。
     * @param isTop 是否置顶。
     * @param callback 设置置顶或取消置顶是否成功的回调。 该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     * @since 5.6.8
     */
    public void setConversationToTop(
            final ConversationIdentifier conversationIdentifier,
            final boolean isTop,
            final RongIMClient.ResultCallback<Boolean> callback) {
        setConversationToTop(conversationIdentifier, isTop, true, callback);
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param type 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param isTop 是否置顶。
     * @param needCreate 会话不存在时，是否创建会话。
     * @param callback 设置置顶或取消置顶是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     * @discussion 5.6.8 版本开始废弃该接口，使用 {@link #setConversationToTop(ConversationIdentifier, boolean,
     *     RongIMClient.ResultCallback)} 替换。
     */
    @Deprecated
    public void setConversationToTop(
            final Conversation.ConversationType type,
            final String targetId,
            final boolean isTop,
            final boolean needCreate,
            final RongIMClient.ResultCallback<Boolean> callback) {
        setConversationToTop(
                ConversationIdentifier.obtain(type, targetId, ""), isTop, needCreate, callback);
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param conversationIdentifier 会话标识。
     * @param isTop 是否置顶。
     * @param needCreate 会话不存在时，是否创建会话。
     * @param needUpdateTime 是否更新操作时间。
     * @param callback 设置置顶或取消置顶是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     * @discussion 5.6.8 版本开始废弃该接口，使用 {@link #setConversationToTop(ConversationIdentifier, boolean,
     *     RongIMClient.ResultCallback)} 替换。
     */
    public void setConversationToTop(
            final ConversationIdentifier conversationIdentifier,
            final boolean isTop,
            final boolean needCreate,
            final boolean needUpdateTime,
            final RongIMClient.ResultCallback<Boolean> callback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "setConversationToTop conversationIdentifier is null");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        ChannelClient.getInstance()
                .setConversationToTop(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        isTop,
                        needCreate,
                        needUpdateTime,
                        new IRongCoreCallback.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (callback != null) callback.onSuccess(bool);
                                for (RongIMClient.ConversationStatusListener listener :
                                        mConversationStatusObserverList) {
                                    ConversationStatus conversationStatus =
                                            new ConversationStatus();
                                    conversationStatus.setTargetId(
                                            conversationIdentifier.getTargetId());
                                    conversationStatus.setConversationType(
                                            conversationIdentifier.getTypeValue());
                                    HashMap<String, String> statusMap = new HashMap<>();
                                    statusMap.put(
                                            ConversationStatus.TOP_KEY,
                                            isTop
                                                    ? ConversationStatus.TopStatus.TOP.value
                                                    : ConversationStatus.TopStatus.UNTOP.value);
                                    conversationStatus.setStatus(statusMap);
                                    listener.onStatusChanged(
                                            new ConversationStatus[] {conversationStatus});
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (callback != null)
                                    callback.onError(RongIMClient.ErrorCode.valueOf(e.code));
                            }
                        });
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param conversationIdentifier 会话标识。
     * @param isTop 是否置顶。
     * @param needCreate 会话不存在时，是否创建会话。
     * @param callback 设置置顶或取消置顶是否成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     * @discussion 5.6.8 版本开始废弃该接口，使用 {@link #setConversationToTop(ConversationIdentifier, boolean,
     *     RongIMClient.ResultCallback)} 替换。
     */
    @Deprecated
    public void setConversationToTop(
            final ConversationIdentifier conversationIdentifier,
            final boolean isTop,
            final boolean needCreate,
            final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance()
                .setConversationToTop(conversationIdentifier, isTop, needCreate, false, callback);
    }

    /**
     * 设置会话消息提醒状态。
     *
     * @param conversationType 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param notificationStatus 是否屏蔽。
     * @param callback 设置状态的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void setConversationNotificationStatus(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final Conversation.ConversationNotificationStatus notificationStatus,
            final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>
                    callback) {
        setConversationNotificationStatus(
                ConversationIdentifier.obtain(conversationType, targetId, ""),
                notificationStatus,
                callback);
    }

    /**
     * 设置会话消息提醒状态。
     *
     * @param conversationIdentifier 会话标识。
     * @param notificationStatus 是否屏蔽。
     * @param callback 设置状态的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void setConversationNotificationStatus(
            final ConversationIdentifier conversationIdentifier,
            final Conversation.ConversationNotificationStatus notificationStatus,
            final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>
                    callback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "setConversationNotificationStatus conversationIdentifier is null");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        ChannelClient.getInstance()
                .setConversationNotificationStatus(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        notificationStatus,
                        new IRongCoreCallback.ResultCallback<
                                Conversation.ConversationNotificationStatus>() {
                            @Override
                            public void onSuccess(
                                    Conversation.ConversationNotificationStatus status) {
                                if (callback != null) {
                                    callback.onSuccess(status);
                                }
                                for (RongIMClient.ConversationStatusListener listener :
                                        mConversationStatusObserverList) {
                                    ConversationStatus conversationStatus =
                                            new ConversationStatus();
                                    conversationStatus.setTargetId(
                                            conversationIdentifier.getTargetId());
                                    conversationStatus.setConversationType(
                                            conversationIdentifier.getTypeValue());
                                    HashMap<String, String> statusMap = new HashMap<>();
                                    statusMap.put(
                                            ConversationStatus.NOTIFICATION_KEY,
                                            notificationStatus.equals(
                                                            Conversation
                                                                    .ConversationNotificationStatus
                                                                    .DO_NOT_DISTURB)
                                                    ? "1"
                                                    : "0");
                                    conversationStatus.setStatus(statusMap);
                                    listener.onStatusChanged(
                                            new ConversationStatus[] {conversationStatus});
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (callback != null) {
                                    callback.onError(RongIMClient.ErrorCode.valueOf(e.code));
                                }
                            }
                        });
    }

    /**
     * 向本地会话中插入一条消息，方向为发送。这条消息只是插入本地会话，不会实际发送给服务器和对方。 插入消息需为入库消息，即 {@link
     * MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param type 会话类型。
     * @param targetId 目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param sentStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content 消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param resultCallback 获得消息发送实体的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void insertOutgoingMessage(
            Conversation.ConversationType type,
            String targetId,
            Message.SentStatus sentStatus,
            MessageContent content,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        insertOutgoingMessage(
                ConversationIdentifier.obtain(type, targetId, ""),
                sentStatus,
                content,
                System.currentTimeMillis(),
                resultCallback);
    }

    public void insertOutgoingMessage(
            Conversation.ConversationType type,
            String targetId,
            Message.SentStatus sentStatus,
            MessageContent content,
            long time,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        insertOutgoingMessage(
                ConversationIdentifier.obtain(type, targetId, ""),
                sentStatus,
                content,
                time,
                resultCallback);
    }

    /**
     * 向本地会话中插入一条消息，方向为发送。这条消息只是插入本地会话，不会实际发送给服务器和对方。 插入消息需为入库消息，即 {@link
     * MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param conversationIdentifier 会话标识。
     * @param sentStatus 发送状态 @see {@link Message.SentStatus}
     * @param content 消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param time 插入消息所要模拟的发送时间。
     * @param resultCallback 获得消息发送实体的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void insertOutgoingMessage(
            ConversationIdentifier conversationIdentifier,
            Message.SentStatus sentStatus,
            MessageContent content,
            long time,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        if (conversationIdentifier == null) {
            RLog.e(TAG, "insertOutgoingMessage conversationIdentifier is null");
            if (resultCallback != null) {
                resultCallback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        if (mMessageInterceptor != null
                && mMessageInterceptor.interceptOnInsertOutgoingMessage(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        sentStatus,
                        content,
                        time,
                        resultCallback)) {
            RLog.d(TAG, "message insertOut has been intercepted.");
            return;
        }
        ChannelClient.getInstance()
                .insertOutgoingMessage(
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId(),
                        conversationIdentifier.getChannelId(),
                        sentStatus,
                        content,
                        time,
                        new IRongCoreCallback.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message message) {
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onInsertMessage(new InsertEvent(message));
                                }
                                if (resultCallback != null) {
                                    resultCallback.onSuccess(message);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                if (resultCallback != null) {
                                    resultCallback.onError(
                                            RongIMClient.ErrorCode.valueOf(errorCode.getValue()));
                                }
                            }
                        });
    }

    public void insertIncomingMessage(
            Conversation.ConversationType type,
            String targetId,
            String senderId,
            Message.ReceivedStatus receivedStatus,
            MessageContent content,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        insertIncomingMessage(
                type,
                targetId,
                senderId,
                receivedStatus,
                content,
                System.currentTimeMillis(),
                resultCallback);
    }

    public void insertIncomingMessage(
            Conversation.ConversationType type,
            String targetId,
            String senderId,
            Message.ReceivedStatus receivedStatus,
            MessageContent content,
            long time,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        if (mMessageInterceptor != null
                && mMessageInterceptor.interceptOnInsertIncomingMessage(
                        type, targetId, senderId, receivedStatus, content, time)) {
            RLog.d(TAG, "message insertIncoming has been intercepted.");
            return;
        }

        RongIMClient.getInstance()
                .insertIncomingMessage(
                        type,
                        targetId,
                        senderId,
                        receivedStatus,
                        content,
                        time,
                        new RongIMClient.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message message) {
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onInsertMessage(new InsertEvent(message));
                                }
                                if (resultCallback != null) {
                                    resultCallback.onSuccess(message);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (resultCallback != null) {
                                    resultCallback.onError(errorCode);
                                }
                            }
                        });
    }

    /**
     * 清除指定会话的消息。
     *
     * <p>此接口会删除指定会话中数据库的所有消息，同时，会清理数据库空间。 如果数据库特别大，超过几百 M，调用该接口会有少许耗时。
     *
     * @param conversationType 要删除的消息 Id 数组。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback 是否删除成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void deleteMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .deleteMessages(
                        conversationType,
                        targetId,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (bool) {
                                    for (MessageEventListener item : mMessageEventListeners) {
                                        item.onClearMessages(
                                                new ClearEvent(conversationType, targetId));
                                    }
                                }
                                if (callback != null) callback.onSuccess(bool);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        });
    }

    /**
     * 删除会话里的一条或多条消息。
     *
     * @param conversationType 会话类型
     * @param targetId 会话 Id
     * @param messageIds 待删除的消息 Id 数组。
     * @param callback 删除操作的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void deleteMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final int[] messageIds,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .deleteMessages(
                        messageIds,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                if (aBoolean) {
                                    for (MessageEventListener item : mMessageEventListeners) {
                                        item.onDeleteMessage(
                                                new DeleteEvent(
                                                        conversationType, targetId, messageIds));
                                    }
                                }
                                if (callback != null) callback.onSuccess(aBoolean);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) callback.onError(errorCode);
                            }
                        });
    }

    /**
     * 删除指定的一条或者一组消息。会同时删除本地和远端消息。
     *
     * <p>请注意，此方法会删除远端消息，请慎重使用
     *
     * <p>5.6.9版本以下不支持超级群会话类型，从 5.6.9 版本开始支持超级群会话类型
     *
     * @param conversationType 会话类型。暂时不支持聊天室
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、客服 Id。
     * @param messages 要删除的消息数组, 数组大小不能超过100条。
     * @param callback 是否删除成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void deleteRemoteMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final Message[] messages,
            final RongIMClient.OperationCallback callback) {
        deleteRemoteMessages(
                ConversationIdentifier.obtain(conversationType, targetId, ""), messages, callback);
    }

    /**
     * 删除指定的一条或者一组消息。会同时删除本地和远端消息。
     *
     * <p>请注意，此方法会删除远端消息，请慎重使用
     *
     * <p>5.6.9版本以下不支持超级群会话类型，从 5.6.9 版本开始支持超级群会话类型
     *
     * @param identifier 会话标识。会话类型不支持聊天室。
     * @param messages 要删除的消息数组, 数组大小不能超过100条。
     * @param callback 是否删除成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     * @since 5.6.9
     */
    public void deleteRemoteMessages(
            final ConversationIdentifier identifier,
            final Message[] messages,
            final RongIMClient.OperationCallback callback) {
        ChannelClient.getInstance()
                .deleteRemoteMessages(
                        identifier.getType(),
                        identifier.getTargetId(),
                        identifier.optChannelId(),
                        messages,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                int[] messageIds = new int[messages.length];
                                for (int i = 0; i < messages.length; i++) {
                                    messageIds[i] = messages[i].getMessageId();
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDeleteMessage(
                                            new DeleteEvent(
                                                    identifier.getType(),
                                                    identifier.getTargetId(),
                                                    messageIds));
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(
                                            RongIMClient.ErrorCode.valueOf(errorCode.code));
                                }
                            }
                        });
    }

    /** 断开连接(断开后继续接收 Push 消息)。 */
    public void disconnect() {
        RongIMClient.getInstance().disconnect();
        RongExtensionManager.getInstance().disconnect();
    }

    /**
     * 下载文件。
     *
     * <p>用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param conversationType 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param mediaType 文件类型。
     * @param imageUrl 文件的 URL 地址。
     * @param callback 下载文件的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void downloadMedia(
            Conversation.ConversationType conversationType,
            String targetId,
            RongIMClient.MediaType mediaType,
            String imageUrl,
            final RongIMClient.DownloadMediaCallback callback) {
        RongIMClient.getInstance()
                .downloadMedia(conversationType, targetId, mediaType, imageUrl, callback);
    }

    /**
     * 下载文件 支持断点续传
     *
     * @param uid 文件唯一标识
     * @param fileUrl 文件下载地址
     * @param fileName 文件名
     * @param path 文件下载保存目录，如果是 targetVersion 29 为目标，由于访问权限原因，建议使用 context.getExternalFilesDir()
     *     方法保存到私有目录
     * @param callback 回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void downloadMediaFile(
            final String uid,
            String fileUrl,
            String fileName,
            String path,
            final IRongCallback.IDownloadMediaFileCallback callback) {
        RongIMClient.getInstance()
                .downloadMediaFile(
                        uid,
                        fileUrl,
                        fileName,
                        path,
                        new IRongCallback.IDownloadMediaFileCallback() {
                            @Override
                            public void onFileNameChanged(String newFileName) {
                                IRongCallback.IDownloadMediaFileCallback listener =
                                        mMediaListeners.get(uid);
                                if (listener != null) {
                                    listener.onFileNameChanged(newFileName);
                                }
                                if (callback != null) {
                                    callback.onFileNameChanged(newFileName);
                                }
                            }

                            @Override
                            public void onSuccess() {
                                IRongCallback.IDownloadMediaFileCallback listener =
                                        mMediaListeners.get(uid);
                                if (listener != null) {
                                    listener.onSuccess();
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onProgress(int progress) {
                                IRongCallback.IDownloadMediaFileCallback listener =
                                        mMediaListeners.get(uid);
                                if (listener != null) {
                                    listener.onProgress(progress);
                                }
                                if (callback != null) {
                                    callback.onProgress(progress);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode code) {
                                IRongCallback.IDownloadMediaFileCallback listener =
                                        mMediaListeners.get(uid);
                                if (listener != null) {
                                    listener.onError(code);
                                }
                                if (callback != null) {
                                    callback.onError(code);
                                }
                            }

                            @Override
                            public void onCanceled() {
                                IRongCallback.IDownloadMediaFileCallback listener =
                                        mMediaListeners.get(uid);
                                if (listener != null) {
                                    listener.onCanceled();
                                }
                                if (callback != null) {
                                    callback.onCanceled();
                                }
                            }
                        });
    }

    /**
     * 取消下载多媒体文件。
     *
     * @param message 包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消下载多媒体文件时的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void cancelDownloadMediaMessage(
            Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelDownloadMediaMessage(message, callback);
    }

    /**
     * 清空所有会话及会话内的消息，回调方式通知是否清空成功。
     *
     * @param callback 是否清空成功的回调。
     * @param conversationTypes 会话类型。
     */
    public void clearConversations(
            final RongIMClient.ResultCallback callback,
            final Conversation.ConversationType... conversationTypes) {
        RongIMClient.getInstance()
                .clearConversations(
                        new RongIMClient.ResultCallback() {
                            @Override
                            public void onSuccess(Object o) {
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearConversations(conversationTypes);
                                }
                                if (callback != null) callback.onSuccess(o);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        },
                        conversationTypes);
    }

    /**
     * 下载文件
     *
     * <p>用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param message 文件消息。
     * @param callback 下载文件的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void downloadMediaMessage(
            Message message, final IRongCallback.IDownloadMediaMessageCallback callback) {
        for (MessageEventListener item : mMessageEventListeners) {
            item.onDownloadMessage(new DownloadEvent(DownloadEvent.START, message));
        }
        RongIMClient.getInstance()
                .downloadMediaMessage(
                        message,
                        new IRongCallback.IDownloadMediaMessageCallback() {
                            @Override
                            public void onSuccess(Message message) {
                                // 进度事件
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDownloadMessage(
                                            new DownloadEvent(DownloadEvent.SUCCESS, message));
                                }
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                            }

                            @Override
                            public void onProgress(Message message, int progress) {
                                // 进度事件
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDownloadMessage(
                                            new DownloadEvent(
                                                    DownloadEvent.PROGRESS, message, progress));
                                }
                                if (callback != null) {
                                    callback.onProgress(message, progress);
                                }
                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode code) {
                                message = message == null ? new Message() : message;
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDownloadMessage(
                                            new DownloadEvent(DownloadEvent.ERROR, message, code));
                                }
                                if (callback != null) {
                                    callback.onError(message, code);
                                }
                            }

                            @Override
                            public void onCanceled(Message message) {
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDownloadMessage(
                                            new DownloadEvent(DownloadEvent.CANCEL, message));
                                }
                                if (callback != null) {
                                    callback.onCanceled(message);
                                }
                            }
                        });
    }

    /** 注销当前登录，执行该方法后不会再收到 push 消息。 */
    public void logout() {
        RongIMClient.getInstance().logout();
        RongExtensionManager.getInstance().disconnect();
    }

    /**
     * 设置接收消息时的拦截器
     *
     * @param messageInterceptor 拦截器
     */
    public void setMessageInterceptor(MessageInterceptor messageInterceptor) {
        mMessageInterceptor = messageInterceptor;
    }

    /**
     * 判断是否支持断点续传。
     *
     * @param url 文件 Url
     * @param callback 回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void supportResumeBrokenTransfer(
            String url, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .supportResumeBrokenTransfer(
                        url,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                if (callback != null) {
                                    callback.onSuccess(aBoolean);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    /**
     * 暂停下载多媒体文件
     *
     * @param message 包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 暂停下载多媒体文件时的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void pauseDownloadMediaMessage(
            final Message message, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .pauseDownloadMediaMessage(
                        message,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDownloadMessage(
                                            new DownloadEvent(DownloadEvent.PAUSE, message));
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                            }
                        });
    }

    /**
     * 通知会话页面刷新某条消息
     *
     * @param message
     */
    public void refreshMessage(Message message) {
        for (MessageEventListener item : mMessageEventListeners) {
            item.onRefreshEvent(new RefreshEvent(message));
        }
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * 发送地理位置消息。并同时更新界面。
     *
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为 {@link LocationMessage}, 否则返回失败。
     *
     * <p>其中的缩略图地址 scheme 只支持 file:// 和 http:// 其他暂不支持。
     *
     * @param message 消息实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param sendMessageCallback 发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     *     该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void sendLocationMessage(
            Message message,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMessageCallback sendMessageCallback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance()
                .sendLocationMessage(
                        message,
                        pushContent,
                        pushData,
                        new IRongCallback.ISendMessageCallback() {
                            @Override
                            public void onAttached(Message message) {
                                if (sendMessageCallback != null) {
                                    sendMessageCallback.onAttached(message);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.ATTACH, message));
                                }
                            }

                            @Override
                            public void onSuccess(Message message) {
                                filterSentMessage(message, null, null);
                                if (sendMessageCallback != null) {
                                    sendMessageCallback.onSuccess(message);
                                }
                                if (mMessageInterceptor != null
                                        && mMessageInterceptor.interceptOnSentMessage(message)) {
                                    RLog.d(TAG, "message has been intercepted.");
                                    return;
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.SUCCESS, message));
                                }
                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                                filterSentMessage(
                                        message,
                                        errorCode,
                                        new FilterSentListener() {
                                            @Override
                                            public void onComplete() {
                                                // do nothing
                                            }
                                        });
                                if (sendMessageCallback != null) {
                                    sendMessageCallback.onError(message, errorCode);
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onSendMessage(new SendEvent(SendEvent.ERROR, message));
                                }
                            }
                        });
    }

    /**
     * 根据消息 Message 设置消息状态，回调方式获取设置是否成功。
     *
     * @param message 消息实体。要设置的发送状态包含在 message 中
     * @param callback 是否设置成功的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void setMessageSentStatus(
            final Message message, final RongIMClient.ResultCallback<Boolean> callback) {
        if (message == null || message.getMessageId() <= 0 || message.getSentStatus() == null) {
            RLog.e(TAG, "setMessageSentStatus message is null or messageId <= 0");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        RongIMClient.getInstance()
                .setMessageSentStatus(
                        message,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (callback != null) callback.onSuccess(bool);

                                if (bool) {
                                    refreshMessage(message);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        });
    }

    /**
     * 发送消息之前的内部逻辑处理。
     *
     * @param message 待发送的消息
     */
    public void handleBeforeSend(Message message) {
        if (RongUserInfoManager.getInstance().getUserInfoAttachedState()
                && message.getContent() != null
                && message.getContent().getUserInfo() == null) {
            UserInfo userInfo = RongUserInfoManager.getInstance().getCurrentUserInfo();
            if (userInfo != null) {
                message.getContent().setUserInfo(userInfo);
            }
        }
    }

    /**
     * 发送消息结果的处理
     *
     * @param message
     * @param errorCode
     */
    private void filterSentMessage(
            Message message, RongIMClient.ErrorCode errorCode, final FilterSentListener listener) {
        // 排除空指针问题
        if (message == null) {
            RLog.e(TAG, "filterSentMessage message is null");
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }
        if (errorCode != null
                && errorCode.getValue()
                        != IRongCoreEnum.CoreErrorCode.RC_MSG_REPLACED_SENSITIVE_WORD.getValue()) {
            if (errorCode.getValue() == IRongCoreEnum.CoreErrorCode.NOT_IN_DISCUSSION.getValue()
                    || errorCode.getValue() == IRongCoreEnum.CoreErrorCode.NOT_IN_GROUP.getValue()
                    || errorCode.getValue()
                            == IRongCoreEnum.CoreErrorCode.NOT_IN_CHATROOM.getValue()
                    || errorCode.getValue()
                            == IRongCoreEnum.CoreErrorCode.REJECTED_BY_BLACKLIST.getValue()
                    || errorCode.getValue()
                            == IRongCoreEnum.CoreErrorCode.FORBIDDEN_IN_GROUP.getValue()
                    || errorCode.getValue()
                            == IRongCoreEnum.CoreErrorCode.FORBIDDEN_IN_CHATROOM.getValue()
                    || errorCode.getValue()
                            == IRongCoreEnum.CoreErrorCode.KICKED_FROM_CHATROOM.getValue()) {

                if (message.getContent() instanceof ReadReceiptMessage) {
                    return;
                }
                InformationNotificationMessage informationMessage = null;
                if (errorCode.getValue() == RongIMClient.ErrorCode.NOT_IN_DISCUSSION.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_not_in_discussion));
                } else if (errorCode.getValue()
                        == IRongCoreEnum.CoreErrorCode.NOT_IN_GROUP.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_not_in_group));
                } else if (errorCode.getValue()
                        == IRongCoreEnum.CoreErrorCode.NOT_IN_CHATROOM.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_not_in_chatroom));
                } else if (errorCode.getValue()
                        == IRongCoreEnum.CoreErrorCode.REJECTED_BY_BLACKLIST.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_rejected_by_blacklist_prompt));
                } else if (errorCode.getValue()
                        == IRongCoreEnum.CoreErrorCode.FORBIDDEN_IN_GROUP.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_forbidden_to_talk));
                } else if (errorCode.getValue()
                        == IRongCoreEnum.CoreErrorCode.FORBIDDEN_IN_CHATROOM.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_forbidden_in_chatroom));
                } else if (errorCode.getValue()
                        == IRongCoreEnum.CoreErrorCode.KICKED_FROM_CHATROOM.getValue()) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_kicked_from_chatroom));
                }

                Message.ReceivedStatus receivedStatus =
                        new io.rong.imlib.model.Message.ReceivedStatus(0);
                insertIncomingMessage(
                        message.getConversationType(),
                        message.getTargetId(),
                        message.getSenderUserId(),
                        receivedStatus,
                        informationMessage,
                        null);
            }
        }
        MessageContent content = message.getContent();
        if (content == null) {
            RLog.e(TAG, "filterSentMessage content is null");
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }
        if (errorCode != null
                && errorCode.code == IRongCoreEnum.CoreErrorCode.RC_VIDEO_COMPRESS_FAILED.code
                && message.getContent() instanceof SightMessage) {
            // 压缩失败不走重发队列，需要用户自己重试
            if (listener != null) {
                listener.onComplete();
            }
            return;
        }
        MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
        if (RongConfigCenter.conversationConfig().rc_enable_resend_message
                && tag != null
                && ((tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED
                        || content instanceof ReadReceiptMessage)) {
            // 发送失败的消息存入重发列表
            ResendManager.getInstance()
                    .addResendMessage(
                            message,
                            errorCode,
                            new ResendManager.AddResendMessageCallBack() {
                                @Override
                                public void onComplete(
                                        Message message, RongIMClient.ErrorCode errorCode) {
                                    if (listener != null) {
                                        ExecutorHelper.getInstance()
                                                .mainThread()
                                                .execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                listener.onComplete();
                                                            }
                                                        });
                                    }
                                }
                            });
        } else {
            if (listener != null) {
                listener.onComplete();
            }
        }
    }

    /**
     * 取消发送多媒体文件。
     *
     * @param message 包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消发送多媒体文件时的回调。该回调在主线程中执行，请避免在回调中执行耗时操作，防止 SDK 线程阻塞。
     */
    public void cancelSendMediaMessage(
            final Message message, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .cancelSendMediaMessage(
                        message,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                                for (RongIMClient.ResultCallback<Message> listener :
                                        mCancelSendMediaMessageListeners) {
                                    listener.onSuccess(message);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                                for (RongIMClient.ResultCallback<Message> listener :
                                        mCancelSendMediaMessageListeners) {
                                    listener.onError(errorCode);
                                }
                            }
                        });
    }

    /**
     * 设置消息接收状态
     *
     * @param message 消息实体
     * @param receivedStatus 接收状态
     * @param callback 设置消息接收状态的回调
     * @since 5.12.2
     */
    @Deprecated
    public void updateGroupInfo(
            @NonNull GroupInfo groupInfo, IRongCoreCallback.OperationCallbackEx<String> callback) {
        RongCoreClient.getInstance()
                .updateGroupInfo(
                        groupInfo,
                        new IRongCoreCallback.OperationCallbackEx<String>() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateGroupInfo(
                                            groupInfo, IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, String errorData) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateGroupInfo(groupInfo, errorCode);
                                }
                                if (callback != null) {
                                    callback.onError(errorCode, errorData);
                                }
                            }
                        });
    }

    /**
     * 设置群组资料
     *
     * @param groupInfo 群信息
     * @param callback 设置消息接收状态的回调
     * @since 5.16.0
     */
    public void updateGroupInfo(
            @NonNull GroupInfo groupInfo, IRongCoreCallback.ExamineOperationCallback callback) {
        RongCoreClient.getInstance()
                .updateGroupInfo(
                        groupInfo,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateGroupInfo(
                                            groupInfo, IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, List<String> errorKeys) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateGroupInfo(groupInfo, errorCode);
                                }
                                if (callback != null) {
                                    callback.onError(errorCode, errorKeys);
                                }
                            }
                        });
    }

    /**
     * 设置群组信息
     *
     * @param groupId 群组 Id
     * @param groupName 群组名称
     * @param portrait 群组头像
     * @param callback 设置群组信息回调
     * @since 5.12.2
     */
    @Deprecated
    public void setGroupMemberInfo(
            String groupId,
            String userId,
            String nickname,
            String extra,
            IRongCoreCallback.OperationCallback callback) {
        RongCoreClient.getInstance()
                .setGroupMemberInfo(
                        groupId,
                        userId,
                        nickname,
                        extra,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setGroupMemberInfo(
                                            groupId,
                                            userId,
                                            nickname,
                                            extra,
                                            IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setGroupMemberInfo(
                                            groupId, userId, nickname, extra, coreErrorCode);
                                }
                                if (callback != null) {
                                    callback.onError(coreErrorCode);
                                }
                            }
                        });
    }

    /**
     * 设置群组成员信息
     *
     * @param groupId 群组 Id
     * @param userId 成员 Id
     * @param nickname 名称
     * @param extra 附加信息
     * @param callback 设置群组信息回调
     * @since 5.16.0
     */
    public void setGroupMemberInfo(
            String groupId,
            String userId,
            String nickname,
            String extra,
            IRongCoreCallback.ExamineOperationCallback callback) {
        RongCoreClient.getInstance()
                .setGroupMemberInfo(
                        groupId,
                        userId,
                        nickname,
                        extra,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setGroupMemberInfo(
                                            groupId,
                                            userId,
                                            nickname,
                                            extra,
                                            IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                    List<String> errorKeys) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setGroupMemberInfo(
                                            groupId, userId, nickname, extra, coreErrorCode);
                                }
                                if (callback != null) {
                                    callback.onError(coreErrorCode, errorKeys);
                                }
                            }
                        });
    }

    /**
     * 设置群组备注
     *
     * @param groupId 群组 Id
     * @param remark 备注
     * @param callback 设置群组备注回调
     * @since 5.12.2
     */
    public void setGroupRemark(
            String groupId, final String remark, IRongCoreCallback.OperationCallback callback) {
        RongCoreClient.getInstance()
                .setGroupRemark(
                        groupId,
                        remark,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setGroupRemark(
                                            groupId, remark, IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setGroupRemark(groupId, remark, coreErrorCode);
                                }
                                if (callback != null) {
                                    callback.onError(coreErrorCode);
                                }
                            }
                        });
    }

    /**
     * 更新我的用户信息
     *
     * @param userProfile 用户信息
     * @param callback 更新用户信息回调
     * @since 5.12.2
     */
    @Deprecated
    public void updateMyUserProfile(
            UserProfile userProfile, IRongCoreCallback.UpdateUserProfileCallback callback) {
        RongCoreClient.getInstance()
                .updateMyUserProfile(
                        userProfile,
                        new IRongCoreCallback.UpdateUserProfileCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateMyUserProfile(
                                            userProfile, IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(int errorCode, String errorData) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateMyUserProfile(
                                            userProfile,
                                            IRongCoreEnum.CoreErrorCode.valueOf(errorCode));
                                }
                                if (callback != null) {
                                    callback.onError(errorCode, errorData);
                                }
                            }
                        });
    }

    /**
     * 更新我的用户信息
     *
     * @param userProfile 用户信息
     * @param callback 更新用户信息回调
     * @since 5.16.0
     */
    public void updateMyUserProfile(
            UserProfile userProfile, IRongCoreCallback.UpdateUserProfileEnhancedCallback callback) {
        RongCoreClient.getInstance()
                .updateMyUserProfile(
                        userProfile,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateMyUserProfile(
                                            userProfile, IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, List<String> errorKeys) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.updateMyUserProfile(userProfile, errorCode);
                                }
                                if (callback != null) {
                                    callback.onError(errorCode, errorKeys);
                                }
                            }
                        });
    }

    /**
     * 设置好友信息
     *
     * @param userId 用户 Id
     * @param remark 备注
     * @param extProfile 扩展信息
     * @param callback 设置好友信息回调
     * @since 5.12.2
     */
    @Deprecated
    public void setFriendInfo(
            final String userId,
            final String remark,
            final Map<String, String> extProfile,
            IRongCoreCallback.OperationCallback callback) {
        RongCoreClient.getInstance()
                .setFriendInfo(
                        userId,
                        remark,
                        extProfile,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setFriendInfo(
                                            userId,
                                            remark,
                                            extProfile,
                                            IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setFriendInfo(
                                            userId, remark, extProfile, coreErrorCode);
                                }
                                if (callback != null) {
                                    callback.onError(coreErrorCode);
                                }
                            }
                        });
    }

    /**
     * 设置好友信息
     *
     * @param userId 用户 Id
     * @param remark 备注
     * @param extProfile 扩展信息
     * @param callback 设置好友信息回调
     * @since 5.16.0
     */
    public void setFriendInfo(
            final String userId,
            final String remark,
            final Map<String, String> extProfile,
            IRongCoreCallback.ExamineOperationCallback callback) {
        RongCoreClient.getInstance()
                .setFriendInfo(
                        userId,
                        remark,
                        extProfile,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setFriendInfo(
                                            userId,
                                            remark,
                                            extProfile,
                                            IRongCoreEnum.CoreErrorCode.SUCCESS);
                                }
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                    List<String> errorKeys) {
                                for (OnGroupAndUserEventListener listener :
                                        onGroupAndUserEventListeners) {
                                    listener.setFriendInfo(
                                            userId, remark, extProfile, coreErrorCode);
                                }
                                if (callback != null) {
                                    callback.onError(coreErrorCode, errorKeys);
                                }
                            }
                        });
    }

    public void changeMessageReceivedStatus(
            int messageId,
            Conversation.ConversationType conversationType,
            String targetId,
            Message.ReceivedStatus status) {
        for (ConversationEventListener conversationEventListener : mConversationEventListener) {
            conversationEventListener.onMessageReceivedStatusChange(
                    messageId, conversationType, targetId, status);
        }
    }

    public void addOnReceiveMessageListener(RongIMClient.OnReceiveMessageWrapperListener listener) {
        if (listener == null || mOnReceiveMessageObserverList.contains(listener)) {
            return;
        }
        mOnReceiveMessageObserverList.add(listener);
    }

    public void removeOnReceiveMessageListener(
            RongIMClient.OnReceiveMessageWrapperListener listener) {
        if (listener != null) {
            mOnReceiveMessageObserverList.remove(listener);
        }
    }

    public void addAsyncOnReceiveMessageListener(
            RongIMClient.OnReceiveMessageWrapperListener listener) {
        if (listener == null || mAsyncOnReceiveMessageObserverList.contains(listener)) {
            return;
        }
        mAsyncOnReceiveMessageObserverList.add(listener);
    }

    public void removeAsyncOnReceiveMessageListener(
            RongIMClient.OnReceiveMessageWrapperListener listener) {
        if (listener != null) {
            mAsyncOnReceiveMessageObserverList.remove(listener);
        }
    }

    public void addSyncConversationReadStatusListener(
            RongIMClient.SyncConversationReadStatusListener listener) {
        if (listener == null || mSyncConversationReadStatusListeners.contains(listener)) {
            return;
        }
        mSyncConversationReadStatusListeners.add(listener);
    }

    public void removeSyncConversationReadStatusListeners(
            RongIMClient.SyncConversationReadStatusListener listener) {
        mSyncConversationReadStatusListeners.remove(listener);
    }

    public void addConnectStatusListener(RongIMClient.ConnectCallback callback) {
        if (callback != null && mConnectStatusListener.contains(callback)) {
            return;
        }
        mConnectStatusListener.add(callback);
    }

    public void removeConnectStatusListener(RongIMClient.ConnectCallback callback) {
        mConnectStatusListener.remove(callback);
    }

    /**
     * 设置连接状态变化的监听器。
     *
     * <p><strong> 当回调状态为{@link
     * RongIMClient.ConnectionStatusListener.ConnectionStatus#TOKEN_INCORRECT}, 需要获取正确的token,
     * 并主动调用{@link RongIM#connect(String, int, RongIMClient.ConnectCallback)} </strong>
     *
     * @param listener 连接状态变化的监听器。
     */
    public void addConnectionStatusListener(RongIMClient.ConnectionStatusListener listener) {
        if (listener == null || mConnectionStatusObserverList.contains(listener)) {
            return;
        }
        mConnectionStatusObserverList.add(listener);
    }

    /**
     * 移除连接状态监听器
     *
     * @param listener 连接状态变化监听器
     */
    public void removeConnectionStatusListener(RongIMClient.ConnectionStatusListener listener) {
        mConnectionStatusObserverList.remove(listener);
    }

    public void addConversationEventListener(ConversationEventListener listener) {
        if (!mConversationEventListener.contains(listener)) {
            mConversationEventListener.add(listener);
        }
    }

    public void removeConversationEventListener(ConversationEventListener listener) {
        mConversationEventListener.remove(listener);
    }

    public void addMessageEventListener(MessageEventListener listener) {
        if (listener == null || mMessageEventListeners.contains(listener)) {
            return;
        }
        mMessageEventListeners.add(listener);
    }

    public void removeMessageEventListener(MessageEventListener listener) {
        mMessageEventListeners.remove(listener);
    }

    /**
     * 添加群组和用户信息事件监听
     *
     * @param listener 群组和用户信息事件监听
     * @since 5.12.2
     */
    public void addOnGroupAndUserEventListener(OnGroupAndUserEventListener listener) {
        if (listener == null || onGroupAndUserEventListeners.contains(listener)) {
            return;
        }
        onGroupAndUserEventListeners.add(listener);
    }

    /**
     * 移除群组和用户信息事件监听
     *
     * @param listener 群组和用户信息事件监听
     * @since 5.12.2
     */
    public void removeOnGroupAndUserEventListener(OnGroupAndUserEventListener listener) {
        onGroupAndUserEventListeners.remove(listener);
    }

    public void addMediaListener(String uid, IRongCallback.IDownloadMediaFileCallback listener) {
        if (listener == null) {
            return;
        }
        mMediaListeners.put(uid, listener);
    }

    public void removeMediaListener(String uid) {
        mMediaListeners.remove(uid);
    }

    public void addConversationStatusListener(RongIMClient.ConversationStatusListener listener) {
        if (listener == null || mConversationStatusObserverList.contains(listener)) {
            return;
        }
        mConversationStatusObserverList.add(listener);
    }

    public void removeConversationStatusListener(RongIMClient.ConversationStatusListener listener) {
        mConversationStatusObserverList.remove(listener);
    }

    public void addOnRecallMessageListener(RongIMClient.OnRecallMessageListener listener) {
        if (listener == null || mOnRecallMessageObserverList.contains(listener)) {
            return;
        }
        mOnRecallMessageObserverList.add(listener);
    }

    public void removeOnRecallMessageListener(RongIMClient.OnRecallMessageListener listener) {
        mOnRecallMessageObserverList.remove(listener);
    }

    public void addReadReceiptListener(RongIMClient.ReadReceiptListener listener) {
        if (listener == null || mReadReceiptObserverList.contains(listener)) {
            return;
        }
        mReadReceiptObserverList.add(listener);
    }

    public void removeReadReceiptListener(RongIMClient.ReadReceiptListener listener) {
        mReadReceiptObserverList.remove(listener);
    }

    public void addTypingStatusListener(RongIMClient.TypingStatusListener listener) {
        if (listener == null || mTypingStatusListeners.contains(listener)) {
            return;
        }
        mTypingStatusListeners.add(listener);
    }

    public void removeTypingStatusListener(RongIMClient.TypingStatusListener listener) {
        mTypingStatusListeners.remove(listener);
    }

    public void addCancelSendMediaMessageListener(RongIMClient.ResultCallback<Message> listener) {
        if (listener == null || mCancelSendMediaMessageListeners.contains(listener)) {
            return;
        }
        mCancelSendMediaMessageListeners.add(listener);
    }

    public void removeCancelSendMediaMessageListener(
            RongIMClient.ResultCallback<Message> listener) {
        mCancelSendMediaMessageListeners.remove(listener);
    }

    /**
     * 添加群组事件监听器
     *
     * @param listener 群组事件监听器
     * @since 5.12.0
     */
    public void addGroupEventListener(@NonNull GroupEventListener listener) {
        mGroupEventListeners.add(listener);
    }

    /**
     * 移除群组事件监听器
     *
     * @param listener 群组事件监听器
     * @since 5.12.0
     */
    public void removeGroupEventListener(@NonNull GroupEventListener listener) {
        mGroupEventListeners.remove(listener);
    }

    /**
     * 添加订阅事件监听器
     *
     * @param listener 订阅事件监听器
     * @since 5.12.0
     */
    public void addSubscribeEventListener(@NonNull OnSubscribeEventListener listener) {
        mSubscribeEventListeners.add(listener);
    }

    /**
     * 添加好友事件监听器
     *
     * @param listener 好友事件监听器
     * @since 5.12.0
     */
    public void addFriendEventListener(FriendEventListener listener) {
        if (listener != null) {
            friendEventListeners.add(listener);
        }
    }

    /**
     * 移除好友事件监听器
     *
     * @param listener 好友事件监听器
     * @since 5.12.0
     */
    public void removeFriendEventListener(FriendEventListener listener) {
        if (listener != null) {
            friendEventListeners.remove(listener);
        }
    }

    /**
     * 移除订阅事件监听器
     *
     * @param listener 订阅事件监听器
     * @since 5.12.0
     */
    public void removeSubscribeEventListener(@NonNull OnSubscribeEventListener listener) {
        mSubscribeEventListeners.remove(listener);
    }

    /** 语音消息类型 */
    public enum VoiceMessageType {
        /** 普通音质语音消息 */
        Ordinary,
        /** 高音质语音消息 */
        HighQuality
    }

    public interface FilterSentListener {
        void onComplete();
    }

    private static class SingletonHolder {
        static IMCenter sInstance = new IMCenter();
    }
}
