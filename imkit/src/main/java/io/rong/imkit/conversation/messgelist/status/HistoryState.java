package io.rong.imkit.conversation.messgelist.status;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;

import android.os.Bundle;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.ScrollEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.model.Message;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class HistoryState implements IMessageState {
    private static final String TAG = "HistoryState";
    private boolean isLoading;
    private final StateContext context;

    HistoryState(StateContext context) {
        this.context = context;
    }

    /** 跳转到指定历史消息 */
    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        long indexTime = 0;
        if (bundle != null) {
            indexTime = bundle.getLong(RouteUtils.INDEX_MESSAGE_TIME, 0);
        }

        if (indexTime > 0) {
            isLoading = true;
            WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
            MessageProcessor.getMessagesAll(
                    messageViewModel,
                    indexTime,
                    5,
                    5,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(list, isHasMoreMsg);
                                // '5'标识定位消息的下标
                                weakVM.get().executePageEvent(new ScrollEvent(5));
                            }
                            isLoading = false;
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            isLoading = false;
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(list);
                                // '5'标识定位消息的下标
                                weakVM.get().executePageEvent(new ScrollEvent(5));
                            }

                            isLoading = false;
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            isLoading = false;
                        }
                    });
        }
    }

    @Override
    public void onLoadMore(final MessageViewModel viewModel) {
        if (!isLoading) {
            isLoading = true;
            WeakReference<MessageViewModel> weakVM = new WeakReference<>(viewModel);
            MessageProcessor.getMessagesDirection(
                    viewModel,
                    viewModel.getLoadMoreSentTime(),
                    DEFAULT_COUNT,
                    false,
                    new MessageProcessor.GetMessageCallback() {
                        @Override
                        public void onSuccess(
                                List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                            if (weakVM.get() != null) {
                                executeHistoryLoadMore(list, weakVM.get());
                            }
                        }

                        @Override
                        public void onErrorAsk(List<Message> list) {
                            if (weakVM.get() != null) {
                                weakVM.get().onGetHistoryMessage(Collections.<Message>emptyList());
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.LoadFinish));
                            }
                            isLoading = false;
                        }

                        @Override
                        public void onErrorAlways(List<Message> list) {
                            if (weakVM.get() != null) {
                                executeHistoryLoadMore(list, weakVM.get());
                            }
                            isLoading = false;
                        }

                        @Override
                        public void onErrorOnlySuccess() {
                            context.setCurrentState(context.normalState);
                            if (weakVM.get() != null) {
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.LoadFinish));
                            }
                            isLoading = false;
                        }
                    });
        }
    }

    private void executeHistoryLoadMore(List<Message> messageList, MessageViewModel viewModel) {
        if (messageList.size() < DEFAULT_COUNT) {
            viewModel.onLoadMoreMessage(messageList);
            context.setCurrentState(context.normalState);
        } else {
            viewModel.onLoadMoreMessage(messageList);
        }
        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
        isLoading = false;
    }

    @Override
    public void onRefresh(MessageViewModel viewModel) {
        context.normalState.onRefresh(viewModel);
    }

    @Override
    public void onReceived(
            MessageViewModel viewModel,
            UiMessage uiMessage,
            int left,
            boolean hasPackage,
            boolean offline) {
        // 不在最底部，添加到未读列表
        if (!viewModel.isScrollToBottom()) {
            if (!viewModel.filterMessageToHideNewMessageBar(uiMessage)) {
                viewModel.addUnreadNewMessage(uiMessage);
            }
            if (RongConfigCenter.conversationConfig()
                    .isShowNewMentionMessageBar(uiMessage.getConversationType())) {
                viewModel.updateMentionMessage(uiMessage.getMessage());
            }
        }
        // 直接显示messagebar
        viewModel.processNewMessageUnread(false);
    }

    @Override
    public void onNewMessageBarClick(final MessageViewModel viewModel) {
        // 拉取 默认值+ 1（11）条记录，如果小于 11 条，则说明本地消息拉取完成
        viewModel.cleanUnreadNewCount();
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(viewModel);
        MessageProcessor.getMessagesDirection(
                viewModel,
                0,
                DEFAULT_COUNT + 1,
                true,
                new MessageProcessor.GetMessageCallback() {

                    @Override
                    public void onSuccess(
                            List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {
                        if (weakVM.get() != null) {
                            executeNewMessageBarClick(list, weakVM.get());
                        }
                    }

                    @Override
                    public void onErrorAsk(List<Message> list) {
                        context.setCurrentState(context.normalState);
                        if (weakVM.get() != null) {
                            weakVM.get().refreshAllMessage();
                        }
                    }

                    @Override
                    public void onErrorAlways(List<Message> list) {
                        if (weakVM.get() != null) {
                            executeNewMessageBarClick(list, weakVM.get());
                        }
                    }

                    @Override
                    public void onErrorOnlySuccess() {
                        context.setCurrentState(context.normalState);
                        if (weakVM.get() != null) {
                            weakVM.get().refreshAllMessage();
                        }
                    }
                });
    }

    private void executeNewMessageBarClick(List<Message> messageList, MessageViewModel viewModel) {
        if (messageList.size() < DEFAULT_COUNT + 1) {
            viewModel.onReloadMessage(messageList);
        } else {
            viewModel.onReloadMessage(messageList.subList(0, DEFAULT_COUNT));
        }
        viewModel.executePageEvent(new ScrollToEndEvent());
        context.setCurrentState(context.normalState);
        viewModel.refreshAllMessage();
    }

    @Override
    public void onNewMentionMessageBarClick(MessageViewModel viewModel) {
        context.normalState.onNewMentionMessageBarClick(viewModel);
    }

    /**
     * @param viewModel 历史消息滑动到底部不做任何处理，继续加载更多
     */
    @Override
    public void onScrollToBottom(MessageViewModel viewModel) {
        onLoadMore(viewModel);
    }

    /**
     * @param viewModel 历史状态不做任何处理
     */
    @Override
    public void onHistoryBarClick(MessageViewModel viewModel) {
        context.normalState.onHistoryBarClick(viewModel);
    }

    @Override
    public void onClearMessage(MessageViewModel viewModel) {
        // do nothing
    }

    @Override
    public boolean isNormalState(MessageViewModel viewModel) {
        return false;
    }
}
