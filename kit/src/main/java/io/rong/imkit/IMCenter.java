package io.rong.imkit;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.InformationNotificationMessage;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.TextMessage;

public class IMCenter {
    private static final String TAG = IMCenter.class.getSimpleName();
    private Context mContext;
    private MessageInterceptor mMessageInterceptor;
    private List<RongIMClient.ConnectionStatusListener> mConnectionStatusObserverList = new CopyOnWriteArrayList<>();
    private List<RongIMClient.OnReceiveMessageWrapperListener>
            mOnReceiveMessageObserverList = new CopyOnWriteArrayList<>();
    private List<RongIMClient.ConversationStatusListener> mConversationStatusObserverList = new CopyOnWriteArrayList<>();
    private List<RongIMClient.ReadReceiptListener> mReadReceiptObserverList = new CopyOnWriteArrayList<>();
    private List<RongIMClient.OnRecallMessageListener> mOnRecallMessageObserverList = new CopyOnWriteArrayList<>();

    private List<MessageEventListener> mMessageEventListeners = new CopyOnWriteArrayList<>();
    private List<ConversationEventListener> mConversationEventListener = new CopyOnWriteArrayList<>();

    private List<RongIMClient.ConnectCallback> mConnectStatusListener = new CopyOnWriteArrayList<>();
    private List<RongIMClient.SyncConversationReadStatusListener> mSyncConversationReadStatusListeners = new CopyOnWriteArrayList<>();
    private List<RongIMClient.TypingStatusListener> mTypingStatusListeners = new CopyOnWriteArrayList<>();

    private IMCenter() {

    }

    private static class SingletonHolder {
        static IMCenter sInstance = new IMCenter();
    }

