package io.rong.imkit.manager;

import io.rong.common.RLog;
import io.rong.imkit.BaseConversationEventListener;
import io.rong.imkit.ConversationEventListener;
import io.rong.imkit.IMCenter;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.InsertEvent;
import io.rong.imkit.event.actionevent.MessageEventListener;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;
import java.util.ArrayList;
import java.util.List;

public class UnReadMessageManager extends RongIMClient.OnReceiveMessageWrapperListener
        implements RongIMClient.SyncConversationReadStatusListener {
    private static final String TAG = "UnReadMessageManager";
    private final List<MultiConversationUnreadMsgInfo> mMultiConversationUnreadInfos;
    private ConversationEventListener mConversationEventListener =
            new BaseConversationEventListener() {
                @Override
                public void onClearedUnreadStatus(
                        Conversation.ConversationType type, String targetId) {
                    syncUnreadCount();
                }

                @Override
                public void onConversationRemoved(
                        Conversation.ConversationType type, String targetId) {
                    syncUnreadCount();
                }
            };
    private MessageEventListener mMessageEventListener =
            new BaseMessageEvent() {
                @Override
                public void onInsertMessage(InsertEvent event) {
                    if (event == null) {
                        return;
                    }
                    Message message = event.getMessage();
                    if (message != null
                            && message.getMessageDirection()
                                    .equals(Message.MessageDirection.RECEIVE)
                            && !message.getReceivedStatus().isRead()) {
                        syncUnreadCount();
                    }
                }
            };

    private UnReadMessageManager() {
        this.mMultiConversationUnreadInfos = new ArrayList<>();
        IMCenter.getInstance().addOnReceiveMessageListener(this);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
        IMCenter.getInstance().addMessageEventListener(mMessageEventListener);
        IMCenter.getInstance()
                .addConnectStatusListener(
                        new RongIMClient.ConnectCallback() {
                            @Override
                            public void onSuccess(String s) {}

                            @Override
                            public void onError(
                                    RongIMClient.ConnectionErrorCode connectionErrorCode) {}

                            @Override
                            public void onDatabaseOpened(
                                    RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                                syncUnreadCount();
                            }
                        });
        IMCenter.getInstance()
                .addOnRecallMessageListener(
                        new RongIMClient.OnRecallMessageListener() {
                            @Override
                            public boolean onMessageRecalled(
                                    Message message,
                                    RecallNotificationMessage recallNotificationMessage) {
                                syncUnreadCount();
                                return false;
                            }
                        });
        IMCenter.getInstance().addSyncConversationReadStatusListener(this);
    }

    public static UnReadMessageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    @Override
    public void onSyncConversationReadStatus(Conversation.ConversationType type, String targetId) {
        syncUnreadCount();
    }

    @Override
    public boolean onReceived(Message message, int left, boolean hasPackage, boolean offline) {
        if (left == 0 || !hasPackage) {
            syncUnreadCount();
        }
        return false;
    }

    private void syncUnreadCount() {
        for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
            RongIMClient.getInstance()
                    .getUnreadCount(
                            msgInfo.conversationTypes,
                            new RongIMClient.ResultCallback<Integer>() {
                                @Override
                                public void onSuccess(Integer integer) {
                                    RLog.d(TAG, "get result: " + integer);
                                    msgInfo.count = integer;
                                    msgInfo.observer.onCountChanged(integer);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {}
                            });
        }
    }

    /**
     * 设置未读消息数变化监听器。 注意:如果是在 activity 中设置,那么要在 activity 销毁时, 调用 {@link
     * UnReadMessageManager#removeObserver(UnReadMessageManager.IUnReadMessageObserver)} 否则会造成内存泄漏。
     *
     * @param observer 接收未读消息消息的监听器。
     * @param conversationTypes 接收未读消息的会话类型。
     */
    public void addObserver(
            Conversation.ConversationType[] conversationTypes,
            final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.e(TAG, "can't add a null observer!");
            return;
        }
        if (conversationTypes == null) {
            conversationTypes =
                    RongConfigCenter.conversationListConfig().getDataProcessor().supportedTypes();
        }
        synchronized (mMultiConversationUnreadInfos) {
            final MultiConversationUnreadMsgInfo msgInfo = new MultiConversationUnreadMsgInfo();
            msgInfo.conversationTypes = conversationTypes;
            msgInfo.observer = observer;
            mMultiConversationUnreadInfos.add(msgInfo);
            RongIMClient.getInstance()
                    .getUnreadCount(
                            conversationTypes,
                            new RongIMClient.ResultCallback<Integer>() {
                                @Override
                                public void onSuccess(Integer integer) {
                                    msgInfo.count = integer;
                                    msgInfo.observer.onCountChanged(integer);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {}
                            });
        }
    }

    public void removeObserver(final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        synchronized (mMultiConversationUnreadInfos) {
            MultiConversationUnreadMsgInfo result = null;
            for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
                if (msgInfo.observer == observer) {
                    result = msgInfo;
                    break;
                }
            }
            if (result != null) {
                mMultiConversationUnreadInfos.remove(result);
            }
        }
    }

    public void clearObserver() {
        synchronized (mMultiConversationUnreadInfos) {
            mMultiConversationUnreadInfos.clear();
        }
    }

    public interface IUnReadMessageObserver {
        void onCountChanged(int count);
    }

    private static class SingletonHolder {
        static UnReadMessageManager sInstance = new UnReadMessageManager();
    }

    private class MultiConversationUnreadMsgInfo {
        Conversation.ConversationType[] conversationTypes;
        int count;
        IUnReadMessageObserver observer;
    }
}
