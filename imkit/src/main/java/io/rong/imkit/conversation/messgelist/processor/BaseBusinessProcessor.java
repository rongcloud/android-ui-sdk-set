package io.rong.imkit.conversation.messgelist.processor;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;

import android.content.Context;
import android.os.Bundle;
import io.rong.imkit.conversation.messgelist.status.StateContext;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UnknownMessage;
import io.rong.imlib.model.UserInfo;
import java.util.List;

/** 处理单聊、群聊、讨论组逻辑的公共类 */
public abstract class BaseBusinessProcessor implements IConversationBusinessProcessor {

    protected StateContext mState;

    @Override
    public void init(MessageViewModel messageViewModel, Bundle bundle) {
        int state;
        if (bundle != null) {
            long indexTime = bundle.getLong(RouteUtils.INDEX_MESSAGE_TIME, 0);
            if (indexTime > 0) {
                state = StateContext.HISTORY_STATE;
            } else {
                state = StateContext.NORMAL_STATE;
            }
        } else {
            state = StateContext.NORMAL_STATE;
        }
        mState = new StateContext(state);
        mState.init(messageViewModel, bundle);
    }

    @Override
    public boolean onReceived(
            MessageViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        if (mState != null) {
            mState.onReceived(viewModel, message, left, hasPackage, offline);
        }
        return false;
    }

    @Override
    public boolean onReceivedCmd(MessageViewModel messageViewModel, Message message) {
        return !(message.getContent() instanceof UnknownMessage);
    }

    @Override
    public void onMessageItemClick(UiMessage uiMessage) {}

    @Override
    public boolean onMessageItemLongClick(UiMessage uiMessage) {
        return false;
    }

    @Override
    public void onUserPortraitClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId) {}

    @Override
    public boolean onUserPortraitLongClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId) {
        return false;
    }

    @Override
    public boolean onBackPressed(MessageViewModel viewModel) {
        return false;
    }

    @Override
    public void onDestroy(MessageViewModel viewModel) {}

    @Override
    public void onExistUnreadMessage(
            MessageViewModel viewModel, Conversation conversation, int unreadMessageCount) {}

    @Override
    public void onMessageReceiptRequest(
            MessageViewModel viewModel,
            Conversation.ConversationType conversationType,
            String targetId,
            String messageUId) {}

    @Override
    public void onLoadMessage(MessageViewModel viewModel, List<Message> messages) {}

    /** 检查已读状态，如果 sp 有内容，则发送 */
    @Override
    public void onConnectStatusChange(
            MessageViewModel viewModel,
            RongIMClient.ConnectionStatusListener.ConnectionStatus status) {}

    @Override
    public void onResume(MessageViewModel viewModel) {}

    /** @return 初始化时，拉取的历史条数，目前只有聊天室需要复写 */
    public int getHistoryMessageCount() {
        return DEFAULT_COUNT + 1;
    }

    @Override
    public void onLoadMore(MessageViewModel viewModel) {
        if (mState != null) {
            mState.onLoadMore(viewModel);
        }
    }

    @Override
    public void onClearMessage(MessageViewModel viewModel) {
        if (mState != null) {
            mState.onClearMessage(viewModel);
        }
    }

    @Override
    public void onRefresh(MessageViewModel viewModel) {
        if (mState != null) {
            mState.onRefresh(viewModel);
        }
    }

    @Override
    public void newMessageBarClick(MessageViewModel viewModel) {
        if (mState != null) {
            mState.onNewMessageBarClick(viewModel);
        }
    }

    @Override
    public void unreadBarClick(MessageViewModel viewModel) {
        if (mState != null) {
            mState.onHistoryBarClick(viewModel);
        }
    }

    @Override
    public void newMentionMessageBarClick(MessageViewModel viewModel) {
        if (mState != null) {
            mState.newMentionMessageBarClick(viewModel);
        }
    }

    @Override
    public boolean isNormalState(MessageViewModel viewModel) {
        if (mState == null) {
            return true;
        }
        return mState.isNormalState(viewModel);
    }

    @Override
    public void onScrollToBottom(MessageViewModel viewModel) {
        if (mState != null) {
            mState.onScrollToBottom(viewModel);
        }
    }
}
