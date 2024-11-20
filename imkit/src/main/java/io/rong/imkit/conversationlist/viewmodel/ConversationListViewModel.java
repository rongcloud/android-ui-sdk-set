package io.rong.imkit.conversationlist.viewmodel;

import android.app.Application;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import io.rong.common.rlog.RLog;
import io.rong.imkit.ConversationEventListener;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.RongIM;
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
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.widget.refresh.constant.RefreshState;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.RongIMClient.ConnectionStatusListener.ConnectionStatus;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConversationListViewModel extends AndroidViewModel
        implements RongUserInfoManager.UserDataObserver {
    private final String TAG = ConversationListViewModel.class.getSimpleName();
    private final int REFRESH_INTERVAL = 500;
    protected Conversation.ConversationType[] mSupportedTypes;
    protected int mSizePerPage;

    protected int mPageLimit;

    protected boolean mTopPriority;
    protected Application mApplication;
    protected CopyOnWriteArrayList<BaseUiConversation> mUiConversationList =
            new CopyOnWriteArrayList<>();
    protected MediatorLiveData<List<BaseUiConversation>> mConversationListLiveData;
    protected DataProcessor<Conversation> mDataFilter;
    protected HandlerThread workThread;
    protected Handler mHandler;
    private MutableLiveData<ConnectionStatus> mConnectionStatusLiveData = new MutableLiveData<>();
    private MutableLiveData<NoticeContent> mNoticeContentLiveData = new MutableLiveData<>();
    private MutableLiveData<Event.RefreshEvent> mRefreshEventLiveData = new MutableLiveData<>();
    private boolean isTaskScheduled;
    private int mTime = 500;
    private int mDelayRefreshTime = 5000;
    private ConversationEventListener mConversationEventListener =
            new ConversationEventListener() {
                @Override
                public void onSaveDraft(
                        Conversation.ConversationType type, String targetId, String content) {
                    getConversation(type, targetId);
                }

                @Override
                public void onClearedMessage(Conversation.ConversationType type, String targetId) {
                    getConversation(type, targetId);
                }

                @Override
                public void onClearedUnreadStatus(
                        Conversation.ConversationType type, String targetId) {
                    getConversation(type, targetId);
                }

                @Override
                public void onConversationRemoved(
                        Conversation.ConversationType type, String targetId) {
                    getConversation(type, targetId);
                }

                @Override
                public void onOperationFailed(RongIMClient.ErrorCode code) {
                    // do nothing
                }

                @Override
                public void onClearConversations(
                        Conversation.ConversationType... conversationTypes) {
                    getConversationList(false, false, 0);
                }

                @Override
                public void onMessageReceivedStatusChange(
                        int messageId,
                        Conversation.ConversationType conversationType,
                        String targetId,
                        Message.ReceivedStatus status) {
                    getConversation(conversationType, targetId);
                }
            };
    private MessageEventListener mMessageEventListener =
            new MessageEventListener() {
                @Override
                public void onSendMessage(SendEvent event) {
                    if (event != null && event.getMessage() != null) {
                        getConversation(
                                event.getMessage().getConversationType(),
                                event.getMessage().getTargetId());
                    }
                }

                @Override
                public void onSendMediaMessage(SendMediaEvent event) {
                    if (event != null
                            && event.getEvent() != SendMediaEvent.PROGRESS
                            && event.getMessage() != null) {
                        getConversation(
                                event.getMessage().getConversationType(),
                                event.getMessage().getTargetId());
                    }
                }

                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    if (event == null) {
                        return;
                    }
                    Conversation.ConversationType type = event.getMessage().getConversationType();
                    String targetId = event.getMessage().getTargetId();
                    BaseUiConversation oldItem =
                            findConversationFromList(
                                    type,
                                    targetId,
                                    mDataFilter.isGathered(
                                            ConversationIdentifier.obtain(type, targetId, "")));
                    if (oldItem != null
                            && oldItem.mCore.getLatestMessageId()
                                    == event.getMessage().getMessageId()
                            && event.getEvent() != DownloadEvent.PROGRESS) {
                        getConversation(type, targetId);
                    }
                }

                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    if (event != null) {
                        getDeletedMsgConversation(
                                event.getConversationType(),
                                event.getTargetId(),
                                event.getMessageIds());
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
                    if (event == null || event.getMessage() == null) {
                        return;
                    }
                    getConversation(
                            event.getMessage().getConversationType(),
                            event.getMessage().getTargetId());
                }

                @Override
                public void onInsertMessage(InsertEvent event) {
                    if (event == null) {
                        return;
                    }
                    Conversation.ConversationType type = event.getMessage().getConversationType();
                    String targetId = event.getMessage().getTargetId();
                    BaseUiConversation oldItem =
                            findConversationFromList(
                                    type,
                                    targetId,
                                    mDataFilter.isGathered(
                                            ConversationIdentifier.obtain(type, targetId, "")));
                    getConversation(type, targetId);
                }

                @Override
                public void onClearMessages(ClearEvent event) {
                    getConversation(event.getConversationType(), event.getTargetId());
                }
            };
    private RongIMClient.OnReceiveMessageWrapperListener mOnReceiveMessageListener =
            new RongIMClient.OnReceiveMessageWrapperListener() {
                @Override
                public boolean onReceived(
                        Message message, int left, boolean hasPackage, boolean offline) {
                    if (!offline) {
                        mTime = 500;
                    } else if (offline && !hasPackage && left == 0) {
                        mTime = 500;
                    } else {
                        mTime = mDelayRefreshTime;
                    }
                    getConversationList(false, false, mTime);
                    return false;
                }
            };
    private RongIMClient.ReadReceiptListener mReadReceiptListener =
            new RongIMClient.ReadReceiptListener() {
                @Override
                public void onReadReceiptReceived(Message message) {
                    if (message != null && message.getContent() instanceof ReadReceiptMessage) {
                        mHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        Conversation.ConversationType type =
                                                message.getConversationType();
                                        BaseUiConversation oldItem =
                                                findConversationFromList(
                                                        type,
                                                        message.getTargetId(),
                                                        mDataFilter.isGathered(
                                                                ConversationIdentifier.obtain(
                                                                        type,
                                                                        message.getTargetId(),
                                                                        "")));
                                        if (oldItem != null
                                                && type.equals(
                                                        Conversation.ConversationType.PRIVATE)
                                                && oldItem.mCore.getSentTime()
                                                        <= ((ReadReceiptMessage)
                                                                        message.getContent())
                                                                .getLastMessageSendTime()) {
                                            oldItem.mCore.setSentStatus(Message.SentStatus.READ);
                                            refreshConversationList();
                                        }
                                    }
                                });
                    }
                }

                @Override
                public void onMessageReceiptRequest(
                        Conversation.ConversationType type, String targetId, String messageUId) {
                    // do nothing
                }

                @Override
                public void onMessageReceiptResponse(
                        Conversation.ConversationType type,
                        String targetId,
                        String messageUId,
                        HashMap<String, Long> respondUserIdList) {
                    // do nothing
                }
            };
    private RongIMClient.OnRecallMessageListener mOnRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (message != null) {
                        getConversation(message.getConversationType(), message.getTargetId());
                    }
                    return false;
                }
            };
    private RongIMClient.SyncConversationReadStatusListener mSyncConversationReadStatusListener =
            new RongIMClient.SyncConversationReadStatusListener() {
                @Override
                public void onSyncConversationReadStatus(
                        Conversation.ConversationType type, String targetId) {
                    mHandler.post(
                            new Runnable() {
                                @Override
                                public void run() {
                                    BaseUiConversation oldItem =
                                            findConversationFromList(
                                                    type,
                                                    targetId,
                                                    mDataFilter.isGathered(
                                                            ConversationIdentifier.obtain(
                                                                    type, targetId, "")));
                                    if (oldItem != null) {
                                        Conversation conversation =
                                                oldItem.currentConversation(targetId);
                                        conversation.setUnreadMessageCount(0);
                                        conversation.setMentionedCount(0);
                                        conversation.setMentionedMeCount(0);
                                        oldItem.onConversationUpdate(conversation);
                                        refreshConversationList();
                                    }
                                }
                            });
                }
            };
    private ConnectionStatus preConnectionStatus;
    private RongIMClient.ConnectionStatusListener mConnectionStatusListener =
            new RongIMClient.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus status) {
                    mConnectionStatusLiveData.postValue(status);
                    if (status.equals(ConnectionStatus.CONNECTED)) {
                        getConversationList(false, false, 0);
                    }
                    // 更新连接状态通知信息
                    updateNoticeContent(status);
                    preConnectionStatus = status;
                }
            };
    private RongIMClient.ConversationStatusListener mConversationStatusListener =
            new RongIMClient.ConversationStatusListener() {
                @Override
                public void onStatusChanged(ConversationStatus[] conversationStatus) {
                    mHandler.post(() -> onConversationStatusChange(conversationStatus));
                }
            };

    private RongIMClient.ResultCallback<Message> mCancelSendMediaMessageListener =
            new RongIMClient.ResultCallback<Message>() {

                @Override
                public void onSuccess(Message message) {
                    getConversation(message.getConversationType(), message.getTargetId());
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    // do nothing
                }
            };

    public ConversationListViewModel(Application application) {
        super(application);
        mApplication = application;
        workThread = new HandlerThread("Conversation_Thread");
        workThread.start();
        mHandler = new Handler(workThread.getLooper());
        mSupportedTypes =
                RongConfigCenter.conversationListConfig().getDataProcessor().supportedTypes();
        //        mSizePerPage =
        // RongConfigCenter.conversationListConfig().getConversationCountPerPage();;
        mSizePerPage = 10;
        mPageLimit = 10;
        mDataFilter = RongConfigCenter.conversationListConfig().getDataProcessor();
        mDelayRefreshTime = RongConfigCenter.conversationListConfig().getDelayRefreshTime();
        mTopPriority = RongConfigCenter.conversationListConfig().isTopPriority();

        mConversationListLiveData = new MediatorLiveData<>();
        RongUserInfoManager.getInstance().addUserDataObserver(this);
        IMCenter.getInstance().addAsyncOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().addConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().addConversationStatusListener(mConversationStatusListener);
        IMCenter.getInstance().addReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance()
                .addSyncConversationReadStatusListener(mSyncConversationReadStatusListener);
        IMCenter.getInstance().addOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
        IMCenter.getInstance().addMessageEventListener(mMessageEventListener);
        IMCenter.getInstance().addCancelSendMediaMessageListener(mCancelSendMediaMessageListener);
        updateNoticeContent(RongIM.getInstance().getCurrentConnectionStatus());
    }

    /**
     * 从本地数据库获取会话列表。 此处借鉴前端的函数节流思想，在 {@link #REFRESH_INTERVAL} 时间内，丢弃掉其它触发，只做一次执行。 以便提高接受大量消息时的刷新性能。
     *
     * @param loadMore 是否根据上次同步的时间戳拉取更多会话。 false: 从数据库拉取最新 N 条会话。true: 根据 UI 上最后一条会话的时间戳，继续拉取之前的 N
     *     条会话。
     * @param isEventManual 是否是用还手动触发的刷新获取，手动触发的需要主动关闭下
     */
    public void getConversationList(
            final boolean loadMore, final boolean isEventManual, long delayTime) {
        if (isTaskScheduled) {
            sendRefreshEvent(isEventManual, loadMore);
            return;
        }
        isTaskScheduled = true;
        GetConversationListRunnable runnable =
                new GetConversationListRunnable(this, loadMore, isEventManual, delayTime);
        if (delayTime == 0) {
            mHandler.post(runnable);
        } else {
            mHandler.postDelayed(runnable, delayTime);
        }
    }

    private void getConversationListFromRunnable(
            ConversationListViewModel viewModel,
            final boolean loadMore,
            final boolean isEventManual,
            final long delayTime) {
        if (viewModel == null) {
            return;
        }

        isTaskScheduled = false;
        ConversationListResultCallback callback =
                new ConversationListResultCallback(viewModel, loadMore, isEventManual, delayTime);
        if (loadMore) {
            mSizePerPage += mPageLimit;
        }
        RongCoreClient.getInstance()
                .getConversationListByPage(
                        callback, 0, mSizePerPage, isTopPriority(), mSupportedTypes);
    }

    private void doUpdate(
            List<Conversation> conversations,
            final boolean loadMore,
            final boolean isEventManual,
            final long delayTime) {
        mHandler.post(
                () -> {
                    if (loadMore) {
                        if (conversations != null) {
                            mSizePerPage = conversations.size();
                        }
                    } else {
                        mUiConversationList.clear();
                    }
                    sendRefreshEvent(isEventManual, loadMore);
                    if (conversations == null || conversations.size() == 0) {
                        return;
                    }
                    RLog.d(TAG, "getConversationListByPage. size:" + conversations.size());
                    CopyOnWriteArrayList<Conversation> copyList =
                            new CopyOnWriteArrayList<>(conversations);
                    List<Conversation> filterResult = mDataFilter.filtered(copyList);
                    if (filterResult != null && filterResult.size() > 0) {
                        for (Conversation conversation : filterResult) {
                            boolean isGathered =
                                    mDataFilter.isGathered(
                                            ConversationIdentifier.obtain(conversation));
                            BaseUiConversation oldItem =
                                    findConversationFromList(
                                            conversation.getConversationType(),
                                            conversation.getTargetId(),
                                            isGathered);

                            if (oldItem != null) {
                                oldItem.onConversationUpdate(conversation);
                            } else {
                                if (isGathered) {
                                    mUiConversationList.add(
                                            new GatheredConversation(
                                                    mApplication.getApplicationContext(),
                                                    conversation));
                                } else if (conversation
                                        .getConversationType()
                                        .equals(Conversation.ConversationType.GROUP)) {
                                    mUiConversationList.add(
                                            new GroupConversation(
                                                    mApplication.getApplicationContext(),
                                                    conversation));
                                } else if (conversation
                                                .getConversationType()
                                                .equals(
                                                        Conversation.ConversationType
                                                                .PUBLIC_SERVICE)
                                        || conversation
                                                .getConversationType()
                                                .equals(
                                                        Conversation.ConversationType
                                                                .APP_PUBLIC_SERVICE)) {
                                    mUiConversationList.add(
                                            new PublicServiceConversation(
                                                    mApplication.getApplicationContext(),
                                                    conversation));
                                } else {
                                    mUiConversationList.add(
                                            new SingleConversation(
                                                    mApplication.getApplicationContext(),
                                                    conversation));
                                }
                            }
                        }
                        sort();
                        refreshConversationList();
                    }
                });
    }

    /**
     * 发送刷新事件
     *
     * @param isEventManual 是否是手动触发的刷新
     * @param loadMore 是否是加载更多
     */
    protected void sendRefreshEvent(boolean isEventManual, boolean loadMore) {
        if (isEventManual) {
            RefreshState refreshState =
                    loadMore ? RefreshState.LoadFinish : RefreshState.RefreshFinish;
            mRefreshEventLiveData.postValue(new Event.RefreshEvent(refreshState));
        }
    }

    protected BaseUiConversation findConversationFromList(
            Conversation.ConversationType conversationType, String targetId, boolean isGathered) {
        List<BaseUiConversation> baseUiConversationArrayList =
                new ArrayList<>(this.mUiConversationList);
        for (int i = baseUiConversationArrayList.size() - 1; i >= 0; i--) {
            BaseUiConversation uiConversation = baseUiConversationArrayList.get(i);
            if (isGathered
                    && uiConversation instanceof GatheredConversation
                    && Objects.equals(
                            conversationType, uiConversation.mCore.getConversationType())) {
                return uiConversation;
            } else if (!isGathered) {
                if (uiConversation.mCore.getConversationType().equals(conversationType)
                        && Objects.equals(uiConversation.mCore.getTargetId(), targetId)) {
                    return uiConversation;
                }
            }
        }
        return null;
    }

    // conversationList排序规律：
    // 1. 首先是top会话，按时间顺序排列。
    // 2. 然后非top会话也是按时间排列。
    protected void sort() {
        //        List temp = Arrays.asList(mUiConversationList.toArray());
        //        Collections.sort(
        //                temp,
        //                new Comparator<BaseUiConversation>() {
        //                    @Override
        //                    public int compare(BaseUiConversation o1, BaseUiConversation o2) {
        //                        if (o1.mCore.isTop() && o2.mCore.isTop()
        //                                || !o1.mCore.isTop() && !o2.mCore.isTop()) {
        //                            if (TimeUtils.getLatestTime(o1.mCore)
        //                                    > TimeUtils.getLatestTime(o2.mCore)) {
        //                                return -1;
        //                            } else if (TimeUtils.getLatestTime(o1.mCore)
        //                                    < TimeUtils.getLatestTime(o2.mCore)) {
        //                                return 1;
        //                            } else {
        //                                return 0;
        //                            }
        //                        } else if (o1.mCore.isTop() && !o2.mCore.isTop()) {
        //                            return -1;
        //                        } else if (!o1.mCore.isTop() && o2.mCore.isTop()) {
        //                            return 1;
        //                        }
        //                        return 0;
        //                    }
        //                });
        //        mUiConversationList.clear();
        //        mUiConversationList.addAll(temp);
    }

    /**
     * 会话状态（置顶或免打扰）发生变化时的回调。
     *
     * @param statuses 发生变更的会话状态。
     */
    private void onConversationStatusChange(ConversationStatus[] statuses) {
        getConversationList(false, false, 0);
    }

    private void getConversation(Conversation.ConversationType type, String targetId) {
        getConversationList(false, false, 0);
        //        RongCoreClient.getInstance()
        //                .getConversation(
        //                        type,
        //                        targetId,
        //                        new IRongCoreCallback.ResultCallback<Conversation>() {
        //                            @Override
        //                            public void onSuccess(Conversation conversation) {
        //                                mHandler.post(
        //                                        new Runnable() {
        //                                            @Override
        //                                            public void run() {
        //                                                if (conversation == null) {
        //                                                    return;
        //                                                }
        //                                                if (Objects.equals(
        //                                                        conversation.getSentStatus(),
        //                                                        Message.SentStatus.FAILED)
        //                                                        && ResendManager.getInstance()
        //                                                        .needResend(
        //                                                                conversation
        //
        // .getLatestMessageId())) {
        //                                                    conversation.setSentStatus(
        //                                                            Message.SentStatus.SENDING);
        //                                                }
        //                                                MessageNotificationHelper.updateLevelMap(
        //                                                        conversation);
        //                                                updateByConversation(conversation);
        //                                            }
        //                                        });
        //                            }
        //
        //                            @Override
        //                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
        //                                // do nothing
        //                            }
        //                        });
    }

    private void getDeletedMsgConversation(
            Conversation.ConversationType type, String targetId, int[] deleteMsgId) {
        RongIMClient.getInstance()
                .getConversation(
                        type,
                        targetId,
                        new RongIMClient.ResultCallback<Conversation>() {
                            @Override
                            public void onSuccess(Conversation conversation) {
                                if (conversation == null) {
                                    return;
                                }

                                // 如果查询到的会话对应LatestMessageId在待删除消息ID数组中，则再延时查询一次
                                for (int id : deleteMsgId) {
                                    if (conversation.getLatestMessageId() == id) {
                                        mHandler.postDelayed(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        getConversation(type, targetId);
                                                    }
                                                },
                                                200);
                                        return;
                                    }
                                }
                                mHandler.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                if (Objects.equals(
                                                                conversation.getSentStatus(),
                                                                Message.SentStatus.FAILED)
                                                        && ResendManager.getInstance()
                                                                .needResend(
                                                                        conversation
                                                                                .getLatestMessageId())) {
                                                    conversation.setSentStatus(
                                                            Message.SentStatus.SENDING);
                                                }
                                                updateByConversation(conversation);
                                            }
                                        });
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                // do nothing
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
        if (filterList != null
                && filterList.size() > 0
                && isSupported(conversation.getConversationType())) {
            BaseUiConversation oldItem =
                    findConversationFromList(
                            conversation.getConversationType(),
                            conversation.getTargetId(),
                            mDataFilter.isGathered(ConversationIdentifier.obtain(conversation)));
            if (oldItem != null) {
                oldItem.onConversationUpdate(conversation);
            } else {
                if (mDataFilter.isGathered(ConversationIdentifier.obtain(conversation))) {
                    mUiConversationList.add(
                            new GatheredConversation(
                                    mApplication.getApplicationContext(), conversation));
                } else if (conversation
                        .getConversationType()
                        .equals(Conversation.ConversationType.GROUP)) {
                    mUiConversationList.add(
                            new GroupConversation(
                                    mApplication.getApplicationContext(), conversation));
                } else if (conversation
                                .getConversationType()
                                .equals(Conversation.ConversationType.PUBLIC_SERVICE)
                        || conversation
                                .getConversationType()
                                .equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
                    mUiConversationList.add(
                            new PublicServiceConversation(
                                    mApplication.getApplicationContext(), conversation));
                } else {
                    mUiConversationList.add(
                            new SingleConversation(
                                    mApplication.getApplicationContext(), conversation));
                }
            }
            sort();
            refreshConversationList();
        }
    }

    protected boolean isSupported(Conversation.ConversationType type) {
        if (mSupportedTypes == null) {
            return false;
        }
        for (Conversation.ConversationType conversationType : mSupportedTypes) {
            if (conversationType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    /** 是否优先显示置顶会话（查询结果的排序方式，是否置顶优先，true 表示置顶会话优先返回，false 结果只以会话时间排序） */
    protected boolean isTopPriority() {
        return mTopPriority;
    }

    @Override
    protected void onCleared() {
        if (workThread != null) {
            workThread.quit();
        }
        RongUserInfoManager.getInstance().removeUserDataObserver(this);
        IMCenter.getInstance().removeConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().removeAsyncOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().removeConversationStatusListener(mConversationStatusListener);
        IMCenter.getInstance().removeMessageEventListener(mMessageEventListener);
        IMCenter.getInstance().removeReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().removeOnRecallMessageListener(mOnRecallMessageListener);
        IMCenter.getInstance().removeConversationEventListener(mConversationEventListener);
        IMCenter.getInstance()
                .removeSyncConversationReadStatusListeners(mSyncConversationReadStatusListener);
        IMCenter.getInstance()
                .removeCancelSendMediaMessageListener(mCancelSendMediaMessageListener);
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
        if (status == preConnectionStatus) {
            return;
        }
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
                || status.equals(ConnectionStatus.SUSPEND)) {
            content = resources.getString(R.string.rc_conversation_list_notice_connecting);
            resId = R.drawable.rc_conversationlist_notice_connecting_animated;
        } else if (status.equals(ConnectionStatus.CONNECTION_STATUS_PROXY_UNAVAILABLE)) {
            content = resources.getString(R.string.rc_conversation_list_notice_proxy_unavailable);
            resId = R.drawable.rc_ic_error_notice;
        } else {
            if (preConnectionStatus == ConnectionStatus.CONNECTION_STATUS_PROXY_UNAVAILABLE) {
                return;
            }
            content = resources.getString(R.string.rc_conversation_list_notice_network_unavailable);
            resId = R.drawable.rc_ic_error_notice;
        }

        noticeContent.setContent(content);
        noticeContent.setShowNotice(isShowContent);
        noticeContent.setIconResId(resId);

        mNoticeContentLiveData.postValue(noticeContent);
    }

    public void clearAllNotification() {
        if (RongConfigCenter.featureConfig().rc_wipe_out_notification_message) {
            RongNotificationManager.getInstance().clearAllNotification();
        }
    }

    protected void refreshConversationList() {
        removeDupData();
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mConversationListLiveData.setValue(mUiConversationList);
        } else {
            mConversationListLiveData.postValue(mUiConversationList);
        }
    }

    // 刷新列表前先做去重操作
    private void removeDupData() {
        List<BaseUiConversation> dupList = new ArrayList<>();
        HashSet<String> removeDupSet = new HashSet<>();
        for (BaseUiConversation c : mUiConversationList) {
            String key = c.getConversationKey();
            if (!removeDupSet.contains(key)) {
                removeDupSet.add(key);
            } else {
                dupList.add(c);
            }
        }
        removeDupSet.clear();
        if (!dupList.isEmpty()) {
            for (BaseUiConversation c : dupList) {
                mUiConversationList.remove(c);
            }
        }
        dupList.clear();
    }

    @Override
    public void onUserUpdate(UserInfo info) {
        if (mTime == mDelayRefreshTime) {
            return;
        }
        if (info == null) {
            return;
        }
        for (BaseUiConversation baseUiConversation : mUiConversationList) {
            baseUiConversation.onUserInfoUpdate(info);
        }
        refreshConversationList();
    }

    @Override
    public void onGroupUpdate(Group group) {
        if (mTime == mDelayRefreshTime) {
            return;
        }
        for (BaseUiConversation baseUiConversation : mUiConversationList) {
            baseUiConversation.onGroupInfoUpdate(group);
        }
        refreshConversationList();
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        if (mTime == mDelayRefreshTime) {
            return;
        }
        for (BaseUiConversation baseUiConversation : mUiConversationList) {
            baseUiConversation.onGroupMemberUpdate(groupUserInfo);
        }
        refreshConversationList();
    }

    private class GetConversationListRunnable implements Runnable {
        private ConversationListViewModel mViewModel;
        final boolean mLoadMore;
        final boolean mIsEventManual;
        final long mDelayTime;

        protected GetConversationListRunnable(
                ConversationListViewModel viewModel,
                final boolean loadMore,
                final boolean isEventManual,
                final long delayTime) {
            mViewModel = viewModel;
            mLoadMore = loadMore;
            mIsEventManual = isEventManual;
            mDelayTime = delayTime;
        }

        @Override
        public void run() {
            getConversationListFromRunnable(mViewModel, mLoadMore, mIsEventManual, mDelayTime);
        }
    }

    private static class ConversationListResultCallback
            extends IRongCoreCallback.ResultCallback<List<Conversation>> {
        private final WeakReference<ConversationListViewModel> viewModelRef;
        final boolean isLoadMore;
        private final boolean isEventManual;
        private final long mDelayTime;
        private final long session;

        ConversationListResultCallback(
                ConversationListViewModel viewModel,
                boolean isLoadMore,
                boolean isEventManual,
                long delayTime) {
            this.viewModelRef = new WeakReference<>(viewModel);
            this.isLoadMore = isLoadMore;
            this.isEventManual = isEventManual;
            this.mDelayTime = delayTime;
            session = System.currentTimeMillis();
            RLog.e(
                    viewModelRef.get().TAG,
                    "session:"
                            + session
                            + " ,loadMore:"
                            + isLoadMore
                            + " ,manual:"
                            + isEventManual
                            + " ,delay:"
                            + mDelayTime);
        }

        @Override
        public void onSuccess(List<Conversation> conversations) {
            if (viewModelRef.get() != null) {
                ConversationListViewModel viewModel = viewModelRef.get();
                viewModelRef.get().doUpdate(conversations, isLoadMore, isEventManual, mDelayTime);
                String log = createLogMsgFromConversationList(conversations);
                RLog.e(viewModelRef.get().TAG, "session:" + session + " ,data:" + log);
            }
        }

        @Override
        public void onError(IRongCoreEnum.CoreErrorCode e) {
            if (viewModelRef.get() != null) {
                viewModelRef.get().sendRefreshEvent(isEventManual, isLoadMore);
            }
        }

        private String createLogMsgFromConversationList(List<Conversation> conversationListList) {
            StringBuilder sb = new StringBuilder();
            for (Conversation c : conversationListList) {
                sb.append("{")
                        .append(c.getConversationType().getValue())
                        .append(",")
                        .append(c.getTargetId())
                        .append("};");
            }
            return sb.toString();
        }
    }
}
