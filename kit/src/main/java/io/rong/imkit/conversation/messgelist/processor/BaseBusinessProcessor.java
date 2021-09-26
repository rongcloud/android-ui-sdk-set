package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;

import java.util.List;

import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UnknownMessage;
import io.rong.imlib.model.UserInfo;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;

/**
 * 处理单聊、群聊、讨论组逻辑的公共类
 */
public abstract class BaseBusinessProcessor implements IConversationBusinessProcessor {


    @Override
    public boolean onReceived(MessageViewModel viewModel, UiMessage message, int left, boolean hasPackage, boolean offline) {
        return false;
    }

    @Override
    public boolean onReceivedCmd(MessageViewModel messageViewModel, Message message) {
        if (message.getContent() instanceof UnknownMessage) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onMessageItemClick(UiMessage uiMessage) {

    }

    @Override
    public boolean onMessageItemLongClick(UiMessage uiMessage) {
        return false;
    }

    @Override
    public void onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId) {

    }

    @Override
    public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId) {
        return false;
    }

    @Override
    public boolean onBackPressed(MessageViewModel viewModel) {
        return false;
    }

    @Override
    public void onDestroy(MessageViewModel viewModel) {

    }

    @Override
    public void onExistUnreadMessage(MessageViewModel viewModel, Conversation conversation, int unreadMessageCount) {

    }

    @Override
    public void onMessageReceiptRequest(MessageViewModel viewModel, Conversation.ConversationType conversationType, String targetId, String messageUId) {

    }

    @Override
    public void onLoadMessage(MessageViewModel viewModel, List<Message> messages) {

    }

    /**
     * 检查已读状态，如果 sp 有内容，则发送
     */
    @Override
    public void onConnectStatusChange(MessageViewModel viewModel, RongIMClient.ConnectionStatusListener.ConnectionStatus status) {

    }

    @Override
    public void onResume(MessageViewModel viewModel) {

    }

    /**
     * @return 初始化时，拉取的历史条数，目前只有聊天室需要复写
     */
    public int getHistoryMessageCount() {
        return DEFAULT_COUNT + 1;
    }

}
