package io.rong.imkit.conversationlist.viewmodel;

import android.app.Application;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.rong.common.RLog;
import io.rong.imkit.ConversationEventListener;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.DataProcessor;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.conversationlist.model.GatheredConversation;
import io.rong.imkit.conversationlist.model.GroupConversation;
import io.rong.imkit.conversationlist.model.PublicServiceConversation;
import io.rong.imkit.conversationlist.model.SingleConversation;
import io.rong.imkit.event.Event;
import io.rong.imkit.event.actionevent.ClearEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.event.actionevent.InsertEvent;
import io.rong.imkit.event.actionevent.MessageEventListener;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.event.actionevent.RefreshEvent;
import io.rong.imkit.event.actionevent.SendEvent;
import io.rong.imkit.event.actionevent.SendMediaEvent;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.NoticeContent;
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.Message;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.imlib.RongIMClient.ConnectionStatusListener.ConnectionStatus;

public class ConversationListViewModel extends AndroidViewModel {
    private final String TAG = ConversationListViewModel.class.getSimpleName();
    private final int REFRESH_INTERVAL = 500;
    protected Conversation.ConversationType[] mSupportedTypes;
    protected int mSizePerPage;
    protected long mLastSyncTime;
    protected Application mApplication;
    protected CopyOnWriteArrayList<BaseUiConversation> mUiConversationList = new CopyOnWriteArrayList<>();
    protected MediatorLiveData<List<BaseUiConversation>> mConversationListLiveData;
    private MutableLiveData<ConnectionStatus> mConnectionStatusLiveData = new MutableLiveData<>();
    private MutableLiveData<NoticeContent> mNoticeContentLiveData = new MutableLiveData<>();
    private MutableLiveData<Event.RefreshEvent> mRefreshEventLiveData = new MutableLiveData<>();
    protected DataProcessor<Conversation> mDataFilter;
    protected Handler mHandler;
    private boolean isTaskScheduled;

