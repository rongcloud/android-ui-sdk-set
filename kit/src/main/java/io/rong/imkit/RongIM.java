package io.rong.imkit;

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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
import io.rong.message.ImageMessage;
import io.rong.message.TextMessage;
import java.util.List;

public class RongIM {
    private static final String TAG = RongIM.class.getSimpleName();
    static RongIMClient.OnReceiveMessageListener sMessageListener;

    private RongIM() {}

    private static class SingletonHolder {
        static RongIM sInstance = new RongIM();
    }

    public static RongIM getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * /~chinese 将某个用户加入黑名单。
     *
     * <p>将对方加入黑名单后，对方再发消息时，提示"您的消息已经发出, 但被对方拒收"。但您仍然可以给对方发送消息。
     *
     * @param userId 用户 id。
     * @param callback 加到黑名单回调。
     * @group 高级功能
     */

    /**
     * /~english Add a user to the blacklist
     *
     * <p>After the other party is added into the blacklist, when the other party sends a message
     * again, the system prompts "your message has been sent, but it has been rejected by the other
     * party." But you can still send messages to each other.
     *
     * @param userId User id.
     * @param callback Add it to the blacklist and call it back.
     */
    public void addToBlacklist(final String userId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance().addToBlacklist(userId, callback);
    }

    /**
     * /~chinese 设置未读消息数变化监听器。 注意:如果是在 activity 中设置,那么要在 activity 销毁时, 调用 {@link
     * UnReadMessageManager#removeObserver(UnReadMessageManager.IUnReadMessageObserver)} 否则会造成内存泄漏。
     *
     * @param observer 接收未读消息消息的监听器。
     * @param conversationTypes 接收未读消息的会话类型。
     */

    /**
     * /~english Set unread message number change listener Note: If it is set in activity, then call
     * UnReadMessageManager.removeObserver(UnReadMessageManager.IUnReadMessageObserver) when the
     * activity is destroyed, otherwise it will cause a memory leak
     *
     * @param observer Listeners that receive unread message messages。
     * @param conversationTypes The conversation type that receives unread messages。
     */
    public void addUnReadMessageCountChangedObserver(
            final UnReadMessageManager.IUnReadMessageObserver observer,
            Conversation.ConversationType... conversationTypes) {
        if (observer == null || conversationTypes == null || conversationTypes.length == 0) {
            io.rong.common.rlog.RLog.w(
                    TAG, "addOnReceiveUnreadCountChangedListener Illegal argument");
            throw new IllegalArgumentException(
                    "observer must not be null and must include at least one conversationType");
        }
        UnReadMessageManager.getInstance().addObserver(conversationTypes, observer);
    }

    /**
     * /~chinese 取消多媒体消息下载。
     *
     * @param message 多媒体文件消息。
     * @param callback 取消下载多媒体文件时的回调。
     * @group 多媒体下载
     */

    /**
     * /~english Cancel multimedia message downloading.
     *
     * @param message Multimedia file message.
     * @param callback Cancel the callback when downloading multimedia files
     */
    public void cancelDownloadMediaMessage(
            Message message, RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().cancelDownloadMediaMessage(message, callback);
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
        IMCenter.getInstance().cancelSendMediaMessage(message, callback);
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
        IMCenter.getInstance().clearConversations(callback, conversationTypes);
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
            Conversation.ConversationType conversationType,
            String targetId,
            RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().clearMessages(conversationType, targetId, callback);
    }

    /**
     * /~chinese 清除某个会话中的未读消息数。
     *
     * <p><strong>注意：不支持聊天室！</strong>
     *
     * @param conversationType 会话类型，不支持聊天室。参考 {@link Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param callback 清除是否成功的回调。
     * @group 消息操作
     */

