package io.rong.imkit.conversation.messgelist.status;

import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_COUNT;
import static io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel.DEFAULT_REMOTE_COUNT;

import io.rong.imkit.IMCenter;
import io.rong.imkit.RongIM;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.actionevent.RefreshEvent;
import io.rong.imkit.event.uievent.ScrollMentionEvent;
import io.rong.imkit.event.uievent.ShowLoadMessageDialogEvent;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.HistoryMessageOption;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageResult;
import io.rong.imlib.params.RefreshReferenceMessageParams;
import io.rong.message.ReferenceMessage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
            ChannelClient.getInstance()
                    .getHistoryMessages(
                            messageViewModel.getCurConversationType(),
                            messageViewModel.getCurTargetId(),
                            messageViewModel.getRefreshMessageId(),
                            DEFAULT_COUNT,
                            messageViewModel.getCurChannelId(),
                            new IRongCoreCallback.ResultCallback<List<Message>>() {
                                @Override
                                public void onSuccess(List<Message> messages) {
                                    refreshMessageExtendInfo(
                                            weakVM.get(),
                                            messages,
                                            new RefreshCallback<List<Message>>() {
                                                @Override
                                                public void onSuccess(List<Message> refreshList) {
                                                    if (!isForward) {
                                                        Collections.reverse(refreshList);
                                                    }
                                                    if (callback != null) {
                                                        callback.onSuccess(refreshList, false);
                                                    }
                                                    // 无论成功或者失败，均需要再调用一次断档接口
                                                    getMessages(
                                                            weakVM.get(),
                                                            sentTime,
                                                            count,
                                                            isForward,
                                                            callback);
                                                }
                                            });
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
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
                        new IRongCoreCallback.IGetMessageCallbackEx() {
                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    long syncTimestamp,
                                    boolean hasMoreMsg,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                RefreshCallback<List<Message>> refreshCallback =
                                        new RefreshCallback<List<Message>>() {
                                            @Override
                                            public void onSuccess(List<Message> refreshList) {
                                                IRongCoreEnum.ConversationLoadMessageType type =
                                                        RongConfigCenter.conversationConfig()
                                                                .getConversationLoadMessageType();
                                                if (!isForward) {
                                                    Collections.reverse(refreshList);
                                                }

                                                if (IRongCoreEnum.CoreErrorCode.SUCCESS.equals(
                                                        errorCode)) {
                                                    if (callback != null) {
                                                        callback.onSuccess(
                                                                refreshList, false, hasMoreMsg);
                                                    }
                                                    return;
                                                }
                                                if (IRongCoreEnum.ConversationLoadMessageType.ASK
                                                        .equals(type)) {
                                                    if (callback != null) {
                                                        if (weakVM.get() != null) {
                                                            weakVM.get()
                                                                    .executePageEvent(
                                                                            new ShowLoadMessageDialogEvent(
                                                                                    callback,
                                                                                    refreshList));
                                                        }
                                                        callback.onErrorAsk(refreshList);
                                                    }
                                                } else if (IRongCoreEnum.ConversationLoadMessageType
                                                        .ONLY_SUCCESS
                                                        .equals(type)) {
                                                    if (weakVM.get() != null) {
                                                        weakVM.get()
                                                                .onGetHistoryMessage(
                                                                        Collections
                                                                                .<Message>
                                                                                        emptyList());
                                                    }
                                                    if (callback != null) {
                                                        callback.onErrorOnlySuccess();
                                                    }
                                                } else {
                                                    if (weakVM.get() != null) {
                                                        weakVM.get()
                                                                .onGetHistoryMessage(refreshList);
                                                    }
                                                    if (callback != null) {
                                                        callback.onErrorAlways(refreshList);
                                                    }
                                                }
                                            }
                                        };
                                refreshMessageExtendInfo(
                                        weakVM.get(), messageList, refreshCallback);
                            }

                            @Override
                            public void onFail(IRongCoreEnum.CoreErrorCode errorCode) {}
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
                        new IRongCoreCallback.IGetMessageCallbackEx() {
                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    long syncTimestamp,
                                    boolean hasMoreMsg,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                RefreshCallback<List<Message>> refreshCallback =
                                        new RefreshCallback<List<Message>>() {
                                            @Override
                                            public void onSuccess(List<Message> refreshList) {
                                                IRongCoreEnum.ConversationLoadMessageType type =
                                                        RongConfigCenter.conversationConfig()
                                                                .getConversationLoadMessageType();
                                                Collections.reverse(refreshList);
                                                allData.addAll(refreshList);
                                                if (errorCode
                                                        == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                                    getMessagesDescend(
                                                            finalDescendSentTime,
                                                            after,
                                                            weakVM.get(),
                                                            allData,
                                                            errorCode,
                                                            callback);
                                                    return;
                                                }

                                                if (IRongCoreEnum.ConversationLoadMessageType.ASK
                                                        .equals(type)) {
                                                    if (weakVM.get() != null) {
                                                        weakVM.get()
                                                                .executePageEvent(
                                                                        new ShowLoadMessageDialogEvent(
                                                                                callback, allData));
                                                    }
                                                    if (callback != null) {
                                                        callback.onErrorAsk(allData);
                                                    }
                                                } else if (IRongCoreEnum.ConversationLoadMessageType
                                                        .ONLY_SUCCESS
                                                        .equals(type)) {
                                                    if (weakVM.get() != null) {
                                                        weakVM.get()
                                                                .onGetHistoryMessage(
                                                                        Collections
                                                                                .<Message>
                                                                                        emptyList());
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
                                        };
                                refreshMessageExtendInfo(
                                        weakVM.get(), messageList, refreshCallback);
                            }

                            @Override
                            public void onFail(IRongCoreEnum.CoreErrorCode errorCode) {}
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
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(viewModel);
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
                        new IRongCoreCallback.IGetMessageCallbackEx() {
                            @Override
                            public void onComplete(
                                    List<Message> messageList,
                                    long syncTimestamp,
                                    boolean hasMoreMsg,
                                    IRongCoreEnum.CoreErrorCode errorCode) {
                                RefreshCallback<List<Message>> refreshCallback =
                                        new RefreshCallback<List<Message>>() {
                                            @Override
                                            public void onSuccess(List<Message> refreshList) {
                                                if (!(refreshList == null
                                                        || refreshList.isEmpty())) {
                                                    allData.addAll(refreshList);
                                                }
                                                if (callback != null) {
                                                    if (code
                                                            == IRongCoreEnum.CoreErrorCode
                                                                    .SUCCESS) {
                                                        callback.onSuccess(
                                                                allData, false, hasMoreMsg);
                                                    } else {
                                                        callback.onErrorAlways(allData);
                                                    }
                                                }
                                            }
                                        };
                                refreshMessageExtendInfo(
                                        weakVM.get(), messageList, refreshCallback);
                            }

                            @Override
                            public void onFail(IRongCoreEnum.CoreErrorCode errorCode) {}
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
        ChannelClient.getInstance()
                .getTheFirstUnreadMessage(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getCurChannelId(),
                        new IRongCoreCallback.ResultCallback<Message>() {
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
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
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
            ChannelClient.getInstance()
                    .getUnreadMentionedMessages(
                            messageViewModel.getCurConversationType(),
                            messageViewModel.getCurTargetId(),
                            messageViewModel.getCurChannelId(),
                            new IRongCoreCallback.ResultCallback<List<Message>>() {
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
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
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
        ChannelClient.getInstance()
                .getHistoryMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getRefreshMessageId(),
                        DEFAULT_COUNT + 1,
                        messageViewModel.getCurChannelId(),
                        new IRongCoreCallback.ResultCallback<List<Message>>() {
                            // 返回列表（10，9，8，7，6，按messageId倒序）
                            @Override
                            public void onSuccess(List<Message> messages) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                RefreshCallback<List<Message>> refreshCallback =
                                        new RefreshCallback<List<Message>>() {
                                            @Override
                                            public void onSuccess(List<Message> refreshList) {
                                                // 不为空且大于0证明还有本地数据
                                                if (refreshList != null && refreshList.size() > 0) {
                                                    List<Message> result;
                                                    // 如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                                                    if (refreshList.size() < DEFAULT_COUNT + 1) {
                                                        result = refreshList;
                                                    } else {
                                                        result =
                                                                refreshList.subList(
                                                                        0, DEFAULT_COUNT);
                                                    }
                                                    weakVM.get().onGetHistoryMessage(result);
                                                    weakVM.get()
                                                            .executePageEvent(
                                                                    new Event.RefreshEvent(
                                                                            RefreshState
                                                                                    .RefreshFinish));
                                                } else {
                                                    // 如果远端消息已经全部拉取完，则直接关闭
                                                    if (!weakVM.get().isRemoteMessageLoadFinish()) {
                                                        // 拉取不到本地消息，表示拉取完,调用拉取远端离线消息
                                                        getRemoteMessage(weakVM.get());
                                                    } else {
                                                        weakVM.get()
                                                                .executePageEvent(
                                                                        new Event.RefreshEvent(
                                                                                RefreshState
                                                                                        .RefreshFinish));
                                                    }
                                                }
                                            }
                                        };
                                refreshMessageExtendInfo(weakVM.get(), messages, refreshCallback);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                // do nothing
                            }
                        });
    }

    private static void getRemoteMessage(final MessageViewModel messageViewModel) {
        if (messageViewModel == null) {
            return;
        }
        WeakReference<MessageViewModel> weakVM = new WeakReference<>(messageViewModel);
        ChannelClient.getInstance()
                .getRemoteHistoryMessages(
                        messageViewModel.getCurConversationType(),
                        messageViewModel.getCurTargetId(),
                        messageViewModel.getCurChannelId(),
                        messageViewModel.getRefreshSentTime(),
                        DEFAULT_REMOTE_COUNT,
                        new IRongCoreCallback.ResultCallback<List<Message>>() {
                            @Override
                            public void onSuccess(List<Message> messages) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                RefreshCallback<List<Message>> refreshCallback =
                                        new RefreshCallback<List<Message>>() {
                                            @Override
                                            public void onSuccess(List<Message> refreshList) {
                                                // 不为空且大于0证明还有本地数据
                                                if (refreshList != null && refreshList.size() > 0) {
                                                    // 如果不等于默认拉取条数，则证明本地拉取完毕记录标记位
                                                    List<Message> result;
                                                    result = refreshList;
                                                    weakVM.get().onGetHistoryMessage(result);
                                                }
                                                weakVM.get()
                                                        .executePageEvent(
                                                                new Event.RefreshEvent(
                                                                        RefreshState
                                                                                .RefreshFinish));
                                            }
                                        };
                                refreshMessageExtendInfo(weakVM.get(), messages, refreshCallback);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                if (weakVM.get() == null) {
                                    return;
                                }
                                weakVM.get()
                                        .executePageEvent(
                                                new Event.RefreshEvent(RefreshState.RefreshFinish));
                            }
                        });
    }

    /** 刷新消息的一些扩展属性：已读回执V5信息、引用类型消息的引用状态 */
    private static void refreshMessageExtendInfo(
            MessageViewModel messageViewModel,
            List<Message> messagesList,
            RefreshCallback<List<Message>> callback) {
        if (messageViewModel == null) {
            callback.onSuccess(messagesList);
            return;
        }
        ConversationIdentifier id = messageViewModel.getConversationIdentifier();
        // 查询消息的已读回执V5信息，查询后绑定到UIMessage再刷新一次。
        messageViewModel.getMessageReadReceiptInfoV5(messagesList);
        // 查询引用消息状态
        if (messagesList == null || messagesList.isEmpty()) {
            callback.onSuccess(messagesList);
            return;
        }
        List<String> uIds = new ArrayList<>();
        for (Message message : messagesList) {
            if (message.getContent() instanceof ReferenceMessage) {
                ReferenceMessage.ReferenceMessageStatus status =
                        ((ReferenceMessage) message.getContent()).getReferMsgStatus();
                if (status == ReferenceMessage.ReferenceMessageStatus.MODIFIED
                        || status == ReferenceMessage.ReferenceMessageStatus.DEFAULT) {
                    uIds.add(message.getUId());
                }
            }
        }
        if (uIds.isEmpty()) {
            callback.onSuccess(messagesList);
            return;
        }
        List<Message> resultList = new ArrayList<>(messagesList);

        RefreshReferenceMessageParams params = new RefreshReferenceMessageParams(id, uIds);
        RongCoreClient.getInstance()
                .refreshReferenceMessageWithParams(
                        params,
                        new IRongCoreCallback.RefreshReferenceMessageCallback() {
                            @Override
                            public void onLocalMessageBlock(List<MessageResult> msgList) {
                                if (msgList == null || msgList.isEmpty()) {
                                    callback.onSuccess(resultList);
                                    return;
                                }
                                HashMap<String, MessageResult> map = new HashMap<>();
                                for (MessageResult messageResult : msgList) {
                                    if (messageResult.getMessage() != null) {
                                        map.put(messageResult.getMessageUId(), messageResult);
                                    }
                                }
                                for (int i = 0; i < resultList.size(); i++) {
                                    Message msg = resultList.get(i);
                                    if (msg.getContent() instanceof ReferenceMessage) {
                                        MessageResult msgResult = map.get(msg.getUId());
                                        if (msgResult != null && msgResult.getMessage() != null) {
                                            resultList.set(i, msgResult.getMessage());
                                        }
                                    }
                                }
                                callback.onSuccess(resultList);
                            }

                            @Override
                            public void onRemoteMessageBlock(List<MessageResult> msgList) {
                                if (msgList == null || msgList.isEmpty()) {
                                    return;
                                }
                                List<Message> messages = new ArrayList<>();
                                for (MessageResult messageResult : msgList) {
                                    if (messageResult.getMessage() != null) {
                                        messages.add(messageResult.getMessage());
                                    }
                                }
                                RefreshEvent event = new RefreshEvent(messages, true);
                                IMCenter.getInstance().refreshMessage(event);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                callback.onSuccess(resultList);
                            }
                        });
    }

    public interface RefreshCallback<T> {
        void onSuccess(T result);
    }

    public interface GetMessageCallback {

        default void onSuccess(List<Message> list, boolean loadOnlyOnce, boolean isHasMoreMsg) {}

        default void onSuccess(List<Message> list, boolean loadOnlyOnce) {
            onSuccess(list, loadOnlyOnce, true);
        }

        void onErrorAsk(List<Message> list);

        void onErrorAlways(List<Message> list);

        void onErrorOnlySuccess();
    }
}
