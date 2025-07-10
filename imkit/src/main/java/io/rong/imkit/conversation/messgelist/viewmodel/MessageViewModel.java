package io.rong.imkit.conversation.messgelist.viewmodel;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.FileUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.BaseConversationEventListener;
import io.rong.imkit.IMCenter;
import io.rong.imkit.MessageItemLongClickAction;
import io.rong.imkit.MessageItemLongClickActionManager;
import io.rong.imkit.R;
import io.rong.imkit.RongIM;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.moreaction.IClickActions;
import io.rong.imkit.conversation.messgelist.processor.ConversationProcessorFactory;
import io.rong.imkit.conversation.messgelist.processor.IConversationBusinessProcessor;
import io.rong.imkit.conversation.messgelist.provider.MessageClickType;
import io.rong.imkit.event.actionevent.ClearEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.event.actionevent.InsertEvent;
import io.rong.imkit.event.actionevent.MessageEventListener;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.event.actionevent.RefreshEvent;
import io.rong.imkit.event.actionevent.SendEvent;
import io.rong.imkit.event.actionevent.SendMediaEvent;
import io.rong.imkit.event.uievent.InputBarEvent;
import io.rong.imkit.event.uievent.MessageEvent;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.event.uievent.ScrollEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.event.uievent.ShowLongClickDialogEvent;
import io.rong.imkit.event.uievent.ToastEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.feature.forward.ForwardClickActions;
import io.rong.imkit.feature.forward.ForwardManager;
import io.rong.imkit.feature.translation.RCTranslationResultWrapper;
import io.rong.imkit.feature.translation.TranslationProvider;
import io.rong.imkit.feature.translation.TranslationResultListenerWrapper;
import io.rong.imkit.handler.ReadReceiptV5Handler;
import io.rong.imkit.handler.StreamMessageHandler;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.IAudioPlayListener;
import io.rong.imkit.manager.hqvoicemessage.AutoDownloadEntry;
import io.rong.imkit.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.cache.MessageList;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.ReadReceiptResponseV5;
import io.rong.imlib.model.UnknownMessage;
import io.rong.imlib.model.UserInfo;
import io.rong.message.HQVoiceMessage;
import io.rong.message.HistoryDividerMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.VoiceMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageViewModel extends AndroidViewModel
        implements MessageEventListener,
                RongUserInfoManager.UserDataObserver,
                TranslationResultListenerWrapper {
    public static final int DEFAULT_COUNT =
            RongConfigCenter.conversationConfig().getConversationHistoryMessageCount();
    public static final int DEFAULT_REMOTE_COUNT =
            RongConfigCenter.conversationConfig().getConversationRemoteMessageCount();
    public static final int SHOW_UNREAD_MESSAGE_COUNT =
            RongConfigCenter.conversationConfig().getConversationShowUnreadMessageCount();
    private static final String TAG = "MessageViewModel";
    public static String[] writePermission =
            new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private List<UiMessage> mUiMessages = new MessageList<>(6000);
    private List<UiMessage> mSelectedUiMessage = new ArrayList<>();
    private MediatorLiveData<PageEvent> mPageEventLiveData = new MediatorLiveData<>();
    private MediatorLiveData<List<UiMessage>> mUiMessageLiveData = new MediatorLiveData<>();
    private ConversationIdentifier mConversationIdentifier;
    private final StreamMessageHandler mStreamMessageHandler;
    private final ReadReceiptV5Handler mReadReceiptV5Handler;
    private final RongIMClient.ReadReceiptListener mReadReceiptListener =
            new RongIMClient.ReadReceiptListener() {
                @Override
                public void onReadReceiptReceived(Message message) {
                    if (!isSameConversationMessage(message)) {
                        return;
                    }
                    if (message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                        ReadReceiptMessage content = (ReadReceiptMessage) message.getContent();
                        long ntfTime = content.getLastMessageSendTime();
                        int count = 0;
                        for (int i = mUiMessages.size() - 1; i >= 0; i--) {
                            UiMessage uiMessage = mUiMessages.get(i);
                            if (Message.MessageDirection.SEND.equals(
                                            uiMessage.getMessage().getMessageDirection())
                                    && (Message.SentStatus.SENT.equals(
                                            uiMessage.getMessage().getSentStatus()))
                                    && ntfTime >= uiMessage.getMessage().getSentTime()) {
                                uiMessage.setSentStatus(Message.SentStatus.READ);
                                count++;
                            }
                        }
                        // 只要消息列表中，存在需要设置已读状态的消息，再刷新列表
                        if (count > 0) {
                            mUiMessageLiveData.setValue(mUiMessages);
                        }
                    }
                }

                @Override
                public void onMessageReceiptRequest(
                        Conversation.ConversationType conversationType,
                        String targetId,
                        String messageUId) {
                    if (!Objects.equals(getCurConversationType(), conversationType)
                            || !Objects.equals(getCurTargetId(), targetId)) {
                        return;
                    }
                    if (!(Conversation.ConversationType.GROUP.equals(getCurConversationType())
                            || Conversation.ConversationType.DISCUSSION.equals(
                                    getCurConversationType()))) {
                        return;
                    }
                    if (mProcessor != null) {
                        mProcessor.onMessageReceiptRequest(
                                MessageViewModel.this,
                                getCurConversationType(),
                                targetId,
                                messageUId);
                    }
                }

                @Override
                public void onMessageReceiptResponse(
                        Conversation.ConversationType conversationType,
                        String targetId,
                        String messageUid,
                        HashMap<String, Long> respondUserIdList) {
                    if (getCurConversationType() == null || TextUtils.isEmpty(getCurTargetId())) {
                        return;
                    }
                    if (!(Conversation.ConversationType.GROUP.equals(conversationType)
                            || Conversation.ConversationType.DISCUSSION.equals(conversationType)
                            || Conversation.ConversationType.PRIVATE.equals(conversationType))) {
                        return;
                    }
                    if (!conversationType.equals(getCurConversationType())
                            || !targetId.equals(getCurTargetId())) {
                        return;
                    }
                    for (final UiMessage item : mUiMessages) {
                        if (item.getMessage().getUId() != null
                                && item.getMessage().getUId().equals(messageUid)) {
                            ReadReceiptInfo readReceiptInfo =
                                    item.getMessage().getReadReceiptInfo();
                            if (readReceiptInfo == null) {
                                readReceiptInfo = new ReadReceiptInfo();
                                readReceiptInfo.setIsReadReceiptMessage(true);
                                item.setReadReceiptInfo(readReceiptInfo);
                            }
                            readReceiptInfo.setRespondUserIdList(respondUserIdList);
                            refreshSingleMessage(item);
                            break;
                        }
                    }
                }
            };
    // 远端数据是否全部拉取完成
    private boolean mRemoteMessageLoadFinish = false;
    private boolean mInitUnreadMessageFinish;
    private List<UiMessage> mNewUnReadMessages = new ArrayList<>();
    private List<Message> mNewUnReadMentionMessages = new CopyOnWriteArrayList<>();
    private MediatorLiveData<Integer> mHistoryMessageUnreadLiveData = new MediatorLiveData<>();
    private MediatorLiveData<Integer> mNewMessageUnreadLiveData = new MediatorLiveData<>();
    private MediatorLiveData<Integer> mNewMentionMessageUnreadLiveData = new MediatorLiveData<>();
    private boolean mInitMentionedMessageFinish;
    private MediatorLiveData<Boolean> mIsEditStatus = new MediatorLiveData<>();
    private MessageItemLongClickAction mMoreAction;
    // 应用是否在前台
    private boolean mIsForegroundActivity;
    // 是否滑动到页面最底部
    private boolean mScrollToBottom;
    private IConversationBusinessProcessor mProcessor;
    private Bundle mBundle;
    private Message mFirstUnreadMessage;
    private Handler mainHandler;
    private RongIMClient.OnReceiveMessageWrapperListener mOnReceiveMessageListener =
            new RongIMClient.OnReceiveMessageWrapperListener() {
                @Override
                public boolean onReceived(
                        Message message, int left, boolean hasPackage, boolean offline) {
                    if (!isSameConversationMessage(message)) {
                        return false;
                    }
                    ExecutorHelper.getInstance()
                            .mainThread()
                            .execute(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mProcessor == null) {
                                                return;
                                            }

                                            final MessageTag msgTag =
                                                    message.getContent()
                                                            .getClass()
                                                            .getAnnotation(MessageTag.class);
                                            if (!(msgTag.flag() == MessageTag.ISCOUNTED
                                                    || msgTag.flag() == MessageTag.ISPERSISTED)) {
                                                if (mProcessor.onReceivedCmd(
                                                        MessageViewModel.this, message)) {
                                                    return;
                                                }
                                            }
                                            UiMessage uiMessage = mapUIMessage(message);
                                            // 处理在线消息，高清语音下载、消息回执、多端同步
                                            if (left == 0 && !hasPackage) {
                                                // 高清语音下载
                                                if (message.getContent()
                                                        instanceof HQVoiceMessage) {
                                                    if (RongConfigCenter.conversationConfig()
                                                            .rc_enable_automatic_download_voice_msg) {
                                                        HQVoiceMsgDownloadManager.getInstance()
                                                                .enqueue(
                                                                        new AutoDownloadEntry(
                                                                                message,
                                                                                AutoDownloadEntry
                                                                                        .DownloadPriority
                                                                                        .HIGH));
                                                    } else {
                                                        RLog.e(
                                                                TAG,
                                                                "rc_enable_automatic_download_voice_msg disable");
                                                    }
                                                    uiMessage.setState(State.PROGRESS);
                                                }
                                            }
                                            // 已读状态设置
                                            if (message.getMessageId() > 0) {
                                                if (isForegroundActivity()) {
                                                    message.getReceivedStatus().setRead();
                                                    RongIMClient.getInstance()
                                                            .setMessageReceivedStatus(
                                                                    message.getMessageId(),
                                                                    message.getReceivedStatus(),
                                                                    null);

                                                    fetchReadReceiptInfoForMessages(
                                                            Collections.singletonList(message));
                                                }
                                            }
                                            mProcessor.onReceived(
                                                    MessageViewModel.this,
                                                    uiMessage,
                                                    left,
                                                    hasPackage,
                                                    offline);
                                        }
                                    });
                    return false;
                }
            };

    /** 消息被撤回时的回调。 */
    private RongIMClient.OnRecallMessageListener mRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (!isSameConversationMessage(message)) {
                        return false;
                    }
                    RLog.d(TAG, "onRecallMessage");
                    Conversation.ConversationType conversationType = message.getConversationType();
                    String targetId = message.getTargetId();
                    UiMessage uiMessage = findUIMessage(message.getMessageId());
                    removeRecallMentionMsg(message);
                    // 当uiMessage = null, 从mNewUnReadMessages中遍历
                    UiMessage newUnreadMessage = findNewUnreadMessage(message.getMessageId());
                    if (newUnreadMessage != null) {
                        mNewUnReadMessages.remove(newUnreadMessage);
                        processNewMessageUnread(true);
                    }

                    if (uiMessage != null) {
                        // 如果消息处于多选状态, 需要移除该消息
                        if (uiMessage.isSelected()) {
                            mSelectedUiMessage.remove(uiMessage);
                            if (mSelectedUiMessage.size() <= 0) {
                                mPageEventLiveData.postValue(
                                        new InputBarEvent(
                                                InputBarEvent.Type.InactiveMoreMenu, null));
                            }
                            uiMessage.setSelected(false);
                        }
                        MessageContent content = uiMessage.getMessage().getContent();
                        if (recallNotificationMessage == null) {
                            // 代表该消息被删除
                            removeUIMessage(uiMessage.getMessageId());
                            return false;
                        }
                        if (content instanceof VoiceMessage) {
                            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                            if (playingUri.equals(((VoiceMessage) content).getUri())) {
                                AudioPlayManager.getInstance().stopPlay();
                            }
                            stopDestructTime(uiMessage);
                        } else if (content instanceof HQVoiceMessage) {
                            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                            if (playingUri.equals(((HQVoiceMessage) content).getLocalPath())) {
                                AudioPlayManager.getInstance().stopPlay();
                            }
                            stopDestructTime(uiMessage);
                        } else if (content instanceof MediaMessageContent) {
                            RongIMClient.getInstance()
                                    .cancelDownloadMediaMessage(uiMessage.getMessage(), null);
                        }
                        uiMessage.setContent(recallNotificationMessage);
                        refreshSingleMessage(uiMessage);
                    }
                    return false;
                }
            };

    private RongIMClient.ConnectionStatusListener mConnectionStatusListener =
            new RongIMClient.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus connectionStatus) {
                    if (mProcessor != null) {
                        mProcessor.onConnectStatusChange(MessageViewModel.this, connectionStatus);
                    }
                }
            };
    private BaseConversationEventListener mConversationEventListener =
            new BaseConversationEventListener() {
                @Override
                public void onClearedMessage(Conversation.ConversationType type, String targetId) {
                    if (getCurConversationType() == null || TextUtils.isEmpty(getCurTargetId())) {
                        return;
                    }
                    if (type.equals(getCurConversationType())
                            && targetId.equals(getCurTargetId())) {
                        mUiMessages.clear();
                        mFirstUnreadMessage = null;
                        mRemoteMessageLoadFinish = false;
                        mProcessor.onClearMessage(MessageViewModel.this);
                        mUiMessageLiveData.setValue(mUiMessages);
                        mNewUnReadMentionMessages.clear();
                        updateNewMentionMessageUnreadBar();
                        hideHistoryBar();
                    }
                }
            };

    public MessageViewModel(@NonNull Application application) {
        super(application);
        mainHandler = new Handler(Looper.getMainLooper());
        mStreamMessageHandler = new StreamMessageHandler();
        mStreamMessageHandler.addDataChangeListener(
                StreamMessageHandler.KEY_FETCH_STREAM_MESSAGE,
                uiMessage -> {
                    if (uiMessage != null && uiMessage.getMessage() != null) {
                        UiMessage findUiMessage =
                                findUIMessage(uiMessage.getMessage().getMessageId());
                        if (findUiMessage != null) {
                            findUiMessage.setMessage(uiMessage.getMessage());
                            findUiMessage.setBusinessState(uiMessage.getBusinessState());
                            refreshSingleMessage(findUiMessage);
                        }
                    }
                });
        mReadReceiptV5Handler = new ReadReceiptV5Handler();
        mReadReceiptV5Handler.addDataChangeListener(
                ReadReceiptV5Handler.KEY_GET_MESSAGE_READ_RECEIPT_INFO_V5,
                readReceiptInfoList -> {
                    if (readReceiptInfoList != null && !readReceiptInfoList.isEmpty()) {
                        updateReadCountForMessages(readReceiptInfoList);
                    }
                });
        mReadReceiptV5Handler.addDataChangeListener(
                ReadReceiptV5Handler.KEY_MESSAGE_READ_RECEIPT_V5_LISTENER,
                responses -> {
                    if (responses != null && !responses.isEmpty()) {
                        handleReadReceiptV5Responses(responses);
                    }
                });
        IMCenter.getInstance().addAsyncOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().addConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().addReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().addOnRecallMessageListener(mRecallMessageListener);
        IMCenter.getInstance().addMessageEventListener(this);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
        RongUserInfoManager.getInstance().addUserDataObserver(this);
        initTranslationListener();
    }

    private void stopDestructTime(UiMessage uiMessage) {
        if (uiMessage.getContent().isDestruct()) {
            DestructManager.getInstance().stopDestruct(uiMessage.getMessage());
        }
    }

    public void bindConversation(ConversationIdentifier conversationIdentifier, Bundle bundle) {
        mConversationIdentifier = conversationIdentifier;
        mProcessor =
                ConversationProcessorFactory.getInstance().getProcessor(getCurConversationType());
        mBundle = bundle;
        mProcessor.init(this, bundle);
        mIsEditStatus.setValue(false);
    }

    /** 初始化加载本地消息 下拉加载历史消息 */
    public void onGetHistoryMessage(List<Message> messages, boolean isHasMoreMsg) {
        onGetHistoryMessage(messages);
        // 没有更多历史消息
        if (!isHasMoreMsg) {
            executePageEvent(new MessageEvent(false));
        }
    }

    /** 初始化加载本地消息 下拉加载历史消息 */
    public void onGetHistoryMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        mProcessor.onLoadMessage(this, messages);
        for (Message message : messages) {
            final MessageTag msgTag =
                    message.getContent().getClass().getAnnotation(MessageTag.class);
            if (!(msgTag.flag() == MessageTag.ISCOUNTED
                    || msgTag.flag() == MessageTag.ISPERSISTED)) {
                if (mProcessor.onReceivedCmd(MessageViewModel.this, message)) {
                    continue;
                }
            }
            // 去重处理
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getMessageId() == message.getMessageId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains && shouldContainUnknownMessage(message)) {
                mUiMessages.add(0, mapUIMessage(message));
            }
        }
        processHistoryDividerMessage();
        refreshAllMessage();

        // 批量获取历史消息的已读回执信息
        fetchReadReceiptInfoForMessages(messages);
    }

    public UiMessage mapUIMessage(Message message) {
        UiMessage uiMessage = new UiMessage(message);
        if (mIsEditStatus.getValue() != null) {
            uiMessage.setEdit(mIsEditStatus.getValue());
        }
        return uiMessage;
    }

    public void processHistoryDividerMessage() {
        // 存在历史消息
        if (getFirstUnreadMessage() == null) {
            return;
        }
        int position = findPositionByMessageId(getFirstUnreadMessage().getMessageId());
        // 找到第一条历史消息
        if (position >= 0) {
            // 插入历史未读数
            if (RongConfigCenter.conversationConfig().isShowHistoryDividerMessage()) {
                if (hasHistoryDividerMessage()) {
                    return;
                }
                Message hisMessage =
                        Message.obtain(
                                mConversationIdentifier,
                                HistoryDividerMessage.obtain(
                                        getApplication()
                                                .getString(
                                                        R.string.rc_new_message_divider_content)));
                hisMessage.setSenderUserId(RongIMClient.getInstance().getCurrentUserId());
                UiMessage uiMessage = new UiMessage(hisMessage);
                UiMessage firstUiMessage = mUiMessages.get(position);
                // 比第一条历史消息时间戳少1毫秒
                uiMessage.setSentTime(firstUiMessage.getMessage().getSentTime() - 1);
                mUiMessages.add(position, uiMessage);
            }
        }
    }

    private boolean hasHistoryDividerMessage() {
        for (UiMessage uiMessage : mUiMessages) {
            if (uiMessage.getContent() instanceof HistoryDividerMessage) {
                return true;
            }
        }
        return false;
    }

    public void refreshAllMessage() {
        refreshAllMessage(true);
    }

    public Message getFirstUnreadMessage() {
        return mFirstUnreadMessage;
    }

    public void setFirstUnreadMessage(Message firstUnreadMessage) {
        mFirstUnreadMessage = firstUnreadMessage;
    }

    public int findPositionByMessageId(int messageId) {
        int position = -1;
        for (int i = 0; i < mUiMessages.size(); i++) {
            UiMessage item = mUiMessages.get(i);
            if (item.getMessage().getMessageId() == messageId) {
                position = i;
                break;
            }
        }
        return position;
    }

    public String getCurTargetId() {
        if (mConversationIdentifier == null) {
            return null;
        }
        return mConversationIdentifier.getTargetId();
    }

    public Conversation.ConversationType getCurConversationType() {
        if (mConversationIdentifier == null) {
            return null;
        }
        return mConversationIdentifier.getType();
    }

    public ConversationIdentifier getConversationIdentifier() {
        return mConversationIdentifier;
    }

    public void refreshAllMessage(boolean force) {
        if (force) {
            for (UiMessage item : mUiMessages) {
                item.change();
            }
        }
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            mUiMessageLiveData.setValue(mUiMessages);
        } else {
            mUiMessageLiveData.postValue(mUiMessages);
        }
    }

    /** 上拉加载更多消息 */
    public void onLoadMoreMessage(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        mProcessor.onLoadMessage(this, messages);
        ArrayList<UiMessage> list = new ArrayList<>();
        for (Message message : messages) {
            // 去重处理
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getMessageId() == message.getMessageId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains && shouldContainUnknownMessage(message)) {
                list.add(0, mapUIMessage(message));
            }
        }
        mUiMessages.addAll(list);
        processHistoryDividerMessage();
        refreshAllMessage();

        // 批量获取更多消息的已读回执信息
        fetchReadReceiptInfoForMessages(messages);
    }

    /** normal状态点击历史消息bar history状态点击新消息bar */
    public void onReloadMessage(List<Message> messages) {
        mProcessor.onLoadMessage(this, messages);
        mUiMessages.clear();
        for (Message message : messages) {
            // 去重处理
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getMessageId() == message.getMessageId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains && shouldContainUnknownMessage(message)) {
                mUiMessages.add(0, mapUIMessage(message));
            }
        }
        processHistoryDividerMessage();
        refreshAllMessage();

        // 批量获取重新加载消息的已读回执信息
        fetchReadReceiptInfoForMessages(messages);
    }

    public MediatorLiveData<PageEvent> getPageEventLiveData() {
        return mPageEventLiveData;
    }

    public void onWarnClick(final UiMessage uiMessage) {
        final Message msg = uiMessage.getMessage();
        int position = findPositionByMessageId(msg.getMessageId());
        if (position >= 0) {
            mUiMessages.remove(position);
            mUiMessageLiveData.setValue(mUiMessages);
        }
        reSendMessage(msg);
    }

    public void onItemClick(UiMessage uiMessage) {
        Conversation.ConversationType type = uiMessage.getMessage().getConversationType();
        ConversationProcessorFactory.getInstance().getProcessor(type).onMessageItemClick(uiMessage);
    }

    public boolean onItemLongClick(final UiMessage uiMessage) {
        if (RongConfigCenter.conversationConfig().isShowMoreClickAction()) {
            if (mMoreAction == null) {
                mMoreAction =
                        new MessageItemLongClickAction.Builder()
                                .titleResId(R.string.rc_dialog_item_message_more)
                                .actionListener(
                                        new MessageItemLongClickAction
                                                .MessageItemLongClickListener() {
                                            @Override
                                            public boolean onMessageItemLongClick(
                                                    Context context, UiMessage message) {
                                                List<IClickActions> actions =
                                                        RongConfigCenter.conversationConfig()
                                                                .getMoreClickActions();
                                                if (actions != null && actions.size() > 0) {
                                                    if (!containsForwardClickAction(actions)
                                                            && RongConfigCenter.conversationConfig()
                                                                    .rc_enable_send_combine_message) {
                                                        RongConfigCenter.conversationConfig()
                                                                .addMoreClickAction(
                                                                        0,
                                                                        new ForwardClickActions());
                                                    }
                                                    for (IClickActions temp : actions) {
                                                        boolean filter = temp.filter(message);
                                                        if (temp instanceof ForwardClickActions) {
                                                            if (filter) {
                                                                actions.remove(temp);
                                                            }
                                                        }
                                                    }
                                                }
                                                enterEditState();
                                                onViewClick(MessageClickType.EDIT_CLICK, message);
                                                return true;
                                            }
                                        })
                                .showFilter(
                                        new MessageItemLongClickAction.Filter() {
                                            @Override
                                            public boolean filter(UiMessage message) {
                                                return !message.getConversationType()
                                                        .equals(
                                                                Conversation.ConversationType
                                                                        .SYSTEM);
                                            }
                                        })
                                .build();
                MessageItemLongClickActionManager.getInstance()
                        .addMessageItemLongClickAction(mMoreAction);
            }
        }

        final List<MessageItemLongClickAction> messageItemLongClickActions =
                MessageItemLongClickActionManager.getInstance()
                        .getMessageItemLongClickActions(uiMessage);
        executePageEvent(
                new ShowLongClickDialogEvent(
                        new MessageItemLongClickBean(messageItemLongClickActions, uiMessage)));
        return true;
    }

    private boolean containsForwardClickAction(List<IClickActions> iClickActions) {
        for (IClickActions iClickAction : iClickActions) {
            if (iClickAction instanceof ForwardClickActions) {
                return true;
            }
        }
        return false;
    }

    public void onUserPortraitClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId) {
        ConversationProcessorFactory.getInstance()
                .getProcessor(conversationType)
                .onUserPortraitClick(context, conversationType, userInfo, targetId);
    }

    public boolean onUserPortraitLongClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId) {
        return ConversationProcessorFactory.getInstance()
                .getProcessor(conversationType)
                .onUserPortraitLongClick(context, conversationType, userInfo, targetId);
    }

    public void reSendMessage(Message message) {
        if (message.getContent() instanceof LocationMessage) {
            IMCenter.getInstance().sendLocationMessage(message, null, null, null);
        } else if (message.getContent() instanceof ReferenceMessage) {
            IMCenter.getInstance()
                    .sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
        } else if (message.getContent() instanceof MediaMessageContent) {
            IMCenter.getInstance()
                    .sendMediaMessage(
                            message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
        } else {
            IMCenter.getInstance().sendMessage(message, null, null, null);
        }
    }

    public void onAudioClick(UiMessage uiMessage) {
        // 处理暂停逻辑
        MessageContent content = uiMessage.getMessage().getContent();
        if (content instanceof HQVoiceMessage) {
            if (AudioPlayManager.getInstance().isPlaying()) {
                Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                AudioPlayManager.getInstance().stopPlay();
                // 暂停的是当前播放的 Uri
                if (playingUri.equals(((HQVoiceMessage) content).getLocalPath())) return;
            }
            // 如果被 voip 占用通道，则不播放，弹提示框
            if (AudioPlayManager.getInstance().isInVOIPMode(getApplication())) {
                mPageEventLiveData.setValue(
                        new ToastEvent(getApplication().getString(R.string.rc_voip_occupying)));
                return;
            }
            playOrDownloadHQVoiceMsg(
                    (HQVoiceMessage) uiMessage.getMessage().getContent(), uiMessage);
        } else if (content instanceof VoiceMessage) {
            if (AudioPlayManager.getInstance().isPlaying()) {
                Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                AudioPlayManager.getInstance().stopPlay();
                // 暂停的是当前播放的 Uri
                if (playingUri.equals(((VoiceMessage) content).getUri())) return;
            }
            // 如果被 voip 占用通道，则不播放，弹提示框
            if (AudioPlayManager.getInstance().isInVOIPMode(getApplication())) {
                mPageEventLiveData.setValue(
                        new ToastEvent(getApplication().getString(R.string.rc_voip_occupying)));
                return;
            }
            playVoiceMessage(uiMessage);
        }
    }

    private void playOrDownloadHQVoiceMsg(HQVoiceMessage content, UiMessage uiMessage) {
        boolean ifDownloadHQVoiceMsg =
                (content.getLocalPath() == null
                        || TextUtils.isEmpty(content.getLocalPath().toString())
                        || !FileUtils.isFileExistsWithUri(
                                getApplication(), content.getLocalPath()));
        if (ifDownloadHQVoiceMsg) {
            downloadHQVoiceMsg(uiMessage);
        } else {
            playVoiceMessage(uiMessage);
        }
    }

    private void downloadHQVoiceMsg(final UiMessage uiMessage) {
        RongIMClient.getInstance()
                .downloadMediaMessage(
                        uiMessage.getMessage(),
                        new IRongCallback.IDownloadMediaMessageCallback() {
                            @Override
                            public void onSuccess(Message message) {
                                uiMessage.setState(State.NORMAL);
                                refreshSingleMessage(uiMessage);
                                playVoiceMessage(uiMessage);
                            }

                            @Override
                            public void onProgress(Message message, int progress) {
                                uiMessage.setState(State.PROGRESS);
                                uiMessage.setProgress(progress);
                                refreshSingleMessage(uiMessage);
                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode code) {
                                uiMessage.setState(State.ERROR);
                                refreshSingleMessage(uiMessage);
                            }

                            @Override
                            public void onCanceled(Message message) {
                                uiMessage.setState(State.CANCEL);
                                refreshSingleMessage(uiMessage);
                            }
                        });
    }

    private void playVoiceMessage(final UiMessage uiMessage) {
        final MessageContent content = uiMessage.getMessage().getContent();
        Uri voicePath = null;
        if (content instanceof HQVoiceMessage) {
            voicePath = ((HQVoiceMessage) content).getLocalPath();
        } else if (content instanceof VoiceMessage) {
            voicePath = ((VoiceMessage) content).getUri();
        }
        if (voicePath != null) {
            AudioPlayManager.getInstance()
                    .startPlay(
                            getApplication(),
                            voicePath,
                            new IAudioPlayListener() {
                                @Override
                                public void onStart(Uri uri) {
                                    uiMessage.setPlaying(true);
                                    Message message = uiMessage.getMessage();
                                    message.getReceivedStatus().setListened();
                                    RongIM.getInstance()
                                            .setMessageReceivedStatus(
                                                    message.getMessageId(),
                                                    getCurConversationType(),
                                                    getCurTargetId(),
                                                    message.getReceivedStatus(),
                                                    null);
                                    if (message.getContent().isDestruct()
                                            && message.getMessageDirection()
                                                    .equals(Message.MessageDirection.RECEIVE)) {
                                        uiMessage.setReadTime(0);
                                        DestructManager.getInstance()
                                                .stopDestruct(uiMessage.getMessage());
                                    }
                                    refreshSingleMessage(uiMessage);
                                }

                                @Override
                                public void onStop(Uri uri) {
                                    uiMessage.setPlaying(false);
                                    if (content.isDestruct()
                                            && uiMessage
                                                    .getMessage()
                                                    .getMessageDirection()
                                                    .equals(Message.MessageDirection.RECEIVE)) {
                                        DestructManager.getInstance()
                                                .startDestruct(uiMessage.getMessage());
                                    }
                                    refreshSingleMessage(uiMessage);
                                }

                                @Override
                                public void onComplete(Uri uri) {
                                    uiMessage.setPlaying(false);
                                    // 找到下个播放消息继续播放
                                    if (content.isDestruct()
                                            && uiMessage
                                                    .getMessage()
                                                    .getMessageDirection()
                                                    .equals(Message.MessageDirection.RECEIVE)) {
                                        DestructManager.getInstance()
                                                .startDestruct(uiMessage.getMessage());
                                        refreshSingleMessage(uiMessage);
                                    } else {
                                        refreshSingleMessage(uiMessage);
                                        // 不切换线程会造成，ui 一直显示播放的 bug
                                        ExecutorHelper.getInstance()
                                                .mainThread()
                                                .execute(
                                                        new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                findNextHQVoice(uiMessage);
                                                            }
                                                        });
                                    }
                                }
                            });
        }
    }

    private void findNextHQVoice(UiMessage uiMessage) {
        if (!RongConfigCenter.conversationConfig().rc_play_audio_continuous) {
            RLog.e(TAG, "rc_play_audio_continuous is disabled.");
            return;
        }
        int position = findPositionByMessageId(uiMessage.getMessage().getMessageId());
        if (position == -1) {
            RLog.w(TAG, "the message isn't found in the list.");
            return;
        }
        for (int i = position; i < mUiMessages.size(); i++) {
            UiMessage item = mUiMessages.get(i);
            if (item.getMessage().getContent() instanceof HQVoiceMessage) {
                if (!item.getMessage().getReceivedStatus().isListened()
                        && !item.getMessage().getContent().isDestruct()
                        && !TextUtils.equals(
                                item.getMessage().getSenderUserId(),
                                RongIM.getInstance().getCurrentUserId())) {
                    onAudioClick(item);
                    break;
                }
            } else if (item.getMessage().getContent() instanceof VoiceMessage) {
                if (!item.getMessage().getReceivedStatus().isListened()
                        && !item.getMessage().getContent().isDestruct()
                        && !TextUtils.equals(
                                item.getMessage().getSenderUserId(),
                                RongIM.getInstance().getCurrentUserId())) {
                    onAudioClick(item);
                    break;
                }
            }
        }
    }

    public boolean onBackPressed() {
        if (Objects.equals(mIsEditStatus.getValue(), true)) {
            quitEditMode();
            return true;
        }
        return mProcessor.onBackPressed(this);
    }

    /** 退出编辑模式 */
    public void quitEditMode() {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mIsEditStatus.setValue(false);
        } else {
            mIsEditStatus.postValue(false);
        }
        List<UiMessage> uiMessageList = mUiMessages;
        for (UiMessage uiMessage : uiMessageList) {
            uiMessage.setEdit(false);
            uiMessage.setSelected(false);
        }
        mSelectedUiMessage.clear();
        mUiMessageLiveData.setValue(mUiMessages);
        // 通知输入框刷新
        mPageEventLiveData.setValue(
                new InputBarEvent(
                        InputBarEvent.Type.HideMoreMenu, getCurConversationType().getName()));
    }

    public UiMessage findUIMessage(String messageUId) {
        UiMessage uiMessage = null;
        for (UiMessage item : mUiMessages) {
            if (messageUId.equals(item.getMessage().getUId())) {
                uiMessage = item;
                break;
            }
        }

        return uiMessage;
    }

    @Override
    public void onSendMessage(SendEvent event) {
        Message msg = event.getMessage();
        if (isSameConversationMessage(msg) && msg.getMessageId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getMessageId());
            boolean isAdd = uiMessage == null;
            if (isAdd) {
                uiMessage = mapUIMessage(msg);
            } else {
                uiMessage.setMessage(msg);
            }
            long sentTime;
            switch (event.getEvent()) {
                case SendEvent.ATTACH:
                    sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(sentTime); // 更新成服务器时间
                    uiMessage.setState(State.PROGRESS);
                    break;
                case SendEvent.ERROR:
                    sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(sentTime); // 更新成服务器时间
                    uiMessage.setState(State.ERROR);
                    break;
                case SendEvent.SUCCESS:
                    uiMessage.setState(State.NORMAL);
                    break;
            }
            if (isAdd) {
                sendMessageEvent(uiMessage);
            } else {
                refreshSingleMessage(uiMessage);
            }
        }
    }

    @Override
    public void onSendMediaMessage(SendMediaEvent event) {
        Message msg = event.getMessage();
        if (isSameConversationMessage(msg) && msg.getMessageId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getMessageId());
            boolean isAdd = uiMessage == null;
            if (isAdd) {
                uiMessage = mapUIMessage(msg);
            } else {
                uiMessage.setMessage(msg);
            }
            long sentTime;
            switch (event.getEvent()) {
                case SendMediaEvent.ATTACH:
                    sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(sentTime); // 更新成服务器时间
                    break;
                case SendMediaEvent.PROGRESS:
                    uiMessage.setState(State.PROGRESS);
                    uiMessage.setProgress(event.getProgress());
                    break;
                case SendMediaEvent.ERROR:
                    if (event.getCode() != null) {
                        int code = event.getCode().code;
                        if (code == IRongCoreEnum.CoreErrorCode.RC_MEDIA_EXCEPTION.getValue()) {
                            ToastUtils.s(
                                    getApplication(),
                                    getApplication().getString(R.string.rc_media_upload_error));
                        } else if (code
                                == IRongCoreEnum.CoreErrorCode.RC_GIF_MSG_SIZE_LIMIT_EXCEED
                                        .getValue()) {
                            ToastUtils.s(
                                    getApplication(),
                                    getApplication().getString(R.string.rc_gif_message_too_large));
                        } else if (code
                                == IRongCoreEnum.CoreErrorCode.RC_FILE_SIZE_EXCEED_LIMIT
                                        .getValue()) {
                            ToastUtils.s(
                                    getApplication(),
                                    getApplication().getString(R.string.rc_upload_file_too_large));
                        }
                    }
                    sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(sentTime); // 更新成服务器时间
                    uiMessage.setState(State.ERROR);
                    break;
                case SendMediaEvent.SUCCESS:
                    uiMessage.setProgress(100);
                    uiMessage.setState(State.NORMAL);
                    break;
                case SendMediaEvent.CANCEL:
                    uiMessage.setState(State.CANCEL);
                    break;
            }
            uiMessage.setMessage(msg);

            if (isAdd) {
                sendMessageEvent(uiMessage);
            } else {
                refreshSingleMessage(uiMessage);
            }
        }
    }

    @Override
    public void onDownloadMessage(DownloadEvent event) {
        Message msg = event.getMessage();
        if (isSameConversationMessage(msg) && msg.getMessageId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getMessageId());
            if (uiMessage != null) {
                switch (event.getEvent()) {
                    case DownloadEvent.SUCCESS:
                        uiMessage.setProgress(100);
                        uiMessage.setState(State.NORMAL);
                        break;
                    case DownloadEvent.PROGRESS:
                        uiMessage.setState(State.PROGRESS);
                        uiMessage.setProgress(event.getProgress());
                        break;
                    case DownloadEvent.ERROR:
                        uiMessage.setProgress(0);
                        uiMessage.setState(State.ERROR);
                        break;
                    case DownloadEvent.CANCEL:
                        uiMessage.setState(State.CANCEL);
                        break;
                    case DownloadEvent.PAUSE:
                        uiMessage.setState(State.PAUSE);
                        break;
                }
                uiMessage.setMessage(msg);
                refreshSingleMessage(uiMessage);
            }
        }
    }

    @Override
    public void onDeleteMessage(DeleteEvent event) {
        for (int messageId : event.getMessageIds()) {
            int position = findPositionByMessageId(messageId);
            if (position >= 0) {
                UiMessage uiMessage = mUiMessages.get(position);
                MessageContent content = uiMessage.getMessage().getContent();
                if (AudioPlayManager.getInstance().isPlaying()) {
                    if (content instanceof VoiceMessage) {
                        Uri voiceUri = ((VoiceMessage) content).getUri();
                        if (voiceUri != null
                                && voiceUri.equals(
                                        AudioPlayManager.getInstance().getPlayingUri())) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                    } else if (content instanceof HQVoiceMessage) {
                        Uri hqVoiceUri = ((HQVoiceMessage) content).getLocalPath();
                        if (hqVoiceUri != null
                                && hqVoiceUri.equals(
                                        AudioPlayManager.getInstance().getPlayingUri())) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                    }
                }
                if (content instanceof MediaMessageContent) {
                    IMCenter.getInstance().cancelDownloadMediaMessage(uiMessage.getMessage(), null);
                }
                mUiMessages.remove(position);
            }
        }
        mUiMessageLiveData.setValue(mUiMessages);

        // 当会话页面删除消息后列表消息为空时，重新刷新列表
        if (RongConfigCenter.conversationConfig().isNeedRefreshWhenListIsEmptyAfterDelete()
                && mUiMessages.isEmpty()
                && mProcessor != null) {
            onRefresh();
        }
    }

    @Override
    public void onRecallEvent(RecallEvent event) {
        UiMessage uiMessage = findUIMessage(event.getMessageId());
        if (uiMessage != null) {
            MessageContent content = uiMessage.getMessage().getContent();
            if (AudioPlayManager.getInstance().isPlaying()) {
                if (content instanceof VoiceMessage) {
                    if (((VoiceMessage) content)
                            .getUri()
                            .equals(AudioPlayManager.getInstance().getPlayingUri())) {
                        AudioPlayManager.getInstance().stopPlay();
                    }
                } else if (content instanceof HQVoiceMessage) {
                    if (((HQVoiceMessage) content)
                            .getLocalPath()
                            .equals(AudioPlayManager.getInstance().getPlayingUri())) {
                        AudioPlayManager.getInstance().stopPlay();
                    }
                }
            }
            if (content instanceof MediaMessageContent) {
                IMCenter.getInstance().cancelDownloadMediaMessage(uiMessage.getMessage(), null);
            }
            uiMessage.setContent(event.getRecallNotificationMessage());
            refreshSingleMessage(uiMessage);
            UiMessage newUnreadMessage =
                    findNewUnreadMessage(uiMessage.getMessage().getMessageId());
            if (newUnreadMessage != null) {
                mNewUnReadMessages.remove(newUnreadMessage);
                processNewMessageUnread(true);
            }
        }
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        Message message = event.getMessage();
        UiMessage uiMessage = findUIMessage(message.getMessageId());
        if (uiMessage != null) {
            uiMessage.setMessage(message);
            refreshSingleMessage(uiMessage);
        }
    }

    @Override
    public void onInsertMessage(InsertEvent event) {
        Message msg = event.getMessage();
        if (mConversationIdentifier != null
                && mConversationIdentifier.equalsWithMessage(msg)
                && msg.getMessageId() > 0
                && shouldContainUnknownMessage(msg)) {
            long sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
            msg.setSentTime(sentTime); // 更新成服务器时间
            int position = findPositionBySendTime(msg.getSentTime());
            mUiMessages.add(position, mapUIMessage(msg));
            refreshAllMessage();
            executePageEvent(new ScrollEvent(position));
        }
    }

    @Override
    public void onClearMessages(ClearEvent event) {
        RLog.d(TAG, "onClearMessages");
        if (event.getTargetId().equals(getCurTargetId())
                && event.getConversationType().equals(getCurConversationType())) {
            mUiMessages.clear();
            mUiMessageLiveData.setValue(mUiMessages);
            mNewUnReadMentionMessages.clear();
            updateNewMentionMessageUnreadBar();
            hideHistoryBar();
        }
    }

    public UiMessage findUIMessage(int messageId) {
        UiMessage uiMessage = null;
        for (UiMessage item : mUiMessages) {
            if (item.getMessage().getMessageId() == messageId) {
                uiMessage = item;
                break;
            }
        }
        return uiMessage;
    }

    public void removeUIMessage(int messageId) {
        UiMessage uiMessage = findUIMessage(messageId);
        if (uiMessage != null) {
            mUiMessages.remove(uiMessage);
            refreshAllMessage();
        }
    }

    private void sendMessageEvent(UiMessage uiMessage) {
        if (!shouldContainUnknownMessage(uiMessage.getMessage())) {
            return;
        }
        mUiMessages.add(uiMessage);
        refreshAllMessage();
        executePageEvent(new ScrollToEndEvent());

        // 获取新添加消息的已读回执信息
        fetchReadReceiptInfoForSingleMessage(uiMessage);
    }

    public void refreshSingleMessage(UiMessage uiMessage) {
        int position = findPositionByMessageId(uiMessage.getMessage().getMessageId());
        if (position != -1) {
            uiMessage.setChange(true);
            mUiMessageLiveData.postValue(mUiMessages);
        }
    }

    public int findPositionBySendTime(long sentTime) {
        for (int i = mUiMessages.size() - 1; i >= 0; i--) {
            UiMessage message = mUiMessages.get(i);
            if (message.getSentTime() <= sentTime) {
                return i + 1;
            }
        }
        return 0;
    }

    public void executePageEvent(PageEvent pageEvent) {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            mPageEventLiveData.setValue(pageEvent);
        } else {
            mPageEventLiveData.postValue(pageEvent);
        }
    }

    private UiMessage findNewUnreadMessage(int messageId) {
        UiMessage result = null;
        for (UiMessage item : mNewUnReadMessages) {
            if (item.getMessageId() == messageId) {
                result = item;
                break;
            }
        }
        return result;
    }

    public void processNewMessageUnread(boolean isMainThread) {
        if (RongConfigCenter.conversationConfig().isShowNewMessageBar(getCurConversationType())) {
            // 之前逻辑是主线程setValue、非主线程postValue，有时序问题。统一切到主线程执行避免时序问题
            mainHandler.post(() -> mNewMessageUnreadLiveData.setValue(mNewUnReadMessages.size()));
        }
    }

    public void updateNewMentionMessageUnreadBar() {
        if (RongConfigCenter.conversationConfig()
                .isShowNewMentionMessageBar(getCurConversationType())) {
            if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
                mNewMentionMessageUnreadLiveData.setValue(mNewUnReadMentionMessages.size());
            } else {
                mNewMentionMessageUnreadLiveData.postValue(mNewUnReadMentionMessages.size());
            }
        }
    }

    public void hideHistoryBar() {
        setFirstUnreadMessage(null);
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            mHistoryMessageUnreadLiveData.setValue(0);
        } else {
            mHistoryMessageUnreadLiveData.postValue(0);
        }
    }

    public int getRefreshMessageId() {
        int result = -1;
        if (mUiMessages.size() > 0) {
            for (UiMessage item : mUiMessages) {
                if (!(item.getMessageId() == 0 || item.getMessageId() == -1)) {
                    result = item.getMessageId();
                    break;
                }
            }
        }
        return result;
    }

    public long getRefreshSentTime() {
        long result = 0;
        if (mUiMessages.size() > 0) {
            for (UiMessage item : mUiMessages) {
                if (item.getSentTime() > 0) {
                    result = item.getSentTime();
                    break;
                }
            }
        }
        return result;
    }

    public long getLoadMoreSentTime() {
        long result = 0;
        if (mUiMessages.size() > 0) {
            for (int i = mUiMessages.size() - 1; i >= 0; i--) {
                if (Message.SentStatus.SENT.equals(mUiMessages.get(i).getSentStatus())) {
                    result = mUiMessages.get(i).getSentTime();
                } else {
                    result =
                            mUiMessages.get(i).getSentTime()
                                    - RongIMClient.getInstance().getDeltaTime();
                }
                if (result > 0) {
                    break;
                }
            }
        }
        return result;
    }

    public boolean isRemoteMessageLoadFinish() {
        return mRemoteMessageLoadFinish;
    }

    public void setRemoteMessageLoadFinish(boolean remoteMessageLoadFinish) {
        this.mRemoteMessageLoadFinish = remoteMessageLoadFinish;
    }

    public boolean isScrollToBottom() {
        return mScrollToBottom;
    }

    public void setScrollToBottom(boolean scrollToBottom) {
        mScrollToBottom = scrollToBottom;
    }

    public void updateMentionMessage(io.rong.imlib.model.Message message) {
        if (RongConfigCenter.conversationConfig()
                        .isShowNewMentionMessageBar(message.getConversationType())
                && message != null
                && message.getContent() != null
                && message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            MentionedInfo.MentionedType type = mentionedInfo.getType();
            if (type == MentionedInfo.MentionedType.ALL
                    && message.getSenderUserId() != null
                    && !message.getSenderUserId()
                            .equals(RongIMClient.getInstance().getCurrentUserId())) {
                mNewUnReadMentionMessages.add(message);
            } else if (type == MentionedInfo.MentionedType.PART
                    && mentionedInfo.getMentionedUserIdList() != null
                    && mentionedInfo
                            .getMentionedUserIdList()
                            .contains(RongIMClient.getInstance().getCurrentUserId())) {
                mNewUnReadMentionMessages.add(message);
            }
            updateNewMentionMessageUnreadBar();
        }
    }

    /**
     * 异步线程切换到主线程执行
     *
     * @param pageEvent 执行的 event
     */
    public void executePostPageEvent(PageEvent pageEvent) {
        mPageEventLiveData.postValue(pageEvent);
    }

    public void onReadReceiptRequestClick(final UiMessage uiMessage) {
        final Message message = uiMessage.getMessage();
        RongIMClient.getInstance()
                .sendReadReceiptRequest(
                        message,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                ReadReceiptInfo readReceiptInfo = message.getReadReceiptInfo();
                                if (readReceiptInfo == null) {
                                    readReceiptInfo = new ReadReceiptInfo();
                                    message.setReadReceiptInfo(readReceiptInfo);
                                }
                                readReceiptInfo.setIsReadReceiptMessage(true);
                                refreshSingleMessage(uiMessage);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                RLog.e(
                                        TAG,
                                        "sendReadReceiptRequest failed, errorCode = " + errorCode);
                                if ((errorCode != null
                                                && errorCode.getValue()
                                                        == IRongCoreEnum.CoreErrorCode
                                                                .RC_NET_CHANNEL_INVALID
                                                                .getValue())
                                        || (errorCode != null
                                                && errorCode.getValue()
                                                        == IRongCoreEnum.CoreErrorCode
                                                                .RC_NET_UNAVAILABLE
                                                                .getValue())) {
                                    executePageEvent(
                                            new ToastEvent(
                                                    getApplication()
                                                            .getString(
                                                                    R.string
                                                                            .rc_notice_network_unavailable)));
                                }
                            }
                        });
    }

    public void onReadReceiptStateClick(UiMessage uiMessage) {
        // do nothing
    }

    public void onReEditClick(UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (isSameConversationMessage(message)) {
            MessageContent messageContent = message.getContent();
            if (messageContent instanceof RecallNotificationMessage) {
                String content = ((RecallNotificationMessage) messageContent).getRecallContent();
                if (!TextUtils.isEmpty(content)) {
                    executePageEvent(new InputBarEvent(InputBarEvent.Type.ReEdit, content));
                }
            }
        }
    }

    public void onRefresh() {
        mProcessor.onRefresh(MessageViewModel.this);
    }

    public void cleanUnreadNewCount() {
        mNewUnReadMessages.clear();
    }

    public void addUnreadNewMessage(UiMessage message) {
        if (Message.MessageDirection.SEND.equals(message.getMessageDirection())) {
            return;
        }
        UiMessage newUnreadMessage = findNewUnreadMessage(message.getMessageId());
        if (newUnreadMessage == null) {
            mNewUnReadMessages.add(message);
        }
    }

    public void newMessageBarClick() {
        mProcessor.newMessageBarClick(MessageViewModel.this);
    }

    public void unreadBarClick() {
        mProcessor.unreadBarClick(MessageViewModel.this);
    }

    public void newMentionMessageBarClick() {
        mProcessor.newMentionMessageBarClick(MessageViewModel.this);
    }

    /** 启动编辑模式 */
    public void enterEditState() {
        if (Thread.currentThread().equals(Looper.getMainLooper().getThread())) {
            mIsEditStatus.setValue(true);
        } else {
            mIsEditStatus.postValue(true);
        }
        List<UiMessage> uiMessageList = mUiMessages;
        for (UiMessage uiMessage : uiMessageList) {
            uiMessage.setEdit(true);
            uiMessage.setSelected(false);
        }
        mSelectedUiMessage.clear();
        mUiMessageLiveData.setValue(mUiMessages);
        // 通知输入框刷新
        mPageEventLiveData.setValue(new InputBarEvent(InputBarEvent.Type.ShowMoreMenu, ""));
    }

    public void forwardMessage(Intent data) {
        if (data == null) return;
        List<Message> messageList = new ArrayList<>();
        for (UiMessage uiMessage : getSelectedUiMessages()) {
            messageList.add(uiMessage.getMessage());
        }
        ForwardManager.getInstance()
                .forwardMessages(
                        data.getIntExtra(RouteUtils.FORWARD_TYPE, 0),
                        data.<Conversation>getParcelableArrayListExtra("conversations"),
                        data.getIntegerArrayListExtra(RouteUtils.MESSAGE_IDS),
                        messageList);
        quitEditMode();
    }

    public List<UiMessage> getSelectedUiMessages() {
        return mSelectedUiMessage;
    }

    public void onLoadMore() {
        mProcessor.onLoadMore(this);
    }

    private void removeRecallMentionMsg(Message message) {
        // 遍历 @消息未读列表，如果存在撤回消息则移除，刷新 @ Bar
        boolean needRefresh = false;

        int size = mNewUnReadMentionMessages.size();
        for (int i = size - 1; i >= 0; i--) {
            if (mNewUnReadMentionMessages.get(i).getMessageId() == message.getMessageId()) {
                mNewUnReadMentionMessages.remove(mNewUnReadMentionMessages.get(i));
                needRefresh = true;
                break;
            }
        }
        if (needRefresh) {
            updateNewMentionMessageUnreadBar();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mStreamMessageHandler.stop();
        mReadReceiptV5Handler.stop();
        IMCenter.getInstance().removeAsyncOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().removeConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().removeReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
        IMCenter.getInstance().removeMessageEventListener(this);
        IMCenter.getInstance().removeConversationEventListener(mConversationEventListener);
        RongUserInfoManager.getInstance().removeUserDataObserver(this);
        unInitTranslationListener();
    }

    public boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }

    public void onViewClick(int clickType, UiMessage data) {
        IMessageViewModelProcessor viewModelProcessor =
                RongConfigCenter.conversationConfig().getViewModelProcessor();
        boolean isProcess = false;
        if (viewModelProcessor != null) {
            isProcess = viewModelProcessor.onViewClick(this, clickType, data);
        }
        if (!isProcess) {
            switch (clickType) {
                case MessageClickType.AUDIO_CLICK:
                    onAudioClick(data);
                    break;
                case MessageClickType.WARNING_CLICK:
                    onWarnClick(data);
                    break;
                case MessageClickType.REEDIT_CLICK:
                    onReEditClick(data);
                    break;
                case MessageClickType.READ_RECEIPT_REQUEST_CLICK:
                    onReadReceiptRequestClick(data);
                    break;
                case MessageClickType.READ_RECEIPT_STATE_CLICK:
                    onReadReceiptStateClick(data);
                    break;
                case MessageClickType.CONTENT_CLICK:
                    onItemClick(data);
                    break;
                case MessageClickType.USER_PORTRAIT_CLICK:
                    onUserPortraitClick(
                            getApplication(),
                            data.getMessage().getConversationType(),
                            data.getUserInfo(),
                            data.getMessage().getTargetId());
                    break;
                case MessageClickType.EDIT_CLICK:
                    boolean selected = data.isSelected();
                    int preSize = mSelectedUiMessage.size();
                    if (selected) {
                        mSelectedUiMessage.remove(data);
                        if (mSelectedUiMessage.size() <= 0) {
                            mPageEventLiveData.postValue(
                                    new InputBarEvent(InputBarEvent.Type.InactiveMoreMenu, null));
                        }
                        data.setSelected(false);
                        refreshSingleMessage(data);
                    } else {
                        if (mSelectedUiMessage.size()
                                < RongConfigCenter.conversationConfig()
                                        .rc_max_message_selected_count) {
                            mSelectedUiMessage.add(data);
                            if (mSelectedUiMessage.size() > 0 && preSize <= 0) {
                                mPageEventLiveData.setValue(
                                        new InputBarEvent(InputBarEvent.Type.ActiveMoreMenu, null));
                            }
                            data.setSelected(true);
                            refreshSingleMessage(data);
                        } else {
                            if (RongConfigCenter.conversationConfig().rc_max_message_selected_count
                                    == 100) {
                                executePageEvent(
                                        new ToastEvent(
                                                getApplication()
                                                        .getString(
                                                                R.string
                                                                        .rc_exceeded_max_limit_100)));
                            }
                        }
                    }
                    break;
                case MessageClickType.STREAM_MSG_PULL:
                    mStreamMessageHandler.fetchStreamMessage(
                            data.getUId(),
                            Objects.equals(
                                    data.getBusinessState(),
                                    StreamMessageHandler.State.RETRY_PULL));
                    break;
                default:
                    break;
            }
        }
    }

    public boolean onViewLongClick(int clickType, UiMessage data) {
        IMessageViewModelProcessor viewModelProcessor =
                RongConfigCenter.conversationConfig().getViewModelProcessor();
        boolean isProcess = false;
        if (viewModelProcessor != null) {
            isProcess = viewModelProcessor.onViewLongClick(this, clickType, data);
        }
        if (!isProcess) {
            switch (clickType) {
                case MessageClickType.CONTENT_LONG_CLICK:
                    return onItemLongClick(data);
                case MessageClickType.USER_PORTRAIT_LONG_CLICK:
                    return onUserPortraitLongClick(
                            getApplication(),
                            data.getMessage().getConversationType(),
                            data.getUserInfo(),
                            data.getMessage().getTargetId());
                default:
                    return false;
            }
        } else {
            return true;
        }
    }

    public LiveData<List<UiMessage>> getUiMessageLiveData() {
        return mUiMessageLiveData;
    }

    public MediatorLiveData<Integer> getNewMessageUnreadLiveData() {
        return mNewMessageUnreadLiveData;
    }

    public MediatorLiveData<Boolean> IsEditStatusLiveData() {
        return mIsEditStatus;
    }

    public List<UiMessage> getNewUnReadMessages() {
        return mNewUnReadMessages;
    }

    public List<Message> getNewUnReadMentionMessages() {
        return mNewUnReadMentionMessages;
    }

    public void setNewUnReadMentionMessages(List<Message> newUnReadMentionMessages) {
        this.mNewUnReadMentionMessages = newUnReadMentionMessages;
    }

    public void showHistoryBar(int unreadMessageCount) {
        mHistoryMessageUnreadLiveData.setValue(unreadMessageCount);
    }

    public LiveData<Integer> getHistoryMessageUnreadLiveData() {
        return mHistoryMessageUnreadLiveData;
    }

    public void showNewMentionMessageBar(int unreadMessageCount) {
        mNewMentionMessageUnreadLiveData.setValue(unreadMessageCount);
    }

    public void hideNewMentionMessageBar() {
        if (mNewUnReadMentionMessages != null) {
            mNewUnReadMentionMessages.clear();
        }
        mNewMentionMessageUnreadLiveData.setValue(0);
    }

    public LiveData<Integer> getNewMentionMessageUnreadLiveData() {
        return mNewMentionMessageUnreadLiveData;
    }

    public void onScrolled(
            RecyclerView recyclerView, int dx, int dy, int headerCount, int footerCount) {
        // 判断是否滑动到底部
        if (!recyclerView.canScrollVertically(1)) {
            setScrollToBottom(true);
            mProcessor.onScrollToBottom(this);
        } else {
            setScrollToBottom(false);
        }
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (RongConfigCenter.conversationConfig().isShowHistoryMessageBar(getCurConversationType())
                && getFirstUnreadMessage() != null) {
            int firstPosition = findPositionByMessageId(getFirstUnreadMessage().getMessageId());
            if (layoutManager instanceof LinearLayoutManager) {
                int firstVisibleItemPosition =
                        ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                if (firstVisibleItemPosition <= firstPosition) {
                    hideHistoryBar();
                }
            }
        }

        if (RongConfigCenter.conversationConfig().isShowHistoryMessageBar(getCurConversationType())
                && mNewUnReadMentionMessages != null
                && mNewUnReadMentionMessages.size() > 0
                && getUiMessages().size() > 0) {
            int firstVisibleItemPosition = 0;
            int lastPosition = 0;
            if (layoutManager instanceof LinearLayoutManager) {
                firstVisibleItemPosition =
                        ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition()
                                - headerCount;
                lastPosition =
                        ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition()
                                - headerCount;
            }
            // 计算第一个和最后一个可见的消息的位置，需要确保在List的范围内
            int msgSize = getUiMessages().size();
            int firstMessagePosition = Math.min(msgSize - 1, Math.max(firstVisibleItemPosition, 0));
            int lastMessagePosition =
                    lastPosition < msgSize && lastPosition >= 0 ? lastPosition : msgSize - 1;
            UiMessage firstMessage = getUiMessages().get(0);
            if (firstMessagePosition >= 0 && firstMessagePosition < msgSize) {
                firstMessage = getUiMessages().get(firstMessagePosition);
            }
            UiMessage lastMessage = getUiMessages().get(msgSize - 1);
            if (lastMessagePosition >= 0 && lastMessagePosition < msgSize) {
                lastMessage = getUiMessages().get(lastMessagePosition);
            }
            long topTime = firstMessage.getSentTime();
            long bottomTime = lastMessage.getSentTime();
            int size = mNewUnReadMentionMessages.size();
            for (int i = size - 1; i >= 0; i--) {
                if (i < mNewUnReadMentionMessages.size()) {
                    Message newUnReadMentionMessage = mNewUnReadMentionMessages.get(i);
                    if (newUnReadMentionMessage.getSentTime() >= topTime
                            && newUnReadMentionMessage.getSentTime() <= bottomTime) {
                        mNewUnReadMentionMessages.remove(newUnReadMentionMessage);
                    }
                }
            }
        }
        updateNewMentionMessageUnreadBar();
    }

    public List<UiMessage> getUiMessages() {
        return mUiMessages;
    }

    public void onExistUnreadMessage(Conversation conversation, int unreadMessageCount) {
        if (mProcessor != null)
            mProcessor.onExistUnreadMessage(this, conversation, unreadMessageCount);
    }

    public void onResume() {
        mIsForegroundActivity = true;
        if (mProcessor != null) mProcessor.onResume(this);
        cleanUnreadStatus();
        fetchReadReceiptInfoForUiMessages(getUiMessages());
        if (RongConfigCenter.featureConfig().rc_wipe_out_notification_message) {
            clearAllNotification();
        }
    }

    /** 清理未读状态 */
    public void cleanUnreadStatus() {
        if (isInitUnreadMessageFinish() && isInitMentionedMessageFinish()) {
            IMCenter.getInstance().clearMessagesUnreadStatus(mConversationIdentifier, null);
        }
    }

    private void clearAllNotification() {
        RongNotificationManager.getInstance().clearAllNotification();
    }

    public boolean isInitUnreadMessageFinish() {
        return mInitUnreadMessageFinish;
    }

    public void setInitUnreadMessageFinish(boolean initUnreadMessageFinish) {
        mInitUnreadMessageFinish = initUnreadMessageFinish;
    }

    public boolean isInitMentionedMessageFinish() {
        return mInitMentionedMessageFinish;
    }

    public void setInitMentionedMessageFinish(boolean initMentionedMessageFinish) {
        mInitMentionedMessageFinish = initMentionedMessageFinish;
    }

    public void onPause() {
        stopPlay();
    }

    public void stopPlay() {
        AudioPlayManager.getInstance().stopPlay();
    }

    public void onStop() {
        mIsForegroundActivity = false;
    }

    public void onDestroy() {
        stopPlay();
        MessageItemLongClickActionManager.getInstance()
                .removeMessageItemLongClickAction(mMoreAction);
        mMoreAction = null;
        if (mProcessor != null) mProcessor.onDestroy(this);
    }

    /**
     * 过滤只存储不计数消息，使其不显示聊天页面右下角新消息气泡UI.
     *
     * @param uiMessage
     * @return
     */
    public boolean filterMessageToHideNewMessageBar(UiMessage uiMessage) {
        if (uiMessage == null
                || uiMessage.getMessage() == null
                || uiMessage.getMessage().getContent() == null) {
            return false;
        }

        final MessageTag msgTag = uiMessage.getContent().getClass().getAnnotation(MessageTag.class);
        if (msgTag.flag() == MessageTag.ISPERSISTED) {
            return true;
        }

        return false;
    }

    public boolean isNormalState() {
        return mProcessor.isNormalState(MessageViewModel.this);
    }

    public boolean isHistoryState() {
        return mProcessor.isHistoryState(MessageViewModel.this);
    }

    @Override
    public void onUserUpdate(UserInfo user) {
        if (user != null) {
            for (UiMessage item : getUiMessages()) {
                item.onUserInfoUpdate(user);
            }
            refreshAllMessage(false);
        }
    }

    @Override
    public void onGroupUpdate(Group group) {
        // default implementation ignored
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        if (!Conversation.ConversationType.GROUP.equals(getCurConversationType())
                || groupUserInfo == null
                || !groupUserInfo.getGroupId().equals(getCurTargetId())) {
            return;
        }
        for (UiMessage item : getUiMessages()) {
            item.onGroupMemberInfoUpdate(groupUserInfo);
        }
        refreshAllMessage(false);
    }

    public void initTranslationListener() {
        if (RongCoreClient.getInstance().isTextTranslationSupported()) {
            TranslationProvider.getInstance().addListener(this);
        }
    }

    public void unInitTranslationListener() {
        if (RongCoreClient.getInstance().isTextTranslationSupported()) {
            TranslationProvider.getInstance().removeListener(this);
        }
    }

    @Override
    public void onTranslationResult(final int code, final RCTranslationResultWrapper result) {
        if (mainHandler == null) {
            return;
        }
        mainHandler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        UiMessage message = getUiMessageById(result.getMessageId());
                        if (message == null) {
                            return;
                        }
                        if (code == IRongCoreEnum.CoreErrorCode.RC_TRANSLATION_CODE_SUCCESS.code) {
                            RLog.d(TAG, "translation success: " + result.getTranslatedText());
                            message.setTranslatedContent(result.getTranslatedText());
                            message.setTranslateStatus(State.SUCCESS);
                        } else {
                            message.setTranslateStatus(State.ERROR);
                            mPageEventLiveData.setValue(
                                    new ToastEvent(
                                            getApplication()
                                                    .getString(
                                                            R.string
                                                                    .rc_translate_failed_try_again)));
                        }
                        refreshSingleMessage(message);
                    }
                });
    }

    private UiMessage getUiMessageById(int messageId) {
        for (int i = mUiMessages.size() - 1; i >= 0; i--) {
            if (mUiMessages.get(i).getMessageId() == messageId) {
                return mUiMessages.get(i);
            }
        }
        return null;
    }

    /**
     * 是否是同一个会话的消息，同一个会话的消息才需要处理
     *
     * @param message 消息
     * @return true 代表和 mConversationIdentifier 相同会话的消息。false 代表和 mConversationIdentifier 不同的会话消息
     */
    private boolean isSameConversationMessage(Message message) {
        if (mConversationIdentifier == null
                || mConversationIdentifier.getType() == null
                || TextUtils.isEmpty(mConversationIdentifier.getTargetId())) {
            return false;
        }
        if (message == null
                || message.getConversationType() == null
                || TextUtils.isEmpty(message.getTargetId())) {
            return false;
        }
        if (mConversationIdentifier.equalsWithMessage(message)) {
            return true;
        }
        return false;
    }

    /**
     * 更新消息的已读人数
     *
     * @param readReceiptInfoList 已读回执信息列表
     */
    private void updateReadCountForMessages(
            List<io.rong.imlib.model.ReadReceiptInfoV5> readReceiptInfoList) {
        for (io.rong.imlib.model.ReadReceiptInfoV5 receiptInfo : readReceiptInfoList) {
            String messageUId = receiptInfo.getMessageUId();
            int readCount = receiptInfo.getReadCount();

            // 根据messageUId找到对应的UiMessage并更新readCount
            for (UiMessage uiMessage : mUiMessages) {
                if (messageUId.equals(uiMessage.getUId())) {
                    uiMessage.setReadReceiptCount(readCount);
                    refreshSingleMessage(uiMessage);
                    break;
                }
            }
        }
    }

    /**
     * 获取消息的已读回执信息
     *
     * @param messageUIds 消息UID列表
     */
    private void fetchReadReceiptInfo(List<String> messageUIds) {
        if (messageUIds == null || messageUIds.isEmpty() || mConversationIdentifier == null) {
            return;
        }

        mReadReceiptV5Handler.getMessageReadReceiptInfoV5(mConversationIdentifier, messageUIds);
    }

    /**
     * 发送已读回执响应
     *
     * @param messageUIds 消息UID列表
     */
    private void sendReadReceiptResponse(List<String> messageUIds) {
        if (messageUIds == null || messageUIds.isEmpty() || mConversationIdentifier == null) {
            return;
        }

        mReadReceiptV5Handler.sendReadReceiptResponseV5(mConversationIdentifier, messageUIds);
    }

    /**
     * 批量获取消息列表的已读回执信息
     *
     * @param messages 消息列表
     */
    private void fetchReadReceiptInfoForMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<String> sendMessageUIds = new ArrayList<>();
        List<String> receiveMessageUIds = new ArrayList<>();

        for (Message message : messages) {
            if (message.getUId() != null
                    && !message.getUId().isEmpty()
                    && message.isNeedReceipt()) {
                if (Message.MessageDirection.SEND.equals(message.getMessageDirection())) {
                    sendMessageUIds.add(message.getUId());
                } else if (Message.MessageDirection.RECEIVE.equals(message.getMessageDirection())
                        && message.isNeedReceipt()
                        && !message.isSentReceipt()) {
                    receiveMessageUIds.add(message.getUId());
                }
            }
        }

        // 对发送的消息获取已读回执信息
        if (!sendMessageUIds.isEmpty()) {
            fetchReadReceiptInfo(sendMessageUIds);
        }

        // 对接收的消息发送已读回执响应
        if (!receiveMessageUIds.isEmpty()) {
            sendReadReceiptResponse(receiveMessageUIds);
        }
    }

    /**
     * 批量获取UI消息列表的已读回执信息
     *
     * @param uiMessages UI消息列表
     */
    private void fetchReadReceiptInfoForUiMessages(List<UiMessage> uiMessages) {
        if (uiMessages == null || uiMessages.isEmpty()) {
            return;
        }

        List<String> sendMessageUIds = new ArrayList<>();
        List<String> receiveMessageUIds = new ArrayList<>();

        for (UiMessage uiMessage : uiMessages) {
            Message message = uiMessage.getMessage();
            if (message != null
                    && message.getUId() != null
                    && !message.getUId().isEmpty()
                    && message.isNeedReceipt()) {
                if (Message.MessageDirection.SEND.equals(message.getMessageDirection())) {
                    sendMessageUIds.add(message.getUId());
                } else if (Message.MessageDirection.RECEIVE.equals(message.getMessageDirection())
                        && message.isNeedReceipt()
                        && !message.isSentReceipt()) {
                    receiveMessageUIds.add(message.getUId());
                }
            }
        }

        // 对发送的消息获取已读回执信息
        if (!sendMessageUIds.isEmpty()) {
            fetchReadReceiptInfo(sendMessageUIds);
        }

        // 对接收的消息发送已读回执响应
        if (!receiveMessageUIds.isEmpty()) {
            sendReadReceiptResponse(receiveMessageUIds);
        }
    }

    /**
     * 获取单个消息的已读回执信息
     *
     * @param uiMessage UI消息对象
     */
    private void fetchReadReceiptInfoForSingleMessage(UiMessage uiMessage) {
        if (uiMessage == null
                || uiMessage.getUId() == null
                || uiMessage.getUId().isEmpty()
                || uiMessage.getMessage() == null
                || !uiMessage.getMessage().isNeedReceipt()) {
            return;
        }

        List<String> messageUIds = new ArrayList<>();
        messageUIds.add(uiMessage.getUId());

        if (Message.MessageDirection.SEND.equals(uiMessage.getMessage().getMessageDirection())) {
            fetchReadReceiptInfo(messageUIds);
        } else if (Message.MessageDirection.RECEIVE.equals(
                        uiMessage.getMessage().getMessageDirection())
                && !uiMessage.getMessage().isSentReceipt()) {
            sendReadReceiptResponse(messageUIds);
        }
    }

    /**
     * 处理V5已读回执响应列表
     *
     * @param responses 已读回执响应列表
     */
    private void handleReadReceiptV5Responses(List<ReadReceiptResponseV5> responses) {
        if (responses == null || responses.isEmpty() || mConversationIdentifier == null) {
            return;
        }

        for (ReadReceiptResponseV5 response : responses) {
            // 检查identifier是否与当前会话一致
            if (response.getIdentifier() != null && isIdentifierMatched(response.getIdentifier())) {

                String messageUId = response.getMessageUId();
                int readCount = response.getReadCount();

                // 根据messageUId查找对应的UiMessage
                UiMessage targetMessage = findUIMessageByUId(messageUId);
                if (targetMessage != null) {
                    // 更新readCount
                    targetMessage.setReadReceiptCount(readCount);
                    // 刷新消息
                    refreshSingleMessage(targetMessage);

                    RLog.d(
                            TAG,
                            "Updated readCount for message: "
                                    + messageUId
                                    + ", readCount: "
                                    + readCount);
                } else {
                    RLog.w(TAG, "Message not found for UID: " + messageUId);
                }
            }
        }
    }

    /**
     * 检查ConversationIdentifier是否与当前会话匹配
     *
     * @param identifier 要检查的会话标识
     * @return true如果匹配当前会话
     */
    private boolean isIdentifierMatched(ConversationIdentifier identifier) {
        if (mConversationIdentifier == null || identifier == null) {
            return false;
        }

        return mConversationIdentifier.getType() == identifier.getType()
                && Objects.equals(mConversationIdentifier.getTargetId(), identifier.getTargetId());
    }

    /**
     * 根据消息UID查找UiMessage
     *
     * @param messageUId 消息UID
     * @return 找到的UiMessage，如果没找到返回null
     */
    private UiMessage findUIMessageByUId(String messageUId) {
        if (messageUId == null || messageUId.isEmpty()) {
            return null;
        }

        for (UiMessage uiMessage : mUiMessages) {
            if (messageUId.equals(uiMessage.getUId())) {
                return uiMessage;
            }
        }
        return null;
    }

    /**
     * 检查是否应该跳过 UnknownMessage
     *
     * @param message 消息对象
     * @return true 表示应该跳过该消息，false 表示不应该跳过
     */
    private boolean shouldContainUnknownMessage(Message message) {
        if (message != null && message.getContent() instanceof UnknownMessage) {
            return RongConfigCenter.featureConfig().isShowUnknownMessage();
        }
        return true;
    }
}
