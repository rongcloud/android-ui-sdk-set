package io.rong.imkit.conversation.messgelist.status;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;

import android.os.Bundle;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import java.util.List;

public class ChatroomNormalState implements IMessageState {

    private StateContext context;

    ChatroomNormalState(StateContext context) {
        this.context = context;
    }

    @Override
    public void init(final MessageViewModel viewModel, Bundle bundle) {
        RongIMClient.getInstance()
                .getHistoryMessages(
                        viewModel.getCurConversationType(),
                        viewModel.getCurTargetId(),
                        -1,
                        DEFAULT_COUNT + 1,
                        new RongIMClient.ResultCallback<List<Message>>() {
                            @Override
                            // 返回列表（10，9，8，7，6，按messageId倒序）
                            public void onSuccess(List<Message> messages) {
                                // 不为空且大于0证明还有本地数据
                                if (messages != null && messages.size() > 0) {
                                    List<Message> result;
                                    // 如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                                    if (messages.size() < DEFAULT_COUNT + 1) {
                                        result = messages;
                                    } else {
                                        result = messages.subList(0, DEFAULT_COUNT);
                                    }
                                    viewModel.onGetHistoryMessage(result);
                                }
                                // 处理未读消息
                                MessageProcessor.processUnread(viewModel);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {}
                        });
    }

    @Override
    public void onLoadMore(MessageViewModel viewModel) {}

    @Override
    public void onRefresh(MessageViewModel viewModel) {
        MessageProcessor.getLocalMessage(viewModel);
    }

    @Override
    public void onReceived(
            MessageViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {}

    @Override
    public void onNewMessageBarClick(MessageViewModel viewModel) {}

    @Override
    public void onNewMentionMessageBarClick(MessageViewModel viewModel) {}

    @Override
    public void onScrollToBottom(MessageViewModel viewModel) {}

    @Override
    public void onHistoryBarClick(MessageViewModel viewModel) {}

    @Override
    public void onClearMessage(MessageViewModel viewModel) {}

    @Override
    public boolean isNormalState(MessageViewModel viewModel) {
        return false;
    }
}
