package io.rong.imkit.conversation.messgelist.status;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;
import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_REMOTE_COUNT;

import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.ScrollMentionEvent;
import io.rong.imkit.event.uievent.ShowLoadMessageDialogEvent;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.HistoryMessageOption;
import io.rong.imlib.model.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageProcessor {

    public static void getMessagesDirection(
            final MessageViewModel messageViewModel,
            long sentTime,
            int count,
            final boolean isForward,
            final GetMessageCallback callback) {
        HistoryMessageOption historyMessageOption = new HistoryMessageOption();
        historyMessageOption.setDataTime(sentTime);
        historyMessageOption.setCount(count);
        if (isForward) {
            historyMessageOption.setOrder(HistoryMessageOption.PullOrder.DESCEND);
        } else {
            historyMessageOption.setOrder(HistoryMessageOption.PullOrder.ASCEND);
        }
        RongCoreClient.getInstance()
                .getMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        historyMessageOption,
                        new IRongCoreCallback.IGetMessageCallback() {
                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                IRongCoreEnum.ConversationLoadMessageType type =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationLoadMessageType();
                                if (!isForward) {
                                    Collections.reverse(messageList);
                                }

                                if (IRongCoreEnum.CoreErrorCode.SUCCESS.equals(errorCode)) {
                                    if (callback != null) {
                                        callback.onSuccess(messageList, false);
                                    }
                                    return;
                                }
                                if (IRongCoreEnum.ConversationLoadMessageType.ASK.equals(type)) {
                                    if (callback != null) {
                                        messageViewModel.executePageEvent(
                                                new ShowLoadMessageDialogEvent(
                                                        callback, messageList));
                                        callback.onErrorAsk(messageList);
                                    }
                                } else if (IRongCoreEnum.ConversationLoadMessageType.ONLY_SUCCESS
                                        .equals(type)) {
                                    messageViewModel.onGetHistoryMessage(
                                            Collections.<Message>emptyList());
                                    if (callback != null) {
                                        callback.onErrorOnlySuccess();
                                    }
                                } else {
                                    messageViewModel.onGetHistoryMessage(messageList);
                                    if (callback != null) {
                                        callback.onErrorAlways(messageList);
                                    }
                                }
                            }
                        });
    }

    public static void getMessagesAll(
            final MessageViewModel messageViewModel,
            final long sentTime,
            int before,
            final int after,
            final GetMessageCallback callback) {
        final List<Message> allData = new ArrayList<>();
        HistoryMessageOption historyMessageOption = new HistoryMessageOption();
        historyMessageOption.setDataTime(sentTime - 2);
        historyMessageOption.setCount(before);
        historyMessageOption.setOrder(HistoryMessageOption.PullOrder.ASCEND);
        RongCoreClient.getInstance()
                .getMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        historyMessageOption,
                        new IRongCoreCallback.IGetMessageCallback() {

                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                IRongCoreEnum.ConversationLoadMessageType type =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationLoadMessageType();
                                Collections.reverse(messageList);
                                allData.addAll(messageList);
                                if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                    getMessagesDescend(
                                            sentTime,
                                            after,
                                            messageViewModel,
                                            allData,
                                            errorCode,
                                            callback);
                                    return;
                                }

                                if (IRongCoreEnum.ConversationLoadMessageType.ASK.equals(type)) {
                                    if (callback != null) {
                                        messageViewModel.executePageEvent(
                                                new ShowLoadMessageDialogEvent(callback, allData));
                                        callback.onErrorAsk(allData);
                                    }
                                } else if (IRongCoreEnum.ConversationLoadMessageType.ONLY_SUCCESS
                                        .equals(type)) {
                                    messageViewModel.onGetHistoryMessage(
                                            Collections.<Message>emptyList());
                                    if (callback != null) {
                                        callback.onErrorOnlySuccess();
                                    }
                                } else {
                                    getMessagesDescend(
                                            sentTime,
                                            after,
                                            messageViewModel,
                                            allData,
                                            errorCode,
                                            callback);
                                }
                            }
                        });
    }

    private static void getMessagesDescend(
            final long sentTime,
            int after,
            final MessageViewModel viewModel,
            final List<Message> allData,
            final IRongCoreEnum.CoreErrorCode code,
            final GetMessageCallback callback) {
        HistoryMessageOption historyMessageOption = new HistoryMessageOption();
        historyMessageOption.setDataTime(sentTime);
        historyMessageOption.setCount(after);
        historyMessageOption.setOrder(HistoryMessageOption.PullOrder.DESCEND);
        RongCoreClient.getInstance()
                .getMessages(
                        viewModel.getCurConversationType(),
                        viewModel.getCurTargetId(),
                        historyMessageOption,
                        new IRongCoreCallback.IGetMessageCallback() {

                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                if (!(messageList == null || messageList.size() == 0)) {
                                    allData.addAll(messageList);
                                    if (callback != null) {
                                        if (code == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                            callback.onSuccess(allData, false);
                                        } else {
                                            callback.onErrorAlways(allData);
                                        }
                                    }
                                }
                            }
                        });
    }

    public static void processUnread(final MessageViewModel messageViewModel) {
        RongIMClient.getInstance()
                .getConversation(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        new RongIMClient.ResultCallback<Conversation>() {
                            @Override
                            public void onSuccess(Conversation conversation) {
                                // 设置历史未读数
                                if (conversation == null) {
                                    return;
                                }
                                int unreadMessageCount = conversation.getUnreadMessageCount();
                                // 交给不同会话类型处理未读消息
                                if (unreadMessageCount > 0) {
                                    messageViewModel.onExistUnreadMessage(
                                            conversation, unreadMessageCount);
                                }
                                // 获得第一条未读消息
                                initUnreadMessage(messageViewModel, unreadMessageCount);
                                // 判断有无 @ 记录
                                initMentionedMessage(conversation, messageViewModel);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {}
                        });
    }

    private static void initUnreadMessage(
            final MessageViewModel messageViewModel, final int unreadMessageCount) {
        RongIMClient.getInstance()
                .getTheFirstUnreadMessage(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        new RongIMClient.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message message) {
                                if (unreadMessageCount > MessageViewModel.SHOW_UNREAD_MESSAGE_COUNT
                                        && message != null) {
                                    messageViewModel.setFirstUnreadMessage(message);
                                    if (RongConfigCenter.conversationConfig()
                                            .isShowHistoryMessageBar(
                                                    messageViewModel.getCurConversationType())) {
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

    private static void initMentionedMessage(
            Conversation conversation, final MessageViewModel messageViewModel) {
        if (conversation.getMentionedCount() > 0) {
            RongIMClient.getInstance()
                    .getUnreadMentionedMessages(
                            messageViewModel.getCurConversationType(),
                            messageViewModel.getCurTargetId(),
                            new RongIMClient.ResultCallback<List<Message>>() {
                                @Override
                                public void onSuccess(List<Message> messages) {
                                    if (messages != null && messages.size() > 0) {
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

    public static void getLocalMessage(final MessageViewModel messageViewModel) {
        RongIMClient.getInstance()
                .getHistoryMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getRefreshMessageId(),
                        DEFAULT_COUNT + 1,
                        new RongIMClient.ResultCallback<List<Message>>() {
                            // 返回列表（10，9，8，7，6，按messageId倒序）
                            @Override
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
                                    messageViewModel.onGetHistoryMessage(result);
                                    messageViewModel.executePageEvent(
                                            new Event.RefreshEvent(RefreshState.RefreshFinish));
                                } else {
                                    // 如果远端消息已经全部拉取完，则直接关闭
                                    if (!messageViewModel.isRemoteMessageLoadFinish()) {
                                        // 拉取不到本地消息，表示拉取完,调用拉取远端离线消息
                                        getRemoteMessage(messageViewModel);
                                    } else {
                                        messageViewModel.executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                                    }
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {}
                        });
    }

    private static void getRemoteMessage(final MessageViewModel messageViewModel) {
        RongIMClient.getInstance()
                .getRemoteHistoryMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getRefreshSentTime(),
                        DEFAULT_REMOTE_COUNT,
                        new RongIMClient.ResultCallback<List<Message>>() {
                            @Override
                            public void onSuccess(List<Message> messages) {
                                // 不为空且大于0证明还有本地数据
                                if (messages != null && messages.size() > 0) {
                                    // 如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                                    List<Message> result;
                                    result = messages;
                                    messageViewModel.onGetHistoryMessage(result);
                                }
                                messageViewModel.executePageEvent(
                                        new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                messageViewModel.executePageEvent(
                                        new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }
                        });
    }

    public interface GetMessageCallback {
        void onSuccess(List<Message> list, boolean loadOnlyOnce);

        void onErrorAsk(List<Message> list);

        void onErrorAlways(List<Message> list);

        void onErrorOnlySuccess();
    }
}
