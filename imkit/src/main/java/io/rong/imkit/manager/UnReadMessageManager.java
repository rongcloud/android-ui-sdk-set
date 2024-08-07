package io.rong.imkit.manager;

import io.rong.common.rlog.RLog;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UnReadMessageManager extends RongIMClient.OnReceiveMessageWrapperListener
        implements RongIMClient.SyncConversationReadStatusListener {
    private static final String TAG = "UnReadMessageManager";
    private final List<WeakReference<MultiConversationUnreadMsgInfo>> mMultiConversationUnreadInfos;
    private final List<MultiConversationUnreadMsgInfo> mForeverMultiConversationUnreadInfos;
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

    private RongIMClient.ConnectCallback connectCallback =
            new RongIMClient.ConnectCallback() {
                @Override
                public void onSuccess(String s) {
                    // do nothing
                }

                @Override
                public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {
                    // do nothing
                }

                @Override
                public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                    syncUnreadCount();
                }
            };

    private RongIMClient.OnRecallMessageListener recallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    syncUnreadCount();
                    return false;
                }
            };

    private UnReadMessageManager() {
        this.mMultiConversationUnreadInfos = new ArrayList<>();
        this.mForeverMultiConversationUnreadInfos = new ArrayList<>();
        IMCenter.getInstance().addAsyncOnReceiveMessageListener(this);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
        IMCenter.getInstance().addMessageEventListener(mMessageEventListener);
        IMCenter.getInstance().addConnectStatusListener(connectCallback);
        IMCenter.getInstance().addOnRecallMessageListener(recallMessageListener);
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
        syncWeakReferenceUnreadCount();
        syncForeverObserverUnreadCount();
    }

    private void syncWeakReferenceUnreadCount() {
        for (final WeakReference<MultiConversationUnreadMsgInfo> weakMsgInfo :
                mMultiConversationUnreadInfos) {
            if (weakMsgInfo == null) {
                continue;
            }
            MultiConversationUnreadMsgInfo msgInfo = weakMsgInfo.get();
            if (msgInfo == null) {
                continue;
            }
            RongIMClient.getInstance()
                    .getUnreadCount(
                            msgInfo.conversationTypes,
                            new RongIMClient.ResultCallback<Integer>() {
                                @Override
                                public void onSuccess(Integer integer) {
                                    RLog.d(TAG, "get result: " + integer);
                                    if (weakMsgInfo == null) {
                                        return;
                                    }
                                    MultiConversationUnreadMsgInfo msgInfo = weakMsgInfo.get();
                                    if (msgInfo == null) {
                                        return;
                                    }
                                    msgInfo.count = integer;
                                    msgInfo.observer.onCountChanged(integer);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                    // do nothing
                                }
                            });
        }
    }

    private void syncForeverObserverUnreadCount() {
        for (MultiConversationUnreadMsgInfo msgInfo : mForeverMultiConversationUnreadInfos) {
            if (msgInfo == null) {
                continue;
            }
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
                                public void onError(RongIMClient.ErrorCode e) {
                                    // do nothing
                                }
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
            final WeakReference<MultiConversationUnreadMsgInfo> weakMsgInfo =
                    new WeakReference<>(msgInfo);
            mMultiConversationUnreadInfos.add(weakMsgInfo);
            RongIMClient.getInstance()
                    .getUnreadCount(
                            conversationTypes,
                            new RongIMClient.ResultCallback<Integer>() {
                                @Override
                                public void onSuccess(Integer integer) {
                                    if (weakMsgInfo.get() == null) {
                                        return;
                                    }
                                    weakMsgInfo.get().count = integer;
                                    weakMsgInfo.get().observer.onCountChanged(integer);
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                    // do nothing
                                }
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
    public void addForeverObserver(
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
        synchronized (mForeverMultiConversationUnreadInfos) {
            final MultiConversationUnreadMsgInfo msgInfo = new MultiConversationUnreadMsgInfo();
            msgInfo.conversationTypes = conversationTypes;
            msgInfo.observer = observer;
            mForeverMultiConversationUnreadInfos.add(msgInfo);
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
                                public void onError(RongIMClient.ErrorCode e) {
                                    // do nothing
                                }
                            });
        }
    }

    public void removeObserver(final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        synchronized (mMultiConversationUnreadInfos) {
            WeakReference<MultiConversationUnreadMsgInfo> result = null;
            for (final WeakReference<MultiConversationUnreadMsgInfo> weakMsgInfo :
                    mMultiConversationUnreadInfos) {
                if (weakMsgInfo == null) {
                    continue;
                }
                MultiConversationUnreadMsgInfo msgInfo = weakMsgInfo.get();
                if (msgInfo == null) {
                    continue;
                }
                if (msgInfo.observer == observer) {
                    result = weakMsgInfo;
                    break;
                }
            }
            if (result != null) {
                mMultiConversationUnreadInfos.remove(result);
            }
        }
    }

    public void removeForeverObserver(final IUnReadMessageObserver observer) {
        if (observer == null) {
            RLog.w(TAG, "removeOnReceiveUnreadCountChangedListener Illegal argument");
            return;
        }
        synchronized (mForeverMultiConversationUnreadInfos) {
            MultiConversationUnreadMsgInfo result = null;
            for (MultiConversationUnreadMsgInfo msgInfo : mForeverMultiConversationUnreadInfos) {
                if (msgInfo == null) {
                    continue;
                }
                if (msgInfo.observer == observer) {
                    result = msgInfo;
                    break;
                }
            }
            if (result != null) {
                mForeverMultiConversationUnreadInfos.remove(result);
            }
        }
    }

    public void clearObserver() {
        synchronized (mMultiConversationUnreadInfos) {
            mMultiConversationUnreadInfos.clear();
        }
    }

    public void clearForeverObserver() {
        synchronized (mForeverMultiConversationUnreadInfos) {
            mForeverMultiConversationUnreadInfos.clear();
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