    /**
     * /~english Clear unread messages in a conversation.
     *
     * <p><strong>Note: The chat rooms are not supported!</strong>
     *
     * @param conversationType Type of conversation, which does not support chat rooms. Refer to
     *     Conversation.ConversationType.
     * @param targetId Conversation id. Depending on different conversationType, it may be user id,
     *     discussion group id or group id.
     * @param callback Callback for whether clearing is successful or not.
     */
    public void clearMessagesUnreadStatus(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().clearMessagesUnreadStatus(conversationType, targetId, callback);
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
        IMCenter.getInstance().clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * /~chinese 初始化融云 SDK，在整个应用程序全局只需要调用一次。
     *
     * <p>只能在主进程调用，建议在 Application 继承类中调用。
     *
     * @param application Application 类对象。
     * @param appKey 从融云开发者平台创建应用后获取到的 App Key。
     * @group 连接
     */

    /**
     * /~english Initialize the RongCloud SDK, which only needs to be called once in the whole
     * application globally.
     *
     * <p>It can only be called in the main process and it is recommended to call in the Application
     * inheritance class.
     *
     * @param application Application class object.
     * @param appKey The App Key obtained after an application is created on RongCLoud developer
     *     platform.
     */
    public static void init(Application application, String appKey) {
        IMCenter.init(application, appKey, true);
    }

    /**
     * /~chinese 初始化 SDK，在整个应用程序全局只需要调用一次, 建议在 Application 继承类中调用。
     *
     * @param application Application 类对象。
     * @param appKey 从融云开发者平台创建应用后获取到的 App Key。
     *     <p>此参数可选。如果不传值，SDK 会使用 AndroidManifest.xml 里配置的 RONG_CLOUD_APP_KEY。<br>
     *     {@code <meta-data android:name="RONG_CLOUD_APP_KEY" android:value="******" /> }
     * @param enablePush 是否使用推送功能。 {@code true} 启用推送； {@code false} 关闭推送。
     * @group 连接
     */

    /**
     * /~english SDK Initialization shall be called only once globally throughout the application,
     * and it is recommended to call in the Application inheritance class.
     *
     * @param application Application class object.
     * @param appKey The App Key obtained after an application is created on RongCLoud developer
     *     platform. This parameter is optional. If you do not pass a value, SDK will use the
     *     RONG_CLOUD_APP_KEY configured in AndroidManifest.xml. <meta-data
     *     android:name="RONG_CLOUD_APP_KEY" android:value="******" />
     * @param enablePush Whether to use push function true Enable push; false Disable push.
     */
    public static void init(Application application, String appKey, boolean enablePush) {
        IMCenter.init(application, appKey, enablePush);
    }

    /**
     * /chinese 向本地会话中插入一条消息，方向为接收。
     *
     * <p>这条消息只是插入本地会话，不会实际发送给服务器和对方。插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR}。
     *
     * @param type 会话类型。
     * @param targetId 会话 id。比如私人会话时，是对方的 id； 群组会话时，是群 id； 讨论组会话时，则为该讨论组的 id。
     * @param senderUserId 发送方 id
     * @param receivedStatus 接收状态 {@link Message.ReceivedStatus}
     * @param content 消息内容。如 {@link TextMessage} {@link ImageMessage}等。
     * @param sentTime 消息的发送时间 {@link Message#getSentTime()} 。
     * @param callback 获得消息发送实体的回调。
     * @group 消息操作
     */

    /**
     * /~english Insert a message into the local conversation in the direction of receiving
     *
     * <p>his message will only be inserted into the local conversation and will not actually be
     * sent to the server and the other party. The message to be inserted must be stored into the
     * database, namely {@link MessageTag#ISPERSISTED}, otherwise {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR} will be called back.
     *
     * @param type Conversation type
     * @param targetId Conversation id. For example: It is an id of other party for a private
     *     conversation, the group id for a group conversation, and the id of the discussion group
     *     for a discussion group conversation.
     * @param senderUserId Sender id
     * @param receivedStatus Receiving statusMessage.ReceivedStatus.
     * @param content Message content E.g. {@link TextMessage} {@link ImageMessage}, etc.
     * @param sentTime Sending time of messages {@link Message#getSentTime()}.
     * @param callback Get the callback for the message sending entity
     */
    public void insertIncomingMessage(
            Conversation.ConversationType type,
            String targetId,
            String senderUserId,
            Message.ReceivedStatus receivedStatus,
            MessageContent content,
            long sentTime,
            final RongIMClient.ResultCallback<Message> callback) {
        IMCenter.getInstance()
                .insertIncomingMessage(
                        type, targetId, senderUserId, receivedStatus, content, sentTime, callback);
    }

    /**
     * /~chinese 向本地会话中插入一条消息，方向为接收。
     *
     * <p>这条消息只是插入本地会话，不会实际发送给服务器和对方。插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR}。
     *
     * @param type 会话类型。
     * @param targetId 会话 id。比如私人会话时，是对方的 id； 群组会话时，是群 id； 讨论组会话时，则为该讨论,组的 id。
     * @param senderUserId 发送方 id。
     * @param receivedStatus 接收状态 {@link Message.ReceivedStatus}。
     * @param content 消息内容。如{@link TextMessage} {@link ImageMessage}等。
     * @param callback 获得消息发送实体的回调。
     * @group 消息操作
     */

    /**
     * /~english Insert a message into the local conversation in the direction of receiving
     *
     * <p>his message will only be inserted into the local conversation and will not actually be
     * sent to the server and the other party. The message to be inserted must be stored into the
     * database, namely {@link MessageTag#ISPERSISTED}, otherwise {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR} will be called back.
     *
     * @param type Conversation type
     * @param targetId Conversation id. For example: It is an id of other party for a private
     *     conversation, the group id for a group conversation, and the id of the discussion group
     *     for a discussion group conversation.
     * @param senderUserId Sender id
     * @param receivedStatus Receiving statusMessage.ReceivedStatus.
     * @param content Message content E.g. {@link TextMessage} {@link ImageMessage}, etc.
     * @param callback Get the callback for the message sending entity
     */
    public void insertIncomingMessage(
            Conversation.ConversationType type,
            String targetId,
            String senderUserId,
            Message.ReceivedStatus receivedStatus,
            MessageContent content,
            RongIMClient.ResultCallback<Message> callback) {
        insertIncomingMessage(
                type,
                targetId,
                senderUserId,
                receivedStatus,
                content,
                System.currentTimeMillis(),
                callback);
    }

    /**
     * /~chinese 向本地会话中插入一条消息。
     *
     * <p>这条消息只是插入本地会话，不会实际发送给服务器和对方。 插入消息需为入库消息，即{@link MessageTag#ISPERSISTED}，否者会回调 {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR}
     *
     * @param type 会话类型。
     * @param targetId 会话 id。比如私人会话时，是对方的 id； 群组会话时，是群 id； 讨论组会话时，则为该讨论组的 id。
     * @param sentStatus 发送状态 {@link Message.SentStatus}
     * @param content 消息内容。如{@link TextMessage} {@link ImageMessage} 等。
     * @param callback 获得消息发送实体的回调。
     * @group 消息操作
     */

    /**
     * /~english Insert a message into the local conversation in the direction of sending
     *
     * <p>his message will only be inserted into the local conversation and will not actually be
     * sent to the server and the other party. The message to be inserted must be stored into the
     * database, namely {@link MessageTag#ISPERSISTED}, otherwise {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR} will be called back.
     *
     * @param type Conversation type
     * @param targetId Conversation id. For example: It is an id of other party for a private
     *     conversation, the group id for a group conversation, and the id of the discussion group
     *     for a discussion group conversation.
     * @param sentStatus Sending status
     * @param content Message content E.g. {@link TextMessage} {@link ImageMessage}, etc.
     * @param callback Get the callback for the message sending entity
     */
    public void insertOutgoingMessage(
            Conversation.ConversationType type,
            String targetId,
            Message.SentStatus sentStatus,
            MessageContent content,
            RongIMClient.ResultCallback<Message> callback) {
        insertOutgoingMessage(
                type, targetId, sentStatus, content, System.currentTimeMillis(), callback);
    }

    /**
     * /~chinese 向本地会话中插入一条消息，方向为发送。
     *
     * <p>这条消息只是插入本地会话，不会实际发送给服务器和对方。 插入消息需为入库消息，即 {@link MessageTag#ISPERSISTED}，否者会回调 {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR}
     *
     * @param type 会话类型。
     * @param targetId 会话 id。比如私人会话时，是对方的 id； 群组会话时，是群 id； 讨论组会话时，则为该讨论组的 id。
     * @param sentStatus 发送状态 {@link Message.SentStatus}
     * @param content 消息内容。如{@link TextMessage} {@link ImageMessage} 等。
     * @param sentTime 消息的发送时间 {@link Message#getSentTime()} 。
     * @param callback 获得消息发送实体的回调。
     * @group 消息操作
     */

    /**
     * /~english Insert a message into the local conversation in the direction of sending
     *
     * <p>his message will only be inserted into the local conversation and will not actually be
     * sent to the server and the other party. The message to be inserted must be stored into the
     * database, namely {@link MessageTag#ISPERSISTED}, otherwise {@link
     * io.rong.imlib.IRongCoreEnum.CoreErrorCode#PARAMETER_ERROR} will be called back.
     *
     * @param type Conversation type
     * @param targetId Conversation id. For example: It is an id of other party for a private
     *     conversation, the group id for a group conversation, and the id of the discussion group
     *     for a discussion group conversation.
     * @param sentStatus Sending status
     * @param content Message content E.g. {@link TextMessage} {@link ImageMessage}, etc.
     * @param sentTime Sending time of messages {@link Message#getSentTime()}.
     * @param callback Get the callback for the message sending entity
     */
    public void insertOutgoingMessage(
            Conversation.ConversationType type,
            String targetId,
            Message.SentStatus sentStatus,
            MessageContent content,
            long sentTime,
            final RongIMClient.ResultCallback<Message> callback) {
        IMCenter.getInstance()
                .insertOutgoingMessage(type, targetId, sentStatus, content, sentTime, callback);
    }

    /**
     * /~chinese 加入聊天室。
     *
     * <p>如果聊天室不存在，SDK 会创建聊天室并加入，如果已存在，则直接加入。 <br>
     * 可以通过传入的 defMessageCount 设置加入聊天室成功之后需要获取的历史消息数量。 -1 表示不获取任何历史消息，0 表示不特殊设置而使用 sdk 默认的设置（默认为获取
     * 10 条），defMessageCount（0 < defMessageCount <= 50） 为获取的消息数量，最大值为 50。
     *
     * @param chatRoomId 聊天室 id。
     * @param defMessageCount 进入聊天室拉取消息数量，-1 时不拉取任何消息，0 时拉取 10 条消息，最多只能拉取 50 条。
     *     （加入聊天室时会传本地最后一条消息的时间戳，拉取的是这个时间戳之后的消息。比如：这个时间戳之后有 3 条消息，defMessageCount传 10，也只能拉取 3 条消息。）
     * @param callback 状态回调。
     * @group 聊天室
     */

    /**
     * /~english Join the chat room
     *
     * <p>If a chat room does not exist, SDK creates a chat room and joins it, or directly joins if
     * it already exists. You can use the passed defMessageCount to set the number of historical
     * messages that shall be obtained after joining the chat room successfully. - 1 indicates no
     * historical messages are obtained, 0 means to use the default setting of SDK without special
     * settings (default value is to get 10), defMessageCount (0 < defMessageCount <= 50) is totally
     * obtained message amount, maximum 50.
     *
     * @param chatRoomId Chat room id
     * @param defMessageCount Enter the chat room to pull the number of messages. No messages are
     *     pulled at-1, 10 messages are pulled at 0, and a maximum of 50 messages can be pulled (The
     *     timestamp of the last local message will be sent when you join the chat room, and the
     *     message will be pulled after this timestamp. For example, there are 3 messages after this
     *     timestamp, and defMessageCount sends 10, and only 3 messages can be pulled. )
     * @param callback Status callback.
     */
    public void joinChatRoom(
            final String chatRoomId,
            final int defMessageCount,
            final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .joinChatRoom(
                        chatRoomId,
                        defMessageCount,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                                if (callback != null) {
                                    callback.onError(
                                            RongIMClient.ErrorCode.valueOf(
                                                    coreErrorCode.getValue()));
                                }
                            }
                        });
    }

    /**
     * /~chinese 加入已存在的聊天室。
     *
     * <p>如果聊天室不存在，则加入失败。加入聊天室时，可以设置拉取聊天室消息数目。
     *
     * @param chatroomId 聊天室 id。
     * @param defMessageCount 进入聊天室拉取消息数目，-1 时不拉取任何消息，0 时拉取 10 条消息，最多只能拉取 40 条。
     * @param callback 状态回调。
     * @group 聊天室
     */

    /**
     * /~english Join an existing chat room
     *
     * <p>If the chat room does not exist, it fails to join. When you join a chat room, you can set
     * the number of messages to be pulled from the chat room.
     *
     * @param chatroomId Chat room id
     * @param defMessageCount Enter the chat room to pull the number of messages. No messages are
     *     pulled at-1, 10 messages are pulled at 0, and a maximum of 40 messages can be pulled.
     * @param callback Status callback.
     */
    public void joinExistChatRoom(
            final String chatroomId,
            final int defMessageCount,
            final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .joinExistChatRoom(
                        chatroomId,
                        defMessageCount,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                                if (callback != null) {
                                    callback.onError(
                                            RongIMClient.ErrorCode.valueOf(
                                                    coreErrorCode.getValue()));
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
        IMCenter.getInstance().logout();
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
            Message message, RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().pauseDownloadMediaMessage(message, callback);
    }

    /**
     * /~chinese 退出聊天室。
     *
     * @param chatRoomId 聊天室 id。
     * @param callback 状态回调。
     * @group 聊天室
     */

    /**
     * /~english Quit the chat room
     *
     * @param chatRoomId Chat room id
     * @param callback Status callback
     */
    public void quitChatRoom(
            final String chatRoomId, final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .quitChatRoom(
                        chatRoomId,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                                if (callback != null) {
                                    callback.onError(
                                            RongIMClient.ErrorCode.valueOf(
                                                    coreErrorCode.getValue()));
                                }
                            }
                        });
    }

    /**
     * /~chinese 撤回消息
     *
     * @param message 被撤回的消息
     * @param pushContent 当下发 push 消息时，在通知栏里会显示这个字段，不设置将使用融云默认推送内容
     * @group 高级功能
     */

    /**
     * /~english Recall the message
     *
     * @param message Withdrawn messages
     * @param pushContent When a push message is distributed, this field will be displayed in the
     *     notification bar. If it is not set, the default content will be pushed by RongCloud.
     */
    public void recallMessage(final Message message, String pushContent) {
        IMCenter.getInstance().recallMessage(message, pushContent, null);
    }

    /**
     * /~chinese 刷新、更改群组用户缓存数据。
     *
     * @param groupUserInfo 需要更新的群组用户缓存数据。
     */

    /**
     * /~english Refresh and change group user cache data
     *
     * @param groupUserInfo Cached date of the group users to update。
     */
    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {
        RongUserInfoManager.getInstance().refreshGroupUserInfoCache(groupUserInfo);
    }

    /**
     * /~chinese 刷新公众服务账号缓存数据。
     *
     * @param publicServiceProfile 需要更新的公众服务账号缓存数据。
     */

    /**
     * /~english Refresh public service account cache data
     *
     * @param publicServiceProfile Cached data of the public service account to update
     */
    public void refreshPublicServiceProfile(PublicServiceProfile publicServiceProfile) {
        PublicServiceManager.getInstance().refreshPublicServiceProfile(publicServiceProfile);
    }

    /**
     * /~chinese 连接融云服务器，在整个应用程序全局，只需要调用一次，需在 {@link #init(Application, String, boolean)} 之后调用。
     *
     * <p>调用此接口返回非业务错误码时，SDK 会启动重连机制进行重连；如果仍没有连接成功，会在设备网络状态变化时再次进行重连。<br>
     * <strong>如果您使用 IMKit，请使用 RongIM 中的同名方法建立与融云服务器的连接，而不要使用此方法。</strong>
     *
     * @param token 从服务端获取的用户身份令牌（Token）。
     * @param connectCallback
     *     连接回调扩展类，新增打开数据库的回调（OnDatabaseOpened(DatabaseOpenStatus)），会返回数据库打开的状态，{@link
     *     DatabaseOpenStatus}； {@code DATABASE_OPEN_SUCCESS} 执行拉取会话列表操作，{@code DATABASE_OPEN_ERROR}
     *     不处理。<br>
     *     如连接失败，参见 {@link ConnectionErrorCode} 错误码具体说明。
     * @return RongIMClient IM 客户端核心类的实例。
     * @discussion 调用该接口，SDK 会在连接失败之后尝试重连，将出现以下两种情况： 第一、连接成功，回调 onSuccess(userId)。 第二、出现 SDK
     *     无法处理的错误，回调 onError(errorCode)（如 token 非法），并不再重连。
     *     <p>如果您不想一直进行重连，可以使用 connect(String,int,ConnectCallback) 接口并设置连接超时时间 timeLimit。
     * @discussion 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     * @group 连接
     */

    /**
     * /~english Connect to the RongCloud server, which only needs to be called once in the whole
     * application globally, and needs to be called after {@link #init(Application, String,
     * boolean)} .
     *
     * <p>When a non-business error code is returned by calling this API, SDK will activate the
     * reconnection mechanism for reconnection; if the connection is still not successful, it will
     * reconnect again when the network status of the device changes. If you use IMKit, the method
     * of the same name in RongIM is used to establish a connection to the RongCloud server instead
     * of using this method.
     *
     * @param token User identity token (Token) obtained from the server.
     * @param connectCallback Connection callback extension class. When the new callback
     *     (OnDatabaseOpened (DatabaseOpenStatus)) to open the database is added, it will return the
     *     open status of the database.DatabaseOpenStatus; DATABASE_OPEN_SUCCESS exepull the
     *     conversation list and DATABASE_OPEN_ERROR is not handled. If the connection fails, see
     *     the specific description of ConnectionErrorCode error code.
     * @return Instance of the RongCoreClient IM client core class.
     */
    public static void connect(
            final String token, final RongIMClient.ConnectCallback connectCallback) {
        connect(token, -1, connectCallback);
    }

    /**
     * /~chinese 连接融云服务器，在整个应用程序全局，只需要调用一次，需在 {@link #init(Context)} 之后调用。
     *
     * <p>调用此接口返回非业务错误码时，SDK 会启动重连机制进行重连；如果仍没有连接成功，会在设备网络状态变化时再次进行重连。<br>
     * <strong>如果您使用 IMKit，请使用 RongIM 中的同名方法建立与融云服务器的连接，而不要使用此方法。</strong>
     *
     * @param token 从服务端获取的用户身份令牌（Token）。
     * @param timeLimit 连接超时时间，单位：秒。timeLimit <= 0，则 IM 将一直连接，直到连接成功或者无法连接（如 token 非法） timeLimit > 0
     *     ,则 IM 将最多连接 timeLimit 秒： 如果在 timeLimit 秒内连接成功，后面再发生了网络变化或前后台切换，SDK 会自动重连； 如果在 timeLimit
     *     秒无法连接成功则不再进行重连，通过 onError 告知连接超时，您需要再自行调用 connect 接口
     * @param connectCallback
     *     连接回调扩展类，新增打开数据库的回调（OnDatabaseOpened(DatabaseOpenStatus)），会返回数据库打开的状态，{@link
     *     DatabaseOpenStatus}； {@code DATABASE_OPEN_SUCCESS} 执行拉取会话列表操作，{@code DATABASE_OPEN_ERROR}
     *     不处理。<br>
     *     如连接失败，参见 {@link ConnectionErrorCode} 错误码具体说明。
     * @return RongIMClient IM 客户端核心类的实例。
     * @discussion 调用该接口，SDK 会在 timeLimit 秒内尝试重连，直到出现下面三种情况之一： 第一、连接成功，回调 onSuccess(userId)。
     *     第二、超时，回调 onError(RC_CONNECT_TIMEOUT)。 第三、出现 SDK 无法处理的错误，回调 onError(errorCode)（如 token
     *     非法）。
     * @discussion 连接成功后，SDK 将接管所有的重连处理。当因为网络原因断线的情况下，SDK 会不停重连直到连接成功为止，不需要您做额外的连接操作。
     * @group 连接
     */

    /**
     * /~english Connect to the RongCloud server, which only needs to be called once in the whole
     * application globally, and needs to be called after init(Context).
     *
     * <p>When a non-business error code is returned by calling this API, SDK will activate the
     * reconnection mechanism for reconnection; if the connection is still not successful, it will
     * reconnect again when the network status of the device changes. If you use IMKit, the method
     * of the same name in RongIM is used to establish a connection to the RongCloud server instead
     * of using this method.
     *
     * @param token User identity token (Token) obtained from the server.
     * @param timeLimit Connection timeout (in seconds). timeLimit <= 0, then Im will continue to
     *     connect until the connection is successful or unable to connect (for example, the token
     *     is illegal) with timelimit > 0, then Im will connect with timelimit seconds at most: If
     *     the connection is successful within timeLimit second(s), the SDK will automatically
     *     reconnect if there is a network change or a switch between the foreground and background.
     *     If the connection cannot be successfully connected in timeLimit second(s), the connection
     *     will not be reconnected. You shall call the connect interfaceagain when you are told by
     *     onError that the connection has timed out.
     * @param connectCallback Connection callback extension class. When the new callback
     *     (OnDatabaseOpened (DatabaseOpenStatus)) to open the database is added, it will return the
     *     open status of the database.DatabaseOpenStatus; DATABASE_OPEN_SUCCESS exepull the
     *     conversation list and DATABASE_OPEN_ERROR is not handled. If the connection fails, see
     *     the specific description of ConnectionErrorCode error code.
     * @return Instance of the RongCoreClient IM client core class.
     */
    public static void connect(
            String token, int timeLimit, final RongIMClient.ConnectCallback connectCallback) {
        IMCenter.getInstance().connect(token, timeLimit, connectCallback);
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
        IMCenter.getInstance().deleteMessages(conversationType, targetId, callback);
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
            Conversation.ConversationType conversationType,
            String targetId,
            int[] messageIds,
            final RongIMClient.ResultCallback<Boolean> callback) {
        IMCenter.getInstance().deleteMessages(conversationType, targetId, messageIds, callback);
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
            Conversation.ConversationType conversationType,
            String targetId,
            final Message[] messages,
            final RongIMClient.OperationCallback callback) {
        IMCenter.getInstance().deleteRemoteMessages(conversationType, targetId, messages, callback);
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
        IMCenter.getInstance().disconnect();
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
        IMCenter.getInstance()
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
        IMCenter.getInstance().downloadMediaFile(uid, fileUrl, fileName, path, callback);
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
        IMCenter.getInstance().downloadMediaMessage(message, callback);
    }

    /**
     * /~chinese 设置会话界面未读新消息是否展示 注:未读新消息大于1条即展示 目前仅支持单群聊
     *
     * @param state true 展示，false 不展示。
     */
    /**
     * /~english Set whether to display unread new messages in the conversation interface. Note:
     * Unread new messages will be displayed if the number of unread new messages is more than 1.
     * Only single group chat is supported.
     *
     * @param state True for display and false for no display
     */
    public void enableNewComingMessageIcon(boolean state) {
        RongConfigCenter.conversationConfig().setShowNewMessageBar(state);
    }

    /**
     * /~chinese 设置会话界面历史消息是否展示 注:历史消息大于10条即展示 目前仅支持单群聊
     *
     * @param state true 展示，false 不展示。
     */
    /**
     * /~english Set whether to display history messages in the conversation interface. Note:
     * History messages will be displayed if there are more than 10 historical messages. Only single
     * group chat is supported.
     *
     * @param state True for display and false for no display
     */
    public void enableUnreadMessageIcon(boolean state) {
        RongConfigCenter.conversationConfig().setShowHistoryMessageBar(state);
    }

    /**
     * /~chinese 注册消息模板。
     *
     * @param provider 模板类型。
     */
    /**
     * /~english Register the message template
     *
     * @param provider Template type
     */
    public static void registerMessageTemplate(IMessageProvider provider) {
        RongConfigCenter.conversationConfig().addMessageProvider(provider);
    }

    /**
     * /~chinese 设置接收消息的监听器。
     *
     * <p>所有接收到的消息、通知、状态都经由此处设置的监听器处理。包括私聊消息、讨论组消息、群组消息、聊天室消息以及各种状态。
     *
     * @param listener 接收消息的监听器。
     */
    /**
     * /~english Set the listener that receives the message All received messages, notifications,
     * and status are processed by the listeners set here It includes private chat messages,
     * discussion group messages, group messages, chat room messages, and various states.
     *
     * @param listener Listeners that receive messages
     */
    public static void addOnReceiveMessageListener(
            RongIMClient.OnReceiveMessageWrapperListener listener) {
        IMCenter.getInstance().addOnReceiveMessageListener(listener);
    }

    /**
     * /~chinese 移除接收消息的监听器。
     *
     * @param listener 接收消息的监听器。
     */
    /**
     * /~english Remove the listener that receives the message。
     *
     * @param listener Listeners that receive messages
     */
    public static void removeOnReceiveMessageListener(
            RongIMClient.OnReceiveMessageWrapperListener listener) {
        IMCenter.getInstance().removeOnReceiveMessageListener(listener);
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
    public static void setConnectionStatusListener(
            final RongIMClient.ConnectionStatusListener listener) {
        IMCenter.getInstance().addConnectionStatusListener(listener);
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
     * /~chinese Set the listener for the conversation list interface operation.
     *
     * @param listener Set the listener for the conversation list interface operation.
     */
    public static void setConversationListBehaviorListener(
            ConversationListBehaviorListener listener) {
        RongConfigCenter.conversationListConfig().setBehaviorListener(listener);
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
        IMCenter.getInstance()
                .setConversationNotificationStatus(
                        conversationType, targetId, notificationStatus, callback);
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
        IMCenter.getInstance().setConversationToTop(type, id, isTop, needCreate, callback);
    }

    /**
     * /~chinese 启动单聊界面。
     *
     * @param context 应用上下文。
     * @param targetUserId 要与之聊天的用户 Id。
     * @param title 聊天的标题。开发者需要在聊天界面获取该值, 再手动设置为聊天界面的标题。
     */
    /**
     * /~english Start the single chat interface
     *
     * @param context Application context.
     * @param targetUserId Id of the user to chat with
     * @param title The title of the chat Developers shall get this value in the chat interface, and
     *     then manually set it as the title of the chat interface.
     */
    public void startPrivateChat(Context context, String targetUserId, String title) {
        startConversation(context, Conversation.ConversationType.PRIVATE, targetUserId, title);
    }

    /**
     * /~chinese 启动群组聊天界面。
     *
     * @param context 应用上下文。
     * @param targetGroupId 要聊天的群组 Id。
     * @param title 聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值,
     *     再手动设置为聊天界面的标题。
     */

    /**
     * /~english Start the group chat interface
     *
     * @param context Application context.
     * @param targetGroupId Group Id to chat
     * @param title The title of the chat Developers shall get this value through intent.getData ().
     *     GetQueryParameter ("title") in the chat interface, and then manually set it as the title
     *     of the chat interface
     */
    public void startGroupChat(Context context, String targetGroupId, String title) {
        startConversation(context, Conversation.ConversationType.GROUP, targetGroupId, title);
    }

    /**
     * /~chinese
     *
     * <p>启动会话界面。
     *
     * @param context 应用上下文。
     * @param conversationType 会话类型。
     * @param targetId 根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title 聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值,
     *     再手动设置为聊天界面的标题。
     */

    /**
     * /~english Start the conversation interface
     *
     * @param context Application context.
     * @param conversationType Conversation type
     * @param targetId Depending on the conversationType, it may be user Id, discussion group Id,
     *     group Id or chat room Id
     * @param title The title of the chat Developers shall get this value through intent.getData ().
     *     GetQueryParameter ("title") in the chat interface, and then manually set it as the title
     *     of the chat interface
     */
    public void startConversation(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            String title) {
        startConversation(context, conversationType, targetId, title, 0);
    }

    /**
     * /~chinese
     *
     * <p>启动会话界面，并跳转到指定的消息位置
     *
     * <p>使用时，可以传入多种会话类型 {@link Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。 如果传入的是 {@link
     * Conversation.ConversationType#CHATROOM}，sdk 会默认调用 {@link RongIMClient#joinChatRoom(String,
     * int, RongIMClient.OperationCallback)} 加入聊天室。
     *
     * @param context 应用上下文。
     * @param conversationType 会话类型。
     * @param targetId 根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title 聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值,
     *     再手动设置为聊天界面的标题。
     * @param fixedMsgSentTime 需要定位的消息发送时间
     */

    /**
     * /~english Start the conversation interface and jump to the specified message location
     * Multiple conversation types Conversation.ConversationType can be passed in to correspond to
     * different conversation types and open different conversation interfaces in use. If
     * Conversation.ConversationType#CHATROOM is passed in, RongIMClient#joinChatRoom(String, int,
     * RongIMClient.OperationCallback) will be called by default to join the chat room.
     *
     * @param context Application context.
     * @param conversationType Conversation type
     * @param targetId Depending on the conversationType, it may be user Id, discussion group Id,
     *     group Id or chat room Id
     * @param title The title of the chat Developers shall get this value through intent.getData ().
     *     GetQueryParameter ("title") in the chat interface, and then manually set it as the title
     *     of the chat interface
     * @param fixedMsgSentTime Message sending time that shall be located
     */
    public void startConversation(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            String title,
            long fixedMsgSentTime) {
        if (context == null || TextUtils.isEmpty(targetId) || conversationType == null) {
            RLog.e(
                    TAG,
                    "startConversation. context, targetId or conversationType can not be empty!!!");
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
     * /~chinese 根据不同会话类型的目标Id，回调方式获取某一会话信息。
     *
     * @param type 会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 获取会话信息的回调。
     */

    /**
     * /~english Get information of a conversation according to the target Id, callback method of
     * different conversation types.
     *
     * @param type Conversation type
     * @param targetId Target Id Depending on the conversationType, it may be user Id, discussion
     *     group Id, group Id, or chatroom Id.
     * @param callback Callback for getting conversation information
     */
    public void getConversation(
            Conversation.ConversationType type,
            String targetId,
            RongIMClient.ResultCallback<Conversation> callback) {
        RongIMClient.getInstance().getConversation(type, targetId, callback);
    }

    /**
     * /~chinese 获取会话列表。
     *
     * @param callback 会话列表数据回调。
     */
    /**
     * /~english Get the list of conversations.
     *
     * @param callback Conversation list data callback.
     */
    public void getConversationList(RongIMClient.ResultCallback<List<Conversation>> callback) {
        RongIMClient.getInstance().getConversationList(callback);
    }

    /**
     * /~chinese 根据会话类型，回调方式获取会话列表。
     *
     * @param callback 获取会话列表的回调。
     * @param types 会话类型。
     */
    /**
     * /~english Get the conversation list according to the conversation type and callback method.
     *
     * @param callback Callback for getting the conversation list
     * @param types Conversation type
     */
    public void getConversationList(
            RongIMClient.ResultCallback<List<Conversation>> callback,
            Conversation.ConversationType... types) {
        RongIMClient.getInstance().getConversationList(callback, types);
    }

    /**
     * /~chinese 获取会话消息提醒状态。
     *
     * <p><strong>注意：不支持聊天室！</strong>
     *
     * @param conversationType 会话类型，不支持聊天室（聊天室默认是不接受会话消息提醒的）。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param callback 获取消息提醒状态的回调。
     * @group 会话
     * @see io.rong.imlib.model.Conversation.ConversationNotificationStatus
     */

    /**
     * /~english Get the conversation message reminder status.
     *
     * <p><strong>Note: The chat rooms are not supported!</strong>
     *
     * @param conversationType Conversation type The chat room is not supported because the chat
     *     rooms do not accept message reminders by default.
     * @param targetId Conversation id. Depending on different conversationType, it may be user id,
     *     discussion group id or group id.
     * @param callback Callback for getting the message reminder status.
     */
    public void getConversationNotificationStatus(
            final Conversation.ConversationType conversationType,
            final String targetId,
            final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>
                    callback) {
        RongIMClient.getInstance()
                .getConversationNotificationStatus(conversationType, targetId, callback);
    }

    /**
     * /~chinese 获取当前用户设置的黑名单列表。
     *
     * @param callback 获取黑名单回调。参考 {@link io.rong.imlib.RongIMClient.GetBlacklistCallback} 。
     * @group 高级功能
     */

    /**
     * ~english Get a blacklist set by the current user.
     *
     * @param callback Callback for getting blacklist Refer to {@link
     *     io.rong.imlib.RongIMClient.GetBlacklistCallback}.
     */
    public void getBlacklist(RongIMClient.GetBlacklistCallback callback) {
        RongIMClient.getInstance().getBlacklist(callback);
    }

    /**
     * /~chinese 获取某用户是否在黑名单中。
     *
     * @param userId 用户 id。
     * @param callback 获取用户是否在黑名单回调。
     * @group 高级功能
     * @see io.rong.imlib.RongIMClient.BlacklistStatus
     */

    /**
     * /~english Check whether a user is on the blacklist.
     *
     * @param userId User id.
     * @param callback Callback for getting whether the user is in the blacklist
     * @see io.rong.imlib.RongIMClient.BlacklistStatus
     */
    public void getBlacklistStatus(
            String userId, RongIMClient.ResultCallback<RongIMClient.BlacklistStatus> callback) {
        RongIMClient.getInstance().getBlacklistStatus(userId, callback);
    }

    /**
     * /~chinese 获取连接状态。
     *
     * @return 连接状态枚举。
     */
    /**
     * /~english Get the connection status.
     *
     * @return Connection status enumeration
     */
    public RongIMClient.ConnectionStatusListener.ConnectionStatus getCurrentConnectionStatus() {
        return IMCenter.getInstance().getCurrentConnectionStatus();
    }

    /**
     * /~chinese 获取本地时间与服务器时间的时间差。
     *
     * <p>消息发送成功后，SDK 与服务器同步时间，消息所在数据库中存储的时间就是服务器时间。 System.currentTimeMillis() - getDeltaTime()
     * 可以获取服务器当前时间。
     *
     * @return 本地时间与服务器时间的差值。
     * @group 数据获取
     */

    /**
     * /~english Get the time difference between the local time and the server time.
     *
     * <p>After the message is sent successfully, the SDK synchronizes the time with the server, and
     * the time stored in the database where the message is located is the server time.
     * System.currentTimeMillis() - getDeltaTime() can get the current time of the server.
     *
     * @return
     */
    public long getDeltaTime() {
        return RongIMClient.getInstance().getDeltaTime();
    }

    /**
     * /~chinese 获取会话中，从指定消息之前、指定数量的、指定消息类型的最新消息实体。
     *
     * @param conversationType 会话类型，参考 {@link Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param oldestMessageId 最后一条消息的 id。获取此消息之前的 count 条消息，没有消息第一次调用应设置为 -1。
     * @param count 要获取的消息数量。
     * @param callback 获取历史消息的回调，按照时间顺序从新到旧排列。 @@group 消息操作
     */

    /**
     * /~english Get the latest message entity of the specified number and specified message types
     * before the specified message in the conversation.
     *
     * @param conversationType Type of conversation, which does not support chat rooms. Refer to
     *     Conversation.ConversationType.
     * @param targetId Conversation id. Depending on differnet conversationType, it may be user id,
     *     discussion group id or group id.
     * @param oldestMessageId Id of the last message. Get the messages of the count value before
     *     this message. If there are no messages, it shall be set as -1 for the first call.
     * @param count Number of messages to get
     * @param callback Callback for getting historical messages, which are in chronological order
     *     from earliest to most recent.
     */
    public void getHistoryMessages(
            Conversation.ConversationType conversationType,
            String targetId,
            int oldestMessageId,
            int count,
            RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance()
                .getHistoryMessages(conversationType, targetId, oldestMessageId, count, callback);
    }

    /**
     * /~chinese 获取会话中符合条件的消息列表。
     *
     * <p>返回的消息中不包含 oldestMessageId 对应那条消息，如果会话中的消息数量小于参数 count 的值，会将该会话中的所有消息返回。 如：oldestMessageId
     * 为 10，count 为 2，会返回 messageId 为 9 和 8 的 Message 对象列表。<br>
     *
     * @param conversationType 会话类型 {@link io.rong.imlib.model.Conversation.ConversationType}
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 。
     * @param objectName 消息类型标识。
     * @param oldestMessageId 最后一条消息的 Id，获取此消息之前的 count 条消息,没有消息第一次调用应设置为:-1。
     * @param count 要获取的消息数量
     * @param callback 获取历史消息的回调，按照时间顺序从新到旧排列。
     * @group 消息操作
     */

    /**
     * /~english Gets a list of eligible messages in the conversation.
     *
     * <p>The returned message does not contain the message corresponding to oldestMessageId. If the
     * number of messages in the conversation is less than the value of the parameter count, all
     * messages in the conversation will be returned. For example, if the oldestMessageId is 10 and
     * the count is 2, a list of Message objects with messageId as 9 and 8 will be returned. Note:
     * Do not support to pull the historical messages of the chat room
     * Conversation.ConversationType.CHATROOM
     *
     * @param conversationType Type of conversation Conversation.ConversationType, the chat room is
     *     not supported.
     * @param targetId Conversation id. Depending on differnet conversationType, it may be user id,
     *     discussion group id or group id.
     * @param objectName Identifier of message type
     * @param oldestMessageId Id of the last message and messages of the count value before this
     *     message is got. If there is no message, it should be set to -1 in case of the first call.
     * @param count Number of messages to get
     * @param callback Callback for getting historical messages, which are in chronological order
     *     from earliest to most recent.
     */
    public void getHistoryMessages(
            Conversation.ConversationType conversationType,
            String targetId,
            String objectName,
            int oldestMessageId,
            int count,
            RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance()
                .getHistoryMessages(
                        conversationType, targetId, objectName, oldestMessageId, count, callback);
    }

    /**
     * /~chinese 获取指定会话的最新消息。
     *
     * @param conversationType 会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param count 需要获取的消息数量。
     * @param callback 获取最新消息的回调，按照时间顺序从新到旧排列，如果会话中的消息数量小于参数 count 的值，会将该会话中的所有消息返回。
     * @group 消息操作
     */

    /**
     * /~english Gets the latest messages from the specified conversation.
     *
     * @param conversationType Conversation type {@link
     *     io.rong.imlib.model.Conversation.ConversationType}.
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param count Number of messages to be got.
     * @param callback Callback for getting the latest messages, which are in chronological order
     *     from earliest to most recent. If the number of messages in the conversation is less than
     *     the value of the parameter count, all messages in the conversation will be returned.
     */
    public void getLatestMessages(
            Conversation.ConversationType conversationType,
            String targetId,
            int count,
            RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance().getLatestMessages(conversationType, targetId, count, callback);
    }

    /**
     * /~chinese 获取己关注公共账号列表。
     *
     * @param callback 获取己关注公共账号列表回调。
     * @group 公众号
     * @see PublicServiceProfileList
     */

    /**
     * /~english Get a list of followed public accounts.
     *
     * @param callback Callback for getting the list of public accounts you have followed
     * @see PublicServiceProfileList
     */
    public void getPublicServiceList(
            final RongIMClient.ResultCallback<PublicServiceProfileList> callback) {
        RongIMClient.getInstance()
                .getPublicServiceList(
                        new RongIMClient.ResultCallback<PublicServiceProfileList>() {
                            @Override
                            public void onSuccess(
                                    PublicServiceProfileList publicServiceProfileList) {
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
     * /~chinese 获取某公共服务信息。
     *
     * @param publicServiceType 公众服务类型。参考 {@link io.rong.imlib.model.Conversation.PublicServiceType}
     *     。
     * @param publicServiceId 公共服务 id。
     * @param callback 公共账号信息回调。
     * @group 公众号
     * @see PublicServiceProfile
     */

    /**
     * /~english Get some public service information.
     *
     * @param publicServiceType Types of public services Refer to {@link
     *     io.rong.imlib.model.Conversation.PublicServiceType}
     * @param publicServiceId Public Service id
     * @param callback Callback for public account information
     * @see PublicServiceProfile
     */
    public void getPublicServiceProfile(
            Conversation.PublicServiceType publicServiceType,
            String publicServiceId,
            final RongIMClient.ResultCallback<PublicServiceProfile> callback) {
        RongIMClient.getInstance()
                .getPublicServiceProfile(
                        publicServiceType,
                        publicServiceId,
                        new RongIMClient.ResultCallback<PublicServiceProfile>() {
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
     * /~chinese 从服务器端获取指定时间之前的历史消息。
     *
     * <p>区别于 {@link
     * #getHistoryMessages}，该接口是从融云服务器中拉取。从服务端拉取消息后，客户端会做排重，返回排重后的数据。通常用于更换新设备后，拉取历史消息。<br>
     * 使用的时候，建议优先通过 {@link #getHistoryMessages(Conversation.ConversationType, String, int, int,
     * RongIMClient.ResultCallback)} 从本地数据库拉取历史消息，
     * 当本地数据库没有历史消息后，再通过此接口获取服务器历史消息，时间戳传入本地数据库里最早的消息时间戳。<br>
     * <strong>注意：<br>
     * 1. 此功能需要在融云开发者后台开启历史消息云存储功能。<br>
     * 2. 当本地数据库中已存在将要获取的消息时，此接口不会再返回数据。</strong>
     *
     * @param conversationType 会话类型。
     * @param targetId 目标会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param dateTime 从该时间点开始获取消息。即：消息中的 sentTime {@link Message#getSentTime()}；如果本地库中没有消息，第一次可传
     *     0，否则传入最早消息的sentTime，获取最新 count 条。
     * @param count 需要获取的消息数量， 0 < count <= 20。
     * @param callback 获取历史消息的回调，按照时间顺序从新到旧排列。
     * @group 消息操作
     */

    /**
     * /~english Get historical message before the specified time from the server.
     *
     * <p>It is different from getHistoryMessages(io.rong.imlib.model.Conversation.ConversationType,
     * java.lang.String, int, int). This interface is pulled from the RongCloud server. After
     * messages are pulled from the server, the client will sort them again and return the sorted
     * data. It is usually used to pull historical messages after replacement of new equipment. When
     * it is used, it is recommended to pull the historical messages from the local database through
     * #getHistoryMessages(Conversation.ConversationType, String, int, int, ResultCallback) When
     * there are no historical messages in the local database, you can get the historical message on
     * the server through this interface and the earliest message timestamp in the local database is
     * transferred in. Note: 1. To enable this function, you need to enable the historical message
     * cloud storage function at the backend of the RongCLoud developer platform. 2. This interface
     * does not return data when the message to be fetched already exists in the local database.
     *
     * @param conversationType Conversation type
     * @param targetId Id of destination conversation. Depending on differnet conversationType, it
     *     may be user id, discussion group id or group id.
     * @param dateTime Get the message from this point in time Namely, sentTime {@link
     *     Message#getSentTime()} in the message; 0 can be passed for the first time if there is no
     *     message in the local library, otherwise the sentTime of the earliest message is passed to
     *     get the latest count messages.
     * @param count The number of messages to be obtained, 0 < count <= 20.
     * @param callback Callback for getting historical messages, which are in chronological order
     *     from earliest to most recent.
     */
    public void getRemoteHistoryMessages(
            Conversation.ConversationType conversationType,
            String targetId,
            long dateTime,
            int count,
            RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance()
                .getRemoteHistoryMessages(conversationType, targetId, dateTime, count, callback);
    }

    /**
     * /~chinese 获取指定会话远端历史消息。
     *
     * <p>此功能需要在融云开发者后台开启历史消息云存储功能。<br>
     * <strong>注意：不支持聊天室！</strong>
     *
     * @param conversationType 会话类型，不支持聊天室。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param remoteHistoryMsgOption 可配置的参数 {@link RemoteHistoryMsgOption}
     * @param callback 获取历史消息的回调，按照时间顺序从新到旧排列。
     * @group 消息操作
     */

    /**
     * /~english Get the remote historical message from the specified conversation.
     *
     * <p>To enable this function, you need to enable the historical message cloud storage function
     * at the backend of the RongCLoud developer platform. Note: The chat rooms are not supported!
     *
     * @param conversationType Type of conversation, which does not support chat rooms.
     * @param targetId Conversation id. Depending on differnet conversationType, it may be user id,
     *     discussion group id or group id.
     * @param remoteHistoryMsgOption Configurable parameters {@link RemoteHistoryMsgOption}
     * @param callback Callback for getting historical messages, which are in chronological order
     *     from earliest to most recent.
     */
    public void getRemoteHistoryMessages(
            Conversation.ConversationType conversationType,
            String targetId,
            RemoteHistoryMsgOption remoteHistoryMsgOption,
            RongIMClient.ResultCallback<List<Message>> callback) {
        RongIMClient.getInstance()
                .getRemoteHistoryMessages(
                        conversationType, targetId, remoteHistoryMsgOption, callback);
    }

    /**
     * /~chinese 获取会话中的草稿信息。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id 或聊天室 id。
     * @param callback 获取草稿文字内容的回调。
     * @group 会话
     */

    /**
     * /~english Get draft information in a conversation.
     *
     * @param conversationType Conversation type Refer to {@link
     *     io.rong.imlib.model.Conversation.ConversationType} .
     * @param targetId Conversation id. Depending on the conversationType, it may be user id,
     *     discussion group id, group id, or chatroom id.
     * @param callback Callback for getting the text content of the draft.
     */
    public void getTextMessageDraft(
            Conversation.ConversationType conversationType,
            String targetId,
            RongIMClient.ResultCallback<String> callback) {
        RongIMClient.getInstance().getTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * /~chinese 获取指定会话的未读消息数。
     *
     * <p><Strong>注意：不包含聊天室，聊天室消息不计数。</Strong>
     *
     * @param conversationType 会话类型 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param targetId 会话 id。根据不同的 conversationType，可能是用户 id、讨论组 id、群组 id。
     * @param callback 未读消息数的回调。
     * @group 会话
     */

    /**
     * /~english Get the number of unread messages for the specified conversation.
     *
     * <p><Strong>Note: Chat rooms are not included and messages in chat rooms are not
     * counted.</Strong>
     *
     * @param conversationType Conversation type {@link
     *     io.rong.imlib.model.Conversation.ConversationType}.
     * @param targetId Conversation id. Depending on differnet conversationType, it may be user id,
     *     discussion group id or group id.
     * @param callback Callback for the number of unread messages
     */
    public void getUnreadCount(
            Conversation.ConversationType conversationType,
            String targetId,
            RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationType, targetId, callback);
    }

    /**
     * /~chinese 获取指定会话类型的总未读消息数。
     *
     * <p><Strong>注意：不包含聊天室，聊天室消息不计数。</Strong>
     *
     * @param callback 未读消息数的回调。
     * @param conversationTypes 会话类型，可传多个会话类型。
     * @group 会话
     */

    /**
     * /~english Get the total number of unread messages for the specified conversation type.
     *
     * <p><Strong>Note: Chat rooms are not included and messages in chat rooms are not
     * counted.</Strong>
     *
     * @param callback Callback for the number of unread messages
     * @param conversationTypes Type of conversation. Multiple conversation types can be passed.
     */
    public void getUnreadCount(
            RongIMClient.ResultCallback<Integer> callback,
            Conversation.ConversationType... conversationTypes) {
        RongIMClient.getInstance().getUnreadCount(callback, conversationTypes);
    }

    /**
     * /~chinese 获取指定会话类型的总未读消息数。
     *
     * <p><Strong>注意：不包含聊天室，聊天室消息不计数。</Strong>
     *
     * @param conversationTypes 会话类型数组 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param containBlocked 是否包含免打扰消息的未读消息数。
     * @param callback 未读消息数的回调。
     * @group 会话
     */

    /**
     * /~english Get the total number of unread messages for the specified conversation type.
     *
     * <p><Strong>Note: Chat rooms are not included and messages in chat rooms are not
     * counted.</Strong>
     *
     * @param conversationTypes An array of conversation types {@link
     *     io.rong.imlib.model.Conversation.ConversationType}.
     * @param containBlocked Whether the number of unread Do Not Disturb messages is included.
     * @param callback Callback for the number of unread messages
     */
    public void getUnreadCount(
            Conversation.ConversationType[] conversationTypes,
            boolean containBlocked,
            RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationTypes, containBlocked, callback);
    }

    /**
     * /~chinese 获取指定会话类型的总未读消息数。
     *
     * <p><Strong>注意：不包含聊天室，聊天室消息不计数。</Strong>
     *
     * @param conversationTypes 会话类型数组 {@link io.rong.imlib.model.Conversation.ConversationType} 。
     * @param callback 未读消息数的回调。
     * @group 会话
     */

    /**
     * /~english Get the total number of unread messages for the specified conversation type.
     *
     * <p><Strong>Note: Chat rooms are not included and messages in chat rooms are not
     * counted.</Strong>
     *
     * @param conversationTypes An array of conversation types {@link
     *     io.rong.imlib.model.Conversation.ConversationType}.
     * @param callback Callback for the number of unread messages
     */
    public void getUnreadCount(
            Conversation.ConversationType[] conversationTypes,
            RongIMClient.ResultCallback<Integer> callback) {
        RongIMClient.getInstance().getUnreadCount(conversationTypes, callback);
    }

    /**
     * /~chinese
     *
     * <p>设置用户信息的提供者，供 RongIM 调用获取用户名称和头像信息。 设置后，当 sdk 界面展示用户信息时，会回调 {@link
     * UserDataProvider.UserInfoProvider#getUserInfo(String)} 使用者只需要根据对应的 userId 提供对应的用户信息。
     * 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。 在异步请求结果返回后，根据返回的结果调用 {@link
     * #refreshUserInfoCache(UserInfo)} 刷新用户信息。
     *
     * @param userInfoProvider 用户信息提供者 {@link UserDataProvider.UserInfoProvider}。
     * @param isCacheUserInfo 设置是否由 IMKit 来缓存用户信息。<br>
     *     如果 App 提供的 UserInfoProvider。 每次都需要通过网络请求用户数据，而不是将用户数据缓存到本地，会影响用户信息的加载速度；<br>
     *     此时最好将本参数设置为 true，由 IMKit 来缓存用户信息。
     */

    /**
     * /~english Set the provider of the user information for the RongIM call to get the user name
     * and portrait information After setting, when the user information is displayed in the sdk
     * interface, the UserDataProvider.UserInfoProvider#getUserInfo(String) user will be called back
     * and only shall provide the corresponding user information according to the corresponding
     * userId. If you shall get user information asynchronously from the server, the consumer can
     * initiate an asynchronous request in this method and then return null information. After the
     * result of the asynchronous request is returned, refreshUserInfoCache(UserInfo) is called to
     * refresh the user information according to the returned result.
     *
     * @param userInfoProvider User information provider UserDataProvider.UserInfoProvider.
     * @param isCacheUserInfo Set whether user information is cached by IMKit. If the
     *     UserInfoProvider provided by App. Each time you shall request user data over the network
     *     instead of caching it locally, which will affect the loading speed of user information.
     *     In this case, it is best to set this parameter to true, to cache user information by
     *     IMKit.
     */
    public static void setUserInfoProvider(
            UserDataProvider.UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        RongUserInfoManager.getInstance().setUserInfoProvider(userInfoProvider, isCacheUserInfo);
    }

    /**
     * /~chinese 刷新用户缓存数据。
     *
     * @param userInfo 需要更新的用户缓存数据。
     */
    /**
     * /~english Refresh user cache data
     *
     * @param userInfo User cache data that shall be updated
     */
    public void refreshUserInfoCache(UserInfo userInfo) {
        RongUserInfoManager.getInstance().refreshUserInfoCache(userInfo);
    }

    /**
     * /~chinese 刷新群组缓存数据。
     *
     * @param group 需要更新的群组缓存数据。
     */

    /**
     * /~english Refresh the group cache data
     *
     * @param group Group cache data that shall be updated
     */
    public void refreshGroupInfoCache(Group group) {
        RongUserInfoManager.getInstance().refreshGroupInfoCache(group);
    }

    /**
     * /~chinese 注册会话列表消息模板提供者。
     *
     * @param provider 会话列表模板提供者。
     */
    /**
     * /~english Register the conversation list message template provider
     *
     * @param provider Conversation list template provider
     */
    public void registerConversationTemplate(BaseConversationProvider provider) {
        RongConfigCenter.conversationListConfig().getProviderManager().addProvider(provider);
    }

    /**
     * /~chinese 设置会话通知免打扰时间。
     *
     * @param startTime 起始时间 格式 HH:MM:SS。
     * @param spanMinutes 间隔分钟数大于 0 小于 1440。
     * @param callback 设置会话通知免打扰时间回调。
     */
    /**
     * /~english Set the conversation notification do not disturb time
     *
     * @param startTime Start time format HH:MM:SS
     * @param spanMinutes The interval (in minutes) is greater than 0 and less than 1440
     * @param callback Callback for setting conversation notification do not disturb time
     */
    public void setNotificationQuietHours(
            final String startTime,
            final int spanMinutes,
            final RongIMClient.OperationCallback callback) {
        RongNotificationManager.getInstance()
                .setNotificationQuietHours(startTime, spanMinutes, callback);
    }

    /**
     * /~chinese 获取会话通知免打扰时间。
     *
     * @param callback 获取会话通知免打扰时间回调。
     */
    /**
     * /~chinese Get the conversation notification do not disturb time.
     *
     * @param callback Get the conversation notification do not disturb time
     */
    public void getNotificationQuietHours(
            final RongIMClient.GetNotificationQuietHoursCallback callback) {
        RongNotificationManager.getInstance().getNotificationQuietHours(callback);
    }

    /**
     * /~chinese 移除会话通知免打扰时间。
     *
     * @param callback 移除会话通知免打扰时间回调。
     */
    /**
     * /~english Remove the conversation notification do not disturb time
     *
     * @param callback Remove the conversation notification do not disturb time.
     */
    public void removeNotificationQuietHours(final RongIMClient.OperationCallback callback) {
        RongNotificationManager.getInstance().removeNotificationQuietHours(callback);
    }

    /**
     * /~chinese 注销已注册的未读消息数变化监听器。
     *
     * @param observer 接收未读消息消息的监听器。
     */
    /**
     * /~english Unregister the registered unread message number change listener
     *
     * @param observer Listeners that receive unread message messages
     */
    public void removeUnReadMessageCountChangedObserver(
            final UnReadMessageManager.IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        UnReadMessageManager.getInstance().removeObserver(observer);
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
        IMCenter.getInstance()
                .sendDirectionalMediaMessage(message, userIds, pushContent, pushData, callback);
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
        IMCenter.getInstance()
                .sendDirectionalMessage(
                        type, targetId, content, userIds, pushContent, pushData, callback);
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
        IMCenter.getInstance()
                .sendLocationMessage(message, pushContent, pushData, sendMessageCallback);
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
        IMCenter.getInstance().sendMediaMessage(message, pushContent, pushData, callback);
    }

    /**
     * /~chinese 发送多媒体消息，可以使用该方法将多媒体文件上传到自己的服务器。
     *
     * <p>上传多媒体文件时，会回调 {@link
     * io.rong.imlib.IRongCallback.ISendMediaMessageCallbackWithUploader#onAttached(Message,
     * IRongCallback.MediaMessageUploader)} 此回调中携带 {@link IRongCallback.MediaMessageUploader}
     * 对象，使用者只需要调用此对象中的 <br>
     * {@link IRongCallback.MediaMessageUploader#update(int)} 更新进度 <br>
     * {@link IRongCallback.MediaMessageUploader#success(Uri)} 更新成功状态，并告知上传成功后的文件地址 <br>
     * {@link IRongCallback.MediaMessageUploader#error()} 更新失败状态 <br>
     * {@link IRongCallback.MediaMessageUploader#cancel()} ()} 更新取消状态 <br>
     *
     * @param message 发送消息的实体。
     * @param pushContent 当下发远程推送消息时，在通知栏里会显示这个字段。 如果发送的是自定义消息，该字段必须填写，否则无法收到远程推送消息。 如果发送 SDK
     *     中默认的消息类型，例如: RC:TxtMsg, RC:VcMsg, RC:ImgMsg, RC:FileMsg，则不需要填写，默认已经指定。
     * @param pushData 远程推送附加信息。如果设置该字段，用户在收到远程推送消息时，能通过 {@link
     *     io.rong.push.notification.PushNotificationMessage#getPushData()} 方法获取。
     * @param callback 发送消息的回调，回调中携带 {@link IRongCallback.MediaMessageUploader} 对象，用户调用该对象中的方法更新状态。
     * @group 消息操作
     */

    /**
     * /~english Send multimedia messages, which can be used to upload multimedia files to your own
     * server
     *
     * <p>When multimedia files are uploaded,
     * IRongCoreCallback.ISendMediaMessageCallbackWithUploader.onAttached(Message,
     * IRongCoreCallback.MediaMessageUploader) will be called back. This callback carries
     * IRongCoreCallback.MediaMessageUploader object and a user only needs to call
     * IRongCoreCallback.MediaMessageUploader.update(int) in this object, update the progress
     * IRongCoreCallback.MediaMessageUploader.success(Uri) and success state, inform the address of
     * successfully uploaded files IRongCoreCallback.MediaMessageUploader.error(), and update
     * failure status IRongCoreCallback.MediaMessageUploader.cancel() ()} Update cancel status
     *
     * @param message The entity that sent the message.
     * @param pushContent This field is displayed in the notification bar when a remote push message
     *     is sent. If you are sending a custom message, this field must be completed, otherwise you
     *     will not be able to receive a remote push message. If you send the default message type
     *     in SDK, such as RC:TxtMsg, RC:VcMsg, RC:ImgMsg and RC:FileMsg, this field does not need
     *     to be filled in and has been specified by default.
     * @param pushData Push additional information remotely. If this field is set, users can get it
     *     by using io.rong.push.notification.PushNotificationMessage#getPushData() when they
     *     receive a push message.
     * @param callback Callback for sending messages, which carries
     *     IRongCoreCallback.MediaMessageUploader object and a user calls the method in this object
     *     to update the status.
     */
    public void sendMediaMessage(
            Message message,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMediaMessageCallbackWithUploader callback) {
        IMCenter.getInstance().sendMediaMessage(message, pushContent, pushData, callback);
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
     * @param callback Callback for sending messages, refer to {@link
     *     io.rong.imlib.IRongCallback.ISendMessageCallback}.
     */
    public void sendMessage(
            Message message,
            String pushContent,
            final String pushData,
            final IRongCallback.ISendMessageCallback callback) {
        IMCenter.getInstance().sendMessage(message, pushContent, pushData, callback);
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
        IMCenter.getInstance().sendMessage(message, pushContent, pushData, option, callback);
    }

    /**
     * /~chinese
     *
     * <p>设置群组信息的提供者。
     *
     * <p>设置后，当 sdk 界面展示群组信息时，会回调 {@link UserDataProvider.GroupInfoProvider#getGroupInfo(String)}
     * 使用者只需要根据对应的 groupId 提供对应的群组信息。 如果需要异步从服务器获取群组信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupInfoCache(Group)} 刷新信息。
     *
     * @param groupInfoProvider 群组信息提供者。
     * @param isCacheGroupInfo 设置是否由 IMKit 来缓存用户信息。<br>
     *     如果 App 提供的 GroupInfoProvider。 每次都需要通过网络请求群组数据，而不是将群组数据缓存到本地，会影响群组信息的加载速度；<br>
     *     此时最好将本参数设置为 true，由 IMKit 来缓存群组信息。
     */

    /**
     * /~english Set the provider of group information After the setting is set, when the group
     * information is displayed in the sdk interface, it will call back
     * UserDataProvider.GroupInfoProvider#getGroupInfo(String) and users only shall provide the
     * corresponding group information according to the corresponding groupId. If you shall get the
     * group information asynchronously from the server, the consumer can initiate an asynchronous
     * request in this method and then return the null information. After the result of the
     * asynchronous request is returned, refreshGroupInfoCache(Group) refresh information is called
     * based on the returned result.
     *
     * @param groupInfoProvider Group information provider
     * @param isCacheGroupInfo Set whether user information is cached by IMKit. If the
     *     GroupInfoProvider provided by App. Each time you shall request group data through the
     *     network instead of caching the group data locally, which will affect the loading speed of
     *     the group information. In this case, it is best to set this parameter to true, and cache
     *     the group information by IMKit.
     */
    public static void setGroupInfoProvider(
            UserDataProvider.GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {
        RongUserInfoManager.getInstance().setGroupInfoProvider(groupInfoProvider, isCacheGroupInfo);
    }

    /**
     * /~chinese
     *
     * <p>设置GroupUserInfo提供者，供RongIM 调用获取GroupUserInfo
     *
     * <p>可以使用此方法，修改群组中用户昵称
     *
     * <p>设置后，当 sdk 界面展示用户信息时，会回调 {@link
     * UserDataProvider.GroupUserInfoProvider#getGroupUserInfo(String, String)} 使用者只需要根据对应的 groupId,
     * userId 提供对应的用户信息 {@link GroupUserInfo}。 如果需要异步从服务器获取用户信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshGroupUserInfoCache(GroupUserInfo)} 刷新信息。
     *
     * @param groupUserInfoProvider 群组用户信息提供者。
     * @param isCacheGroupUserInfo 设置是否由 IMKit 来缓存 GroupUserInfo。<br>
     *     如果 App 提供的 GroupUserInfoProvider。 每次都需要通过网络请求数据，而不是将数据缓存到本地，会影响信息的加载速度；<br>
     *     此时最好将本参数设置为 true，由 IMKit 来缓存信息。
     */

    /**
     * /~english Set the GroupUserInfo provider for RongIM calls to get GroupUserInfo
     *
     * <p>You can use this method to modify user nicknames in the group
     *
     * <p>After setting, when the user information is displayed in the sdk interface, the
     * UserDataProvider.GroupUserInfoProvider#getGroupUserInfo(String, String) will be called back
     * and the user only shall provide the corresponding user information GroupUserInfo according to
     * the corresponding groupId, userId. If you shall get user information asynchronously from the
     * server, the consumer can initiate an asynchronous request in this method and then return null
     * information. After the result of the asynchronous request is returned,
     * refreshGroupUserInfoCache(GroupUserInfo) refresh information is called based on the returned
     * result.
     *
     * @param groupUserInfoProvider Group user information provider.
     * @param isCacheGroupUserInfo Set whether GroupUserInfo is cached by IMKit. If the
     *     GroupUserInfoProvider is provided by App Each time you shall request data over the
     *     network instead of caching the data locally, which will affect the loading speed of the
     *     information. In this case, it is best to set this parameter to true, and the information
     *     is cached by IMKit.
     */
    public static void setGroupUserInfoProvider(
            UserDataProvider.GroupUserInfoProvider groupUserInfoProvider,
            boolean isCacheGroupUserInfo) {
        RongUserInfoManager.getInstance()
                .setGroupUserInfoProvider(groupUserInfoProvider, isCacheGroupUserInfo);
    }

    /**
     * /~chinese 设置位置信息的提供者。
     *
     * @param locationProvider 位置信息提供者。
     */
    /**
     * /~english Set the provider of location information.
     *
     * @param locationProvider Location information provider.
     */
    public static void setLocationProvider(LocationManager.LocationProvider locationProvider) {
        LocationManager.getInstance().setLocationProvider(locationProvider);
    }

    /**
     * /~chinese 设置群组成员提供者。
     *
     * <p>'@' 功能和VoIP功能在选人界面,需要知道群组内成员信息,开发者需要设置该提供者。 开发者需要在回调中获取到群成员信息 并通过{@link
     * RongMentionManager.IGroupMemberCallback}中的方法设置到 sdk 中
     *
     * <p>
     *
     * @param groupMembersProvider 群组成员提供者。
     */
    /**
     * /~english Set the group member provider The '@ 'function and the VoIP function are in the
     * selection interface. You shall know the information about the members in the group, and the
     * developer shall set the provider. The developer shall obtain the group member information in
     * the callback and set it to the sdk through the method in
     * RongMentionManager.IGroupMemberCallback.
     *
     * @param groupMembersProvider Group member provider。
     */
    public void setGroupMembersProvider(
            RongMentionManager.IGroupMembersProvider groupMembersProvider) {
        RongMentionManager.getInstance().setGroupMembersProvider(groupMembersProvider);
    }

    /**
     * /~chinese 获取当前连接用户的信息。
     *
     * @return 当前连接用户的信息。
     * @group 数据获取
     */

    /**
     * /~english Get information about the currently connected user.
     *
     * @return Information about the currently connected user.
     */
    public String getCurrentUserId() {
        return RongIMClient.getInstance().getCurrentUserId();
    }

    /**
     * /~chinese 设置语音消息的最大长度
     *
     * @param sec 默认值是60s
     */
    /**
     * /~english Set the maximum length of voice messages.
     *
     * @param sec The default value is 60s
     */
    public void setMaxVoiceDuration(int sec) {
        AudioRecordManager.getInstance().setMaxVoiceDuration(sec);
    }

    /**
     * /~chinese 语音消息类型
     *
     * @return 当前设置的语音消息类型
     */

    /**
     * /~english Voice message type
     *
     * @return Currently set voice message type
     */
    public IMCenter.VoiceMessageType getVoiceMessageType() {
        return RongConfigCenter.featureConfig().getVoiceMessageType();
    }

    /**
     * /~chinese 设置语音消息类型
     *
     * @param voiceMessageType 消息类型{@link IMCenter.VoiceMessageType}
     */
    /**
     * /~english Set voice message type
     *
     * @param voiceMessageType Message type IMCenter.VoiceMessageType
     */
    public void setVoiceMessageType(IMCenter.VoiceMessageType voiceMessageType) {
        RongConfigCenter.featureConfig().setVoiceMessageType(voiceMessageType);
    }

    /**
     * /~chinese 设置语音消息采样率
     *
     * @param sampleRate 消息采样率{@link AudioRecordManager.SamplingRate}
     */
    /**
     * /~english Set voice message sampling rate
     *
     * @param sampleRate Message sampling rateAudio RecordManager.SamplingRate
     */
    public void setSamplingRate(AudioRecordManager.SamplingRate sampleRate) {
        AudioRecordManager.getInstance().setSamplingRate(sampleRate);
    }

    /**
     * /~chinese 语音消息采样率
     *
     * @return 当前设置的语音采样率
     */

    /**
     * /~english Sampling rate of voice message
     *
     * @return Currently set voice sampling rate
     */
    public int getSamplingRate() {
        return AudioRecordManager.getInstance().getSamplingRate();
    }

    /**
     * /~chinese 设置消息拦截器。
     *
     * @param interceptor 消息发送或接受的拦截器。
     */
    /**
     * /~english 设置消息拦截器。
     *
     * @param interceptor 消息发送或接受的拦截器。
     */
    public void setMessageInterceptor(MessageInterceptor interceptor) {
        IMCenter.getInstance().setMessageInterceptor(interceptor);
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
        IMCenter.getInstance().setMessageSentStatus(message, callback);
    }

    /**
     * /~chinese 设置公众号界面操作的监听器。
     *
     * @param listener 会话公众号界面操作的监听器。
     */

    /**
     * /~english Set the listener for the operation of the official account interface
     *
     * @param listener Listeners operated by the official account interface of the conversation
     */
    public static void setPublicServiceBehaviorListener(
            PublicServiceManager.PublicServiceBehaviorListener listener) {
        PublicServiceManager.getInstance().setPublicServiceBehaviorListener(listener);
    }

    /**
     * /~chinese 设置公众服务菜单点击监听。 建议使用方法：在进入对应公众服务会话时，设置监听。当退出会话时，重置监听为 null，这样可以防止内存泄露。
     *
     * @param menuClickListener 监听。
     */

    /**
     * /~english Set the public service menu and click listening Recommended method: set listening
     * when entering the corresponding public service conversation When exiting the conversation,
     * reset the listening to null, to prevent memory leaks
     *
     * @param menuClickListener Listening.
     */
    public void setPublicServiceMenuClickListener(
            IPublicServiceMenuClickListener menuClickListener) {
        PublicServiceManager.getInstance().setPublicServiceMenuClickListener(menuClickListener);
    }

    /**
     * /~chinese 设置公众服务账号信息的提供者，供 RongIM 调用获公众服务账号名称，头像信息和公众服务号菜单。
     *
     * <p>目前 sdk 默认的公众号服务不需要开发者设置，这个接口提供了另外一种从 app 层设置公众服务账号信息的方式 设置后，当 sdk 界面展示用户信息时，会回调 {@link
     * PublicServiceManager.PublicServiceProfileProvider#getPublicServiceProfile(Conversation.PublicServiceType,
     * String)} 使用者只需要根据对应的publicServiceType, publicServiceId 提供对应的公众服务账号信息。
     * 如果需要异步从服务器获取公众服务账号信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。 在异步请求结果返回后，根据返回的结果调用 {@link
     * #refreshPublicServiceProfile(PublicServiceProfile)} 刷新公众号信息。
     *
     * @param publicServiceProfileProvider 公众服务账号信息的提供者 {@link
     *     PublicServiceManager.PublicServiceProfileProvider}。
     */

    /**
     * /~english Set the provider of public service account information for RongIM to get the public
     * service account name, portrait information and public service number menu. Currently, the
     * default official account service of sdk shall not be set by developers. This
     * interfaceprovides another way to set public service account information from the app layer.
     * When the user information is displayed in the sdk interface,
     * PublicServiceManager.PublicServiceProfileProvider#getPublicServiceProfile(Conversation.PublicServiceType,
     * String) users only shall provide the corresponding public service account information
     * according to the corresponding publicServiceType, publicServiceId. If you shall obtain the
     * public service account information asynchronously from the server, the consumer can initiate
     * an asynchronous request in this method and then return the null information. After the result
     * of the asynchronous request is returned, refreshPublicServiceProfile(PublicServiceProfile) is
     * called to refresh the official account information according to the returned result.
     *
     * @param publicServiceProfileProvider PublicServiceManager.PublicServiceProfileProvider, the
     *     provider of public service account information.
     */
    public static void setPublicServiceProfileProvider(
            PublicServiceManager.PublicServiceProfileProvider publicServiceProfileProvider) {
        PublicServiceManager.getInstance()
                .setPublicServiceProfileProvider(publicServiceProfileProvider);
    }

    /**
     * /~chinese 设置发送消息回执的会话类型。目前只支持私聊，群组和讨论组。 默认支持私聊。
     *
     * @param types 包含在types里的会话类型中将会发送消息回执。
     */

    /**
     * /~english Set the conversation type for sending message receipts. Currently, only private
     * chats, groups and discussion groups are supported. Private chat is supported by default.
     *
     * @param types A message receipt will be sent in the conversation type included in the types.
     */
    public void setReadReceiptConversationTypeList(Conversation.ConversationType... types) {
        RongConfigCenter.conversationConfig().setSupportReadReceiptConversationType(types);
    }

    /**
     * /~chinese 设置当前用户信息。 如果开发者没有实现用户信息提供者，而是使用消息携带用户信息，需要使用这个方法设置当前用户的信息， 然后在{@link
     * #init(Application, String, boolean)}之后调用{@link
     * RongUserInfoManager#getInstance()}.setMessageAttachedUserInfo(boolean)}，
     * 这样可以在每条消息中携带当前用户的信息，IMKit会在接收到消息的时候取出用户信息并刷新到界面上。
     *
     * @param userInfo 当前用户信息。
     */

    /**
     * /~english Set the current user information If the developer does not implement a user
     * information provider, but uses messages to carry user information, you shall use this method
     * to set the current user's information, and then call
     * RongUserInfoManager.getInstance().setMessageAttachedUserInfo(boolean)} afterinit(Application,
     * String, boolean), so that you can carry the current user's information in each message. IMKit
     * will take out the user's information and refresh it to the interface when it receives the
     * message.
     *
     * @param userInfo Current user information
     */
    public void setCurrentUserInfo(UserInfo userInfo) {
        RongUserInfoManager.getInstance().setCurrentUserInfo(userInfo);
    }

    /**
     * /~chinese 设置消息体内是否携带用户信息。
     *
     * @param state 是否携带用户信息，true 携带，false 不携带。
     */

    /**
     * /~english Set whether the message body carries user information.
     *
     * @param state Whether or not to carry user information, true indicates to carry and false
     *     indicates not to carry.
     */
    public void setMessageAttachedUserInfo(boolean state) {
        RongUserInfoManager.getInstance().setMessageAttachedUserInfo(state);
    }

    /**
     * /~chinese
     *
     * <p>启动聊天室会话。
     *
     * <p>设置参数 createIfNotExist 为 true，对应到 kit 中调用的接口是 {@link RongIMClient#joinChatRoom(String, int,
     * RongIMClient.OperationCallback)}. 如果聊天室不存在，则自动创建并加入，如果回调失败，则弹出 warning。
     *
     * <p>设置参数 createIfNotExist 为 false，对应到 kit 中调用的接口是 {@link
     * RongIMClient#joinExistChatRoom(String, int, RongIMClient.OperationCallback)}. 如果聊天室不存在，则返回错误
     * {@link RongIMClient.ErrorCode#RC_CHATROOM_NOT_EXIST}.
     *
     * @param context 应用上下文。
     * @param chatRoomId 聊天室 id。
     * @param createIfNotExist 如果聊天室不存在，是否创建。
     */

    /**
     * /~english Start a chat room conversation
     *
     * <p>Set the parameter createIfNotExist to true, corresponding interface to be called in kit is
     * RongIMClient#joinChatRoom(String, int, RongIMClient.OperationCallback). If the chat room does
     * not exist, it is automatically created and joined. If the callback fails, warning pops up.
     *
     * <p>Set the parameter createIfNotExist to false, corresponding interface to be called in kit
     * is RongIMClient#joinExistChatRoom(String, int, RongIMClient.OperationCallback) If the chat
     * room does not exist, an error RongIMClient.ErrorCode#RC_CHATROOM_NOT_EXIST is returned.
     *
     * @param context Application context
     * @param chatRoomId Chat room id
     * @param createIfNotExist If the chat room does not exist, whether to create it.
     */
    public void startChatRoomChat(Context context, String chatRoomId, boolean createIfNotExist) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(RouteUtils.CREATE_CHATROOM, createIfNotExist);
        RouteUtils.routeToConversationActivity(
                context, Conversation.ConversationType.CHATROOM, chatRoomId, bundle);
    }

    /**
     * /~chinese
     *
     * <p>启动会话界面。
     *
     * <p>使用时，可以传入多种会话类型 {@link Conversation.ConversationType} 对应不同的会话类型，开启不同的会话界面。 如果传入的是 {@link
     * Conversation.ConversationType#CHATROOM}，sdk 会默认调用 {@link RongIMClient#joinChatRoom(String,
     * int, RongIMClient.OperationCallback)} 加入聊天室。 如果你的逻辑是，只允许加入已存在的聊天室，请使用接口 {@link
     * #startChatRoomChat(Context, String, boolean)} 并且第三个参数为 false
     *
     * @param context 应用上下文。
     * @param conversationType 会话类型。
     * @param targetId 根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param bundle 参数传递 bundle
     */

    /**
     * /~english Start the conversation interface
     *
     * <p>Multiple conversation types Conversation.ConversationType can be passed in to correspond
     * to different conversation types and open different conversation interfaces in use. If
     * Conversation.ConversationType#CHATROOM is passed in, RongIMClient#joinChatRoom(String, int,
     * RongIMClient.OperationCallback) will be called by default to join the chat room. If your
     * logic is that only existing chat rooms can be joined, the interface
     * startChatRoomChat(Context, String, boolean) is used and the third parameter is false.
     *
     * @param context Application context.
     * @param conversationType Conversation type
     * @param targetId Depending on the conversationType, it may be user Id, discussion group Id,
     *     group Id or chat room Id
     * @param bundle Parameter passing bundle
     */
    public void startConversation(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            Bundle bundle) {
        RouteUtils.routeToConversationActivity(context, conversationType, targetId, bundle);
    }

    /**
     * /~chinese 启动客户服聊天界面。
     *
     * @param context 应用上下文。
     * @param customerServiceId 要与之聊天的客服 Id。
     * @param title 聊天的标题。开发者需要在聊天界面通过intent.getData().getQueryParameter("title")获取该值,
     *     再手动设置为聊天界面的标题。
     * @param customServiceInfo 当前使用客服者的用户信息。{@link CSCustomServiceInfo}
     */

    /**
     * /~english Start the customer service chat interface
     *
     * @param context Application context
     * @param customerServiceId Customer service Id to chat with
     * @param title The title of the chat Developers shall get this value through intent.getData ().
     *     GetQueryParameter ("title") in the chat interface, and then manually set it as the title
     *     of the chat interface
     * @param customServiceInfo The user information of the current customer service
     *     user.CSCustomServiceInfo
     */
    public void startCustomerServiceChat(
            Context context,
            String customerServiceId,
            String title,
            CSCustomServiceInfo customServiceInfo) {
        Bundle bundle = new Bundle();
        bundle.putString(RouteUtils.TITLE, title);
        bundle.putParcelable(RouteUtils.CUSTOM_SERVICE_INFO, customServiceInfo);
        RouteUtils.routeToConversationActivity(
                context, Conversation.ConversationType.CUSTOMER_SERVICE, customerServiceId, bundle);
    }

    /**
     * /~chinese 启动聚合后的某类型的会话列表。<br>
     * 例如：如果设置了单聊会话为聚合，则通过该方法可以打开包含所有的单聊会话的列表。
     *
     * @param context 应用上下文。
     * @param conversationType 会话类型。
     */

    /**
     * /~english A list of a specific type of conversations after starting the aggregation For
     * example, if a single chat conversation is set to aggregate, this method allows you to open a
     * list of all single chat conversations.
     *
     * @param context Application context
     * @param conversationType Conversation type
     */
    public void startSubConversationList(
            Context context, Conversation.ConversationType conversationType) {
        RouteUtils.routeToSubConversationListActivity(context, conversationType, "");
    }

    public static String getVersion() {
        return IMKitBuildVar.SDK_VERSION;
    }
}
