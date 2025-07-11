package io.rong.imkit.conversation.messgelist.status;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;
import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_REMOTE_COUNT;

import io.rong.imkit.IMCenter;
import io.rong.imkit.RongIM;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.uievent.ScrollMentionEvent;
import io.rong.imkit.event.uievent.ShowLoadMessageDialogEvent;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.HistoryMessageOption;
import io.rong.imlib.model.Message;
import java.lang.ref.WeakReference;
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
        if (messageViewModel == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(), false);
            }
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        // 为了加速第一次加载速度：如果是私群聊，且sentTime为0。则先加载本地消息，成功或回调后，再调用断档消息。
        if (needLoadLocalMessagesAtFirst(messageViewModel.getCurConversationType(), sentTime)) {
            RongIMClient.getInstance()
                    .getHistoryMessages(
                            messageViewModel.getCurConversationType(),
                            messageViewModel.getCurTargetId(),
                            messageViewModel.getRefreshMessageId(),
                            DEFAULT_COUNT,
                            new RongIMClient.ResultCallback<List<Message>>() {
                                @Override
                                public void onSuccess(List<Message> messages) {
                                    if (!isForward) {
                                        Collections.reverse(messages);
                                    }
                                    // 如果本地查询的消息有断档，则不抛给上层数据，使用断档接口查询最新消息
                                    //                                    if
                                    // (!isMayHasMoreMessagesBefore(messages)) {
                                    //
                                    //                                    }
                                    if (callback != null) {
                                        callback.onSuccess(messages, false);
                                    }
                                    // 无论成功或者失败，均需要再调用一次断档接口
                                    getMessages(weakVM.get(), sentTime, count, isForward, callback);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    // 无论成功或者失败，均需要再调用一次断档接口
                                    getMessages(weakVM.get(), sentTime, count, isForward, callback);
                                }
                            });
        } else {
            getMessages(messageViewModel, sentTime, count, isForward, callback);
        }
    }

    private static boolean needLoadLocalMessagesAtFirst(
            Conversation.ConversationType type, long sentTime) {
        boolean supportType =
                Conversation.ConversationType.PRIVATE.equals(type)
                        || Conversation.ConversationType.GROUP.equals(type);
        return sentTime == 0
                && (supportType
                        || !NetUtils.isNetWorkAvailable(IMCenter.getInstance().getContext()));
    }

    private static boolean isMayHasMoreMessagesBefore(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        for (Message message : messages) {
            if (message.isMayHasMoreMessagesBefore()) {
                return true;
            }
        }
        return false;
    }

    public static void getMessages(
            final MessageViewModel messageViewModel,
            long sentTime,
            int count,
            final boolean isForward,
            final GetMessageCallback callback) {
        if (messageViewModel == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(), false);
            }
            return;
        }
        HistoryMessageOption historyMessageOption = new HistoryMessageOption();
        historyMessageOption.setDataTime(sentTime);
        historyMessageOption.setCount(count);
        if (isForward) {
            historyMessageOption.setOrder(HistoryMessageOption.PullOrder.DESCEND);
        } else {
            historyMessageOption.setOrder(HistoryMessageOption.PullOrder.ASCEND);
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        RongIM.getInstance()
                .getMessages(
                        messageViewModel.getConversationIdentifier(),
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
                                        if (weakVM.get() != null) {
                                            weakVM.get()
                                                    .executePageEvent(
                                                            new ShowLoadMessageDialogEvent(
                                                                    callback, messageList));
                                        }
                                        callback.onErrorAsk(messageList);
                                    }
                                } else if (IRongCoreEnum.ConversationLoadMessageType.ONLY_SUCCESS
                                        .equals(type)) {
                                    if (weakVM.get() != null) {
                                        weakVM.get()
                                                .onGetHistoryMessage(
                                                        Collections.<Message>emptyList());
                                    }
                                    if (callback != null) {
                                        callback.onErrorOnlySuccess();
                                    }
                                } else {
                                    if (weakVM.get() != null) {
                                        weakVM.get().onGetHistoryMessage(messageList);
                                    }
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
        if (messageViewModel == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>(), false);
            }
            return;
        }
        final List<Message> allData = new ArrayList<>();
        HistoryMessageOption historyMessageOption = new HistoryMessageOption();
        historyMessageOption.setDataTime(sentTime);
        historyMessageOption.setCount(before);
        historyMessageOption.setOrder(HistoryMessageOption.PullOrder.ASCEND);
        // 降序拉取的时间
        final long finalDescendSentTime = sentTime + 1;
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        ChannelClient.getInstance()
                .getMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getConversationIdentifier().getChannelId(),
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
                                            finalDescendSentTime,
                                            after,
                                            weakVM.get(),
                                            allData,
                                            errorCode,
                                            callback);
                                    return;
                                }

                                if (IRongCoreEnum.ConversationLoadMessageType.ASK.equals(type)) {
                                    if (weakVM.get() != null) {
                                        weakVM.get()
                                                .executePageEvent(
                                                        new ShowLoadMessageDialogEvent(
                                                                callback, allData));
                                    }
                                    if (callback != null) {
                                        callback.onErrorAsk(allData);
                                    }
                                } else if (IRongCoreEnum.ConversationLoadMessageType.ONLY_SUCCESS
                                        .equals(type)) {
                                    if (weakVM.get() != null) {
                                        weakVM.get()
                                                .onGetHistoryMessage(
                                                        Collections.<Message>emptyList());
                                    }
                                    if (callback != null) {
                                        callback.onErrorOnlySuccess();
                                    }
                                } else {
                                    getMessagesDescend(
                                            finalDescendSentTime,
                                            after,
                                            weakVM.get(),
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
        if (viewModel == null) {
            if (callback != null) {
                callback.onErrorAlways(allData);
            }
            return;
        }
        HistoryMessageOption historyMessageOption = new HistoryMessageOption();
        historyMessageOption.setDataTime(sentTime);
        historyMessageOption.setCount(after);
        historyMessageOption.setOrder(HistoryMessageOption.PullOrder.DESCEND);
        ChannelClient.getInstance()
                .getMessages(
                        viewModel.getCurConversationType(),
                        viewModel.getCurTargetId(),
                        viewModel.getConversationIdentifier().getChannelId(),
                        historyMessageOption,
                        new IRongCoreCallback.IGetMessageCallback() {

                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                if (!(messageList == null || messageList.isEmpty())) {
                                    allData.addAll(messageList);
                                }
                                if (callback != null) {
                                    if (code == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                        callback.onSuccess(allData, false);
                                    } else {
                                        callback.onErrorAlways(allData);
                                    }
                                }
                            }
                        });
    }

    public static void processUnread(final MessageViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        ChannelClient.getInstance()
                .getConversation(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getConversationIdentifier().getChannelId(),
                        new IRongCoreCallback.ResultCallback<Conversation>() {
                            @Override
                            public void onSuccess(Conversation conversation) {
                                // 设置历史未读数
                                if (conversation == null) {
                                    return;
                                }
                                int unreadMessageCount = conversation.getUnreadMessageCount();
                                // 交给不同会话类型处理未读消息
                                if (unreadMessageCount > 0) {
                                    if (weakVM.get() != null) {
                                        weakVM.get()
                                                .onExistUnreadMessage(
                                                        conversation, unreadMessageCount);
                                    }
                                }
                                // 获得第一条未读消息
                                initUnreadMessage(weakVM.get(), unreadMessageCount);
                                // 判断有无 @ 记录
                                initMentionedMessage(conversation, weakVM.get());
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                // default implementation ignored
                            }
                        });
    }

    private static void initUnreadMessage(
            final MessageViewModel messageViewModel, final int unreadMessageCount) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        RongIMClient.getInstance()
                .getTheFirstUnreadMessage(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        new RongIMClient.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message message) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                if (unreadMessageCount > MessageViewModel.SHOW_UNREAD_MESSAGE_COUNT
                                        && message != null) {
                                    weakVM.get().setFirstUnreadMessage(message);
                                    if (RongConfigCenter.conversationConfig()
                                            .isShowHistoryMessageBar(
                                                    weakVM.get().getCurConversationType())) {
                                        weakVM.get().showHistoryBar(unreadMessageCount);
                                    }
                                }
                                weakVM.get().setInitUnreadMessageFinish(true);
                                weakVM.get().cleanUnreadStatus();
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                weakVM.get().setInitUnreadMessageFinish(true);
                                weakVM.get().cleanUnreadStatus();
                            }
                        });
    }

    private static void initMentionedMessage(
            Conversation conversation, final MessageViewModel messageViewModel) {
        if (conversation == null) {
            return;
        }
        if (messageViewModel == null) {
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        if (conversation.getMentionedCount() > 0) {
            RongIMClient.getInstance()
                    .getUnreadMentionedMessages(
                            messageViewModel.getCurConversationType(),
                            messageViewModel.getCurTargetId(),
                            new RongIMClient.ResultCallback<List<Message>>() {
                                @Override
                                public void onSuccess(List<Message> messages) {
                                    if (weakVM.get() == null) {
                                        return;
                                    }
                                    if (messages != null && messages.size() > 0) {
                                        weakVM.get().setNewUnReadMentionMessages(messages);
                                        weakVM.get().executePageEvent(new ScrollMentionEvent());
                                    }
                                    weakVM.get().setInitMentionedMessageFinish(true);
                                    weakVM.get().cleanUnreadStatus();
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                    if (weakVM.get() == null) {
                                        return;
                                    }
                                    weakVM.get().setInitMentionedMessageFinish(true);
                                    weakVM.get().cleanUnreadStatus();
                                }
                            });
        } else {
            messageViewModel.setInitMentionedMessageFinish(true);
            messageViewModel.cleanUnreadStatus();
        }
    }

    public static void getLocalMessage(final MessageViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
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
                                if (weakVM.get() == null) {
                                    return;
                                }
                                // 不为空且大于0证明还有本地数据
                                if (messages != null && messages.size() > 0) {
                                    List<Message> result;
                                    // 如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                                    if (messages.size() < DEFAULT_COUNT + 1) {
                                        result = messages;
                                    } else {
                                        result = messages.subList(0, DEFAULT_COUNT);
                                    }
                                    weakVM.get().onGetHistoryMessage(result);
                                    weakVM.get()
                                            .executePageEvent(
                                                    new Event.RefreshEvent(
                                                            RefreshState.RefreshFinish));
                                } else {
                                    // 如果远端消息已经全部拉取完，则直接关闭
                                    if (!weakVM.get().isRemoteMessageLoadFinish()) {
                                        // 拉取不到本地消息，表示拉取完,调用拉取远端离线消息
                                        getRemoteMessage(weakVM.get());
                                    } else {
                                        weakVM.get()
                                                .executePageEvent(
                                                        new Event.RefreshEvent(
                                                                RefreshState.RefreshFinish));
                                    }
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                // do nothing
                            }
                        });
    }

    private static void getRemoteMessage(final MessageViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        RongIMClient.getInstance()
                .getRemoteHistoryMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getRefreshSentTime(),
                        DEFAULT_REMOTE_COUNT,
                        new RongIMClient.ResultCallback<List<Message>>() {
                            @Override
                            public void onSuccess(List<Message> messages) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                // 不为空且大于0证明还有本地数据
                                if (messages != null && messages.size() > 0) {
                                    // 如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                                    List<Message> result;
                                    result = messages;
                                    weakVM.get().onGetHistoryMessage(result);
                                }
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                weakVM.get()
                                        .executePageEvent(
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
