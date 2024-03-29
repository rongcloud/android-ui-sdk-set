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

/** 会话业务处理器 */
public interface IConversationBusinessProcessor {

    /**
     * 第一次初始化，绑定事件等
     *
     * @param messageViewModel 消息处理类
     * @param bundle 上一个页面传递过来的 bundle
     */
    void init(MessageViewModel messageViewModel, Bundle bundle);

    /**
     * 接收消息回调此接口
     *
     * @param messageViewModel
     * @param message
     * @param left
     * @param hasPackage
     * @param offline
     * @return 是否拦截
     */
    boolean onReceived(
            MessageViewModel messageViewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline);

    /**
     * 接收到命令消息，(MessageTag 为 None 或 Status 的消息)
     *
     * @param messageViewModel
     * @param message 拦截的命令消息
     * @return true，ui 不展示，false 在 ui 展示
     */
    boolean onReceivedCmd(MessageViewModel messageViewModel, Message message);

    /**
     * 消息点击事件
     *
     * @param uiMessage
     */
    void onMessageItemClick(UiMessage uiMessage);

    /**
     * 消息长按事件
     *
     * @param uiMessage
     * @return 是否拦截
     */
    boolean onMessageItemLongClick(UiMessage uiMessage);

    /**
     * 用户头像点击事件
     *
     * @param context
     * @param conversationType
     * @param userInfo
     * @param targetId
     */
    void onUserPortraitClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId);

    /**
     * 用户头像长按事件
     *
     * @param context
     * @param conversationType
     * @param userInfo
     * @param targetId
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

    void onLoadMore(MessageViewModel viewModel);

    void onClearMessage(MessageViewModel viewModel);

    void onRefresh(MessageViewModel viewModel);

    void newMessageBarClick(MessageViewModel viewModel);

    void unreadBarClick(MessageViewModel viewModel);

    void newMentionMessageBarClick(MessageViewModel viewModel);

    boolean isNormalState(MessageViewModel viewModel);

    boolean isHistoryState(MessageViewModel viewModel);

    void onScrollToBottom(MessageViewModel viewModel);
}