    public static IMCenter getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param application  应用上下文。
     * @param appKey       在融云开发者后台注册的应用 AppKey。
     * @param isEnablePush 是否使用推送功能
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
        RongIMClient.setOnReceiveMessageListener(SingletonHolder.sInstance.mOnReceiveMessageListener);
        RongIMClient.setConnectionStatusListener(SingletonHolder.sInstance.mConnectionStatusListener);
        RongIMClient.setOnRecallMessageListener(SingletonHolder.sInstance.mOnRecallMessageListener);
        RongIMClient.getInstance().setConversationStatusListener(SingletonHolder.sInstance.mConversationStatusListener);
        RongIMClient.setReadReceiptListener(SingletonHolder.sInstance.mReadReceiptListener);
        RongIMClient.getInstance().setOnReceiveDestructionMessageListener(SingletonHolder.sInstance.mOnReceiveDestructMessageListener);
        RongIMClient.getInstance().setSyncConversationReadStatusListener(SingletonHolder.sInstance.mSyncConversationReadStatusListener);
        RongIMClient.setTypingStatusListener(SingletonHolder.sInstance.mTypingStatusListener);
        RongIMClient.registerMessageType(CombineMessage.class);
    }

    public void connect(String token, int timeLimit, final RongIMClient.ConnectCallback connectCallback) {
        RongIMClient.connect(token, timeLimit, new RongIMClient.ConnectCallback() {
            @Override
            public void onSuccess(String s) {
                if (connectCallback != null) {
                    connectCallback.onSuccess(s);
                }
                if (!TextUtils.isEmpty(s)) {
                    RongNotificationManager.getInstance().getNotificationQuietHours(null);
                    RongUserInfoManager.getInstance().initAndUpdateUserDataBase(SingletonHolder.sInstance.mContext);
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
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
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
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调方式获取清空是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清空是否成功的回调。
     */
    public void clearMessages(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearMessages(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool) {
                    for (MessageEventListener listener : mMessageEventListeners) {
                        listener.onClearMessages(new ClearEvent(conversationType, targetId));
                    }
                    if (callback != null)
                        callback.onSuccess(bool);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    public void connect(String token, final RongIMClient.ConnectCallback connectCallback) {
        connect(token, -1, connectCallback);
    }

    public void recallMessage(final Message message, String pushContent, final RongIMClient.ResultCallback callback) {
        RongIMClient.getInstance().recallMessage(message, pushContent, new RongIMClient.ResultCallback<RecallNotificationMessage>() {
            @Override
            public void onSuccess(RecallNotificationMessage recallNotificationMessage) {
                if (callback != null) {
                    callback.onSuccess(recallNotificationMessage);
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onRecallEvent(new RecallEvent(message.getConversationType(), message.getTargetId(), message.getMessageId(), recallNotificationMessage));
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
     * <p>
     * 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条。 </p>
     *
     * @param type        会话类型。
     * @param targetId    会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param content     消息内容，例如 {@link TextMessage}, {@link ImageMessage}。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。
     *                    如果发送 SDK 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    远程推送附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调。参考 {@link IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     */
    public void sendMessage(final Conversation.ConversationType type, final String targetId, final MessageContent content, final String pushContent, final String pushData, final IRongCallback.ISendMessageCallback callback) {
        Message message = Message.obtain(targetId, type, content);
        sendMessage(message, pushContent, pushData, callback);
    }

    public void sendMessage(Message message, String pushContent, String pushData, IRongCallback.ISendMessageCallback callback) {
        sendMessage(message, pushContent, pushData, null, callback);
    }

    /**
     * <p>发送消息。
     * 通过 {@link IRongCallback.ISendMessageCallback}
     * 中的方法回调发送的消息状态及消息体。</p>
     *
     * @param message     将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     */
    public void sendMessage(Message message, String pushContent, final String pushData, SendMessageOption option, final IRongCallback.ISendMessageCallback callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance().sendMessage(message, pushContent, pushData, option, new IRongCallback.ISendMessageCallback() {
            @Override
            public void onAttached(Message message) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMessage(new SendEvent(SendEvent.ATTACH, message));
                }
                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
                if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSentMessage(message)) {
                    RLog.d(TAG, "message has been intercepted.");
                    return;
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMessage(new SendEvent(SendEvent.SUCCESS, message));
                }

            }

            @Override
            public void onError(final Message message, final RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode, new FilterSentListener() {
                    @Override
                    public void onComplete() {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMessage(new SendEvent(SendEvent.ERROR, message, errorCode));
                        }
                    }
                });
                if (callback != null)
                    callback.onError(message, errorCode);
            }
        });
    }

    /**
     * 发送已读回执，该方法会触发刷新消息未读数
     *
     * <p>
     * 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。<br>
     * <strong>注意：1 秒钟发送消息不能超过 5 条。 </p>
     *
     * @param conversationType 会话类型。
     * @param targetId         会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param timestamp        时间戳
     * @param callback         发送消息的回调。参考 {@link IRongCallback.ISendMessageCallback}。
     * @group 消息操作
     */
    public void sendReadReceiptMessage(final Conversation.ConversationType conversationType, final String targetId,
                                       long timestamp, final IRongCallback.ISendMessageCallback callback) {
        RongIMClient.getInstance().sendReadReceiptMessage(conversationType, targetId, timestamp, new IRongCallback.ISendMessageCallback() {
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
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearedUnreadStatus(conversationType, targetId);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(message, errorCode);
                }
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearedUnreadStatus(conversationType, targetId);
                }
            }
        });
    }

    /**
     * 同步会话阅读状态。
     *
     * @param type      会话类型
     * @param targetId  会话 id
     * @param timestamp 会话中已读的最后一条消息的发送时间戳 {@link Message#getSentTime()}
     * @param callback  回调函数
     * @group 高级功能
     */
    public void syncConversationReadStatus(final Conversation.ConversationType type, final String targetId, long timestamp, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().syncConversationReadStatus(type, targetId, timestamp, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearedUnreadStatus(type, targetId);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearedUnreadStatus(type, targetId);
                }
            }
        });
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
    public static void setConversationListBehaviorListener(ConversationListBehaviorListener listener) {
        RongConfigCenter.conversationListConfig().setBehaviorListener(listener);
    }



    /**
     * <p>发送消息。
     * 通过 {@link IRongCallback.ISendMessageCallback}
     * 中的方法回调发送的消息状态及消息体。</p>
     *
     * @param message     将要发送的消息体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调，参考 {@link IRongCallback.ISendMediaMessageCallback}。
     */
    public void sendMediaMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMediaMessageCallback callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance().sendMediaMessage(message, pushContent, pushData, new IRongCallback.ISendMediaMessageCallback() {
            @Override
            public void onProgress(Message message, int i) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.PROGRESS, message, i));
                }
                if (callback != null)
                    callback.onProgress(message, i);
            }

            @Override
            public void onCanceled(Message message) {
                filterSentMessage(message, null, null);
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.CANCEL, message));
                }
                if (callback != null)
                    callback.onCanceled(message);
            }

            @Override
            public void onAttached(Message message) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.ATTACH, message));
                }
                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
                if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSentMessage(message)) {
                    RLog.d(TAG, "message has been intercepted.");
                    return;
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.SUCCESS, message));
                }
            }

            @Override
            public void onError(final Message message, final RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode, new FilterSentListener() {
                    @Override
                    public void onComplete() {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.ERROR, message, errorCode));
                        }
                    }
                });
                if (callback != null)
                    callback.onError(message, errorCode);
            }
        });
    }

    public void sendMediaMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMediaMessageCallbackWithUploader callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        IRongCallback.ISendMediaMessageCallbackWithUploader sendMediaMessageCallbackWithUploader = new IRongCallback.ISendMediaMessageCallbackWithUploader() {
            @Override
            public void onAttached(Message message, IRongCallback.MediaMessageUploader uploader) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.ATTACH, message));
                }
                if (callback != null)
                    callback.onAttached(message, uploader);
            }

            @Override
            public void onProgress(Message message, int progress) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.PROGRESS, message, progress));
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
                if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSentMessage(message)) {
                    RLog.d(TAG, "message has been intercepted.");
                    return;
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.SUCCESS, message));
                }
            }

            @Override
            public void onError(final Message message, final RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode, new FilterSentListener() {
                    @Override
                    public void onComplete() {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.ERROR, message, errorCode));
                        }
                    }
                });
                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onCanceled(Message message) {
                filterSentMessage(message, null, null);
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.CANCEL, message));
                }
                if (callback != null)
                    callback.onCanceled(message);
            }
        };

        RongIMClient.getInstance().sendMediaMessage(message, pushContent, pushData, sendMediaMessageCallbackWithUploader);
    }

    /**
     * <p>发送定向消息。向会话中特定的某些用户发送消息，会话中其他用户将不会收到此消息。
     * 通过 {@link IRongCallback.ISendMessageCallback} 中的方法回调发送的消息状态及消息体。</p>
     * 此方法只能发送非多媒体消息，多媒体消息如{@link ImageMessage} {@link FileMessage} ，或者继承自{@link MediaMessageContent}的消息须调用
     * {@link #sendDirectionalMediaMessage(Message, String[], String, String, IRongCallback.ISendMediaMessageCallback)}。
     *
     * @param type        会话类型。
     * @param targetId    目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content     消息内容，例如 {@link TextMessage}
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param userIds     会话中将会接收到此消息的用户列表。
     * @param callback    发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     */
    public void sendDirectionalMessage(Conversation.ConversationType type, String targetId, MessageContent content, final String[] userIds, String pushContent, final String pushData, final IRongCallback.ISendMessageCallback callback) {
        Message message = Message.obtain(targetId, type, content);
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance().sendDirectionalMessage(type, targetId, content, userIds, pushContent, pushData, new IRongCallback.ISendMessageCallback() {
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
                if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSentMessage(message)) {
                    RLog.d(TAG, "message has been intercepted.");
                    return;
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMessage(new SendEvent(SendEvent.SUCCESS, message));
                }
            }

            @Override
            public void onError(final Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode, new FilterSentListener() {
                    @Override
                    public void onComplete() {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMessage(new SendEvent(SendEvent.ERROR, message));
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
     * <p>发送定向多媒体消息</p> 向会话中特定的某些用户发送消息，会话中其他用户将不会收到此消息。
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为多媒体消息，如 {@link ImageMessage} {@link FileMessage} </p>
     * <p>或者其他继承自 {@link MediaMessageContent} 的消息</p>
     *
     * @param message     发送消息的实体。
     * @param userIds     定向接收者 id 数组
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    发送文件消息时，此字段必须填写，否则会收不到 push 推送。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调 {@link RongIMClient.SendMediaMessageCallback}。
     */
    public void sendDirectionalMediaMessage(Message message, String[] userIds, String pushContent,
                                            final String pushData, final IRongCallback.ISendMediaMessageCallback callback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance().sendDirectionalMediaMessage(message, userIds, pushContent, pushData, new IRongCallback.ISendMediaMessageCallback() {
            @Override
            public void onProgress(Message message, int i) {
                if (callback != null) {
                    callback.onProgress(message, i);
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.PROGRESS, message, i));
                }
            }

            @Override
            public void onCanceled(Message message) {
                filterSentMessage(message, null, null);
                if (callback != null) {
                    callback.onCanceled(message);
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.CANCEL, message));
                }
            }

            @Override
            public void onAttached(Message message) {
                if (callback != null) {
                    callback.onAttached(message);
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.ATTACH, message));
                }
            }

            @Override
            public void onSuccess(Message message) {
                filterSentMessage(message, null, null);
                if (callback != null) {
                    callback.onSuccess(message);
                }
                if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSentMessage(message)) {
                    RLog.d(TAG, "message has been intercepted.");
                    return;
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.SUCCESS, message));
                }
            }

            @Override
            public void onError(final Message message, final RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode, new FilterSentListener() {
                    @Override
                    public void onComplete() {
                        for (MessageEventListener item : mMessageEventListeners) {
                            item.onSendMediaMessage(new SendMediaEvent(SendMediaEvent.ERROR, message, errorCode));
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
     * <p>
     * 此方法可从服务器端清除历史消息，<Strong>但是必须先开通历史消息云存储功能。</Strong> <br>
     * 根据会话类型和会话 id 清除某一会话指定时间戳之前的本地数据库消息（服务端历史消息），
     * 清除成功后只能从本地数据库（服务端）获取到该时间戳之后的历史消息。
     * </p>
     *
     * @param conversationType 会话类型。
     * @param targetId         会话 id。
     * @param recordTime       清除消息截止时间戳，【0 <= recordTime <= 当前会话最后一条消息的 sentTime,0 清除所有消息，其他值清除小于等于 recordTime 的消息】。
     * @param cleanRemote      是否删除服务器端消息
     * @param callback         清除消息的回调。
     * @group 消息操作
     */
    public void cleanHistoryMessages(final Conversation.ConversationType conversationType,
                                     final String targetId,
                                     final long recordTime,
                                     final boolean cleanRemote,
                                     final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cleanHistoryMessages(conversationType, targetId, recordTime, cleanRemote, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null)
                    callback.onSuccess();
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearedMessage(conversationType, targetId);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onOperationFailed(errorCode);
                }
            }
        });
    }

    /**
     * 清除某会话的消息未读状态
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清除是否成功的回调。
     */
    public void clearMessagesUnreadStatus(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearMessagesUnreadStatus(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearedUnreadStatus(conversationType, targetId);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 清除某一会话的文字消息草稿，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         是否清除成功的回调。
     */
    public void clearTextMessageDraft(Conversation.ConversationType conversationType, String targetId, RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 保存文字消息草稿，回调方式获取保存是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content          草稿的文字内容。
     * @param callback         是否保存成功的回调。
     */
    public void saveTextMessageDraft(final Conversation.ConversationType conversationType, final String targetId, final String content, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().saveTextMessageDraft(conversationType, targetId, content, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean value) {
                if (callback != null) {
                    callback.onSuccess(value);
                }
                if (value) {
                    for (ConversationEventListener listener : mConversationEventListener) {
                        listener.onSaveDraft(conversationType, targetId, content);
                    }
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onOperationFailed(errorCode);
                }
            }
        });
    }

    /**
     * 从会话列表中移除某一会话，但是不删除会话内的消息。
     * <p/>
     * 如果此会话中有新的消息，该会话将重新在会话列表中显示，并显示最近的历史消息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 移除会话是否成功的回调。
     */
    public void removeConversation(final Conversation.ConversationType type, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().removeConversation(type, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null) {
                    callback.onSuccess(bool);
                }
                if (bool) {
                    for (ConversationEventListener listener : mConversationEventListener) {
                        listener.onConversationRemoved(type, targetId);
                    }
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param type       会话类型。
     * @param id         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param isTop      是否置顶。
     * @param needCreate 会话不存在时，是否创建会话。
     * @param callback   设置置顶或取消置顶是否成功的回调。
     */
    public void setConversationToTop(final Conversation.ConversationType type, final String id, final boolean isTop, final boolean needCreate, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().setConversationToTop(type, id, isTop, needCreate, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);
                for (RongIMClient.ConversationStatusListener listener : mConversationStatusObserverList) {
                    ConversationStatus conversationStatus = new ConversationStatus();
                    conversationStatus.setTargetId(id);
                    conversationStatus.setConversationType(type.getValue());
                    HashMap<String, String> statusMap = new HashMap<>();
                    statusMap.put(ConversationStatus.TOP_KEY, isTop ? ConversationStatus.TopStatus.TOP.value : ConversationStatus.TopStatus.UNTOP.value);
                    conversationStatus.setStatus(statusMap);
                    listener.onStatusChanged(new ConversationStatus[]{conversationStatus});
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 设置会话消息提醒状态。
     *
     * @param conversationType   会话类型。
     * @param targetId           目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param notificationStatus 是否屏蔽。
     * @param callback           设置状态的回调。
     */
    public void setConversationNotificationStatus(final Conversation.ConversationType conversationType, final String targetId, final Conversation.ConversationNotificationStatus notificationStatus, final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus> callback) {
        RongIMClient.getInstance().setConversationNotificationStatus(conversationType, targetId, notificationStatus, new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {
            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }

            @Override
            public void onSuccess(Conversation.ConversationNotificationStatus status) {
                if (callback != null) {
                    callback.onSuccess(status);
                }
                for (RongIMClient.ConversationStatusListener listener : mConversationStatusObserverList) {
                    ConversationStatus conversationStatus = new ConversationStatus();
                    conversationStatus.setTargetId(targetId);
                    conversationStatus.setConversationType(conversationType.getValue());
                    HashMap<String, String> statusMap = new HashMap<>();
                    statusMap.put(ConversationStatus.NOTIFICATION_KEY,
                            notificationStatus.equals(Conversation.ConversationNotificationStatus.DO_NOT_DISTURB) ? "1" : "0");
                    conversationStatus.setStatus(statusMap);
                    listener.onStatusChanged(new ConversationStatus[]{conversationStatus});
                }
            }
        });
    }

    public void insertOutgoingMessage(Conversation.ConversationType type, String targetId, Message.SentStatus sentStatus, MessageContent content, final RongIMClient.ResultCallback<Message> resultCallback) {
        insertOutgoingMessage(type, targetId, sentStatus, content, System.currentTimeMillis(), resultCallback);
    }

    public void insertOutgoingMessage(Conversation.ConversationType type, String targetId, Message.SentStatus sentStatus, MessageContent content, long time, final RongIMClient.ResultCallback<Message> resultCallback) {
        RongIMClient.getInstance().insertOutgoingMessage(type, targetId, sentStatus, content, time, new RongIMClient.ResultCallback<Message>() {
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


    public void insertIncomingMessage(Conversation.ConversationType type, String targetId, String senderId, Message.ReceivedStatus receivedStatus,
                                      MessageContent content, final RongIMClient.ResultCallback<Message> resultCallback) {
        insertIncomingMessage(type, targetId, senderId, receivedStatus, content, System.currentTimeMillis(), resultCallback);
    }

    public void insertIncomingMessage(Conversation.ConversationType type, String targetId, String senderId, Message.ReceivedStatus receivedStatus,
                                      MessageContent content, long time, final RongIMClient.ResultCallback<Message> resultCallback) {
        RongIMClient.getInstance().insertIncomingMessage(type, targetId, senderId, receivedStatus, content, time, new RongIMClient.ResultCallback<Message>() {
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
     * <p>清除指定会话的消息</p>。
     * <p>此接口会删除指定会话中数据库的所有消息，同时，会清理数据库空间。
     * 如果数据库特别大，超过几百 M，调用该接口会有少许耗时。</p>
     *
     * @param conversationType 要删除的消息 Id 数组。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         是否删除成功的回调。
     */
    public void deleteMessages(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().deleteMessages(conversationType, targetId, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool) {
                    for (MessageEventListener item : mMessageEventListeners) {
                        item.onClearMessages(new ClearEvent(conversationType, targetId));
                    }
                }
                if (callback != null)
                    callback.onSuccess(bool);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }


    /**
     * 删除会话里的一条或多条消息。
     *
     * @param conversationType
     * @param targetId
     * @param messageIds
     * @param callback
     */
    public void deleteMessages(final Conversation.ConversationType conversationType, final String targetId, final int[] messageIds, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().deleteMessages(messageIds, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    for (MessageEventListener item : mMessageEventListeners) {
                        item.onDeleteMessage(new DeleteEvent(conversationType, targetId, messageIds));
                    }
                }
                if (callback != null)
                    callback.onSuccess(aBoolean);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 删除指定的一条或者一组消息。会同时删除本地和远端消息。
     * <p>请注意，此方法会删除远端消息，请慎重使用</p>
     *
     * @param conversationType 会话类型。暂时不支持聊天室
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、客服 Id。
     * @param messages         要删除的消息数组, 数组大小不能超过100条。
     * @param callback         是否删除成功的回调。
     */
    public void deleteRemoteMessages(final Conversation.ConversationType conversationType, final String targetId, final Message[] messages, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().deleteRemoteMessages(conversationType, targetId, messages, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                int[] messageIds = new int[messages.length];
                for (int i = 0; i < messages.length; i++) {
                    messageIds[i] = messages[i].getMessageId();
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onDeleteMessage(new DeleteEvent(conversationType, targetId, messageIds));
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
     * 断开连接(断开后继续接收 Push 消息)。
     */
    public void disconnect() {
        RongIMClient.getInstance().disconnect();
        RongExtensionManager.getInstance().disconnect();
    }

    /**
     * 下载文件。
     * <p/>
     * 用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param mediaType        文件类型。
     * @param imageUrl         文件的 URL 地址。
     * @param callback         下载文件的回调。
     */
    public void downloadMedia(Conversation.ConversationType conversationType, String targetId, RongIMClient.MediaType mediaType, String imageUrl, final RongIMClient.DownloadMediaCallback callback) {
        RongIMClient.getInstance().downloadMedia(conversationType, targetId, mediaType, imageUrl, callback);
    }


    /**
     * 下载文件
     * 支持断点续传
     *
     * @param uid      文件唯一标识
     * @param fileUrl  文件下载地址
     * @param fileName 文件名
     * @param path     文件下载保存目录，如果是 targetVersion 29 为目标，由于访问权限原因，建议使用 context.getExternalFilesDir() 方法保存到私有目录
     * @param callback 回调
     */
    public void downloadMediaFile(final String uid, String fileUrl, String fileName, String path, final IRongCallback.IDownloadMediaFileCallback callback) {
        RongIMClient.getInstance().downloadMediaFile(uid, fileUrl, fileName, path, new IRongCallback.IDownloadMediaFileCallback() {
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

    public void cancelDownloadMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelDownloadMediaMessage(message, callback);
    }


    /**
     * 清空所有会话及会话消息，回调方式通知是否清空成功。
     *
     * @param callback          是否清空成功的回调。
     * @param conversationTypes 会话类型。
     */
    public void clearConversations(final RongIMClient.ResultCallback callback, final Conversation.ConversationType... conversationTypes) {
        RongIMClient.getInstance().clearConversations(new RongIMClient.ResultCallback() {
            @Override
            public void onSuccess(Object o) {
                for (ConversationEventListener listener : mConversationEventListener) {
                    listener.onClearConversations(conversationTypes);
                }
                if (callback != null)
                    callback.onSuccess(o);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        }, conversationTypes);
    }

    /**
     * 下载文件
     * <p/>
     * 用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param message  文件消息。
     * @param callback 下载文件的回调。
     */
    public void downloadMediaMessage(Message message, final IRongCallback.IDownloadMediaMessageCallback callback) {
        for (MessageEventListener item : mMessageEventListeners) {
            item.onDownloadMessage(new DownloadEvent(DownloadEvent.START, message));
        }
        RongIMClient.getInstance().downloadMediaMessage(message, new IRongCallback.IDownloadMediaMessageCallback() {
            @Override
            public void onSuccess(Message message) {
                // 进度事件
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onDownloadMessage(new DownloadEvent(DownloadEvent.SUCCESS, message));
                }
                if (callback != null) {
                    callback.onSuccess(message);
                }
            }

            @Override
            public void onProgress(Message message, int progress) {
                // 进度事件
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onDownloadMessage(new DownloadEvent(DownloadEvent.PROGRESS, message, progress));
                }
                if (callback != null) {
                    callback.onProgress(message, progress);
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode code) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onDownloadMessage(new DownloadEvent(DownloadEvent.ERROR, message, code));
                }
                if (callback != null) {
                    callback.onError(message, code);
                }
            }

            @Override
            public void onCanceled(Message message) {
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onDownloadMessage(new DownloadEvent(DownloadEvent.CANCEL, message));
                }
                if (callback != null) {
                    callback.onCanceled(message);
                }
            }
        });
    }

    /**
     * 注销当前登录，执行该方法后不会再收到 push 消息。
     */
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
     * @param url      文件 Url
     * @param callback 回调
     * @group 数据获取
     */
    public void supportResumeBrokenTransfer(String url, final RongIMClient.ResultCallback<Boolean> callback) {
        RongIMClient.getInstance().supportResumeBrokenTransfer(url, new RongIMClient.ResultCallback<Boolean>() {
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
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 暂停下载多媒体文件时的回调
     */
    public void pauseDownloadMediaMessage(final Message message, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().pauseDownloadMediaMessage(message, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onDownloadMessage(new DownloadEvent(DownloadEvent.PAUSE, message));
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
     * 语音消息类型
     */
    public enum VoiceMessageType {
        /**
         * 普通音质语音消息
         */
        Ordinary,
        /**
         * 高音质语音消息
         */
        HighQuality
    }

    /**
     * <p>发送地理位置消息。并同时更新界面。</p>
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为 {@link LocationMessage}, 否则返回失败。</p>
     * <p>其中的缩略图地址 scheme 只支持 file:// 和 http:// 其他暂不支持。</p>
     *
     * @param message             消息实体。
     * @param pushContent         当下发 push 消息时，在通知栏里会显示这个字段。
     *                            如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                            如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg，则不需要填写，默认已经指定。
     * @param pushData            push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param sendMessageCallback 发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     */
    public void sendLocationMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMessageCallback sendMessageCallback) {
        if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSendMessage(message)) {
            RLog.d(TAG, "message has been intercepted.");
            return;
        }
        handleBeforeSend(message);
        RongIMClient.getInstance().sendLocationMessage(message, pushContent, pushData, new IRongCallback.ISendMessageCallback() {
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
                if (mMessageInterceptor != null && mMessageInterceptor.interceptOnSentMessage(message)) {
                    RLog.d(TAG, "message has been intercepted.");
                    return;
                }
                for (MessageEventListener item : mMessageEventListeners) {
                    item.onSendMessage(new SendEvent(SendEvent.SUCCESS, message));
                }
            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                filterSentMessage(message, errorCode, new FilterSentListener() {
                    @Override
                    public void onComplete() {

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
     * @param message  消息实体。要设置的发送状态包含在 message 中
     * @param callback 是否设置成功的回调。
     */
    public void setMessageSentStatus(final Message message, final RongIMClient.ResultCallback<Boolean> callback) {
        if (message == null || message.getMessageId() <= 0 || message.getSentStatus() == null) {
            RLog.e(TAG, "setMessageSentStatus message is null or messageId <= 0");
            if (callback != null) {
                callback.onError(RongIMClient.ErrorCode.PARAMETER_ERROR);
            }
            return;
        }
        RongIMClient.getInstance().setMessageSentStatus(message, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);

                if (bool) {
                    refreshMessage(message);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
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
                && message.getContent() != null && message.getContent().getUserInfo() == null) {
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
    private void filterSentMessage(Message message, RongIMClient.ErrorCode errorCode, final FilterSentListener listener) {

        if (errorCode != null && errorCode != RongIMClient.ErrorCode.RC_MSG_REPLACED_SENSITIVE_WORD) {

            if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION) || errorCode.equals(RongIMClient.ErrorCode.NOT_IN_GROUP)
                    || errorCode.equals(RongIMClient.ErrorCode.NOT_IN_CHATROOM) || errorCode.equals(RongIMClient.ErrorCode.REJECTED_BY_BLACKLIST) || errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_GROUP)
                    || errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_CHATROOM) || errorCode.equals(RongIMClient.ErrorCode.KICKED_FROM_CHATROOM)) {

                InformationNotificationMessage informationMessage = null;

                if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_DISCUSSION)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_not_in_discussion));
                } else if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_GROUP)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_not_in_group));
                } else if (errorCode.equals(RongIMClient.ErrorCode.NOT_IN_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_not_in_chatroom));
                } else if (errorCode.equals(RongIMClient.ErrorCode.REJECTED_BY_BLACKLIST)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_rejected_by_blacklist_prompt));
                } else if (errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_GROUP)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_forbidden_to_talk));
                } else if (errorCode.equals(RongIMClient.ErrorCode.FORBIDDEN_IN_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_forbidden_in_chatroom));
                } else if (errorCode.equals(RongIMClient.ErrorCode.KICKED_FROM_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_kicked_from_chatroom));
                }

                Message.ReceivedStatus
                        receivedStatus =
                        new io.rong.imlib.model.Message
                                .ReceivedStatus(0);
                insertIncomingMessage(message.getConversationType(), message.getTargetId(), message.getSenderUserId(), receivedStatus, informationMessage, null);
            }
        }

        MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
        if (RongConfigCenter.conversationConfig().rc_enable_resend_message && tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
            // 发送失败的消息存入重发列表
            ResendManager.getInstance().addResendMessage(message, errorCode, new ResendManager.AddResendMessageCallBack() {
                @Override
                public void onComplete(Message message, RongIMClient.ErrorCode errorCode) {
                    if (listener != null) {
                        ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
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
     * 取消发送多媒体文件。
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消发送多媒体文件时的回调。
     */
    public void cancelSendMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().cancelSendMediaMessage(message, callback);
    }

    public void addOnReceiveMessageListener(RongIMClient.OnReceiveMessageWrapperListener listener) {
        mOnReceiveMessageObserverList.add(listener);
    }

    public void removeOnReceiveMessageListener(RongIMClient.OnReceiveMessageWrapperListener listener) {
        if (listener != null) {
            mOnReceiveMessageObserverList.remove(listener);
        }
    }

    public void addSyncConversationReadStatusListener(RongIMClient.SyncConversationReadStatusListener listener) {
        mSyncConversationReadStatusListeners.add(listener);
    }

    public void removeSyncConversationReadStatusListeners(RongIMClient.SyncConversationReadStatusListener listener) {
        mSyncConversationReadStatusListeners.remove(listener);
    }

    public void addConnectStatusListener(RongIMClient.ConnectCallback callback) {
        mConnectStatusListener.add(callback);
    }

    public void removeConnectStatusListener(RongIMClient.ConnectCallback callback) {
        mConnectStatusListener.remove(callback);
    }

    /**
     * 设置连接状态变化的监听器。
     * <p>
     * <strong>
     * 当回调状态为{@link RongIMClient.ConnectionStatusListener.ConnectionStatus#TOKEN_INCORRECT},
     * 需要获取正确的token, 并主动调用{@link RongIM#connect(String, int, RongIMClient.ConnectCallback)}
     * </strong>
     *
     * @param listener 连接状态变化的监听器。
     */
    public void addConnectionStatusListener(RongIMClient.ConnectionStatusListener listener) {
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

    /**
     * 连接状态变化的监听器。
     */
    private RongIMClient.ConnectionStatusListener mConnectionStatusListener = new RongIMClient.ConnectionStatusListener() {
        @Override
        public void onChanged(ConnectionStatus connectionStatus) {
            for (RongIMClient.ConnectionStatusListener listener : mConnectionStatusObserverList) {
                listener.onChanged(connectionStatus);
            }
        }
    };

    /**
     * 接受消息的事件监听器。
     */
    private RongIMClient.OnReceiveMessageWrapperListener mOnReceiveMessageListener = new RongIMClient.OnReceiveMessageWrapperListener() {
        @Override
        public boolean onReceived(final Message message, final int left, final boolean hasPackage, final boolean offline) {
            ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    if (mMessageInterceptor != null && mMessageInterceptor.interceptReceivedMessage(message, left, hasPackage, offline)) {
                        RLog.d(TAG, "message has been intercepted.");
                        return;
                    }
                    for (RongIMClient.OnReceiveMessageWrapperListener observer : mOnReceiveMessageObserverList) {
                        observer.onReceived(message, left, hasPackage, offline);
                    }
                }
            });
            return false;
        }
    };
    /**
     * 会话状态监听器。当会话的置顶或者免打扰状态更改时，会回调此方法。
     */
    private RongIMClient.ConversationStatusListener mConversationStatusListener = new RongIMClient.ConversationStatusListener() {
        @Override
        public void onStatusChanged(ConversationStatus[] conversationStatuses) {
            for (RongIMClient.ConversationStatusListener listener : mConversationStatusObserverList) {
                listener.onStatusChanged(conversationStatuses);
            }
        }
    };

    /**
     * 消息被撤回时的监听器。
     */
    private RongIMClient.OnRecallMessageListener mOnRecallMessageListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            for (RongIMClient.OnRecallMessageListener listener : mOnRecallMessageObserverList) {
                listener.onMessageRecalled(message, recallNotificationMessage);
            }
            return false;
        }
    };

    /**
     * 消息回执监听器
     */
    private RongIMClient.ReadReceiptListener mReadReceiptListener = new RongIMClient.ReadReceiptListener() {
        /**
         * 单聊中收到消息回执的回调。
         *
         * @param message 封装了 {@link ReadReceiptMessage}
         */
        @Override
        public void onReadReceiptReceived(final Message message) {
            ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (RongIMClient.ReadReceiptListener listener : mReadReceiptObserverList) {
                        listener.onReadReceiptReceived(message);
                    }
                }
            });

        }


        /**
         * 群组和讨论组中，某人发起了回执请求，会话中其余人会收到该请求，并回调此方法。
         * <p>
         * 接收方需要在合适的时机（读取了消息之后）调用 {@link RongIMClient#sendReadReceiptResponse(Conversation.ConversationType, String, List, RongIMClient.OperationCallback)} 回复响应。
         *
         * @param conversationType       会话类型
         * @param targetId   会话目标 id
         * @param messageUId 请求已读回执的消息 uId
         */
        @Override
        public void onMessageReceiptRequest(final Conversation.ConversationType conversationType, final String targetId, final String messageUId) {
            ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (RongIMClient.ReadReceiptListener listener : mReadReceiptObserverList) {
                        listener.onMessageReceiptRequest(conversationType, targetId, messageUId);
                    }
                }
            });
        }

        /**
         * 在群组和讨论组中发起了回执请求的用户，当收到接收方的响应时，会回调此方法。
         *
         * @param conversationType              会话类型
         * @param targetId          会话 id
         * @param messageUId        收到回执响应的消息的 uId
         * @param respondUserIdList 会话中响应了此消息的用户列表。其中 key： 用户 id ； value： 响应时间。
         */
        @Override
        public void onMessageReceiptResponse(final Conversation.ConversationType conversationType, final String targetId, final String messageUId, final HashMap<String, Long> respondUserIdList) {
            ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
                @Override
                public void run() {
                    for (RongIMClient.ReadReceiptListener listener : mReadReceiptObserverList) {
                        listener.onMessageReceiptResponse(conversationType, targetId, messageUId, respondUserIdList);
                    }
                }
            });
        }
    };

    /**
     * 阅后即焚事件监听器。
     */
    private RongIMClient.OnReceiveDestructionMessageListener mOnReceiveDestructMessageListener = new RongIMClient.OnReceiveDestructionMessageListener() {
        @Override
        public void onReceive(Message message) {
            for (MessageEventListener item : mMessageEventListeners) {
                item.onDeleteMessage(new DeleteEvent(message.getConversationType(), message.getTargetId(), new int[]{message.getMessageId()}));
            }
        }
    };

    private RongIMClient.SyncConversationReadStatusListener mSyncConversationReadStatusListener = new RongIMClient.SyncConversationReadStatusListener() {
        @Override
        public void onSyncConversationReadStatus(Conversation.ConversationType type, String targetId) {
            for (RongIMClient.SyncConversationReadStatusListener item : mSyncConversationReadStatusListeners) {
                item.onSyncConversationReadStatus(type, targetId);
            }
        }
    };

    private RongIMClient.TypingStatusListener mTypingStatusListener = new RongIMClient.TypingStatusListener() {
        @Override
        public void onTypingStatusChanged(Conversation.ConversationType type, String targetId, Collection<TypingStatus> typingStatusSet) {
            for (RongIMClient.TypingStatusListener item : mTypingStatusListeners) {
                item.onTypingStatusChanged(type, targetId, typingStatusSet);
            }
        }
    };

}
