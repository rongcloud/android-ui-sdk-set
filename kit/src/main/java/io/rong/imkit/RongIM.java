package io.rong.imkit;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.config.ConversationClickListener;
import io.rong.imkit.config.ConversationListBehaviorListener;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.provider.IMessageProvider;
import io.rong.imkit.conversationlist.provider.BaseConversationProvider;
import io.rong.imkit.feature.location.LocationManager;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.feature.publicservice.IPublicServiceMenuClickListener;
import io.rong.imkit.feature.publicservice.PublicServiceManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.manager.UnReadMessageManager;
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.UserDataProvider;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.cs.model.CSCustomServiceInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.RemoteHistoryMsgOption;
import io.rong.imlib.model.SendMessageOption;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.publicservice.model.PublicServiceProfile;
import io.rong.imlib.publicservice.model.PublicServiceProfileList;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.TextMessage;

public class RongIM {
    private final static String TAG = RongIM.class.getSimpleName();
    static RongIMClient.OnReceiveMessageListener sMessageListener;

    private RongIM() {

    }

    private static class SingletonHolder {
        static RongIM sInstance = new RongIM();
    }

    public static RongIM getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 将某个用户加到黑名单中。
     * <p>当你把对方加入黑名单后，对方再发消息时，就会提示“已被加入黑名单，消息发送失败”。
     * 但你依然可以发消息个对方。</p>
     *
     * @param userId   用户 Id。
     * @param callback 加到黑名单回调。
     */
    public void addToBlacklist(final String userId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().addToBlacklist(userId, callback);
    }

    /**
     * 设置未读消息数变化监听器。
     * 注意:如果是在 activity 中设置,那么要在 activity 销毁时,
     * 调用 {@link UnReadMessageManager#removeObserver(UnReadMessageManager.IUnReadMessageObserver)}
     * 否则会造成内存泄漏。
     *
     * @param observer          接收未读消息消息的监听器。
     * @param conversationTypes 接收未读消息的会话类型。
     */
    public void addUnReadMessageCountChangedObserver(final UnReadMessageManager.IUnReadMessageObserver observer, Conversation.ConversationType... conversationTypes) {
        if (observer == null || conversationTypes == null || conversationTypes.length == 0) {
            io.rong.common.rlog.RLog.w(TAG, "addOnReceiveUnreadCountChangedListener Illegal argument");
            throw new IllegalArgumentException("observer must not be null and must include at least one conversationType");
        }
        UnReadMessageManager.getInstance().addObserver(conversationTypes, observer);
    }