    public ConversationListViewModel(Application application) {
        super(application);
        mApplication = application;
        mHandler = new Handler(Looper.getMainLooper());
        mSupportedTypes = RongConfigCenter.conversationListConfig().getDataProcessor().supportedTypes();
        mSizePerPage = RongConfigCenter.conversationListConfig().getConversationCountPerPage();
        mDataFilter = RongConfigCenter.conversationListConfig().getDataProcessor();

        mConversationListLiveData = new MediatorLiveData<>();
        mConversationListLiveData.addSource(RongUserInfoManager.getInstance().getAllUsersLiveData(), new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                RLog.d(TAG, "Users changed.");
                if (mUiConversationList.size() == 0) {
                    return;
                }
                if (users != null && users.size() > 0) {
                    for (BaseUiConversation uiConversation : mUiConversationList) {
                        uiConversation.onUserInfoUpdate(users);
                    }
                    mConversationListLiveData.postValue(mUiConversationList);
                }
            }
        });
        mConversationListLiveData.addSource(RongUserInfoManager.getInstance().getAllGroupsLiveData(), new Observer<List<Group>>() {
            @Override
            public void onChanged(List<Group> groups) {
                if (groups != null && groups.size() > 0) {
                    RLog.d(TAG, "on group list info changed. notify ui to update.");
                    for (BaseUiConversation uiConversation : mUiConversationList) {
                        if (uiConversation instanceof GroupConversation) {
                            ((GroupConversation) uiConversation).onGroupInfoUpdate(groups);
                        }
                        mConversationListLiveData.postValue(mUiConversationList);
                    }
                }
            }
        });

        mConversationListLiveData.addSource(RongUserInfoManager.getInstance().getAllGroupMembersLiveData(), new Observer<List<GroupMember>>() {
            @Override
            public void onChanged(List<GroupMember> groupMembers) {
                if (groupMembers != null && groupMembers.size() > 0) {
                    for (BaseUiConversation uiConversation : mUiConversationList) {
                        if (uiConversation instanceof GroupConversation) {
                            ((GroupConversation) uiConversation).onGroupMemberUpdate(groupMembers);
                            mConversationListLiveData.postValue(mUiConversationList);
                        }
                    }
                }
            }
        });

        IMCenter.getInstance().addOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().addConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().addConversationStatusListener(mConversationStatusListener);
        IMCenter.getInstance().addReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().addSyncConversationReadStatusListener(mSyncConversationReadStatusListener);
        IMCenter.getInstance().addOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
        IMCenter.getInstance().addMessageEventListener(mMessageEventListener);
    }

    /**
     * 从本地数据库获取会话列表。
     * 此处借鉴前端的函数节流思想，在 {@link #REFRESH_INTERVAL} 时间内，丢弃掉其它触发，只做一次执行。
     * 以便提高接受大量消息时的刷新性能。
     * </p>
     *
     * @param loadMore 是否根据上次同步的时间戳拉取更多会话。
     *                 false: 从数据库拉取最新 N 条会话。true: 根据 UI 上最后一条会话的时间戳，继续拉取之前的 N 条会话。
     */
    public void getConversationList(final boolean loadMore) {
        if (isTaskScheduled) {
            return;
        }
        isTaskScheduled = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long timestamp = 0;
                isTaskScheduled = false;
                if (loadMore) {
                    timestamp = mLastSyncTime;
                }
                RongIMClient.getInstance().getConversationListByPage(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (loadMore) {
                            mRefreshEventLiveData.postValue(new Event.RefreshEvent(RefreshState.LoadFinish));
                        } else {
                            mRefreshEventLiveData.postValue(new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }
                        if (conversations == null || conversations.size() == 0) {
                            return;
                        }
                        RLog.d(TAG, "getConversationListByPage. size:" + conversations.size());
                        mLastSyncTime = conversations.get(conversations.size() - 1).getSentTime();
                        CopyOnWriteArrayList<Conversation> copyList = new CopyOnWriteArrayList<>(conversations);
                        List<Conversation> filterResult = mDataFilter.filtered(copyList);
                        if (filterResult != null && filterResult.size() > 0) {
                            for (Conversation conversation : filterResult) {
                                boolean isGathered = mDataFilter.isGathered(conversation.getConversationType());
                                BaseUiConversation oldItem = findConversationFromList(conversation.getConversationType(), conversation.getTargetId(), isGathered);

                                if (oldItem != null) {
                                    oldItem.onConversationUpdate(conversation);
                                } else {
                                    if (isGathered) {
                                        mUiConversationList.add(new GatheredConversation(mApplication.getApplicationContext(), conversation));
                                    } else if (conversation.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                                        mUiConversationList.add(new GroupConversation(mApplication.getApplicationContext(), conversation));
                                    } else if (conversation.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                                            || conversation.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
                                        mUiConversationList.add(new PublicServiceConversation(mApplication.getApplicationContext(), conversation));
                                    } else {
                                        mUiConversationList.add(new SingleConversation(mApplication.getApplicationContext(), conversation));
                                    }
                                }
                            }
                            sort();
                            mConversationListLiveData.postValue(mUiConversationList);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        if (loadMore) {
                            mRefreshEventLiveData.postValue(new Event.RefreshEvent(RefreshState.LoadFinish));
                        } else {
                            mRefreshEventLiveData.postValue(new Event.RefreshEvent(RefreshState.RefreshFinish));
                        }
                    }
                }, timestamp, mSizePerPage, mSupportedTypes);
            }
        }, REFRESH_INTERVAL);

    }

    protected boolean isSupported(Conversation.ConversationType type) {
        for (Conversation.ConversationType conversationType : mSupportedTypes) {
            if (conversationType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private void getConversation(Conversation.ConversationType type, String targetId) {
        RongIMClient.getInstance().getConversation(type, targetId, new RongIMClient.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                if (conversation == null) {
                    return;
                }
                if (Objects.equals(conversation.getSentStatus(), Message.SentStatus.FAILED)
                        && ResendManager.getInstance().needResend(conversation.getLatestMessageId())) {
                    conversation.setSentStatus(Message.SentStatus.SENDING);
                }
                updateByConversation(conversation);
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                //Todo 数据获取失败，下拉刷新
            }
        });
    }

    protected void updateByConversation(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        List<Conversation> list = new CopyOnWriteArrayList<>();
        list.add(conversation);
        List<Conversation> filterList = mDataFilter.filtered(list);
        if (filterList != null && filterList.size() > 0 && isSupported(conversation.getConversationType())) {
            BaseUiConversation oldItem = findConversationFromList(conversation.getConversationType(), conversation.getTargetId(), mDataFilter.isGathered(conversation.getConversationType()));
            if (oldItem != null) {
                oldItem.onConversationUpdate(conversation);
            } else {
                if (mDataFilter.isGathered(conversation.getConversationType())) {
                    mUiConversationList.add(new GatheredConversation(mApplication.getApplicationContext(), conversation));
                } else if (conversation.getConversationType().equals(Conversation.ConversationType.GROUP)) {
                    mUiConversationList.add(new GroupConversation(mApplication.getApplicationContext(), conversation));
                } else if (conversation.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                        || conversation.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
                    mUiConversationList.add(new PublicServiceConversation(mApplication.getApplicationContext(), conversation));
                } else {
                    mUiConversationList.add(new SingleConversation(mApplication.getApplicationContext(), conversation));
                }
            }
            sort();
            mConversationListLiveData.postValue(mUiConversationList);
        }
    }

    /**
     * 会话状态（置顶或免打扰）发生变化时的回调。
     *
     * @param statuses 发生变更的会话状态。
     */
    private void onConversationStatusChange(ConversationStatus[] statuses) {
        for (ConversationStatus status : statuses) {
            Conversation.ConversationType type = status.getConversationType();
            BaseUiConversation oldItem = findConversationFromList(type, status.getTargetId(), mDataFilter.isGathered(type));
            if (oldItem != null) {
                if (status.getStatus().get(ConversationStatus.TOP_KEY) != null) {
                    oldItem.mCore.setTop(status.isTop());
                }
                if (status.getStatus().get(ConversationStatus.NOTIFICATION_KEY) != null) {
                    oldItem.mCore.setNotificationStatus(status.getNotifyStatus());
                }
                sort();
                mConversationListLiveData.postValue(mUiConversationList);
            } else {
                getConversation(type, status.getTargetId());
            }
        }
    }

    @Override
    protected void onCleared() {
        IMCenter.getInstance().removeConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().removeOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().removeConversationStatusListener(mConversationStatusListener);
        IMCenter.getInstance().removeMessageEventListener(mMessageEventListener);
        IMCenter.getInstance().removeReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().removeOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().removeConversationEventListener(mConversationEventListener);
        IMCenter.getInstance().removeSyncConversationReadStatusListeners(mSyncConversationReadStatusListener);
    }

    // conversationList排序规律：
    // 1. 首先是top会话，按时间顺序排列。
    // 2. 然后非top会话也是按时间排列。
    protected void sort() {
        List temp = Arrays.asList(mUiConversationList.toArray());
        Collections.sort(temp, new Comparator<BaseUiConversation>() {
            @Override
            public int compare(BaseUiConversation o1, BaseUiConversation o2) {
                if (o1.mCore.isTop() && o2.mCore.isTop() || !o1.mCore.isTop() && !o2.mCore.isTop()) {
                    if (o1.mCore.getSentTime() > o2.mCore.getSentTime()) {
                        return -1;
                    } else if (o1.mCore.getSentTime() < o2.mCore.getSentTime()) {
                        return 1;
                    } else {
                        return 0;
                    }
                } else if (o1.mCore.isTop() && !o2.mCore.isTop()) {
                    return -1;
                } else if (!o1.mCore.isTop() && o2.mCore.isTop()) {
                    return 1;
                }
                return 0;
            }
        });
        mUiConversationList.clear();
        mUiConversationList.addAll(temp);
    }

    protected BaseUiConversation findConversationFromList(Conversation.ConversationType conversationType, String targetId, boolean isGathered) {
        for (BaseUiConversation uiConversation : mUiConversationList) {
            if (isGathered && uiConversation instanceof GatheredConversation && ((GatheredConversation) uiConversation).mCore.getConversationType().equals(conversationType)) {
                return uiConversation;
            } else if (!isGathered) {
                if (uiConversation.mCore.getConversationType().equals(conversationType) && uiConversation.mCore.getTargetId().equals(targetId)) {
                    return uiConversation;
                }
            }
        }
        return null;
    }

    public MediatorLiveData<List<BaseUiConversation>> getConversationListLiveData() {
        return mConversationListLiveData;
    }

    /**
     * 获取连接状态通知内容
     *
     * @return
     */
    public LiveData<NoticeContent> getNoticeContentLiveData() {
        return mNoticeContentLiveData;
    }

    /**
     * 获取刷新事件 LiveData
     *
     * @return 刷新事件
     */
    public LiveData<Event.RefreshEvent> getRefreshEventLiveData() {
        return mRefreshEventLiveData;
    }

    /**
     * 更新连接状态通知
     *
     * @param status
     */
    private void updateNoticeContent(ConnectionStatus status) {
        NoticeContent noticeContent = new NoticeContent();
        String content = null;
        boolean isShowContent = true;
        int resId = 0;

        Resources resources = mApplication.getResources();
        if (!RongConfigCenter.conversationListConfig().isEnableConnectStateNotice()) {
            RLog.e(TAG, "rc_is_show_warning_notification is disabled.");
            return;
        }
        if (status.equals(ConnectionStatus.NETWORK_UNAVAILABLE)) {
            content = resources.getString(R.string.rc_conversation_list_notice_network_unavailable);
            resId = R.drawable.rc_ic_error_notice;
        } else if (status.equals(ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT)) {
            content = resources.getString(R.string.rc_conversation_list_notice_kicked);
            resId = R.drawable.rc_ic_error_notice;
        } else if (status.equals(ConnectionStatus.CONNECTED)) {
            isShowContent = false;
        } else if (status.equals(ConnectionStatus.UNCONNECTED)) {
            content = resources.getString(R.string.rc_conversation_list_notice_disconnect);
            resId = R.drawable.rc_ic_error_notice;
        } else if (status.equals(ConnectionStatus.CONNECTING)
                || status.equals(ConnectionStatus.SUSPEND)
        ) {
            content = resources.getString(R.string.rc_conversation_list_notice_connecting);
            resId = R.drawable.rc_conversationlist_notice_connecting_animated;
        }

        noticeContent.setContent(content);
        noticeContent.setShowNotice(isShowContent);
        noticeContent.setIconResId(resId);

        mNoticeContentLiveData.postValue(noticeContent);
    }

    private ConversationEventListener mConversationEventListener = new ConversationEventListener() {
        @Override
        public void onSaveDraft(Conversation.ConversationType type, String targetId, String content) {
            getConversation(type, targetId);
        }

        @Override
        public void onClearedMessage(Conversation.ConversationType type, String targetId) {
            getConversation(type, targetId);
        }

        @Override
        public void onClearedUnreadStatus(Conversation.ConversationType type, String targetId) {
            getConversation(type, targetId);
        }

        @Override
        public void onConversationRemoved(Conversation.ConversationType type, String targetId) {
            BaseUiConversation oldItem = findConversationFromList(type, targetId, mDataFilter.isGathered(type));
            if (oldItem != null) {
                mUiConversationList.remove(oldItem);
                mConversationListLiveData.postValue(mUiConversationList);
            }
        }

        @Override
        public void onOperationFailed(RongIMClient.ErrorCode code) {

        }

        @Override
        public void onClearConversations(Conversation.ConversationType... conversationTypes) {
            RLog.d(TAG, "onClearConversations");
            List<Conversation.ConversationType> clearedTypes = Arrays.asList(conversationTypes);
            Iterator<BaseUiConversation> iterator = mUiConversationList.iterator();
            while (iterator.hasNext()) {
                BaseUiConversation item = iterator.next();
                if (clearedTypes.contains(item.mCore.getConversationType())) {
                        mUiConversationList.remove(item);
                }
            }
            mConversationListLiveData.postValue(mUiConversationList);
        }
    };
    private MessageEventListener mMessageEventListener = new MessageEventListener() {
        @Override
        public void onSendMessage(SendEvent event) {
            if (event != null && event.getMessage() != null) {
                getConversation(event.getMessage().getConversationType(), event.getMessage().getTargetId());
            }
        }

        @Override
        public void onSendMediaMessage(SendMediaEvent event) {
            if (event != null && event.getEvent() != SendMediaEvent.PROGRESS && event.getMessage() != null) {
                getConversation(event.getMessage().getConversationType(), event.getMessage().getTargetId());
            }
        }

        @Override
        public void onDownloadMessage(DownloadEvent event) {
            if (event == null) {
                return;
            }
            Conversation.ConversationType type = event.getMessage().getConversationType();
            String targetId = event.getMessage().getTargetId();
            BaseUiConversation oldItem = findConversationFromList(type, targetId, mDataFilter.isGathered(type));
            if (oldItem != null && oldItem.mCore.getLatestMessageId() == event.getMessage().getMessageId() && event.getEvent() != DownloadEvent.PROGRESS) {
                getConversation(type, targetId);
            }
        }

        @Override
        public void onDeleteMessage(DeleteEvent event) {
            if (event != null) {
                getConversation(event.getConversationType(), event.getTargetId());
            }
        }

        @Override
        public void onRecallEvent(RecallEvent event) {
            if (event != null) {
                getConversation(event.getConversationType(), event.getTargetId());
            }
        }

        @Override
        public void onRefreshEvent(RefreshEvent event) {

        }

        @Override
        public void onInsertMessage(InsertEvent event) {
            if (event == null) {
                return;
            }
            Conversation.ConversationType type = event.getMessage().getConversationType();
            String targetId = event.getMessage().getTargetId();
            BaseUiConversation oldItem = findConversationFromList(type, targetId, mDataFilter.isGathered(type));
            if (oldItem != null) {
                getConversation(type, targetId);
            }
        }

        @Override
        public void onClearMessages(ClearEvent event) {
            getConversation(event.getConversationType(), event.getTargetId());
        }
    };

    private RongIMClient.OnReceiveMessageWrapperListener mOnReceiveMessageListener = new RongIMClient.OnReceiveMessageWrapperListener() {
        @Override
        public boolean onReceived(Message message, int left, boolean hasPackage, boolean offline) {
            getConversationList(false);
            return false;
        }
    };

    private RongIMClient.ReadReceiptListener mReadReceiptListener = new RongIMClient.ReadReceiptListener() {
        @Override
        public void onReadReceiptReceived(Message message) {
            if (message != null && message.getContent() instanceof ReadReceiptMessage) {
                Conversation.ConversationType type = message.getConversationType();
                BaseUiConversation oldItem = findConversationFromList(type, message.getTargetId(), mDataFilter.isGathered(type));
                if (oldItem != null && type.equals(Conversation.ConversationType.PRIVATE)
                        && oldItem.mCore.getSentTime() == ((ReadReceiptMessage) message.getContent()).getLastMessageSendTime()) {
                    oldItem.mCore.setSentStatus(Message.SentStatus.READ);
                    mConversationListLiveData.postValue(mUiConversationList);
                }
            }
        }

        @Override
        public void onMessageReceiptRequest(Conversation.ConversationType type, String targetId, String messageUId) {

        }

        @Override
        public void onMessageReceiptResponse(Conversation.ConversationType type, String targetId, String messageUId, HashMap<String, Long> respondUserIdList) {

        }
    };

    private RongIMClient.OnRecallMessageListener mOnRecallMessageListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            if (message != null) {
                getConversation(message.getConversationType(), message.getTargetId());
            }
            return false;
        }
    };

    private RongIMClient.SyncConversationReadStatusListener mSyncConversationReadStatusListener = new RongIMClient.SyncConversationReadStatusListener() {
        @Override
        public void onSyncConversationReadStatus(Conversation.ConversationType type, String targetId) {
            BaseUiConversation oldItem = findConversationFromList(type, targetId, mDataFilter.isGathered(type));
            if (oldItem != null) {
                oldItem.mCore.setUnreadMessageCount(0);
                mConversationListLiveData.postValue(mUiConversationList);
            }
        }
    };

    private RongIMClient.ConnectionStatusListener mConnectionStatusListener = new RongIMClient.ConnectionStatusListener() {
        @Override
        public void onChanged(ConnectionStatus status) {
            mConnectionStatusLiveData.postValue(status);
            if (status.equals(ConnectionStatus.CONNECTED)) {
                getConversationList(false);
            }
            // 更新连接状态通知信息
            updateNoticeContent(status);
        }
    };

    private RongIMClient.ConversationStatusListener mConversationStatusListener = new RongIMClient.ConversationStatusListener() {
        @Override
        public void onStatusChanged(ConversationStatus[] conversationStatus) {
            onConversationStatusChange(conversationStatus);
        }
    };

    public void clearAllNotification() {
        if (RongConfigCenter.featureConfig().rc_wipe_out_notification_message) {
            RongNotificationManager.getInstance().clearAllNotification();
        }
    }
}
