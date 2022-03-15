package io.rong.imkit;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import io.rong.common.RLog;
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
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.language.RongConfigurationManager;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.SendMessageOption;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.typingmessage.TypingStatus;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class IMCenter {
    private static final String TAG = IMCenter.class.getSimpleName();
    private Context mContext;
    private MessageInterceptor mMessageInterceptor;
    private List<RongIMClient.ConnectionStatusListener> mConnectionStatusObserverList =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.OnReceiveMessageWrapperListener> mOnReceiveMessageObserverList =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.ConversationStatusListener> mConversationStatusObserverList =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.ReadReceiptListener> mReadReceiptObserverList =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.OnRecallMessageListener> mOnRecallMessageObserverList =
            new CopyOnWriteArrayList<>();

    private List<MessageEventListener> mMessageEventListeners = new CopyOnWriteArrayList<>();
    private List<ConversationEventListener> mConversationEventListener =
            new CopyOnWriteArrayList<>();

    private List<RongIMClient.ConnectCallback> mConnectStatusListener =
            new CopyOnWriteArrayList<>();
    private List<RongIMClient.SyncConversationReadStatusListener>
            mSyncConversationReadStatusListeners = new CopyOnWriteArrayList<>();
    private List<RongIMClient.TypingStatusListener> mTypingStatusListeners =
            new CopyOnWriteArrayList<>();

    private IMCenter() {}

    private static class SingletonHolder {
        static IMCenter sInstance = new IMCenter();
    }

    public static IMCenter getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * /~chinese 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param application 应用上下文。
     * @param appKey 在融云开发者后台注册的应用 AppKey。
     * @param isEnablePush 是否使用推送功能
     */

    /**
     * /~english Initialize the SDK, which only needs to be called once in the whole application
     * globally.
     *
     * @param application Application context.
     * @param appKey Application AppKey registered with RongCloud developer backend.
     * @param isEnablePush Whether to use push function
     */
    public static void init(Application application, String appKey, boolean isEnablePush) {
        String current = io.rong.common.SystemUtils.getCurrentProcessName(application);
        String mainProcessName = application.getPackageName();
        if (!mainProcessName.equals(current)) {
            RLog.w(TAG, "Init. Current process : " + current);
            return;
        }
        SingletonHolder.sInstance.mContext = application.getApplicationContext();
        RongConfigCenter.syncFromXml(application);
        RongIMClient.init(application.getApplicationContext(), appKey, isEnablePush);
        RongUserInfoManager.getInstance().initAndUpdateUserDataBase(application);
        RongExtensionManager.init(application.getApplicationContext(), appKey);
        HQVoiceMsgDownloadManager.getInstance().init(application);
        RongNotificationManager.getInstance().init(application);
        RongConfigurationManager.init(application);
        RongIMClient.setOnReceiveMessageListener(
                SingletonHolder.sInstance.mOnReceiveMessageListener);
        RongIMClient.setConnectionStatusListener(
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
        RongIMClient.registerMessageType(CombineMessage.class);
    }

    public void connect(
            String token, int timeLimit, final RongIMClient.ConnectCallback connectCallback) {
        RongIMClient.connect(
                token,
                timeLimit,
                new RongIMClient.ConnectCallback() {
                    @Override
                    public void onSuccess(String s) {
                        if (connectCallback != null) {
                            connectCallback.onSuccess(s);
                        }
                        if (!TextUtils.isEmpty(s)) {
                            RongNotificationManager.getInstance().getNotificationQuietHours(null);
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
                        for (RongIMClient.ConnectCallback callback : mConnectStatusListener) {
                            callback.onDatabaseOpened(databaseOpenStatus);
                        }
                    }
                });
    }

    /**
     * /~chinese 删除某个会话中的所有消息。
     *
     * <p><strong>注意：不支持聊天室！</strong>
     *
     * @param conversationType 会话类型，不支持聊天室。参考 {@link Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param callback 清空是否成功的回调。
     * @group 消息操作
     */

    /**
     * /~english Delete all messages in a conversation.
     *
     * <p><strong>Note: The chat rooms are not supported!</strong>
     *
     * @param conversationType Type of conversation, which does not support chat rooms. Refer to
     *     Conversation.ConversationType.
     * @param targetId Conversation id. Depending on different conversationType, it may be user id,
     *     discussion group id or group id.
     * @param callback Callback for whether clearing is successful or not.
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
     * /~chinese 根据会话类型，发送消息。
     *
     * <p>通过 {@link io.rong.imlib.IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条。
     *
     * @param type 会话类型。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param content 消息内容，例如 {@link TextMessage}, {@link ImageMessage}。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 SDK
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调。参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     */

    /**
     * /~english Send a message.
     *
     * <p>The status and body of the sent message are called back by the method in
     * IRongCoreCallback.ISendMessageCallback. <strong>Note: No more than 5 messages can be sent in
     * 1 second.</strong>
     *
     * @param type Conversation type
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param content Message content, e.g. TextMessage, ImageMessage.
     * @param pushContent This field is displayed in the notification bar when a remote push message
     *     is sent. If you are sending a custom message, this field must be completed, otherwise you
     *     will not be able to receive a remote push message. If you send the default message type
     *     in sdk such as RC:TxtMsg, RC:VcMsg, RC:ImgMsg, this field does not need to be filled in
     *     and has been specified by default.
     * @param pushData Push additional information remotely. If this field is set, users can get it
     *     by using io.rong.push.notification.PushNotificationMessage#getPushData() when they
     *     receive a push message.
     * @param callback Callback for sending messages, refer to {@link
     *     io.rong.imlib.IRongCallback.ISendMessageCallback}.
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

    public void sendMessage(
            Message message,
            String pushContent,
            String pushData,
            IRongCallback.ISendMessageCallback callback) {
        sendMessage(message, pushContent, pushData, null, callback);
    }

    /**
     * /~chinese 发送消息。
     *
     * <p>通过 {@link io.rong.imlib.IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条。
     *
     * @param message 要发送的消息体。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到远程推送消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param option 发送消息附加选项，目前仅支持设置 isVoIPPush，如果对端设备是 iOS，设置 isVoIPPush 为 True，会走 VoIP 通道推送 Push。
     * @param callback 发送消息的回调，参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     */

    /**
     * /~english Send a message.
     *
     * <p>The status and body of the sent message are called back by the method in
     * IRongCoreCallback.ISendMessageCallback. <strong>Note: No more than 5 messages can be sent in
     * 1 second.</strong>
     *
     * @param message Message body to be sent.
     * @param pushContent This field is displayed in the notification bar when a remote push message
     *     is sent. If you are sending a custom message, this field must be completed, otherwise you
     *     will not be able to receive a remote push message. If you send the default message type
     *     in sdk such as RC:TxtMsg, RC:VcMsg, RC:ImgMsg, this field does not need to be filled in
     *     and has been specified by default.
     * @param pushData Push additional information remotely. If this field is set, users can get it
     *     by using io.rong.push.notification.PushNotificationMessage#getPushData() when they
     *     receive a push message.
     * @param option Additional option for sending messages. Currently you can only set isVoIPPush
     *     If the peer device is iOS, isVoIPPush is set as True, Push will be pushed via VoIP
     *     channel.
     * @param callback Callback for sending messages, refer to {@link
     *     io.rong.imlib.IRongCallback.ISendMessageCallback}.
     */
    public void sendMessage(
            Message message,
            String pushContent,
            final String pushData,
            SendMessageOption option,
            final IRongCallback.ISendMessageCallback callback) {
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
     * /~chinese 发送某个会话中的消息阅读回执。
     *
     * <p>使用 IMLib 可以注册监听 {@link #setReadReceiptListener}；使用 IMkit 直接设置 rc_config.xml 中 {@code
     * rc_read_receipt} 为 true。
     *
     * @param conversationType 会话类型（只适用 PRIVATE 和 ENCRYPTED 类型）
     * @param targetId 会话 id
     * @param timestamp 会话中已读的最后一条消息的发送时间戳 {@link Message#getSentTime()}
     * @param callback 发送已读回执消息的回调
     * @group 高级功能
     */

    /**
     * /~english Send a message reading receipt in a conversation.
     *
     * <p>The IMLib can be used to register and listen to
     * setReadReceiptListener(io.rong.imlib.IRongCoreListener.ReadReceiptListener). The IMkit is
     * used to directly set rc_read_receipt in rc_config.xml as true.
     *
     * @param conversationType Type of conversation (It is only applicable to PRIVATE and ENCRYPTED
     *     types)
     * @param targetId Conversation Id
     * @param timestamp The sending timestamp of the last message read in this conversation {@link
     *     Message#getSentTime()}
     * @param callback Callback for sending read receipt messages
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
                                if (callback != null) {
                                    callback.onSuccess(message);
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(conversationType, targetId);
                                }
                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(message, errorCode);
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(conversationType, targetId);
                                }
                            }
                        });
    }

    /**
     * /~chinese 同步会话阅读状态。
     *
     * @param type 会话类型
     * @param targetId 会话 id
     * @param timestamp 会话中已读的最后一条消息的发送时间戳 {@link Message#getSentTime()}
     * @param callback 回调函数
     * @group 高级功能
     */

    /**
     * /~english Synchronize conversation reading status.
     *
     * @param type Conversation type
     * @param targetId Conversation Id
     * @param timestamp The sending timestamp of the last message read in this conversation
     *     Message.getSentTime()
     * @param callback Callback function
     */
    public void syncConversationReadStatus(
            final Conversation.ConversationType type,
            final String targetId,
            long timestamp,
            final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .syncConversationReadStatus(
                        type,
                        targetId,
                        timestamp,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(type, targetId);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(type, targetId);
                                }
                            }
                        });
    }

    /**
     * /~chinese 设置会话界面操作的监听器。
     *
     * @param listener 会话界面操作的监听器。
     */

    /**
     * /~english Set the listener for conversation interface operations.
     *
     * @param listener Listeners for conversation interface operations.
     */
    public static void setConversationClickListener(ConversationClickListener listener) {
        RongConfigCenter.conversationConfig().setConversationClickListener(listener);
    }

    /**
     * /~chinese 设置会话列表界面操作的监听器。
     *
     * @param listener 会话列表界面操作的监听器。
     */

    /**
     * /~english Set the listener for the conversation list interface operation.
     *
     * @param listener Set the listener for the conversation list interface operation.
     */
    public static void setConversationListBehaviorListener(
            ConversationListBehaviorListener listener) {
        RongConfigCenter.conversationListConfig().setBehaviorListener(listener);
    }

    /**
     * /~chinese 发送多媒体消息。
     *
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为多媒体消息。 例如：{@link ImageMessage} 、{@link
     * FileMessage} 或其他继承自 {@link MediaMessageContent} 的消息。
     *
     * @param message 发送消息的实体。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 sdk
     *     中默认的消息类型，例如：RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到远程推送消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调 {@link io.rong.imlib.RongIMClient.SendMediaMessageCallback}。
     * @group 消息操作
     */

    /**
     * /~english Send a multimedia message.
     *
     * <p>The Message message entity is constructed before sending and the contents in the message
     * entity must be multimedia messages. E.g. ImageMessage FileMessage or other message inherited
     * from MediaMessageContent.
     *
     * @param message The entity that sent the message.
     * @param pushContent This field is displayed in the notification bar when a remote push message
     *     is sent. If you are sending a custom message, this field must be completed, otherwise you
     *     will not be able to receive a remote push message. If you send the default message type
     *     in sdk such as RC:TxtMsg, RC:VcMsg, RC:ImgMsg, this field does not need to be filled in
     *     and has been specified by default.
     * @param pushData Push additional information remotely. If this field is set, users can get it
     *     by using io.rong.push.notification.PushNotificationMessage#getPushData() when they
     *     receive a push message.
     * @param callback Callback io.rong.imlib.RongCoreClient.SendMediaMessageCallback for sending
     *     messages.
     */
    public void sendMediaMessage(
            Message message,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMediaMessageCallback callback) {
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
                        message, pushContent, pushData, sendMediaMessageCallbackWithUploader);
    }

    /**
     * /~chinese 发送定向消息。
     *
     * <p>此方法用于在群组和讨论组中发送消息给其中的部分用户，其它用户不会收到这条消息。 通过 {@link
     * io.rong.imlib.IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。 <br>
     * 此方法只能发送非多媒体消息，多媒体消息如{@link ImageMessage} {@link FileMessage} 或其他继承自 {@link
     * MediaMessageContent} 的消息须调用 {@link #sendDirectionalMediaMessage(Message, String[], String,
     * String, IRongCallback.ISendMediaMessageCallback)} <br>
     * 如果您使用 IMLib，可以使用此方法发送定向消息；如果您使用 IMKit，请使用 RongIM 中的同名方法发送定向消息，否则不会自动更新 UI。
     *
     * @param type 会话类型。
     * @param targetId 会话 id。只能是讨论组 id 或群组 id。
     * @param content 消息内容，例如 {@link TextMessage}, {@link ImageMessage}。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 SDK
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到远程推送消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param userIds 讨论组或群组会话中将会接收到此消息的用户列表。
     * @param callback 发送消息的回调，参考 {@link io.rong.imlib.IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     */

    /**
     * /~english Send a directed message.
     *
     * <p>This method is used to send messages to some of the users in groups and discussion groups,
     * and other users will not receive this message. The status and body of the sent message are
     * called back by the method in IRongCoreCallback.ISendMessageCallback. This method can only be
     * used to send non-multimedia messages. For multimedia messages such as ImageMessage
     * FileMessage or other messages inherited from MediaMessageContent, you must call
     * sendDirectionalMediaMessage(Message, String[], String, String,
     * IRongCoreCallback.ISendMediaMessageCallback) . If you use IMLib, you can use this method to
     * send directed messages; and if you use IMKit, you can use the method of the same name in
     * RongIM to send directed messages, otherwise UI will not be updated automatically.
     *
     * @param type Conversation type
     * @param targetId Conversation id. It can only be a discussion group id or a group id.
     * @param content Message content, e.g. TextMessage, ImageMessage.
     * @param pushContent This field is displayed in the notification bar when a remote push message
     *     is sent. If you are sending a custom message, this field must be completed, otherwise you
     *     will not be able to receive a remote push message. If you send the default message type
     *     in SDK such as RC:TxtMsg, RC:VcMsg, RC:ImgMsg, this field does not need to be filled in
     *     and has been specified by default.
     * @param pushData Push additional information remotely. If this field is set, users can get it
     *     by using io.rong.push.notification.PushNotificationMessage#getPushData() when they
     *     receive a push message.
     * @param userIds A list of users in a discussion group or group conversation that will receive
     *     this message.
     * @param callback Callback for sending messages, refer to
     *     IRongCoreCallback.ISendMessageCallback.
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
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance()
                .sendDirectionalMessage(
                        type,
                        targetId,
                        content,
                        userIds,
                        pushContent,
                        pushData,
                        new IRongCallback.ISendMessageCallback() {
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
                            public void onError(
                                    final Message message, RongIMClient.ErrorCode errorCode) {
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
                                                                    SendEvent.ERROR, message));
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
     * /~chinese 发送定向多媒体消息。
     *
     * <p>向会话中特定的某些用户发送消息，会话中其他用户不会收到消息。<br>
     * 发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为多媒体消息。 例如：{@link ImageMessage} 、{@link
     * FileMessage} 或其他继承自 {@link MediaMessageContent} 的消息。
     *
     * @param message 发送消息的实体。
     * @param userIds 定向接收者 id 数组。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 SDK
     *     中默认的消息类型，例如: RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到远程推送消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调 {@link io.rong.imlib.RongIMClient.SendMediaMessageCallback}。
     * @group 消息操作
     */

    /**
     * /~english Send directed multimedia messages.
     *
     * <p>Messages are sent to specific users in the conversation, and other users in the
     * conversation will not receive the message. <br>
     * The Message message entity is constructed before sending and the contents in the message
     * entity must be multimedia messages. E.g. ImageMessage FileMessage or other message inherited
     * from MediaMessageContent.
     *
     * @param message The entity that sent the message.
     * @param userIds Array of directed recipient id.
     * @param pushContent This field is displayed in the notification bar when a remote push message
     *     is sent. If you are sending a custom message, this field must be completed, otherwise you
     *     will not be able to receive a remote push message. If you send the default message type
     *     in SDK, such as RC:TxtMsg, RC:VcMsg, RC:ImgMsg, this field does not need to be filled in
     *     and has been specified by default.
     * @param pushData Push additional information remotely. If this field is set, users can get it
     *     by using io.rong.push.notification.PushNotificationMessage#getPushData() when they
     *     receive a push message.
     * @param callback Callback {@link io.rong.imlib.RongIMClient.SendMediaMessageCallback} for
     *     sending messages.
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
     * /~chinese 删除指定时间戳之前的消息，可选择是否同时删除服务器端消息
     *
     * <p>此方法可从服务器端清除历史消息，<Strong>但是必须先开通历史消息云存储功能。</Strong> <br>
     * 根据会话类型和会话 id 清除某一会话指定时间戳之前的本地数据库消息（服务端历史消息）， 清除成功后只能从本地数据库（服务端）获取到该时间戳之后的历史消息。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 id。
     * @param recordTime 清除消息截止时间戳，【0 <= recordTime <= 当前会话最后一条消息的 sentTime,0 清除所有消息，其他值清除小于等于
     *     recordTime 的消息】。
     * @param cleanRemote 是否删除服务器端消息
     * @param callback 清除消息的回调。
     * @group 消息操作
     */

    /**
     * /~english Delete the message before the specified timestamp. You can choose whether to delete
     * the server-side message at the same time.
     *
     * <p>This method clears historical messages from the server, but you must first activate the
     * historical message cloud storage feature. The local database message (server history message)
     * before the specified timestamp of a conversation is cleared according to the conversation
     * type and conversation id. After successful cleaning, the historical message after the
     * timestamp can only be obtained from the local database (server).
     *
     * @param conversationType Conversation type
     * @param targetId Conversation id.
     * @param recordTime recordTime
     * @param cleanRemote Whether to delete server-side messages
     * @param callback Callback for clearing the message.
     */
    public void cleanHistoryMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final long recordTime,
            final boolean cleanRemote,
            final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .cleanHistoryMessages(
                        conversationType,
                        targetId,
                        recordTime,
                        cleanRemote,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) callback.onSuccess();
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedMessage(conversationType, targetId);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) callback.onError(errorCode);
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onOperationFailed(errorCode);
                                }
                            }
                        });
    }

    /**
     * /~chinese 清除某会话的消息未读状态
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback 清除是否成功的回调。
     */

    /**
     * /~english Clear the message unread status of a conversation
     *
     * @param conversationType Conversation type Incoming ConversationType.CHATROOM is not supported
     * @param targetId Target Id Depending on the conversationType, it may be user Id, discussion
     *     group Id and group Id
     * @param callback Callback for whether clearing is successful or not.
     */
    public void clearMessagesUnreadStatus(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .clearMessagesUnreadStatus(
                        conversationType,
                        targetId,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (callback != null) callback.onSuccess(bool);
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onClearedUnreadStatus(conversationType, targetId);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        });
    }

    /**
     * /~chinese 删除指定会话中的草稿信息。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param callback 是否清除成功的回调。
     * @group 会话
     */

    /**
     * /~english Delete draft information in a specified conversation.
     *
     * @param conversationType Conversation type
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param callback Callback for whether the clearing is successful.
     */
    public void clearTextMessageDraft(
            Conversation.ConversationType conversationType,
            String targetId,
            RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * /~chinese 保存会话草稿信息。
     *
     * @param conversationType 会话类型。参考 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param content 草稿的文字内容。
     * @param callback 是否保存成功的回调。
     * @group 会话
     */

    /**
     * /~english Save the draft information of a conversation.
     *
     * @param conversationType Conversation type Refer to {@link
     *     io.rong.imlib.model.Conversation.ConversationType} .
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param content The text of the draft.
     * @param callback Callback for whether saving is successful.
     */
    public void saveTextMessageDraft(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final String content,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .saveTextMessageDraft(
                        conversationType,
                        targetId,
                        content,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean value) {
                                if (callback != null) {
                                    callback.onSuccess(value);
                                }
                                if (value) {
                                    for (ConversationEventListener listener :
                                            mConversationEventListener) {
                                        listener.onSaveDraft(conversationType, targetId, content);
                                    }
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                                for (ConversationEventListener listener :
                                        mConversationEventListener) {
                                    listener.onOperationFailed(errorCode);
                                }
                            }
                        });
    }

    /**
     * /~chinese 从会话列表中移除某一会话。
     *
     * <p>此方法不删除会话内的消息。如果此会话中有新的消息，该会话将重新在会话列表中显示，并显示最近的历史消息。
     *
     * @param conversationType 会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param callback 移除会话是否成功的回调，回调类型是 Boolean，{@code ResultCallback<Boolean> }。
     * @group 会话
     */

    /**
     * /~english Removes a conversation from the conversation list.
     *
     * <p>This method does not delete messages within the conversation. If there is a new message in
     * this conversation, the conversation will reappear in the conversation list and the most
     * recent historical message will be displayed.
     *
     * @param type Conversation type {@link io.rong.imlib.model.Conversation.ConversationType}.
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param callback Callback for successful or failed conversation removal, which type is
     *     Boolean, {@code ResultCallback<Boolean>}.
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
     * /~chinese 设置会话的置顶状态。
     *
     * <p>若会话不存在，调用此方法 SDK 自动创建会话并置顶。
     *
     * @param conversationType 会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param id 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param isTop 是否置顶。
     * @param needCreate 会话不存在时，是否创建会话。
     * @param callback 设置置顶是否成功的回调。
     * @group 会话
     */

    /**
     * /~english Set the top status of the conversation.
     *
     * <p>If the conversation does not exist, this method SDK will be called to automatically create
     * the conversation and set top.
     *
     * @param type Conversation type {@link io.rong.imlib.model.Conversation.ConversationType}.
     * @param id Conversation id. Depending on the conversationType, it may be user id, discussion
     *     group id, group id, or chatroom id.
     * @param isTop Whether or not to set top
     * @param needCreate Whether to create a conversation if the conversation does not exist.
     * @param callback Callback for whether the conversation is set top successfully.
     */
    public void setConversationToTop(
            final Conversation.ConversationType type,
            final String id,
            final boolean isTop,
            final boolean needCreate,
            final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance()
                .setConversationToTop(
                        type,
                        id,
                        isTop,
                        needCreate,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean bool) {
                                if (callback != null) callback.onSuccess(bool);
                                for (RongIMClient.ConversationStatusListener listener :
                                        mConversationStatusObserverList) {
                                    ConversationStatus conversationStatus =
                                            new ConversationStatus();
                                    conversationStatus.setTargetId(id);
                                    conversationStatus.setConversationType(type.getValue());
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
                            public void onError(RongIMClient.ErrorCode e) {
                                if (callback != null) callback.onError(e);
                            }
                        });
    }

    /**
     * /~chinese 设置会话消息提醒状态。
     *
     * <p><strong>注意：不支持聊天室！</strong>
     *
     * @param conversationType 会话类型。不支持聊天室类型，因为聊天室默认就是不接受消息提醒的。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param notificationStatus 会话设置的消息提醒状态 {@link
     *     io.rong.imlib.model.Conversation.ConversationNotificationStatus} 。
     * @param callback 设置状态的回调。
     * @group 会话
     */

    /**
     * /~english Set the conversation message reminder status.
     *
     * <p><strong>Note: The chat rooms are not supported!</strong>
     *
     * @param conversationType Conversation type The chat room is not supported because the chat
     *     rooms do not accept message reminders by default.
     * @param targetId Conversation id. Depending on different conversationType, it may be user id,
     *     discussion group id or group id.
     * @param notificationStatus Message reminder status set for the conversation
     *     Conversation.ConversationNotificationStatus.
     * @param callback Callback for setting the status.
     */
    public void setConversationNotificationStatus(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final Conversation.ConversationNotificationStatus notificationStatus,
            final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>
                    callback) {
        RongIMClient.getInstance()
                .setConversationNotificationStatus(
                        conversationType,
                        targetId,
                        notificationStatus,
                        new RongIMClient.ResultCallback<
                                Conversation.ConversationNotificationStatus>() {
                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                            }

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
                                    conversationStatus.setTargetId(targetId);
                                    conversationStatus.setConversationType(
                                            conversationType.getValue());
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
                        });
    }

    public void insertOutgoingMessage(
            Conversation.ConversationType type,
            String targetId,
            Message.SentStatus sentStatus,
            MessageContent content,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        insertOutgoingMessage(
                type, targetId, sentStatus, content, System.currentTimeMillis(), resultCallback);
    }

    public void insertOutgoingMessage(
            Conversation.ConversationType type,
            String targetId,
            Message.SentStatus sentStatus,
            MessageContent content,
            long time,
            final RongIMClient.ResultCallback<Message> resultCallback) {
        RongIMClient.getInstance()
                .insertOutgoingMessage(
                        type,
                        targetId,
                        sentStatus,
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
     * /~chinese 删除某个会话中的所有消息。
     *
     * <p>此接口删除指定会话中数据库的所有消息，同时会清理数据库空间，减少占用空间。<br>
     *
     * @param conversationType 会话类型，不支持聊天室。参考 {@link Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是 userId, groupId, discussionId。
     * @param callback 删除是否成功的回调。
     * @group 消息操作
     */

    /**
     * /~english Delete all messages in a conversation.
     *
     * <p>This interface deletes all messages from the database in the specified conversation while
     * cleaning up the database space and reducing the footprint.
     *
     * @param conversationType Type of conversation, which does not support chat rooms. Refer to
     *     Conversation.ConversationType.
     * @param targetId Conversation id. Depending on different conversationType, it may be userId,
     *     groupId or discussionId.
     * @param callback Callback for deletion success or failure.
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
     * /~chinese 删除消息。
     *
     * @param messageIds 要删除的消息 id 数组。
     * @param callback 删除是否成功的回调。
     * @group 消息操作
     */

    /**
     * /~english Delete messages.
     *
     * @param messageIds Id array of messages to delete.
     * @param callback Callback for deletion success or failure.
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
     * /~chinese 批量删除某个会话中的指定远端消息（同时删除对应的本地消息）。
     *
     * <p>一次批量操作仅支持删除属于同一个会话的消息，请确保消息列表中的所有消息来自同一会话，一次最多删除 100 条消息。<br>
     * <strong>注意：不支持聊天室！</strong>
     *
     * <p>
     *
     * @param conversationType 会话类型， 不支持聊天室。参考 {@link Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、客服 id。
     * @param messages 要删除的消息数组， 数组大小不能超过 100 条。
     * @param callback 删除是否成功的回调。
     * @group 消息操作
     */

    /**
     * /~english Deletes specified remote messages in a conversation (while deleting the
     * corresponding local messages) in batches.
     *
     * <p>One batch operation only supports to delete messages belonging to the same conversation,
     * please make sure that all messages in the message list come from the same conversation and
     * delete at most 100 messages at a time. Note: The chat rooms are not supported!
     *
     * @param conversationType Type of conversation, which does not support chat rooms. Refer to
     *     Conversation.ConversationType.
     * @param targetId Conversation id. Depending on different conversationType, it may be user id
     *     and customer service id.
     * @param messages Array of messages to delete, and the size cannot exceed 100.
     * @param callback Callback for deletion success or failure.
     */
    public void deleteRemoteMessages(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final Message[] messages,
            final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .deleteRemoteMessages(
                        conversationType,
                        targetId,
                        messages,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                int[] messageIds = new int[messages.length];
                                for (int i = 0; i < messages.length; i++) {
                                    messageIds[i] = messages[i].getMessageId();
                                }
                                for (MessageEventListener item : mMessageEventListeners) {
                                    item.onDeleteMessage(
                                            new DeleteEvent(
                                                    conversationType, targetId, messageIds));
                                }
                                if (callback != null) {
                                    callback.onSuccess();
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
     * /~chinese 断开与融云服务器的连接，但仍然接收远程推送。
     *
     * <p>若想断开连接后不接受远程推送消息，可以调用{@link #logout()}。<br>
     * <strong>注意：</strong>因为 SDK 在前后台切换或者网络出现异常都会自动重连，保证连接可靠性。 所以除非您的 App
     * 逻辑需要登出，否则一般不需要调用此方法进行手动断开。<br>
     */

    /**
     * /~english Disconnect from the RongCloud server, but still receive remote push.
     *
     * <p>If you want not to receive remote push messages after disconnection, you can call {@link
     * #logout()}. <br>
     * <strong>Note：</strong>The system can automatically reconnect to ensure reliability of the
     * connection because SDK switches between the foreground and background or the network is
     * abnormal. <br>
     * Unless your App logic requires logout, generally you don't need to call this method for
     * manual disconnection.
     */
    public void disconnect() {
        RongIMClient.getInstance().disconnect();
        RongExtensionManager.getInstance().disconnect();
    }

    /**
     * /~chinese 下载多媒体文件。
     *
     * <p>如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param mediaType 文件类型。
     * @param imageUrl 文件的 URL 地址。
     * @param callback 下载文件的回调。
     * @group 多媒体下载
     */

    /**
     * /~english Download multimedia files.
     *
     * <p>If this file is included in the local cache, it is obtained directly from the local cache,
     * otherwise it will be downloaded from the server side.
     *
     * @param conversationType Conversation type
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param mediaType File type
     * @param imageUrl The URL address of the file.
     * @param callback Callback for downloading files
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
     * /~chinese 下载文件。
     *
     * <p>支持断点续传，对应的暂停下载须调用 {@link #pauseDownloadMediaFile(String, RongIMClient.OperationCallback)}。
     *
     * @param fileUniqueId 文件唯一标识, 与 {@link #pauseDownloadMediaFile(String,
     *     RongIMClient.OperationCallback)} 第一个参数对应
     * @param fileUrl 文件下载地址
     * @param fileName 文件名
     * @param path 文件下载保存目录，如果是 targetVersion 29 为目标，由于访问权限原因，建议使用 context.getExternalFilesDir()
     *     方法保存到私有目录
     * @param callback 下载文件的回调
     * @group 多媒体下载
     */

    /**
     * /~english Download the file.
     *
     * <p>Resuming downloading from breakpoints is supported and pauseDownloadMediaFile(String,
     * IRongCoreCallback.OperationCallback) shall be called when downloading pauses.
     *
     * @param uid Unique file identifier, which corresponds to the first parameter of
     *     pauseDownloadMediaFile(String, IRongCoreCallback.OperationCallback)
     * @param fileUrl File download address
     * @param fileName File name
     * @param path File downloading and saving directory. If targetVersion 29 is the target, it is
     *     recommended to use the context.getExternalFilesDir() method to save to a private
     *     directory due to access rights
     * @param callback Callback for downloading files
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
                                if (callback != null) {
                                    callback.onFileNameChanged(newFileName);
                                }
                            }

                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onProgress(int progress) {
                                if (callback != null) {
                                    callback.onProgress(progress);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode code) {
                                if (callback != null) {
                                    callback.onError(code);
                                }
                            }

                            @Override
                            public void onCanceled() {
                                if (callback != null) {
                                    callback.onCanceled();
                                }
                            }
                        });
    }

    public void cancelDownloadMediaMessage(
            Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelDownloadMediaMessage(message, callback);
    }

    /**
     * /~chinese 清空指定会话类型列表中的所有会话及会话信息。
     *
     * @param callback 是否清空成功的回调。
     * @param conversationTypes 需要清空的会话类型列表。
     * @group 会话
     */

    /**
     * /~english Clears all conversations and conversation information in the list of specified
     * conversation types.
     *
     * @param callback Callback for whether the emptying is successful.
     * @param conversationTypes List of conversation types that need to be emptied.
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
     * /~chinese 下载多媒体文件。
     *
     * @param message 媒体消息（FileMessage，SightMessage，GIFMessage, HQVoiceMessage等）。
     * @param callback 下载文件的回调。参考 {@link IRongCallback.IDownloadMediaMessageCallback}。
     * @group 多媒体下载
     */

    /**
     * /~english Download multimedia files.
     *
     * @param message Media messages (FileMessage, SightMessage, GIFMessage, HQVoiceMessage, among
     *     others).
     * @param callback Callback for downloading files Refer to {@link
     *     IRongCallback.IDownloadMediaMessageCallback}.
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

    /**
     * /~chinese 断开与融云服务器的连接，并且不再接收远程推送消息。
     *
     * <p>若想断开连接后仍然接受远程推送消息，可以调用 {@link #disconnect()}
     *
     * @group 连接
     */

    /**
     * /~english Disconnect from the RongCloud server and no longer receive remote push messages.
     *
     * <p>If you want to receive remote push messages after disconnection, you can call {@link
     * #disconnect()}
     */
    public void logout() {
        RongIMClient.getInstance().logout();
        RongExtensionManager.getInstance().disconnect();
    }

    /**
     * /~chinese 设置接收消息时的拦截器
     *
     * @param messageInterceptor 拦截器
     */

    /**
     * /~english Set the interceptor when receiving messages
     *
     * @param messageInterceptor Interceptor
     */
    public void setMessageInterceptor(MessageInterceptor messageInterceptor) {
        mMessageInterceptor = messageInterceptor;
    }

    /**
     * /~chinese 判断是否支持断点续传。
     *
     * @param url 文件 Url
     * @param callback 回调
     * @group 数据获取
     */

    /**
     * /~english Determine whether to support breakpoint continuation.
     *
     * @param url File Url
     * @param callback Callback
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
     * /~chinese 暂停多媒体消息下载。
     *
     * @param message 多媒体文件消息。
     * @param callback 暂停下载多媒体文件时的回调。
     * @group 多媒体下载
     */

    /**
     * /~english Pause downloading of multimedia messages.
     *
     * @param message Multimedia file message.
     * @param callback Callback for pausing downloading of multimedia files.
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
     * /~chinese 通知会话页面刷新某条消息
     *
     * @param message
     */

    /**
     * /~english Notify the conversation page to refresh a message
     *
     * @param message messages
     */
    public void refreshMessage(Message message) {
        for (MessageEventListener item : mMessageEventListeners) {
            item.onRefreshEvent(new RefreshEvent(message));
        }
    }

    public Context getContext() {
        return mContext;
    }

    /** 语音消息类型 */
    public enum VoiceMessageType {
        /** 普通音质语音消息 */
        Ordinary,
        /** 高音质语音消息 */
        HighQuality
    }

    /**
     * /~chinese 发送地理位置消息。
     *
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为 {@link LocationMessage}, 否则返回失败。<br>
     * 其中的缩略图地址 scheme 只支持 file:// 和 http://。也可不设置缩略图地址，传入 NULL。
     *
     * @param message 消息实体。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 sdk
     *     中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到远程推送消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param sendMessageCallback 发送消息的回调，参考 {@link
     *     io.rong.imlib.IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     * @deprecated {@link io.rong.imlib.RongIMClient#sendMessage(Message, String, String,
     *     SendMessageOption, IRongCallback.ISendMessageCallback) }
     */

    /**
     * /~english Send a geolocation message. And update the interface at the same time. The Message
     * message entity is constructed before sending and the content in the message entity must be
     * LocationMessage, or the return fails. The thumbnail address scheme only supports file:// and
     * http:// but not others.
     *
     * @param message Message entity.
     * @param pushContent This field is displayed in the notification bar when a push message is
     *     sent. If you are sending a custom message, this field must be completed, otherwise you
     *     cannot receive the push message. If you send the default message type in sdk such as
     *     RC:TxtMsg, RC:VcMsg, RC:ImgMsg, this field does not need to be filled in and has been
     *     specified by default.
     * @param pushData Push additional information. If this field is set, users can get it through
     *     io.rong.push.notification.PushNotificationMessage#getPushData() method when they receive
     *     a push message.
     * @param sendMessageCallback Callback for sending messages, refer to
     *     IRongCallback.ISendMessageCallback.
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
                                            public void onComplete() {}
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
     * /~chinese 根据消息 Message 设置消息状态，回调方式获取设置是否成功。
     *
     * @param message 消息实体。要设置的发送状态包含在 message 中
     * @param callback 是否设置成功的回调。
     */

    /**
     * /~english The message status is set according to the Message, and the callback method is used
     * to obtain whether the setting is successful.
     *
     * @param message Message entity. The sending status to be set is included in the message
     * @param callback Callback for successful or failed setting.
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
     * /~chinese 发送消息之前的内部逻辑处理。
     *
     * @param message 待发送的消息
     */

    /**
     * /~english Internal logical processing before sending a message.
     *
     * @param message Messages to be sent
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

        if (errorCode != null
                && errorCode != RongIMClient.ErrorCode.RC_MSG_REPLACED_SENSITIVE_WORD) {
            if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION)
                    || errorCode.equals(RongIMClient.ErrorCode.NOT_IN_GROUP)
                    || errorCode.equals(RongIMClient.ErrorCode.NOT_IN_CHATROOM)
                    || errorCode.equals(RongIMClient.ErrorCode.REJECTED_BY_BLACKLIST)
                    || errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_GROUP)
                    || errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_CHATROOM)
                    || errorCode.equals(RongIMClient.ErrorCode.KICKED_FROM_CHATROOM)) {

                InformationNotificationMessage informationMessage = null;

                if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION)) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_not_in_discussion));
                } else if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_GROUP)) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_not_in_group));
                } else if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_CHATROOM)) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_not_in_chatroom));
                } else if (errorCode.equals(RongIMClient.ErrorCode.REJECTED_BY_BLACKLIST)) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_rejected_by_blacklist_prompt));
                } else if (errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_GROUP)) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_info_forbidden_to_talk));
                } else if (errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_CHATROOM)) {
                    informationMessage =
                            InformationNotificationMessage.obtain(
                                    mContext.getString(R.string.rc_forbidden_in_chatroom));
                } else if (errorCode.equals(RongIMClient.ErrorCode.KICKED_FROM_CHATROOM)) {
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
        MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
        if (RongConfigCenter.conversationConfig().rc_enable_resend_message
                && tag != null
                && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
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

    public interface FilterSentListener {
        void onComplete();
    }

    /**
     * /~chinese 取消发送多媒体文件。
     *
     * @param message 多媒体文件消息。
     * @param callback 取消发送多媒体文件时的回调。
     * @group 多媒体下载
     */

    /**
     * /~english Cancel sending multimedia files.
     *
     * @param message Multimedia file message.
     * @param callback Callback for canceling sending of multimedia files.
     */
    public void cancelSendMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelSendMediaMessage(message, callback);
    }

    public void addOnReceiveMessageListener(RongIMClient.OnReceiveMessageWrapperListener listener) {
        mOnReceiveMessageObserverList.add(listener);
    }

    public void removeOnReceiveMessageListener(
            RongIMClient.OnReceiveMessageWrapperListener listener) {
        if (listener != null) {
            mOnReceiveMessageObserverList.remove(listener);
        }
    }

    public void addSyncConversationReadStatusListener(
            RongIMClient.SyncConversationReadStatusListener listener) {
        mSyncConversationReadStatusListeners.add(listener);
    }

    public void removeSyncConversationReadStatusListeners(
            RongIMClient.SyncConversationReadStatusListener listener) {
        mSyncConversationReadStatusListeners.remove(listener);
    }

    public void addConnectStatusListener(RongIMClient.ConnectCallback callback) {
        mConnectStatusListener.add(callback);
    }

    public void removeConnectStatusListener(RongIMClient.ConnectCallback callback) {
        mConnectStatusListener.remove(callback);
    }

    /**
     * /~chinese 设置连接状态变化的监听器。
     *
     * <p><strong> 当回调状态为{@link
     * RongIMClient.ConnectionStatusListener.ConnectionStatus#TOKEN_INCORRECT}, 需要获取正确的token,
     * 并主动调用{@link RongIM#connect(String, int, RongIMClient.ConnectCallback)} </strong>
     *
     * @param listener 连接状态变化的监听器。
     */

    /**
     * /~english Set the listener for changes in the state of the connection.
     *
     * <p>When the callback status is
     * RongIMClient.ConnectionStatusListener.ConnectionStatus#TOKEN_INCORRECT the correct token
     * shall be obtained and actively callRongIM.connect(String, int, RongIMClient.ConnectCallback)
     *
     * @param listener A listener that connects to a state change.
     */
    public void addConnectionStatusListener(RongIMClient.ConnectionStatusListener listener) {
        mConnectionStatusObserverList.add(listener);
    }

    /**
     * /~chinese 移除连接状态监听器
     *
     * @param listener 连接状态变化监听器
     */

    /**
     * /~english Remove connection status listener
     *
     * @param listener Connect the state change listener
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
        mMessageEventListeners.add(listener);
    }

    public void removeMessageEventListener(MessageEventListener listener) {
        mMessageEventListeners.remove(listener);
    }

    public void addConversationStatusListener(RongIMClient.ConversationStatusListener listener) {
        mConversationStatusObserverList.add(listener);
    }

    public void removeConversationStatusListener(RongIMClient.ConversationStatusListener listener) {
        mConversationStatusObserverList.remove(listener);
    }

    public void addOnRecallMessageListener(RongIMClient.OnRecallMessageListener listener) {
        mOnRecallMessageObserverList.add(listener);
    }

    public void removeOnRecallMessageListener(RongIMClient.OnRecallMessageListener listener) {
        mOnRecallMessageObserverList.remove(listener);
    }

    public void addReadReceiptListener(RongIMClient.ReadReceiptListener listener) {
        mReadReceiptObserverList.add(listener);
    }

    public void removeReadReceiptListener(RongIMClient.ReadReceiptListener listener) {
        mReadReceiptObserverList.remove(listener);
    }

    public void addTypingStatusListener(RongIMClient.TypingStatusListener listener) {
        mTypingStatusListeners.add(listener);
    }

    public void removeTypingStatusListener(RongIMClient.TypingStatusListener listener) {
        mTypingStatusListeners.remove(listener);
    }

    /** 连接状态变化的监听器。 */
    private RongIMClient.ConnectionStatusListener mConnectionStatusListener =
            new RongIMClient.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus connectionStatus) {
                    for (RongIMClient.ConnectionStatusListener listener :
                            mConnectionStatusObserverList) {
                        listener.onChanged(connectionStatus);
                    }
                }
            };

    /** 接受消息的事件监听器。 */
    private RongIMClient.OnReceiveMessageWrapperListener mOnReceiveMessageListener =
            new RongIMClient.OnReceiveMessageWrapperListener() {
                @Override
                public boolean onReceived(
                        final Message message,
                        final int left,
                        final boolean hasPackage,
                        final boolean offline) {
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mMessageInterceptor != null
                                                    && mMessageInterceptor.interceptReceivedMessage(
                                                            message, left, hasPackage, offline)) {
                                                RLog.d(TAG, "message has been intercepted.");
                                                return;
                                            }
                                            for (RongIMClient.OnReceiveMessageWrapperListener
                                                    observer : mOnReceiveMessageObserverList) {
                                                observer.onReceived(
                                                        message, left, hasPackage, offline);
                                            }
                                        }
                                    });
                    return false;
                }
            };
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
}
