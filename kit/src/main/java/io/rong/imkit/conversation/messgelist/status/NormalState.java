package io.rong.imkit.conversation.messgelist.status;

import android.os.Bundle;

import java.util.List;

import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.ScrollEvent;
import io.rong.imkit.event.uievent.ScrollMentionEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.event.uievent.SmoothScrollEvent;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;
import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_REMOTE_COUNT;

/**
 * 会话页面当前状态，正常模式
 */
public class NormalState implements IMessageState {

    private boolean isLoading;

    NormalState() {

    }

    /**
     * 正常模式，初始化，拉取本地历史记录，处理未读数
     *
     * @param messageViewModel MessageViewModel
     * @param bundle           Bundle
     */
    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        //拉取 默认值+ 1（11）条记录，如果小于 11 条，则说明本地消息拉取完成
        RongIMClient.getInstance().getHistoryMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), -1, DEFAULT_COUNT + 1, new RongIMClient.ResultCallback<List<Message>>() {
            @Override
            //返回列表（10，9，8，7，6，按messageId倒序）
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
                }
                //处理未读消息
                processUnread(messageViewModel);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });

    }

    private void processUnread(final MessageViewModel messageViewModel) {
        RongIMClient.getInstance().getConversation(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), new RongIMClient.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                //设置历史未读数
                if (conversation == null) {
                    return;
                }
                int unreadMessageCount = conversation.getUnreadMessageCount();
                //交给不同会话类型处理未读消息
                if (unreadMessageCount > 0) {
                    messageViewModel.onExistUnreadMessage(conversation, unreadMessageCount);
                }
                //获得第一条未读消息
                initUnreadMessage(messageViewModel, unreadMessageCount);
                //判断有无 @ 记录
                initMentionedMessage(conversation, messageViewModel);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
    }

    private void initMentionedMessage(Conversation conversation, final MessageViewModel messageViewModel) {
        if (conversation.getMentionedCount() > 0) {
            RongIMClient.getInstance().getUnreadMentionedMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), new RongIMClient.ResultCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    if (messages != null && messages.size() > 0) {
                        int messageId = messages.get(0).getMessageId();
                        int index = messageViewModel.findPositionByMessageId(messageId);
                        if (index >= 0) {
                            messageViewModel.executePageEvent(new ScrollEvent(index));
                        }
                        messageViewModel.setNewUnReadMentionMessages(messages);
                        messageViewModel.executePageEvent(new ScrollMentionEvent());
                    }
                    messageViewModel.setInitMentionedMessageFinish(true);
                    messageViewModel.cleanUnreadStatus();
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    messageViewModel.setInitMentionedMessageFinish(true);
                    messageViewModel.cleanUnreadStatus();
                }
            });
        } else {
            messageViewModel.setInitMentionedMessageFinish(true);
            messageViewModel.cleanUnreadStatus();
        }
    }

    private void initUnreadMessage(final MessageViewModel messageViewModel, final int unreadMessageCount) {
        RongIMClient.getInstance().getTheFirstUnreadMessage(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), new RongIMClient.ResultCallback<Message>() {
            @Override
            public void onSuccess(Message message) {
                if (unreadMessageCount > MessageViewModel.SHOW_UNREAD_MESSAGE_COUNT && message != null) {
                    messageViewModel.setFirstUnreadMessage(message);
                    if (RongConfigCenter.conversationConfig().isShowHistoryMessageBar(messageViewModel.getCurConversationType())) {
                        messageViewModel.showHistoryBar(unreadMessageCount);
                    }
                }
                messageViewModel.setInitUnreadMessageFinish(true);
                messageViewModel.cleanUnreadStatus();
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                messageViewModel.setInitUnreadMessageFinish(true);
                messageViewModel.cleanUnreadStatus();
            }
        });
    }

    /**
     * 正常模式不需要上拉加载更多，直接关闭
     *
     * @param viewModel MessageViewModel
     */
    @Override
    public void onLoadMore(MessageViewModel viewModel) {
        viewModel.executePageEvent(new Event.RefreshEvent(RefreshState.LoadFinish));
    }

    @Override
    public void onRefresh(MessageViewModel viewModel) {
        getLocalMessage(viewModel);
    }

    /**
     * 正常模式，按流程处理
     *
     * @param viewModel  MessageViewModel
     * @param uiMessage  接收到的消息对象
     * @param left       每个数据包数据逐条上抛后，还剩余的条数
     * @param hasPackage 是否在服务端还存在未下发的消息包
     * @param offline    消息是否离线消息
     */
    @Override
    public void onReceived(MessageViewModel viewModel, UiMessage uiMessage, int left, boolean hasPackage, boolean offline) {
        viewModel.getUiMessages().add(uiMessage);
        viewModel.updateUiMessages(false);
        viewModel.updateMentionMessage(uiMessage.getMessage());
        // newMessageBar 逻辑
        if (RongConfigCenter.conversationConfig().isShowNewMessageBar(uiMessage.getConversationType())) {
            //不在最底部，添加到未读列表
            if (!viewModel.isScrollToBottom()) {
                viewModel.getNewUnReadMessages().add(uiMessage);
            }
            //判断ui是否滑动到底部
            if (RongConfigCenter.conversationConfig().isShowNewMessageBar(viewModel.getCurConversationType())) {
                if (viewModel.isScrollToBottom()) {
                    viewModel.executePostPageEvent(new ScrollToEndEvent());
                } else {
                    viewModel.processNewMessageUnread(false);
                }
            }
        } else {
            viewModel.executePostPageEvent(new ScrollToEndEvent());
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
        if (mMentionMessages.size() > 0) {
            io.rong.imlib.model.Message message = mMentionMessages.get(0);
            int position = viewModel.findPositionByMessageId(message.getMessageId());
            if (position >= 0) {
                viewModel.executePageEvent(new ScrollEvent(position));
            } else {
                boolean isNewMentionMessage = false;
                if (viewModel.getUiMessages().size() > 0) {
                    UiMessage lastMessage = viewModel.getUiMessages().get(viewModel.getUiMessages().size() - 1);
                    isNewMentionMessage = message.getSentTime() > lastMessage.getSentTime();
                }
                viewModel.getUiMessages().clear();
                getMentionMessage(viewModel, isNewMentionMessage, message);
            }
        }
        viewModel.processNewMentionMessageUnread(true);
    }

    private void getMentionMessage(final MessageViewModel viewModel, boolean isNewMentionMessage, Message message) {
        if (isNewMentionMessage) {
            if (!isLoading) {
                isLoading = true;
                RongIMClient.getInstance().getHistoryMessages(viewModel.getCurConversationType(), viewModel.getCurTargetId(), message.getSentTime(), DEFAULT_COUNT, DEFAULT_COUNT, new RongIMClient.ResultCallback<List<Message>>() {
                    @Override
                    public void onSuccess(List<Message> messages) {
                        if (messages != null && messages.size() > 0) {
                            //如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                            if (messages.size() < DEFAULT_COUNT * 2 + 1) {
                                viewModel.setState(IMessageState.normalState);
                                viewModel.onLoadMoreMessage(messages);
                            } else {
                                viewModel.onLoadMoreMessage(messages.subList(0, DEFAULT_COUNT * 2));
                                viewModel.setState(IMessageState.historyState);
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
        } else {
            RongIMClient.getInstance().getHistoryMessages(viewModel.getCurConversationType(), viewModel.getCurTargetId(), message.getSentTime(), 0, DEFAULT_COUNT, new RongIMClient.ResultCallback<List<Message>>() {
                //返回列表（10，9，8，7，6，按messageId倒序）
                @Override
                public void onSuccess(List<Message> messages) {
                    //不为空且大于0证明还有本地数据
                    if (messages != null && messages.size() > 0) {
                        List<Message> result;
                        //如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                        if (messages.size() < DEFAULT_COUNT + 1) {
                            result = messages;
                            viewModel.setState(IMessageState.normalState);
                        } else {
                            result = messages.subList(1, DEFAULT_COUNT + 1);
                            viewModel.setState(IMessageState.historyState);
                        }
                        viewModel.onGetHistoryMessage(result);
                        viewModel.executePageEvent(new ScrollEvent(0));
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {

                }
            });
        }
    }

    @Override
    public void onScrollToBottom(MessageViewModel viewModel) {
        if (RongConfigCenter.conversationConfig().isShowNewMessageBar(viewModel.getCurConversationType())) {
            viewModel.cleanUnreadNewCount();
            viewModel.processNewMessageUnread(true);
        }
    }

    @Override
    public void onHistoryBarClick(final MessageViewModel messageViewModel) {
        Message firstUnreadMessage = messageViewModel.getFirstUnreadMessage();
        if (firstUnreadMessage != null) {
            RongIMClient.getInstance().getHistoryMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), firstUnreadMessage.getSentTime(), 0, DEFAULT_COUNT - 1, new RongIMClient.ResultCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messages) {
                    messageViewModel.executePageEvent(new SmoothScrollEvent(0));
                    messageViewModel.onReloadMessage(messages);
                    messageViewModel.hideHistoryBar();
                    messageViewModel.setState(IMessageState.historyState);
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {

                }
            });
        }
    }

    public void getRemoteMessage(final MessageViewModel messageViewModel) {
        RongIMClient.getInstance().getRemoteHistoryMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), messageViewModel.getRefreshSentTime(), DEFAULT_COUNT + 1, new RongIMClient.ResultCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                //不为空且大于0证明还有本地数据
                if (messages != null && messages.size() > 0) {
                    //如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                    List<Message> result;
                    if (messages.size() < DEFAULT_REMOTE_COUNT + 1) {
                        messageViewModel.setRemoteMessageLoadFinish(true);
                        result = messages;
                    } else {
                        result = messages.subList(0, DEFAULT_REMOTE_COUNT);
                    }
                    messageViewModel.onGetHistoryMessage(result);
                } else {
                    messageViewModel.setRemoteMessageLoadFinish(true);
                }
                messageViewModel.executePageEvent(new Event.RefreshEvent(RefreshState.RefreshFinish));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                messageViewModel.executePageEvent(new Event.RefreshEvent(RefreshState.RefreshFinish));
            }
        });
    }


    public void getLocalMessage(final MessageViewModel messageViewModel) {
        RongIMClient.getInstance().getHistoryMessages(messageViewModel.getCurConversationType(), messageViewModel.getCurTargetId(), messageViewModel.getRefreshMessageId(), DEFAULT_COUNT + 1, new RongIMClient.ResultCallback<List<Message>>() {
            //返回列表（10，9，8，7，6，按messageId倒序）
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
                    messageViewModel.executePageEvent(new Event.RefreshEvent(RefreshState.RefreshFinish));
                } else {
                    //如果远端消息已经全部拉取完，则直接关闭
                    if (!messageViewModel.isRemoteMessageLoadFinish()) {
                        //拉取不到本地消息，表示拉取完,调用拉取远端离线消息
                        getRemoteMessage(messageViewModel);
                    } else {
                        messageViewModel.executePageEvent(new Event.RefreshEvent(RefreshState.RefreshFinish));
                    }
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
    }
}
