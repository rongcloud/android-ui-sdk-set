package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;
import android.os.Bundle;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import java.util.List;

/** /~chinese 会话业务处理器 */

/** /~english conversation service processor */
public interface IConversationBusinessProcessor {

    /**
     * /~chinese 第一次初始化，绑定事件等
     *
     * @param messageViewModel 消息处理类
     * @param bundle 上一个页面传递过来的 bundle
     */

    /**
     * /~english Initialization for the first time, binding events, etc
     *
     * @param messageViewModel Message handling class
     * @param bundle Bundle passed on the previous page
     */
    void init(MessageViewModel messageViewModel, Bundle bundle);

    /**
     * /~chinese 接收消息回调此接口
     *
     * @param messageViewModel
     * @param message
     * @param left
     * @param hasPackage
     * @param offline
     * @return 是否拦截
     */

    /**
     * /~english Receive messages and call back this interface
     *
     * @param messageViewModel MessageViewModel
     * @param message Received message object
     * @param left The number of messages left after each packet is thrown up one by one
     * @param hasPackage Whether there are any undistributed message packets on the server
     * @param offline Whether the message is offline
     * @return Whether to intercept
     */
    boolean onReceived(
            MessageViewModel messageViewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline);

    /**
     * /~chinese 接收到命令消息，(MessageTag 为 None 或 Status 的消息)
     *
     * @param messageViewModel
     * @param message 拦截的命令消息
     * @return true，ui 不展示，false 在 ui 展示
     */

    /**
     * /~english Command message received, (message with messagetag of none or status)
     *
     * @param messageViewModel MessageViewModel
     * @param message Received message object
     * @return True, UI is not displayed, false is displayed in UI
     */
    boolean onReceivedCmd(MessageViewModel messageViewModel, Message message);

    /**
     * /~chinese 消息点击事件
     *
     * @param uiMessage
     */

    /**
     * /~english Message click event
     *
     * @param uiMessage
     */
    void onMessageItemClick(UiMessage uiMessage);

    /**
     * /~chinese 消息长按事件
     *
     * @param uiMessage
     * @return 是否拦截
     */

    /**
     * /~english Message hold event
     *
     * @param uiMessage
     * @return Whether to intercept
     */
    boolean onMessageItemLongClick(UiMessage uiMessage);

    /**
     * /~chinese 用户头像点击事件
     *
     * @param context
     * @param conversationType
     * @param userInfo
     * @param targetId
     */

    /**
     * /~english Click event of user portrait
     *
     * @param context Context
     * @param conversationType Type of conversation to get.
     * @param userInfo user info
     * @param targetId Conversation Id
     */
    void onUserPortraitClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId);

    /**
     * /~chinese 用户头像长按事件
     *
     * @param context
     * @param conversationType
     * @param userInfo
     * @param targetId
     * @return
     */

    /**
     * /~english Hold event of user portrait
     *
     * @param context Context
     * @param conversationType Type of conversation to get.
     * @param userInfo user info
     * @param targetId Conversation Id
     * @return
     */
    boolean onUserPortraitLongClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId);

    boolean onBackPressed(MessageViewModel viewModel);

    void onDestroy(MessageViewModel viewModel);

    void onExistUnreadMessage(
            MessageViewModel viewModel, Conversation conversation, int unreadMessageCount);

    void onMessageReceiptRequest(
            MessageViewModel viewModel,
            Conversation.ConversationType conversationType,
            String targetId,
            String messageUId);

    void onLoadMessage(MessageViewModel viewModel, List<Message> messages);

    void onConnectStatusChange(
            MessageViewModel viewModel,
            RongIMClient.ConnectionStatusListener.ConnectionStatus status);

    void onResume(MessageViewModel viewModel);
}
