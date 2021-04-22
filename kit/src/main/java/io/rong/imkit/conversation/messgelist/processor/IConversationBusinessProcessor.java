package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;
import android.os.Bundle;

import java.util.List;

import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

/**
 * 会话业务处理器
 */
public interface IConversationBusinessProcessor {
    /**
     * 第一次初始化，绑定事件等
     *
     * @param messageViewModel
     */
    void init(MessageViewModel messageViewModel, Bundle bundle);

    boolean onReceived(MessageViewModel messageViewModel, UiMessage message, int left, boolean hasPackage, boolean offline);

    void onMessageItemClick(UiMessage uiMessage);

    boolean onMessageItemLongClick(UiMessage uiMessage);

    void onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId);

    boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId);

    boolean onBackPressed(MessageViewModel viewModel);

    void onDestroy(MessageViewModel viewModel);

    void onExistUnreadMessage(MessageViewModel viewModel, Conversation conversation, int unreadMessageCount);

    void onMessageReceiptRequest(MessageViewModel viewModel, Conversation.ConversationType conversationType, String targetId, String messageUId);

    void onLoadMessage(MessageViewModel viewModel, List<Message> messages);

    void onConnectStatusChange(MessageViewModel viewModel, RongIMClient.ConnectionStatusListener.ConnectionStatus status);

    void onResume(MessageViewModel viewModel);
}
