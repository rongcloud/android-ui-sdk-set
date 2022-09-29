package io.rong.imkit.conversation.messgelist.status;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;

import android.os.Bundle;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.ScrollEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.event.uievent.SmoothScrollEvent;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.model.Message;
import java.util.Collections;
import java.util.List;

/** 会话页面当前状态，正常模式 */
public class NormalState implements IMessageState {

    private boolean isLoading;
    private boolean isRefreshLoading;
    private final StateContext context;

    NormalState(StateContext context) {
        this.context = context;
    }

    /**
     * 正常模式，初始化，拉取本地历史记录，处理未读数
     *
     * @param messageViewModel MessageViewModel
     * @param bundle Bundle
     */
    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        MessageProcessor.getMessagesDirection(
                messageViewModel,
                0,
                DEFAULT_COUNT,
                true,
                new MessageProcessor.GetMessageCallback() {
                    @Override
                    public void onSuccess(List<Message> list, boolean loadOnlyOnce) {
                        messageViewModel.onGetHistoryMessage(list);
                        // 处理未读消息
                        MessageProcessor.processUnread(messageViewModel);
                    }

                    @Override
                    public void onErrorAsk(List<Message> list) {
                        messageViewModel.onGetHistoryMessage(Collections.<Message>emptyList());
                    }

                    @Override
                    public void onErrorAlways(List<Message> list) {
                        // 处理未读消息
                        MessageProcessor.processUnread(messageViewModel);
                    }

                    @Override
                    public void onErrorOnlySuccess() {}
                });
    }

    /**
     * 正常模式上拉加载更多
     *
     * @param viewModel MessageViewModel
     */
    @Override
    public void onLoadMore(final MessageViewModel viewModel) {
        MessageProcessor.getMessagesDirection(
                viewModel,
                viewModel.getLoadMoreSentTime() + 1,
                DEFAULT_COUNT,
                false,
                new MessageProcessor.GetMessageCallback() {
                    @Override
                    public void onSuccess(List<Message> list, boolean loadOnlyOnce) {
                        viewModel.onLoadMoreMessage(list);
                        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
                    }

                    @Override
                    public void onErrorAsk(List<Message> list) {
                        viewModel.onLoadMoreMessage(Collections.<Message>emptyList());
                        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
                    }

                    @Override
                    public void onErrorAlways(List<Message> list) {
                        viewModel.onLoadMoreMessage(list);
                        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
                    }

                    @Override
                    public void onErrorOnlySuccess() {
                        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
                    }
                });
    }

    @Override
    public void onRefresh(final MessageViewModel viewModel) {
        if (!isRefreshLoading) {
            isRefreshLoading = true;
            MessageProcessor.getMessagesDirection(
                    viewModel,
                    viewModel.getRefreshSentTime(),
                    DEFAULT_COUNT,
                    true,
                    new MessageProcessor.GetMessageCallback() {

                        @Override
                        public void onSuccess(List<Message> list, boolean loadOnlyOnce) {
                            isRefreshLoading = loadOnlyOnce;
                            viewModel.onGetHistoryMessage(list);
                            viewModel.executePageEvent(
                                    new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            isRefreshLoading = false;
                            viewModel.onGetHistoryMessage(Collections.<Message>emptyList());
                            viewModel.executePageEvent(
                                    new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            isRefreshLoading = false;
                            viewModel.onGetHistoryMessage(list);
                            viewModel.executePageEvent(
                                    new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            isRefreshLoading = false;
                            viewModel.executePageEvent(
                                    new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }
                    });
        } else {
            viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.RefreshFinish));
        }
    }

    /**
     * 正常模式，按流程处理
     *
     * @param viewModel MessageViewModel
     * @param uiMessage 接收到的消息对象
     * @param left 每个数据包数据逐条上抛后，还剩余的条数
     * @param hasPackage 是否在服务端还存在未下发的消息包
     * @param offline 消息是否离线消息
     */
    @Override
    public void onReceived(
            MessageViewModel viewModel,
            UiMessage uiMessage,
            int left,
            boolean hasPackage,
            boolean offline) {
        // 去重处理
        for (UiMessage item : viewModel.getUiMessages()) {
            if (item.getMessageId() == uiMessage.getMessageId()) {
                return;
            }
        }
        viewModel.getUiMessages().add(uiMessage);
        viewModel.refreshAllMessage(false);
        viewModel.updateMentionMessage(uiMessage.getMessage());
        // newMessageBar 逻辑
        if (!RongConfigCenter.conversationConfig()
                .isShowNewMessageBar(uiMessage.getConversationType())) {
            viewModel.executePostPageEvent(new ScrollToEndEvent());
        } else {
            // 如果是离线消息直接滑动到最底部
            // 不在最底部，添加到未读列表
            if (!viewModel.isScrollToBottom()
                    && !viewModel.filterMessageToHideNewMessageBar(uiMessage)) {
                viewModel.getNewUnReadMessages().add(uiMessage);
            }
            // 判断ui是否滑动到底部
            if (RongConfigCenter.conversationConfig()
                    .isShowNewMessageBar(viewModel.getCurConversationType())) {
                if (viewModel.isScrollToBottom()) {
                    viewModel.executePostPageEvent(new ScrollToEndEvent());
                } else {
                    viewModel.processNewMessageUnread(false);
                }
            }
        }
    }

    @Override
    public void onNewMessageBarClick(MessageViewModel viewModel) {
        viewModel.cleanUnreadNewCount();
        viewModel.executePageEvent(new ScrollToEndEvent());
    }

    @Override
    public void onNewMentionMessageBarClick(MessageViewModel viewModel) {
        List<Message> mMentionMessages = viewModel.getNewUnReadMentionMessages();
        if (mMentionMessages.isEmpty()) {
            viewModel.updateNewMentionMessageUnreadBar();
        } else {
            io.rong.imlib.model.Message message = mMentionMessages.get(0);
            int position = viewModel.findPositionByMessageId(message.getMessageId());
            if (position >= 0) {
                viewModel.executePageEvent(new ScrollEvent(position));
            } else {
                boolean isNewMentionMessage = false;
                if (viewModel.getUiMessages().size() > 0) {
                    UiMessage lastMessage =
                            viewModel.getUiMessages().get(viewModel.getUiMessages().size() - 1);
                    isNewMentionMessage = message.getSentTime() > lastMessage.getSentTime();
                }
                viewModel.getUiMessages().clear();
                getMentionMessage(viewModel, isNewMentionMessage, message);
            }
        }
    }

    private void getMentionMessage(
            final MessageViewModel viewModel, boolean isNewMentionMessage, final Message message) {
        if (isNewMentionMessage) {
            if (!isLoading) {
                isLoading = true;
                MessageProcessor.getMessagesAll(
                        viewModel,
                        message.getSentTime(),
                        DEFAULT_COUNT,
                        DEFAULT_COUNT,
                        new MessageProcessor.GetMessageCallback() {
                            @Override
                            public void onSuccess(List<Message> list, boolean loadOnlyOnce) {
                                isLoading = loadOnlyOnce;
                                if (list.size() < DEFAULT_COUNT * 2) {
                                    context.setCurrentState(context.normalState);
                                } else {
                                    context.setCurrentState(context.historyState);
                                }
                                viewModel.onLoadMoreMessage(list);
                                viewModel.executePageEvent(
                                        new Event.RefreshEvent(RefreshState.LoadFinish));
                                int position =
                                        viewModel.findPositionByMessageId(message.getMessageId());
                                if (position >= 0) {
                                    viewModel.executePageEvent(new ScrollEvent(position));
                                }
                            }

                            @Override
                            public void onErrorAsk(List<Message> list) {
                                isLoading = false;
                            }

                            @Override
                            public void onErrorAlways(List<Message> list) {
                                if (list.size() < DEFAULT_COUNT * 2) {
                                    context.setCurrentState(context.normalState);
                                } else {
                                    context.setCurrentState(context.historyState);
                                }
                                viewModel.onLoadMoreMessage(list);
                                viewModel.executePageEvent(
                                        new Event.RefreshEvent(RefreshState.LoadFinish));
                                int position =
                                        viewModel.findPositionByMessageId(message.getMessageId());
                                if (position >= 0) {
                                    viewModel.executePageEvent(new ScrollEvent(position));
                                }
                                isLoading = false;
                            }

                            @Override
                            public void onErrorOnlySuccess() {
                                isLoading = false;
                            }
                        });
            }
        } else {
            MessageProcessor.getMessagesDirection(
                    viewModel,
                    message.getSentTime() - 2,
                    DEFAULT_COUNT,
                    false,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(List<Message> list, boolean loadOnlyOnce) {
                            executeMentionHistoryMsg(list, viewModel);
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            viewModel.onGetHistoryMessage(Collections.<Message>emptyList());
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            executeMentionHistoryMsg(list, viewModel);
                        }

                        @Override
                        public void onErrorOnlySuccess() {}
                    });
        }
    }

    private void executeMentionHistoryMsg(List<Message> messageList, MessageViewModel viewModel) {
        if (messageList.size() < DEFAULT_COUNT) {
            context.setCurrentState(context.normalState);
        } else {
            context.setCurrentState(context.historyState);
        }
        viewModel.onGetHistoryMessage(messageList);
        viewModel.executePageEvent(new ScrollEvent(0));
    }

    @Override
    public void onScrollToBottom(MessageViewModel viewModel) {
        if (RongConfigCenter.conversationConfig()
                .isShowNewMessageBar(viewModel.getCurConversationType())) {
            viewModel.cleanUnreadNewCount();
            viewModel.processNewMessageUnread(true);
        }
    }

    @Override
    public void onHistoryBarClick(final MessageViewModel messageViewModel) {
        Message firstUnreadMessage = messageViewModel.getFirstUnreadMessage();
        if (firstUnreadMessage != null) {
            MessageProcessor.getMessagesDirection(
                    messageViewModel,
                    firstUnreadMessage.getSentTime() - 2,
                    DEFAULT_COUNT,
                    false,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(List<Message> list, boolean loadOnlyOnce) {
                            executeHistoryBarClick(list, messageViewModel);
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            messageViewModel.onGetHistoryMessage(Collections.<Message>emptyList());
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            executeHistoryBarClick(list, messageViewModel);
                        }

                        @Override
                        public void onErrorOnlySuccess() {}
                    });
        }
    }

    @Override
    public void onClearMessage(MessageViewModel viewModel) {}

    @Override
    public boolean isNormalState(MessageViewModel viewModel) {
        return true;
    }

    private void executeHistoryBarClick(
            List<Message> messageList, MessageViewModel messageViewModel) {
        messageViewModel.executePageEvent(new SmoothScrollEvent(0));
        messageViewModel.onReloadMessage(messageList);
        messageViewModel.hideHistoryBar();
        context.setCurrentState(context.historyState);
    }
}
