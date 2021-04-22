package io.rong.imkit.conversation.messgelist.status;

import android.os.Bundle;

import java.util.List;

import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.ScrollEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;

public class HistoryState implements IMessageState {
    private boolean isLoading;

    HistoryState() {
    }

    /**
     * 跳转到指定历史消息
     */
    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        long indexTime = 0;
        if (bundle != null && bundle.get(RouteUtils.INDEX_MESSAGE_TIME) != null) {
            indexTime = (long) bundle.get(RouteUtils.INDEX_MESSAGE_TIME);
        }
        if (indexTime > 0) {
            RongIMClient.getInstance().getHistoryMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), indexTime, 5, 5, new RongIMClient.ResultCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    //不为空且大于0证明还有本地数据
                    if (messages != null && messages.size() > 0) {
                        List<Message> result;
                        //如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                        if (messages.size() < DEFAULT_COUNT + 1) {
                            result = messages;
                        } else {
                            result = messages.subList(0, DEFAULT_COUNT);
                        }
                        messageViewModel.onGetHistoryMessage(result);
                        //'5'标识定位消息的下标
                        messageViewModel.executePageEvent(new ScrollEvent(5));
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {

                }
            });
        }
    }

    @Override
    public void onLoadMore(final MessageViewModel viewModel) {
        if (!isLoading) {
            isLoading = true;
            RongIMClient.getInstance().getHistoryMessages(viewModel.getCurConversationType(), viewModel.getCurTargetId(), viewModel.getLoadMoreSentTime(), 0, DEFAULT_COUNT, new RongIMClient.ResultCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    if (messages != null && messages.size() > 0) {
                        //如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                        if (messages.size() < DEFAULT_COUNT + 1) {
                            viewModel.setState(IMessageState.normalState);
                            viewModel.onLoadMoreMessage(messages);
                        } else {
                            viewModel.onLoadMoreMessage(messages.subList(0, DEFAULT_COUNT));
                        }
                    } else {
                        viewModel.setState(IMessageState.normalState);
                    }
                    viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
                    isLoading = false;
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {
                    viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
                    isLoading = false;
                }
            });
        }
    }

    @Override
    public void onRefresh(MessageViewModel viewModel) {
        IMessageState.normalState.onRefresh(viewModel);
    }

    @Override
    public void onReceived(MessageViewModel viewModel, UiMessage uiMessage, int left, boolean hasPackage, boolean offline) {
        //不在最底部，添加到未读列表
        if (!viewModel.isScrollToBottom()) {
            viewModel.getNewUnReadMessages().add(uiMessage);
            if (RongConfigCenter.conversationConfig().isShowNewMentionMessageBar(uiMessage.getConversationType())) {
                viewModel.updateMentionMessage(uiMessage.getMessage());
            }
        }
        //直接显示messagebar
        viewModel.processNewMessageUnread(false);
    }

    @Override
    public void onNewMessageBarClick(final MessageViewModel viewModel) {
        //拉取 默认值+ 1（11）条记录，如果小于 11 条，则说明本地消息拉取完成
        viewModel.cleanUnreadNewCount();
        RongIMClient.getInstance().getHistoryMessages(viewModel.getCurConversationType(), viewModel.getCurTargetId(), -1, DEFAULT_COUNT + 1, new RongIMClient.ResultCallback<List<Message>>() {
            @Override
            //返回列表（10，9，8，7，6，按messageId倒序）
            public void onSuccess(List<Message> messages) {
                //不为空且大于0证明还有本地数据
                if (messages != null && messages.size() > 0) {
                    //如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                    if (messages.size() < DEFAULT_COUNT + 1) {
                        viewModel.onReloadMessage(messages);
                    } else {
                        viewModel.onReloadMessage(messages.subList(0, DEFAULT_COUNT));
                    }
                    viewModel.executePageEvent(new ScrollToEndEvent());
                }
                viewModel.setState(IMessageState.normalState);
                viewModel.updateUiMessages();
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
            }
        });
    }

    @Override
    public void onNewMentionMessageBarClick(MessageViewModel viewModel) {
        IMessageState.normalState.onNewMentionMessageBarClick(viewModel);
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
        IMessageState.normalState.onHistoryBarClick(viewModel);
    }

}
