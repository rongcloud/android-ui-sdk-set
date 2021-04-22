package io.rong.imkit.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.ConversationEventListener;
import io.rong.imkit.IMCenter;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;

public class UnReadMessageManager extends RongIMClient.OnReceiveMessageWrapperListener implements RongIMClient.SyncConversationReadStatusListener {
    private final static String TAG = "UnReadMessageManager";
    private final List<MultiConversationUnreadMsgInfo> mMultiConversationUnreadInfos;
    private int left;
    private ConversationEventListener mConversationEventListener = new ConversationEventListener() {

        @Override
        public void onSaveDraft(Conversation.ConversationType type, String targetId, String content) {
        }

        @Override
        public void onClearedMessage(Conversation.ConversationType type, String targetId) {
        }

        @Override
        public void onClearedUnreadStatus(Conversation.ConversationType type, String targetId) {
            syncUnreadCount();
        }

        @Override
        public void onConversationRemoved(Conversation.ConversationType type, String targetId) {
            syncUnreadCount();
        }

        @Override
        public void onOperationFailed(RongIMClient.ErrorCode code) {

        }

        @Override
        public void onClearConversations(Conversation.ConversationType... conversationTypes) {

        }
    };

    private UnReadMessageManager() {
        this.mMultiConversationUnreadInfos = new ArrayList<>();
        IMCenter.getInstance().addOnReceiveMessageListener(this);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
        IMCenter.getInstance().addConnectStatusListener(new RongIMClient.ConnectCallback() {
            @Override
            public void onSuccess(String s) {

            }

            @Override
            public void onError(RongIMClient.ConnectionErrorCode connectionErrorCode) {

            }

            @Override
            public void onDatabaseOpened(RongIMClient.DatabaseOpenStatus databaseOpenStatus) {
                syncUnreadCount();
            }
        });
        IMCenter.getInstance().addOnRecallMessageListener(new RongIMClient.OnRecallMessageListener() {
            @Override
            public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
                syncUnreadCount();
                return false;
            }
        });
        IMCenter.getInstance().addSyncConversationReadStatusListener(this);
    }

    @Override
    public void onSyncConversationReadStatus(Conversation.ConversationType type, String targetId) {
        syncUnreadCount();
    }

    private static class SingletonHolder {
        static UnReadMessageManager sInstance = new UnReadMessageManager();
    }

    public static UnReadMessageManager getInstance() {
        return SingletonHolder.sInstance;
    }

    @Override
    public boolean onReceived(Message message, int left, boolean hasPackage, boolean offline) {
        if (left == 0 || this.left == 0) {
            syncUnreadCount();
        }
        return false;
    }

    private void syncUnreadCount() {
        for (final MultiConversationUnreadMsgInfo msgInfo : mMultiConversationUnreadInfos) {
            RongIMClient.getInstance().getUnreadCount(msgInfo.conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    RLog.d(TAG, "get result: " + integer);
                    msgInfo.count = integer;
                    msgInfo.observer.onCountChanged(integer);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    public void addObserver(Conversation.ConversationType[] conversationTypes, final IUnReadMessageObserver observer) {
        if (conversationTypes == null) {
            conversationTypes = RongConfigCenter.conversationListConfig().getDataProcessor().supportedTypes();
        }
        synchronized (mMultiConversationUnreadInfos) {
            final MultiConversationUnreadMsgInfo msgInfo = new MultiConversationUnreadMsgInfo();
            msgInfo.conversationTypes = conversationTypes;
            msgInfo.observer = observer;
            mMultiConversationUnreadInfos.add(msgInfo);
            RongIMClient.getInstance().getUnreadCount(conversationTypes, new RongIMClient.ResultCallback<Integer>() {
                @Override
                public void onSuccess(Integer integer) {
                    msgInfo.count = integer;
                    msgInfo.observer.onCountChanged(integer);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

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

    private class MultiConversationUnreadMsgInfo {
        Conversation.ConversationType[] conversationTypes;
        int count;
        IUnReadMessageObserver observer;
    }

    public interface IUnReadMessageObserver {
        void onCountChanged(int count);
    }
}