    /**
     * 取消下载多媒体文件。
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消下载多媒体文件时的回调。
     */
    public void cancelDownloadMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().cancelDownloadMediaMessage(message, callback);
    }

    /**
     * 取消发送多媒体文件。
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 取消发送多媒体文件时的回调。
     */
    public void cancelSendMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().cancelSendMediaMessage(message, callback);
    }

    /**
     * 清空所有会话及会话消息，回调方式通知是否清空成功。
     *
     * @param callback          是否清空成功的回调。
     * @param conversationTypes 会话类型。
     */
    public void clearConversations(final RongIMClient.ResultCallback callback, final Conversation.ConversationType... conversationTypes) {
        IMCenter.getInstance().clearConversations(callback, conversationTypes);
    }

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调方式获取清空是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清空是否成功的回调。
     */
    public void clearMessages(Conversation.ConversationType conversationType, String targetId, RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().clearMessages(conversationType, targetId, callback);
    }


    /**
     * 根据会话类型，清除目标 Id 的消息未读状态，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清除是否成功的回调。
     */
    public void clearMessagesUnreadStatus(final Conversation.ConversationType conversationType, final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().clearMessagesUnreadStatus(conversationType, targetId, callback);
    }

    /**
     * 清除某一会话的文字消息草稿，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         是否清除成功的回调。
     */
    public void clearTextMessageDraft(Conversation.ConversationType conversationType, String targetId, RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * <p>初始化 SDK，在整个应用程序全局只需要调用一次, 建议在 Application 继承类中调用。
     * 调用此接口传入 AppKey 与在 AndroidManifest.xml 里写入 RONG_CLOUD_APP_KEY 是同样效果，二选一即可。</p>
     *
     * @param application 应用
     * @param appKey      融云注册应用的AppKey。
     */
    public static void init(Application application, String appKey) {
        IMCenter.init(application, appKey, true);
    }

    /**
     * <p>初始化 SDK，在整个应用程序全局只需要调用一次, 建议在 Application 继承类中调用。
     * 调用此接口传入 AppKey 与在 AndroidManifest.xml 里写入 RONG_CLOUD_APP_KEY 是同样效果，二选一即可。</p>
     *
     * @param application 应用
     * @param appKey      融云注册应用的AppKey。
     * @param enablePush  是否使用推送功能。false 代表不使用推送相关功能, SDK 里将不会携带推送相关文件。
     */
    public static void init(Application application, String appKey, boolean enablePush) {
        IMCenter.init(application, appKey, enablePush);
    }


    /**
     * 向本地会话中插入一条消息，方向为接收。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param type           会话类型。
     * @param targetId       目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param senderUserId   发送方 Id
     * @param receivedStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content        消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback       获得消息发送实体的回调。
     */
    public void insertIncomingMessage(Conversation.ConversationType type, String targetId,
                                      String senderUserId, Message.ReceivedStatus receivedStatus,
                                      MessageContent content, long sentTime,
                                      final RongIMClient.ResultCallback<Message> callback) {
        IMCenter.getInstance().insertIncomingMessage(type, targetId, senderUserId, receivedStatus, content, sentTime, callback);
    }

    /**
     * 向本地会话中插入一条消息，方向为接收。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param type           会话类型。
     * @param targetId       目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param senderUserId   发送方 Id
     * @param receivedStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content        消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback       获得消息发送实体的回调。
     */
    public void insertIncomingMessage(Conversation.ConversationType type, String targetId,
                                      String senderUserId, Message.ReceivedStatus receivedStatus,
                                      MessageContent content, RongIMClient.ResultCallback<Message> callback) {
        insertIncomingMessage(type, targetId, senderUserId, receivedStatus, content, System.currentTimeMillis(), callback);
    }


    /**
     * 向本地会话中插入一条消息，方向为发送。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param type       会话类型。
     * @param targetId   目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param sentStatus 接收状态 @see {@link Message.ReceivedStatus}
     * @param content    消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback   获得消息发送实体的回调。
     */
    public void insertOutgoingMessage(Conversation.ConversationType type, String targetId,
                                      Message.SentStatus sentStatus, MessageContent content,
                                      RongIMClient.ResultCallback<Message> callback) {
        insertOutgoingMessage(type, targetId, sentStatus, content, System.currentTimeMillis(), callback);
    }

    /**
     * 向本地会话中插入一条消息，方向为发送。这条消息只是插入本地会话，不会实际发送给服务器和对方。
     * 插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link RongIMClient.ErrorCode#PARAMETER_ERROR}
     *
     * @param type       会话类型。
     * @param targetId   目标会话Id。比如私人会话时，是对方的id； 群组会话时，是群id; 讨论组会话时，则为该讨论,组的id.
     * @param sentStatus 发送状态 @see {@link Message.SentStatus}
     * @param content    消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback   获得消息发送实体的回调。
     */
    public void insertOutgoingMessage(Conversation.ConversationType type, String targetId,
                                      Message.SentStatus sentStatus, MessageContent content,
                                      long sentTime, final RongIMClient.ResultCallback<Message> callback) {
        IMCenter.getInstance().insertOutgoingMessage(type, targetId, sentStatus, content, sentTime, callback);
    }

    /**
     * 加入聊天室。
     * <p>如果聊天室不存在，sdk 会创建聊天室并加入，如果已存在，则直接加入</p>
     * <p>加入聊天室时，可以选择拉取聊天室消息数目。</p>
     *
     * @param chatroomId      聊天室 Id。
     * @param defMessageCount 进入聊天室拉取消息数目，-1 时不拉取任何消息，0 时拉取 10 条消息，最多只能拉取 50 条。（加入聊天室时会传本地最后一条消息的时间戳，拉取的是这个时间戳之后的消息。比如：这个时间戳之后有3条消息，defMessageCount传10，也只能拉到3条消息。）
     * @param callback        状态回调。
     */
    public void joinChatRoom(final String chatroomId, final int defMessageCount, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().joinChatRoom(chatroomId, defMessageCount, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                if (callback != null) {
                    callback.onError(RongIMClient.ErrorCode.valueOf(coreErrorCode.getValue()));
                }
            }
        });
    }

    /**
     * 加入已存在的聊天室。
     * <p>如果聊天室不存在，则加入失败</p>
     * <p>加入聊天室时，可以选择拉取聊天室消息数目。</p>
     *
     * @param chatroomId      聊天室 Id。
     * @param defMessageCount 进入聊天室拉取消息数目，-1 时不拉取任何消息，0 时拉取 10 条消息，最多只能拉取 40 条。
     * @param callback        状态回调。
     */
    public void joinExistChatRoom(final String chatroomId, final int defMessageCount, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().joinExistChatRoom(chatroomId, defMessageCount, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                if (callback != null) {
                    callback.onError(RongIMClient.ErrorCode.valueOf(coreErrorCode.getValue()));
                }
            }
        });
    }

    /**
     * 注销当前登录，执行该方法后不会再收到 push 消息。
     */
    public void logout() {
        IMCenter.getInstance().logout();
    }


    /**
     * 暂停下载多媒体文件
     *
     * @param message  包含多媒体文件的消息，即{@link MessageContent}为 FileMessage, ImageMessage 等。
     * @param callback 暂停下载多媒体文件时的回调
     */
    public void pauseDownloadMediaMessage(Message message, RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().pauseDownloadMediaMessage(message, callback);
    }

    /**
     * 退出聊天室。
     *
     * @param chatroomId 聊天室 Id。
     * @param callback   状态回调。
     */
    public void quitChatRoom(final String chatroomId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().quitChatRoom(chatroomId, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                if (callback != null) {
                    callback.onError(RongIMClient.ErrorCode.valueOf(coreErrorCode.getValue()));
                }
            }
        });
    }

    /**
     * 撤回消息
     *
     * @param message     将被撤回的消息
     * @param pushContent 被撤回时，通知栏显示的信息
     */
    public void recallMessage(final Message message, String pushContent) {
        IMCenter.getInstance().recallMessage(message, pushContent, null);
    }

    /**
     * 刷新、更改群组用户缓存数据。
     *
     * @param groupUserInfo 需要更新的群组用户缓存数据。
     */
    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        RongUserInfoManager.getInstance().refreshGroupUserInfoCache(groupUserInfo);
    }


    /**
     * 刷新公众服务账号缓存数据。
     *
     * @param publicServiceProfile 需要更新的公众服务账号缓存数据。
     */
    public void refreshPublicServiceProfile(PublicServiceProfile publicServiceProfile) {
        PublicServiceManager.getInstance().refreshPublicServiceProfile(publicServiceProfile);
    }

    /**
     * 连接服务器，在整个应用程序全局，只需要调用一次。
     *
     * @param token           从服务端获取的 <a
     *                        href="http://docs.rongcloud.cn/android#token">用户身份令牌（
     *                        Token）</a>。
     * @param connectCallback 连接服务器的回调扩展类，新增打开数据库的回调，用户可以在此回调中执行拉取会话列表操作。
     * @return RongIM IM 客户端核心类的实例。
     * @discussion 调用该接口，SDK 会在连接失败之后尝试重连，将出现以下两种情况：
     * 第一、连接成功，回调 onSuccess(userId)。
     * 第二、出现 SDK 无法处理的错误，回调 onError(errorCode)（如 token 非法），并不再重连。
     * <p>
     * 如果您不想一直进行重连，可以使用 connect(String,int,ConnectCallback) 接口并设置连接超时时间 timeLimit。
     * @discussion 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     */
    public static void connect(final String token, final RongIMClient.ConnectCallback connectCallback) {
        connect(token, -1, connectCallback);
    }

    /**
     * 连接服务器，在整个应用程序全局，只需要调用一次。
     *
     * @param token           从服务端获取的 <a
     *                        href="http://docs.rongcloud.cn/android#token">用户身份令牌（
     *                        Token）</a>。
     * @param timeLimit       连接超时时间，单位：秒。timeLimit <= 0，则 IM 将一直连接，直到连接成功或者无法连接（如 token 非法）
     *                        timeLimit > 0 ,则 IM 将最多连接 timeLimit 秒：
     *                        如果在 timeLimit 秒内连接成功，后面再发生了网络变化或前后台切换，SDK 会自动重连；
     *                        如果在 timeLimit 秒无法连接成功则不再进行重连，通过 onError 告知连接超时，您需要再自行调用 connect 接口
     * @param connectCallback 连接服务器的回调扩展类，新增打开数据库的回调，用户可以在此回调中执行拉取会话列表操作。
     * @return RongIM IM 客户端核心类的实例。
     * @discussion 调用该接口，SDK 会在 timeLimit 秒内尝试重连，直到出现下面三种情况之一：
     * 第一、连接成功，回调 onSuccess(userId)。
     * 第二、超时，回调 onError(RC_CONNECT_TIMEOUT)，并不再重连。
     * 第三、出现 SDK 无法处理的错误，回调 onError(errorCode)（如 token 非法），并不再重连。
     * @discussion 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     */
    public static void connect(String token, int timeLimit,
                               final RongIMClient.ConnectCallback connectCallback) {
        IMCenter.getInstance().connect(token, timeLimit, connectCallback);
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
    public void deleteMessages(final Conversation.ConversationType conversationType,
                               final String targetId, final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().deleteMessages(conversationType, targetId, callback);
    }

    /**
     * 删除指定的一条或者一组消息，回调方式获取是否删除成功。
     *
     * @param messageIds 要删除的消息 Id 数组。
     * @param callback   是否删除成功的回调。
     */
    public void deleteMessages(Conversation.ConversationType conversationType, String targetId,
                               int[] messageIds, final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().deleteMessages(conversationType, targetId, messageIds, callback);
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
    public void deleteRemoteMessages(Conversation.ConversationType conversationType, String
            targetId, final Message[] messages, final RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().deleteRemoteMessages(conversationType, targetId, messages, callback);
    }

    /**
     * 断开连接(断开后继续接收 Push 消息)。
     */
    public void disconnect() {
        IMCenter.getInstance().disconnect();
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
    public void downloadMedia(Conversation.ConversationType conversationType, String
            targetId, RongIMClient.MediaType mediaType, String imageUrl,
                              final RongIMClient.DownloadMediaCallback callback) {
        IMCenter.getInstance().downloadMedia(conversationType, targetId, mediaType, imageUrl, callback);
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
    public void downloadMediaFile(final String uid, String fileUrl, String fileName, String
            path, final IRongCallback.IDownloadMediaFileCallback callback) {
        IMCenter.getInstance().downloadMediaFile(uid, fileUrl, fileName, path, callback);
    }

    /**
     * 下载文件。和{@link #downloadMedia(Conversation.ConversationType, String, RongIMClient.MediaType, String, RongIMClient.DownloadMediaCallback)}的区别是，该方法支持取消操作。
     * <p/>
     * 用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param message  文件消息。
     * @param callback 下载文件的回调。
     */
    public void downloadMediaMessage(Message message,
                                     final IRongCallback.IDownloadMediaMessageCallback callback) {
        IMCenter.getInstance().downloadMediaMessage(message, callback);
    }


    /**
     * 设置会话界面未读新消息是否展示 注:未读新消息大于1条即展示
     * 目前仅支持单群聊
     *
     * @param state true 展示，false 不展示。
     */
    public void enableNewComingMessageIcon(boolean state) {
        RongConfigCenter.conversationConfig().setShowNewMessageBar(state);
    }

    /**
     * 设置会话界面历史消息是否展示 注:历史消息大于10条即展示
     * 目前仅支持单群聊
     *
     * @param state true 展示，false 不展示。
     */
    public void enableUnreadMessageIcon(boolean state) {
        RongConfigCenter.conversationConfig().setShowHistoryMessageBar(state);
    }

    /**
     * 注册消息模板。
     *
     * @param provider 模板类型。
     */
    public static void registerMessageTemplate(IMessageProvider provider) {
        RongConfigCenter.conversationConfig().addMessageProvider(provider);
    }

    /**
     * 设置接收消息的监听器。
     * <p/>
     * 所有接收到的消息、通知、状态都经由此处设置的监听器处理。包括私聊消息、讨论组消息、群组消息、聊天室消息以及各种状态。
     *
     * @param listener 接收消息的监听器。
     */
    public static void addOnReceiveMessageListener(RongIMClient.OnReceiveMessageWrapperListener
                                                           listener) {
        IMCenter.getInstance().addOnReceiveMessageListener(listener);
    }

    /**
     * 移除接收消息的监听器。
     *
     * @param listener 接收消息的监听器。
     */
    public static void removeOnReceiveMessageListener(RongIMClient.OnReceiveMessageWrapperListener
                                                              listener) {
        IMCenter.getInstance().removeOnReceiveMessageListener(listener);
    }

    /**
     * 设置连接状态变化的监听器。
     *
     * @param listener 连接状态变化的监听器。
     */
    public static void setConnectionStatusListener(
            final RongIMClient.ConnectionStatusListener listener) {
        IMCenter.getInstance().addConnectionStatusListener(listener);
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
     * 设置会话消息提醒状态。
     *
     * @param conversationType   会话类型。
     * @param targetId           目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param notificationStatus 是否屏蔽。
     * @param callback           设置状态的回调。
     */
    public void setConversationNotificationStatus(final Conversation.ConversationType conversationType, final String targetId, final Conversation.ConversationNotificationStatus notificationStatus, final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus> callback) {
        IMCenter.getInstance().setConversationNotificationStatus(conversationType, targetId, notificationStatus, callback);
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
        IMCenter.getInstance().setConversationToTop(type, id, isTop, needCreate, callback);
    }

    /**
     * 启动单聊界面。
     *
     * @param context      应用上下文。
     * @param targetUserId 要与之聊天的用户 Id。
     * @param title        聊天的标题。开发者需要在聊天界面获取该值, 再手动设置为聊天界面的标题。
     */
    public void startPrivateChat(Context context, String targetUserId, String title) {
        startConversation(context, Conversation.ConversationType.PRIVATE, targetUserId, title);
    }

    /**
     * 启动群组聊天界面。
     *
     * @param context       应用上下文。
     * @param targetGroupId 要聊天的群组 Id。
     * @param title         聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     */
    public void startGroupChat(Context context, String targetGroupId, String title) {
        startConversation(context, Conversation.ConversationType.GROUP, targetGroupId, title);
    }

    /**
     * <p>启动会话界面。</p>
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title            聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     */
    public void startConversation(Context context, Conversation.ConversationType
            conversationType, String targetId, String title) {
        startConversation(context, conversationType, targetId, title, 0);
    }

    /**
     * <p>启动会话界面，并跳转到指定的消息位置</p>
     * <p>使用时，可以传入多种会话类型 {@link Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。
     * 如果传入的是 {@link Conversation.ConversationType#CHATROOM}，sdk 会默认调用
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)} 加入聊天室。
     * </p>
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title            聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     * @param fixedMsgSentTime 需要定位的消息发送时间
     */
    public void startConversation(Context context, Conversation.ConversationType
            conversationType, String targetId, String title, long fixedMsgSentTime) {
        if (context == null || TextUtils.isEmpty(targetId) || conversationType == null) {
            RLog.e(TAG, "startConversation. context, targetId or conversationType can not be empty!!!");
            return;
        }
        Bundle bundle = new Bundle();
        if (!TextUtils.isEmpty(title)) {
            bundle.putString(RouteUtils.TITLE, title);
            bundle.putLong(RouteUtils.INDEX_MESSAGE_TIME, fixedMsgSentTime);
        }
        RouteUtils.routeToConversationActivity(context, conversationType, targetId, bundle);
    }

    /**
     * 根据不同会话类型的目标Id，回调方式获取某一会话信息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 获取会话信息的回调。
     */
    public void getConversation(Conversation.ConversationType type, String
            targetId, RongIMClient.ResultCallback<Conversation> callback) {
        RongIMClient.getInstance().getConversation(type, targetId, callback);
    }

    /**
     * 获取会话列表。
     *
     * @param callback 会话列表数据回调。
     *                 Conversation。
     */
    public void getConversationList
    (RongIMClient.ResultCallback<List<Conversation>> callback) {
        RongIMClient.getInstance().getConversationList(callback);
    }

    /**
     * 根据会话类型，回调方式获取会话列表。
     *
     * @param callback 获取会话列表的回调。
     * @param types    会话类型。
     */
    public void getConversationList
    (RongIMClient.ResultCallback<List<Conversation>> callback, Conversation.ConversationType...
            types) {
        RongIMClient.getInstance().getConversationList(callback, types);
    }

    /**
     * 获取会话消息提醒状态。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         获取状态的回调。
     */
    public void getConversationNotificationStatus(
            final Conversation.ConversationType conversationType, final String targetId,
            final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus> callback) {
        RongIMClient.getInstance().getConversationNotificationStatus(conversationType, targetId, callback);
    }

    /**
     * 获取当前用户的黑名单列表。
     *
     * @param callback 获取黑名单回调。
     */
    public void getBlacklist(RongIMClient.GetBlacklistCallback callback) {
        RongIMClient.getInstance().getBlacklist(callback);
    }

    /**
     * 获取某用户是否在黑名单中。
     *
     * @param userId   用户 Id。
     * @param callback 获取用户是否在黑名单回调。
     */
    public void getBlacklistStatus(String
                                           userId, RongIMClient.ResultCallback<RongIMClient.BlacklistStatus> callback) {
        RongIMClient.getInstance().getBlacklistStatus(userId, callback);
    }

    /**
     * 获取连接状态。
     *
     * @return 连接状态枚举。
     */
    public RongIMClient.ConnectionStatusListener.ConnectionStatus getCurrentConnectionStatus() {
        return IMCenter.getInstance().getCurrentConnectionStatus();
    }

    /**
     * 获取本地时间与服务器时间的差值。
     * 消息发送成功后，sdk 会与服务器同步时间，消息所在数据库中存储的时间就是服务器时间。
     *
     * @return 本地时间与服务器时间的差值。
     */
    public long getDeltaTime() {
        return RongIMClient.getInstance().getDeltaTime();
    }


    /**
     * 根据会话类型的目标 Id，回调方式获取N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息，没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getHistoryMessages(Conversation.ConversationType conversationType, String
            targetId, int oldestMessageId, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, oldestMessageId, count, callback);
    }

    /**
     * 根据会话类型的目标 Id，回调方式获取某消息类型标识的N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 。
     * @param objectName       消息类型标识。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息,没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getHistoryMessages(Conversation.ConversationType conversationType, String
            targetId, String objectName, int oldestMessageId, int count, RongIMClient.
                                           ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getHistoryMessages(conversationType, targetId, objectName, oldestMessageId, count, callback);
    }


    /**
     * 根据会话类型的目标 Id，回调方式获取最新的 N 条消息记录。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param count            要获取的消息数量。
     * @param callback         获取最新消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getLatestMessages(Conversation.ConversationType conversationType, String
            targetId, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getLatestMessages(conversationType, targetId, count, callback);
    }


    /**
     * 获取己关注公共账号列表。
     *
     * @param callback 获取己关注公共账号列表回调。
     */
    public void getPublicServiceList(final RongIMClient.ResultCallback<PublicServiceProfileList> callback) {
        RongIMClient.getInstance().getPublicServiceList(new RongIMClient.ResultCallback<PublicServiceProfileList>() {
            @Override
            public void onSuccess(PublicServiceProfileList publicServiceProfileList) {
                if (callback != null) {
                    callback.onSuccess(publicServiceProfileList);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError(RongIMClient.ErrorCode.valueOf(e.getValue()));
                }
            }
        });
    }

    /**
     * 获取公众服务信息。
     *
     * @param publicServiceType 会话类型，APP_PUBLIC_SERVICE 或者 PUBLIC_SERVICE。
     * @param publicServiceId   公众服务 Id。
     * @param callback          获取公众号信息回调。
     */
    public void getPublicServiceProfile(Conversation.PublicServiceType
                                                publicServiceType, String
                                                publicServiceId, final RongIMClient.ResultCallback<PublicServiceProfile> callback) {
        RongIMClient.getInstance().getPublicServiceProfile(publicServiceType, publicServiceId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
            @Override
            public void onSuccess(PublicServiceProfile publicServiceProfile) {
                if (callback != null) {
                    callback.onSuccess(publicServiceProfile);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError(RongIMClient.ErrorCode.valueOf(e.getValue()));
                }
            }
        });
    }


    /**
     * 根据会话类型和目标 Id，获取 N 条远端历史消息记录。
     * <p>该方法只支持拉取指定时间之前的远端历史消息</p>
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param dataTime         从该时间点开始获取消息。即：消息中的 sendTime；第一次可传 0，获取最新 count 条。
     * @param count            要获取的消息数量，最多 20 条。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String
            targetId, long dataTime, int count, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getRemoteHistoryMessages(conversationType, targetId, dataTime, count, callback);
    }

    /**
     * 根据会话类型和目标 Id，拉取某时间戳之前或之后的 N 条远端历史消息记录。
     * <p>该方法支持拉取指定时间之前或之后的远端历史消息</p>
     *
     * @param conversationType       会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId               目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param remoteHistoryMsgOption {@link RemoteHistoryMsgOption}
     * @param callback               获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    public void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String
            targetId, RemoteHistoryMsgOption
                                                 remoteHistoryMsgOption, RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getRemoteHistoryMessages(conversationType, targetId, remoteHistoryMsgOption, callback);
    }

    /**
     * 根据会话类型，获取某一会话的文字消息草稿。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         获取草稿文字内容的回调。
     */
    public void getTextMessageDraft(Conversation.ConversationType conversationType, String
            targetId, RongIMClient.ResultCallback<String> callback) {
        RongIMClient.getInstance().getTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 根据会话类型的目标 Id,回调方式获取来自某用户（某会话）的未读消息数。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         未读消息数的回调
     */
    public void getUnreadCount(Conversation.ConversationType conversationType, String
            targetId, RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationType, targetId, callback);
    }

    /**
     * 回调方式获取某会话类型的未读消息数。
     *
     * @param callback          未读消息数的回调。
     * @param conversationTypes 会话类型。
     */
    public void getUnreadCount
    (RongIMClient.ResultCallback<Integer> callback, Conversation.ConversationType...
            conversationTypes) {
        RongIMClient.getInstance().getUnreadCount(callback, conversationTypes);
    }


    /**
     * 回调方式获取某会话类型的未读消息数。可选择包含或者不包含消息免打扰的未读消息数。
     *
     * @param conversationTypes 会话类型。
     * @param containBlocked    是否包含消息免打扰的未读消息数，true 包含， false 不包含
     * @param callback          未读消息数的回调。
     */
    public void getUnreadCount(Conversation.ConversationType[] conversationTypes,
                               boolean containBlocked, RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationTypes, containBlocked, callback);
    }


    /**
     * 根据会话类型数组，回调方式获取某会话类型的未读消息数。
     *
     * @param conversationTypes 会话类型。
     * @param callback          未读消息数的回调。
     */
    public void getUnreadCount(Conversation.ConversationType[]
                                       conversationTypes, RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationTypes, callback);
    }

    /**
     * <p>
     * 设置用户信息的提供者，供 RongIM 调用获取用户名称和头像信息。
     * 设置后，当 sdk 界面展示用户信息时，会回调 {@link UserDataProvider.UserInfoProvider#getUserInfo(String)}
     * 使用者只需要根据对应的 userId 提供对应的用户信息。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshUserInfoCache(UserInfo)} 刷新用户信息。
     * </p>
     *
     * @param userInfoProvider 用户信息提供者 {@link UserDataProvider.UserInfoProvider}。
     * @param isCacheUserInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                         如果 App 提供的 UserInfoProvider。
     *                         每次都需要通过网络请求用户数据，而不是将用户数据缓存到本地，会影响用户信息的加载速度；<br>
     *                         此时最好将本参数设置为 true，由 IMKit 来缓存用户信息。
     */
    public static void setUserInfoProvider(UserDataProvider.UserInfoProvider userInfoProvider,
                                           boolean isCacheUserInfo) {
        RongUserInfoManager.getInstance().setUserInfoProvider(userInfoProvider, isCacheUserInfo);
    }

    /**
     * 刷新用户缓存数据。
     *
     * @param userInfo 需要更新的用户缓存数据。
     */
    public void refreshUserInfoCache(UserInfo userInfo) {
        RongUserInfoManager.getInstance().refreshUserInfoCache(userInfo);
    }

    /**
     * 刷新群组缓存数据。
     *
     * @param group 需要更新的群组缓存数据。
     */
    public void refreshGroupInfoCache(Group group) {
        RongUserInfoManager.getInstance().refreshGroupInfoCache(group);
    }

    /**
     * 注册会话列表消息模板提供者。
     *
     * @param provider 会话列表模板提供者。
     */
    public void registerConversationTemplate(BaseConversationProvider provider) {
        RongConfigCenter.conversationListConfig().getProviderManager().addProvider(provider);
    }

    /**
     * 设置会话通知免打扰时间。
     *
     * @param startTime   起始时间 格式 HH:MM:SS。
     * @param spanMinutes 间隔分钟数大于 0 小于 1440。
     * @param callback    设置会话通知免打扰时间回调。
     */
    public void setNotificationQuietHours(final String startTime, final int spanMinutes,
                                          final RongIMClient.OperationCallback callback) {
        RongNotificationManager.getInstance().setNotificationQuietHours(startTime, spanMinutes, callback);
    }

    /**
     * 获取会话通知免打扰时间。
     *
     * @param callback 获取会话通知免打扰时间回调。
     */
    public void getNotificationQuietHours(
            final RongIMClient.GetNotificationQuietHoursCallback callback) {
        RongNotificationManager.getInstance().getNotificationQuietHours(callback);
    }


    /**
     * 移除会话通知免打扰时间。
     *
     * @param callback 移除会话通知免打扰时间回调。
     */
    public void removeNotificationQuietHours(final RongIMClient.OperationCallback callback) {
        RongNotificationManager.getInstance().removeNotificationQuietHours(callback);
    }

    /**
     * 注销已注册的未读消息数变化监听器。
     *
     * @param observer 接收未读消息消息的监听器。
     */
    public void removeUnReadMessageCountChangedObserver(final UnReadMessageManager.IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        UnReadMessageManager.getInstance().removeObserver(observer);
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
        IMCenter.getInstance().sendDirectionalMediaMessage(message, userIds, pushContent, pushData, callback);
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
        IMCenter.getInstance().sendDirectionalMessage(type, targetId, content, userIds, pushContent, pushData, callback);
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
        IMCenter.getInstance().sendLocationMessage(message, pushContent, pushData, sendMessageCallback);
    }

    /**
     * <p>发送多媒体消息</p>
     * <p>发送前构造 {@link Message} 消息实体，消息实体中的 content 必须为 {@link FileMessage}, 否则返回失败。</p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    发送文件消息时，此字段必须填写，否则会收不到 push 推送。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调 {@link RongIMClient.SendMediaMessageCallback}。
     */
    public void sendMediaMessage(Message message, String pushContent,
                                 final String pushData, final IRongCallback.ISendMediaMessageCallback callback) {
        IMCenter.getInstance().sendMediaMessage(message, pushContent, pushData, callback);
    }

    /**
     * <p>发送多媒体消息，可以使用该方法将多媒体文件上传到自己的服务器。
     * 使用该方法在上传多媒体文件时，会回调 {@link IRongCallback.ISendMediaMessageCallbackWithUploader#onAttached(Message, IRongCallback.MediaMessageUploader)}
     * 此回调中会携带 {@link IRongCallback.MediaMessageUploader} 对象，使用者只需要调用此对象中的
     * {@link IRongCallback.MediaMessageUploader#update(int)} 更新进度
     * {@link IRongCallback.MediaMessageUploader#success(Uri)} 更新成功状态，并告知上传成功后的文件地址
     * {@link IRongCallback.MediaMessageUploader#error()} 更新失败状态
     * </p>
     *
     * @param message     发送消息的实体。
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段。
     *                    如果发送的是自定义消息，该字段必须填写，否则无法收到 push 消息。
     *                    如果发送 sdk 中默认的消息类型，例如 RC:TxtMsg, RC:VcMsg, RC:ImgMsg, RC:FileMsg，则不需要填写，默认已经指定。
     * @param pushData    push 附加信息。如果设置该字段，用户在收到 push 消息时，能通过 {@link io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback    发送消息的回调，回调中携带 {@link IRongCallback.MediaMessageUploader} 对象，用户调用该对象中的方法更新状态。
     */
    public void sendMediaMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMediaMessageCallbackWithUploader callback) {
        IMCenter.getInstance().sendMediaMessage(message, pushContent, pushData, callback);
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
    public void sendMessage(Message message, String pushContent, final String pushData, final IRongCallback.ISendMessageCallback callback) {
        IMCenter.getInstance().sendMessage(message, pushContent, pushData, callback);
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
     * @param option      发送消息附加选项，目前仅支持设置 isVoIPPush，如果对端设备是 iOS,设置 isVoIPPush 为 True，会走 VoIP 通道推送 Push.
     * @param callback    发送消息的回调，参考 {@link IRongCallback.ISendMessageCallback}。
     */
    public void sendMessage(Message message, String pushContent, final String pushData, SendMessageOption option, final IRongCallback.ISendMessageCallback callback) {
        IMCenter.getInstance().sendMessage(message, pushContent, pushData, option, callback);
    }

    /**
     * <p>设置群组信息的提供者。</p>
     * <p>设置后，当 sdk 界面展示群组信息时，会回调 {@link UserDataProvider.GroupInfoProvider#getGroupInfo(String)}
     * 使用者只需要根据对应的 groupId 提供对应的群组信息。
     * 如果需要异步从服务器获取群组信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupInfoCache(Group)} 刷新信息。</p>
     *
     * @param groupInfoProvider 群组信息提供者。
     * @param isCacheGroupInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                          如果 App 提供的 GroupInfoProvider。
     *                          每次都需要通过网络请求群组数据，而不是将群组数据缓存到本地，会影响群组信息的加载速度；<br>
     *                          此时最好将本参数设置为 true，由 IMKit 来缓存群组信息。
     */
    public static void setGroupInfoProvider(UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        RongUserInfoManager.getInstance().setGroupInfoProvider(groupInfoProvider, isCacheGroupInfo);
    }

    /**
     * <p>设置GroupUserInfo提供者，供RongIM 调用获取GroupUserInfo</p>
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
    public static void setGroupUserInfoProvider(UserDataProvider.GroupUserInfoProvider groupUserInfoProvider, boolean isCacheGroupUserInfo) {
        RongUserInfoManager.getInstance().setGroupUserInfoProvider(groupUserInfoProvider, isCacheGroupUserInfo);
    }

    /**
     * 设置位置信息的提供者。
     *
     * @param locationProvider 位置信息提供者。
     */
    public static void setLocationProvider(LocationManager.LocationProvider locationProvider) {
        LocationManager.getInstance().setLocationProvider(locationProvider);
    }

    /**
     * 设置群组成员提供者。
     * <p/>
     * '@' 功能和VoIP功能在选人界面,需要知道群组内成员信息,开发者需要设置该提供者。 开发者需要在回调中获取到群成员信息
     * 并通过{@link RongMentionManager.IGroupMemberCallback}中的方法设置到 sdk 中
     * <p/>
     *
     * @param groupMembersProvider 群组成员提供者。
     */
    public void setGroupMembersProvider(RongMentionManager.IGroupMembersProvider
                                                groupMembersProvider) {
        RongMentionManager.getInstance().setGroupMembersProvider(groupMembersProvider);
    }

    /**
     * 获取当前连接用户的信息。
     *
     * @return 当前连接用户的信息。
     */
    public String getCurrentUserId() {
        return RongIMClient.getInstance().getCurrentUserId();
    }

    /**
     * 设置语音消息的最大长度
     *
     * @param sec 默认值是60s
     */
    public void setMaxVoiceDuration(int sec) {
        AudioRecordManager.getInstance().setMaxVoiceDuration(sec);
    }

    /**
     * 语音消息类型
     *
     * @return 当前设置的语音消息类型
     */
    public IMCenter.VoiceMessageType getVoiceMessageType() {
        return RongConfigCenter.featureConfig().getVoiceMessageType();
    }

    /**
     * 设置语音消息类型
     *
     * @param voiceMessageType 消息类型{@link IMCenter.VoiceMessageType}
     */
    public void setVoiceMessageType(IMCenter.VoiceMessageType voiceMessageType) {
        RongConfigCenter.featureConfig().setVoiceMessageType(voiceMessageType);
    }

    /**
     * 设置语音消息采样率
     *
     * @param sampleRate 消息采样率{@link AudioRecordManager.SamplingRate}
     */
    public void setSamplingRate(AudioRecordManager.SamplingRate sampleRate) {
        AudioRecordManager.getInstance().setSamplingRate(sampleRate);
    }

    /**
     * 语音消息采样率
     *
     * @return 当前设置的语音采样率
     */
    public int getSamplingRate() {
        return AudioRecordManager.getInstance().getSamplingRate();
    }

    /**
     * 设置消息拦截器。
     *
     * @param interceptor 消息发送或接受的拦截器。
     */
    public void setMessageInterceptor(MessageInterceptor interceptor) {
        IMCenter.getInstance().setMessageInterceptor(interceptor);
    }

    /**
     * 根据消息 Message 设置消息状态，回调方式获取设置是否成功。
     *
     * @param message  消息实体。要设置的发送状态包含在 message 中
     * @param callback 是否设置成功的回调。
     */
    public void setMessageSentStatus(final Message message, final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().setMessageSentStatus(message, callback);
    }

    /**
     * 设置公众号界面操作的监听器。
     *
     * @param listener 会话公众号界面操作的监听器。
     */
    public static void setPublicServiceBehaviorListener(PublicServiceManager.PublicServiceBehaviorListener listener) {
        PublicServiceManager.getInstance().setPublicServiceBehaviorListener(listener);
    }

    /**
     * 设置公众服务菜单点击监听。
     * 建议使用方法：在进入对应公众服务会话时，设置监听。当退出会话时，重置监听为 null，这样可以防止内存泄露。
     *
     * @param menuClickListener 监听。
     */
    public void setPublicServiceMenuClickListener(IPublicServiceMenuClickListener menuClickListener) {
        PublicServiceManager.getInstance().setPublicServiceMenuClickListener(menuClickListener);
    }

    /**
     * 设置公众服务账号信息的提供者，供 RongIM 调用获公众服务账号名称，头像信息和公众服务号菜单。
     * <p>
     * 目前 sdk 默认的公众号服务不需要开发者设置，这个接口提供了另外一种从 app 层设置公众服务账号信息的方式
     * 设置后，当 sdk 界面展示用户信息时，会回调 {@link PublicServiceManager.PublicServiceProfileProvider#getPublicServiceProfile(Conversation.PublicServiceType, String)}
     * 使用者只需要根据对应的publicServiceType, publicServiceId 提供对应的公众服务账号信息。
     * 如果需要异步从服务器获取公众服务账号信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshPublicServiceProfile(PublicServiceProfile)} 刷新公众号信息。
     * </p>
     *
     * @param publicServiceProfileProvider 公众服务账号信息的提供者 {@link PublicServiceManager.PublicServiceProfileProvider}。
     */
    public static void setPublicServiceProfileProvider(PublicServiceManager.PublicServiceProfileProvider publicServiceProfileProvider) {
        PublicServiceManager.getInstance().setPublicServiceProfileProvider(publicServiceProfileProvider);
    }

    /**
     * 设置发送消息回执的会话类型。目前只支持私聊，群组和讨论组。
     * 默认支持私聊。
     *
     * @param types 包含在types里的会话类型中将会发送消息回执。
     */
    public void setReadReceiptConversationTypeList(Conversation.ConversationType... types) {
        RongConfigCenter.conversationConfig().setSupportReadReceiptConversationType(types);
    }

    /**
     * 设置当前用户信息。
     * 如果开发者没有实现用户信息提供者，而是使用消息携带用户信息，需要使用这个方法设置当前用户的信息，
     * 然后在{@link #init(Application, String, boolean)}之后调用{@link RongUserInfoManager#getInstance()}.setMessageAttachedUserInfo(boolean)}，
     * 这样可以在每条消息中携带当前用户的信息，IMKit会在接收到消息的时候取出用户信息并刷新到界面上。
     *
     * @param userInfo 当前用户信息。
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        RongUserInfoManager.getInstance().setCurrentUserInfo(userInfo);
    }

    /**
     * 设置消息体内是否携带用户信息。
     *
     * @param state 是否携带用户信息，true 携带，false 不携带。
     */
    public void setMessageAttachedUserInfo(boolean state) {
        RongUserInfoManager.getInstance().setMessageAttachedUserInfo(state);
    }

    /**
     * <p>启动聊天室会话。</p>
     * <p>设置参数 createIfNotExist 为 true，对应到 kit 中调用的接口是
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)}.
     * 如果聊天室不存在，则自动创建并加入，如果回调失败，则弹出 warning。</p>
     * <p>设置参数 createIfNotExist 为 false，对应到 kit 中调用的接口是
     * {@link RongIMClient#joinExistChatRoom(String, int, RongIMClient.OperationCallback)}.
     * 如果聊天室不存在，则返回错误 {@link RongIMClient.ErrorCode#RC_CHATROOM_NOT_EXIST}.
     * </p>
     *
     * @param context          应用上下文。
     * @param chatRoomId       聊天室 id。
     * @param createIfNotExist 如果聊天室不存在，是否创建。
     */
    public void startChatRoomChat(Context context, String chatRoomId, boolean createIfNotExist) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(RouteUtils.CREATE_CHATROOM, createIfNotExist);
        RouteUtils.routeToConversationActivity(context, Conversation.ConversationType.CHATROOM, chatRoomId, bundle);
    }

    /**
     * <p>启动会话界面。</p>
     * <p>使用时，可以传入多种会话类型 {@link Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。
     * 如果传入的是 {@link Conversation.ConversationType#CHATROOM}，sdk 会默认调用
     * {@link RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback)} 加入聊天室。
     * 如果你的逻辑是，只允许加入已存在的聊天室，请使用接口 {@link #startChatRoomChat(Context, String, boolean)} 并且第三个参数为 false</p>
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param bundle           参数传递 bundle
     */
    public void startConversation(Context context, Conversation.ConversationType conversationType, String targetId, Bundle bundle) {
        RouteUtils.routeToConversationActivity(context, conversationType, targetId, bundle);
    }

    /**
     * 启动客户服聊天界面。
     *
     * @param context           应用上下文。
     * @param customerServiceId 要与之聊天的客服 Id。
     * @param title             聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值, 再手动设置为聊天界面的标题。
     * @param customServiceInfo 当前使用客服者的用户信息。{@link CSCustomServiceInfo}
     */
    public void startCustomerServiceChat(Context context, String customerServiceId, String title, CSCustomServiceInfo customServiceInfo) {
        Bundle bundle = new Bundle();
        bundle.putString(RouteUtils.TITLE, title);
        bundle.putParcelable(RouteUtils.CUSTOM_SERVICE_INFO, customServiceInfo);
        RouteUtils.routeToConversationActivity(context, Conversation.ConversationType.CUSTOMER_SERVICE, customerServiceId, bundle);
    }

    /**
     * 启动聚合后的某类型的会话列表。<br> 例如：如果设置了单聊会话为聚合，则通过该方法可以打开包含所有的单聊会话的列表。
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     */
    public void startSubConversationList(Context context, Conversation.ConversationType conversationType) {
        RouteUtils.routeToSubConversationListActivity(context, conversationType, "");
    }

    public static String getVersion() {
        return IMKitBuildVar.SDK_VERSION;
    }

}
