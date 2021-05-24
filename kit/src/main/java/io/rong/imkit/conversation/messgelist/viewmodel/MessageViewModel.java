package io.rong.imkit.conversation.messgelist.viewmodel;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.BaseConversationEventListener;
import io.rong.imkit.IMCenter;
import io.rong.imkit.MessageItemLongClickAction;
import io.rong.imkit.MessageItemLongClickActionManager;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.moreaction.IClickActions;
import io.rong.imkit.conversation.messgelist.processor.ConversationProcessorFactory;
import io.rong.imkit.conversation.messgelist.processor.IConversationBusinessProcessor;
import io.rong.imkit.conversation.messgelist.provider.MessageClickType;
import io.rong.imkit.conversation.messgelist.status.IMessageState;
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
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.event.uievent.ScrollToEndEvent;
import io.rong.imkit.event.uievent.ShowLongClickDialogEvent;
import io.rong.imkit.event.uievent.ToastEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.feature.forward.ForwardClickActions;
import io.rong.imkit.feature.forward.ForwardManager;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.IAudioPlayListener;
import io.rong.imkit.manager.hqvoicemessage.AutoDownloadEntry;
import io.rong.imkit.manager.hqvoicemessage.HQVoiceMsgDownloadManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.notification.RongNotificationManager;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.cache.MessageList;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import io.rong.message.HQVoiceMessage;
import io.rong.message.HistoryDividerMessage;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.VoiceMessage;


public class MessageViewModel extends AndroidViewModel implements MessageEventListener {
    private static final String TAG = "MessageViewModel";
    public static final int DEFAULT_COUNT = RongConfigCenter.conversationConfig().getConversationHistoryMessageCount();
    public static final int DEFAULT_REMOTE_COUNT = RongConfigCenter.conversationConfig().getConversationRemoteMessageCount();
    public static final int SHOW_UNREAD_MESSAGE_COUNT = RongConfigCenter.conversationConfig().getConversationShowUnreadMessageCount();
    private List<UiMessage> mUiMessages = new MessageList<>(6000);
    private List<UiMessage> mSelectedUiMessage = new ArrayList<>();
    private MediatorLiveData<PageEvent> mPageEventLiveData = new MediatorLiveData<>();
    private MediatorLiveData<List<UiMessage>> mUiMessageLiveData = new MediatorLiveData<>();
    private Conversation.ConversationType mCurConversationType = null;
    private String mCurTargetId = null;
    //远端数据是否全部拉取完成
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
    //应用是否在前台
    private boolean mIsForegroundActivity;
    //是否滑动到页面最底部
    private boolean mScrollToBottom;
    private IConversationBusinessProcessor mProcessor;
    private Bundle mBundle;
    private Message mFirstUnreadMessage;
    private IMessageState mState;
    public static String[] writePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public MessageViewModel(@NonNull Application application) {
        super(application);
        IMCenter.getInstance().addOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().addConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().addReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().addOnRecallMessageListener(mRecallMessageListener);
        IMCenter.getInstance().addMessageEventListener(this);
        IMCenter.getInstance().addConversationEventListener(mConversationEventListener);
    }

    public void bindConversation(Conversation.ConversationType type, String targetId, Bundle bundle) {
        mCurTargetId = targetId;
        mCurConversationType = type;
        mProcessor = ConversationProcessorFactory.getInstance().getProcessor(type);
        mBundle = bundle;
        if (bundle != null) {
            long indexTime = bundle.getLong(RouteUtils.INDEX_MESSAGE_TIME, 0);
            if (indexTime > 0) {
                mState = IMessageState.historyState;
            } else {
                mState = IMessageState.normalState;
            }
        } else {
            mState = IMessageState.normalState;
        }
        mState.init(this, bundle);
        mProcessor.init(this, bundle);
        mIsEditStatus.setValue(false);
    }

    /**
     * 初始化加载本地消息
     * 下拉加载历史消息
     */
    public void onGetHistoryMessage(List<Message> messages) {
        mProcessor.onLoadMessage(this, messages);
        for (Message message : messages) {
            //去重处理
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getMessageId() == message.getMessageId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                mUiMessages.add(0, mapUIMessage(message));
            }
        }
        processHistoryDividerMessage();
        updateUiMessages();
    }

    /**
     * 上拉加载更多消息
     */
    public void onLoadMoreMessage(List<Message> messages) {
        mProcessor.onLoadMessage(this, messages);
        ArrayList<UiMessage> list = new ArrayList<>();
        for (Message message : messages) {
            //去重处理
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getMessageId() == message.getMessageId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                list.add(0, mapUIMessage(message));
            }
        }
        mUiMessages.addAll(list);
        processHistoryDividerMessage();
        updateUiMessages();
    }

    /**
     * normal状态点击历史消息bar
     * history状态点击新消息bar
     */
    public void onReloadMessage(List<Message> messages) {
        mProcessor.onLoadMessage(this, messages);
        mUiMessages.clear();
        for (Message message : messages) {
            //去重处理
            boolean contains = false;
            for (UiMessage uiMessage : mUiMessages) {
                if (uiMessage.getMessage().getMessageId() == message.getMessageId()) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                mUiMessages.add(0, mapUIMessage(message));
            }
        }
        processHistoryDividerMessage();
        updateUiMessages();
    }


    public MediatorLiveData<PageEvent> getPageEventLiveData() {
        return mPageEventLiveData;
    }

    public void onWarnClick(final UiMessage uiMessage) {
        final Message msg = uiMessage.getMessage();
        RongIMClient.getInstance().deleteMessages(new int[]{msg.getMessageId()}, new RongIMClient.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                if (aBoolean) {
                    int position = findPositionByMessageId(msg.getMessageId());
                    if (position >= 0) {
                        mUiMessages.remove(position);
                        mUiMessageLiveData.setValue(mUiMessages);
                    }
                    msg.setMessageId(0);
                    reSendMessage(msg);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {

            }
        });
    }

    public void onItemClick(UiMessage uiMessage) {
        Conversation.ConversationType type = uiMessage.getMessage().getConversationType();
        ConversationProcessorFactory.getInstance().getProcessor(type).onMessageItemClick(uiMessage);
    }

    public boolean onItemLongClick(final UiMessage uiMessage) {
        if (RongConfigCenter.conversationConfig().isShowMoreClickAction()) {
            if (mMoreAction == null) {
                mMoreAction = new MessageItemLongClickAction.Builder()
                        .titleResId(R.string.rc_dialog_item_message_more)
                        .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                            @Override
                            public boolean onMessageItemLongClick(Context context, UiMessage message) {
                                List<IClickActions> actions = RongConfigCenter.conversationConfig().getMoreClickActions();
                                if (actions != null && actions.size() > 0) {
                                    if (!containsForwardClickAction(actions) && RongConfigCenter.conversationConfig().rc_enable_send_combine_message) {
                                        RongConfigCenter.conversationConfig().addMoreClickAction(0, new ForwardClickActions());
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
                        }).build();
                MessageItemLongClickActionManager.getInstance().addMessageItemLongClickAction(mMoreAction);
            }
        }

        final List<MessageItemLongClickAction> messageItemLongClickActions =
                MessageItemLongClickActionManager.getInstance()
                        .getMessageItemLongClickActions(uiMessage);
        executePageEvent(new ShowLongClickDialogEvent(new MessageItemLongClickBean(messageItemLongClickActions, uiMessage)));
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

    public void onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId) {
        ConversationProcessorFactory.getInstance().getProcessor(conversationType).onUserPortraitClick(context, conversationType, userInfo, targetId);
    }

    public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo userInfo, String targetId) {
        return ConversationProcessorFactory.getInstance().getProcessor(conversationType).onUserPortraitLongClick(context, conversationType, userInfo, targetId);
    }

    public void processHistoryDividerMessage() {
        //存在历史消息
        if (getFirstUnreadMessage() == null) {
            return;
        }
        int position = findPositionByMessageId(getFirstUnreadMessage().getMessageId());
        //找到第一条历史消息
        if (position >= 0) {
            //插入历史未读数
            if (RongConfigCenter.conversationConfig().isShowHistoryDividerMessage()) {
                Message hisMessage = Message.obtain(getCurTargetId(), getCurConversationType(),
                        HistoryDividerMessage.obtain(getApplication().getString(R.string.rc_new_message_divider_content)));
                hisMessage.setSenderUserId(RongIMClient.getInstance().getCurrentUserId());
                UiMessage uiMessage = new UiMessage(hisMessage);
                UiMessage firstUiMessage = mUiMessages.get(position);
                //比第一条历史消息时间戳少1毫秒
                uiMessage.setSentTime(firstUiMessage.getMessage().getSentTime() - 1);
                mUiMessages.add(position, uiMessage);
                updateUiMessages();
            }
        }
    }

    public void reSendMessage(Message message) {
        if (message.getContent() instanceof LocationMessage) {
            IMCenter.getInstance().sendLocationMessage(message, null, null, null);
        } else if (message.getContent() instanceof ReferenceMessage) {
            IMCenter.getInstance().sendMessage(message, null, null, (IRongCallback.ISendMessageCallback) null);
        } else if (message.getContent() instanceof MediaMessageContent) {
            IMCenter.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
        } else {
            IMCenter.getInstance().sendMessage(message, null, null, null);
        }
    }

    public void onAudioClick(UiMessage uiMessage) {
        //处理暂停逻辑
        MessageContent content = uiMessage.getMessage().getContent();
        if (content instanceof HQVoiceMessage) {
            if (AudioPlayManager.getInstance().isPlaying()) {
                Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                AudioPlayManager.getInstance().stopPlay();
                //暂停的是当前播放的 Uri
                if (playingUri.equals(((HQVoiceMessage) content).getLocalPath()))
                    return;
            }
            //如果被 voip 占用通道，则不播放，弹提示框
            if (!AudioPlayManager.getInstance().isInNormalMode(getApplication()) && AudioPlayManager.getInstance().isInVOIPMode(getApplication())) {
                mPageEventLiveData.setValue(new ToastEvent(getApplication().getString(R.string
                        .rc_voip_occupying)));
                return;
            }
            playOrDownloadHQVoiceMsg((HQVoiceMessage) uiMessage.getMessage().getContent(), uiMessage);
        } else if (content instanceof VoiceMessage) {
            if (AudioPlayManager.getInstance().isPlaying()) {
                Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                AudioPlayManager.getInstance().stopPlay();
                //暂停的是当前播放的 Uri
                if (playingUri.equals(((VoiceMessage) content).getUri()))
                    return;
            }
            //如果被 voip 占用通道，则不播放，弹提示框
            if (!AudioPlayManager.getInstance().isInNormalMode(getApplication()) && AudioPlayManager.getInstance().isInVOIPMode(getApplication())) {
                mPageEventLiveData.setValue(new ToastEvent(getApplication().getString(R.string
                        .rc_voip_occupying)));
                return;
            }
            playVoiceMessage(uiMessage);
        }
    }

    public void stopPlay() {
        AudioPlayManager.getInstance().stopPlay();
    }

    private void playOrDownloadHQVoiceMsg(HQVoiceMessage content, UiMessage uiMessage) {
        boolean ifDownloadHQVoiceMsg = (content.getLocalPath() == null || TextUtils.isEmpty(content.getLocalPath().toString()) || !FileUtils.isFileExistsWithUri(getApplication(), content.getLocalPath()));
        if (ifDownloadHQVoiceMsg) {
            downloadHQVoiceMsg(uiMessage);
        } else {
            playVoiceMessage(uiMessage);
        }
    }

    private void downloadHQVoiceMsg(final UiMessage uiMessage) {
        RongIMClient.getInstance().downloadMediaMessage(uiMessage.getMessage(), new IRongCallback.IDownloadMediaMessageCallback() {
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
            AudioPlayManager.getInstance().startPlay(getApplication(), voicePath, new IAudioPlayListener() {
                @Override
                public void onStart(Uri uri) {
                    uiMessage.setPlaying(true);
                    Message message = uiMessage.getMessage();
                    message.getReceivedStatus().setListened();
                    RongIMClient.getInstance().setMessageReceivedStatus(message.getMessageId(), message.getReceivedStatus(), null);
                    if (message.getContent().isDestruct() && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                        uiMessage.setReadTime(0);
                        DestructManager.getInstance().stopDestruct(uiMessage.getMessage());
                    }
                    refreshSingleMessage(uiMessage);
                }

                @Override
                public void onStop(Uri uri) {
                    uiMessage.setPlaying(false);
                    if (content.isDestruct() && uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                        DestructManager.getInstance().startDestruct(uiMessage.getMessage());
                    }
                    refreshSingleMessage(uiMessage);
                }

                @Override
                public void onComplete(Uri uri) {
                    uiMessage.setPlaying(false);
                    //找到下个播放消息继续播放
                    if (content.isDestruct() && uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                        DestructManager.getInstance().startDestruct(uiMessage.getMessage());
                        refreshSingleMessage(uiMessage);
                    } else {
                        refreshSingleMessage(uiMessage);
                        //不切换线程会造成，ui 一直显示播放的 bug
                        ExecutorHelper.getInstance().mainThread().execute(new Runnable() {
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
        for (int i = position; i < mUiMessages.size(); i++) {
            UiMessage item = mUiMessages.get(i);
            if (item.getMessage().getContent() instanceof HQVoiceMessage) {
                if (!item.getMessage().getReceivedStatus().isListened() && !item.getMessage().getContent().isDestruct()) {
                    onAudioClick(item);
                    break;
                }
            } else if (item.getMessage().getContent() instanceof VoiceMessage) {
                if (!item.getMessage().getReceivedStatus().isListened() && !item.getMessage().getContent().isDestruct()) {
                    onAudioClick(item);
                    break;
                }
            }
        }
    }

    public void refreshSingleMessage(UiMessage uiMessage) {
        int position = findPositionByMessageId(uiMessage.getMessage().getMessageId());
        if (position != -1) {
            uiMessage.setChange(true);
            mUiMessageLiveData.setValue(mUiMessages);
        }
    }

    private void sendMessageEvent(UiMessage uiMessage) {
        int insertPosition = findPositionBySendTime(uiMessage.getMessage().getSentTime());
        mUiMessages.add(insertPosition, uiMessage);
        mUiMessageLiveData.setValue(mUiMessages);
        executePageEvent(new ScrollToEndEvent());
    }

    public UiMessage mapUIMessage(Message message) {
        UiMessage uiMessage = new UiMessage(message);
        if (mIsEditStatus != null) {
            uiMessage.setEdit(mIsEditStatus.getValue());
        }
        return uiMessage;
    }

    public boolean onBackPressed() {
        if (mIsEditStatus != null && mIsEditStatus.getValue()) {
            quitEditMode();
            return true;
        }
        return mProcessor.onBackPressed(this);
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
        if (Objects.equals(mCurTargetId, msg.getTargetId())
                && Objects.equals(mCurConversationType, msg.getConversationType()) && msg.getMessageId() > 0) {
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
                    msg.setSentTime(sentTime);//更新成服务器时间
                    uiMessage.setState(State.PROGRESS);
                    break;
                case SendEvent.ERROR:
                    sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(sentTime);//更新成服务器时间
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
        if (Objects.equals(mCurTargetId, msg.getTargetId())
                && Objects.equals(mCurConversationType, msg.getConversationType()) && msg.getMessageId() > 0) {
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
                    msg.setSentTime(sentTime);//更新成服务器时间
                    uiMessage.setState(State.PROGRESS);
                    break;
                case SendMediaEvent.PROGRESS:
                    uiMessage.setState(State.PROGRESS);
                    uiMessage.setProgress(event.getProgress());
                    break;
                case SendMediaEvent.ERROR:
                    if (event.getCode() != null && event.getCode().code == RongIMClient.ErrorCode.RC_MEDIA_EXCEPTION.code) {
                        ToastUtils.s(getApplication(), getApplication().getString(R.string.rc_media_upload_error));
                    }
                    sentTime = msg.getSentTime() - RongIMClient.getInstance().getDeltaTime();
                    msg.setSentTime(sentTime);//更新成服务器时间
                    uiMessage.setState(State.ERROR);
                    break;
                case SendMediaEvent.SUCCESS:
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
        if (msg != null && Objects.equals(mCurTargetId, msg.getTargetId())
                && Objects.equals(mCurConversationType, msg.getConversationType())
                && msg.getMessageId() > 0) {
            UiMessage uiMessage = findUIMessage(msg.getMessageId());
            if (uiMessage != null) {
                switch (event.getEvent()) {
                    case DownloadEvent.SUCCESS:
                        uiMessage.setState(State.NORMAL);
                        break;
                    case DownloadEvent.PROGRESS:
                        uiMessage.setState(State.PROGRESS);
                        uiMessage.setProgress(event.getProgress());
                        break;
                    case DownloadEvent.ERROR:
                        uiMessage.setProgress(-1);
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
                        if (((VoiceMessage) content).getUri().equals(AudioPlayManager.getInstance().getPlayingUri())) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                    } else if (content instanceof HQVoiceMessage) {
                        if (((HQVoiceMessage) content).getLocalPath().equals(AudioPlayManager.getInstance().getPlayingUri())) {
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
    }

    @Override
    public void onRecallEvent(RecallEvent event) {
        UiMessage uiMessage = findUIMessage(event.getMessageId());
        if (uiMessage != null) {
            MessageContent content = uiMessage.getMessage().getContent();
            if (AudioPlayManager.getInstance().isPlaying()) {
                if (content instanceof VoiceMessage) {
                    if (((VoiceMessage) content).getUri().equals(AudioPlayManager.getInstance().getPlayingUri())) {
                        AudioPlayManager.getInstance().stopPlay();
                    }
                } else if (content instanceof HQVoiceMessage) {
                    if (((HQVoiceMessage) content).getLocalPath().equals(AudioPlayManager.getInstance().getPlayingUri())) {
                        AudioPlayManager.getInstance().stopPlay();
                    }
                }
            }
            if (content instanceof MediaMessageContent) {
                IMCenter.getInstance().cancelDownloadMediaMessage(uiMessage.getMessage(), null);
            }
            uiMessage.setContent(event.getRecallNotificationMessage());
            refreshSingleMessage(uiMessage);
            UiMessage newUnreadMessage = findNewUnreadMessage(uiMessage.getMessage().getMessageId());
            if (newUnreadMessage != null) {
                mNewUnReadMessages.remove(newUnreadMessage);
                processNewMessageUnread(true);
            }
        }
    }

    public void processNewMessageUnread(boolean isMainThread) {
        if (RongConfigCenter.conversationConfig().isShowNewMessageBar(mCurConversationType)) {
            if (isMainThread) {
                mNewMessageUnreadLiveData.setValue(mNewUnReadMessages.size());
            } else {
                mNewMessageUnreadLiveData.postValue(mNewUnReadMessages.size());
            }
        }
    }


    public void processNewMentionMessageUnread(boolean isMainThread) {
        if (RongConfigCenter.conversationConfig().isShowNewMentionMessageBar(mCurConversationType)) {
            if (isMainThread) {
                mNewMentionMessageUnreadLiveData.setValue(mNewUnReadMentionMessages.size());
            } else {
                mNewMentionMessageUnreadLiveData.postValue(mNewUnReadMentionMessages.size());
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
        if (Objects.equals(mCurTargetId, msg.getTargetId())
                && Objects.equals(mCurConversationType, msg.getConversationType())
                && msg.getMessageId() > 0) {
            int position = findPositionByMessageId(msg.getMessageId());
            if (position == -1) {
                mUiMessages.add(mapUIMessage(msg));
                mUiMessageLiveData.setValue(mUiMessages);
                executePageEvent(new ScrollToEndEvent());
            }
        }
    }

    @Override
    public void onClearMessages(ClearEvent event) {
        RLog.d(TAG, "onClearMessages");
        if (event.getTargetId().equals(mCurTargetId) && event.getConversationType().equals(mCurConversationType)) {
            mUiMessages.clear();
            mUiMessageLiveData.setValue(mUiMessages);
            mNewUnReadMentionMessages.clear();
            processNewMentionMessageUnread(true);
            hideHistoryBar();
        }
    }


    private int findPositionBySendTime(long sentTime) {
        int position = mUiMessages.size();
        for (int i = 0; i < mUiMessages.size(); i++) {
            UiMessage item = mUiMessages.get(i);
            if (item.getMessage().getSentTime() > sentTime) {
                position = i;
                break;
            }
        }
        return position;
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

    public Conversation.ConversationType getCurConversationType() {
        return mCurConversationType;
    }


    public String getCurTargetId() {
        return mCurTargetId;
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
                result = mUiMessages.get(i).getSentTime();
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

    public void executePageEvent(PageEvent pageEvent) {
        mPageEventLiveData.setValue(pageEvent);
    }

    public void updateUiMessages() {
        updateUiMessages(true);
    }

    public void updateUiMessages(boolean sync) {
        if (sync) {
            mUiMessageLiveData.setValue(mUiMessages);
        } else {
            mUiMessageLiveData.postValue(mUiMessages);
        }
    }

    public void updateMentionMessage(io.rong.imlib.model.Message message) {
        if (RongConfigCenter.conversationConfig().isShowNewMentionMessageBar(message.getConversationType())
                && message != null && message.getContent() != null && message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            MentionedInfo.MentionedType type = mentionedInfo.getType();
            if (type == MentionedInfo.MentionedType.ALL && message.getSenderUserId() != null &&
                    !message.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                mNewUnReadMentionMessages.add(message);
            } else if (type == MentionedInfo.MentionedType.PART &&
                    mentionedInfo.getMentionedUserIdList() != null &&
                    mentionedInfo.getMentionedUserIdList().contains(RongIMClient.getInstance().getCurrentUserId())) {
                mNewUnReadMentionMessages.add(message);
            }
            processNewMentionMessageUnread(false);
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
        RongIMClient.getInstance().sendReadReceiptRequest(message, new RongIMClient.OperationCallback() {
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
                RLog.e(TAG, "sendReadReceiptRequest failed, errorCode = " + errorCode);
                if (Objects.equals(errorCode, RongIMClient.ErrorCode.RC_NET_CHANNEL_INVALID)
                        || Objects.equals(errorCode, RongIMClient.ErrorCode.RC_NET_UNAVAILABLE)) {
                    executePageEvent(new ToastEvent(getApplication().getString(R.string.rc_notice_network_unavailable)));
                }
            }
        });
    }

    public void onReadReceiptStateClick(UiMessage uiMessage) {

    }

    public void onReEditClick(UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message != null && message.getConversationType() == getCurConversationType()
                && getCurTargetId().equals(message.getTargetId())) {
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
        mState.onRefresh(this);
    }

    public void cleanUnreadNewCount() {
        mNewUnReadMessages.clear();
    }


    public void newMessageBarClick() {
        if (mState != null) {
            mState.onNewMessageBarClick(this);
        }
    }

    public void unreadBarClick() {
        if (mState != null) {
            mState.onHistoryBarClick(this);
        }
    }

    public void newMentionMessageBarClick() {
        if (mState != null) {
            mState.onNewMentionMessageBarClick(this);
        }
    }

    /**
     * 启动编辑模式
     */
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
        //通知输入框刷新
        mPageEventLiveData.setValue(new InputBarEvent(InputBarEvent.Type.ShowMoreMenu, ""));
    }

    /**
     * 退出编辑模式
     */
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
        //通知输入框刷新
        mPageEventLiveData.setValue(new InputBarEvent(InputBarEvent.Type.HideMoreMenu, ""));
    }


    public List<UiMessage> getSelectedUiMessages() {
        return mSelectedUiMessage;
    }

    public void forwardMessage(Intent data) {
        if (data == null) return;
        List<Message> messageList = new ArrayList<>();
        for (UiMessage uiMessage : getSelectedUiMessages()) {
            messageList.add(uiMessage.getMessage());
        }
        ForwardManager.getInstance().forwardMessages(
                data.getIntExtra(RouteUtils.FORWARD_TYPE, 0),
                data.<Conversation>getParcelableArrayListExtra("conversations"),
                data.getIntegerArrayListExtra(RouteUtils.MESSAGE_IDS),
                messageList);
        quitEditMode();
        ForwardManager.getInstance().exitDestructMode();
    }

    public void onLoadMore() {
        if (mState != null)
            mState.onLoadMore(this);
    }

    public boolean isInitMentionedMessageFinish() {
        return mInitMentionedMessageFinish;
    }

    public void setInitMentionedMessageFinish(boolean initMentionedMessageFinish) {
        mInitMentionedMessageFinish = initMentionedMessageFinish;
    }

    public boolean isInitUnreadMessageFinish() {
        return mInitUnreadMessageFinish;
    }

    public void setInitUnreadMessageFinish(boolean initUnreadMessageFinish) {
        mInitUnreadMessageFinish = initUnreadMessageFinish;
    }


    public Message getFirstUnreadMessage() {
        return mFirstUnreadMessage;
    }


    public void setFirstUnreadMessage(Message firstUnreadMessage) {
        mFirstUnreadMessage = firstUnreadMessage;
    }

    public List<UiMessage> getUiMessages() {
        return mUiMessages;
    }

    private RongIMClient.OnReceiveMessageWrapperListener mOnReceiveMessageListener = new RongIMClient.OnReceiveMessageWrapperListener() {
        @Override
        public boolean onReceived(Message message, int left, boolean hasPackage, boolean offline) {
            if (message == null) {
                return false;
            }
            if (mCurConversationType == message.getConversationType() && Objects.equals(mCurTargetId, message.getTargetId())) {
                UiMessage uiMessage = mapUIMessage(message);
                //处理在线消息，高清语音下载、消息回执、多端同步
                if (left == 0 && !hasPackage) {
                    //高清语音下载
                    if (message.getContent() instanceof HQVoiceMessage) {
                        if (PermissionCheckUtil.checkPermissions(getApplication(), MessageViewModel.writePermission)) {
                            if (RongConfigCenter.conversationConfig().rc_enable_automatic_download_voice_msg) {
                                HQVoiceMsgDownloadManager.getInstance().enqueue(new AutoDownloadEntry(message, AutoDownloadEntry.DownloadPriority.HIGH));
                            } else {
                                RLog.e(TAG, "rc_enable_automatic_download_voice_msg disable");
                            }
                            uiMessage.setState(State.PROGRESS);
                        } else {
                            uiMessage.setState(State.ERROR);
                        }
                    }
                }
                //已读状态设置
                if (message.getMessageId() > 0) {
                    if (isForegroundActivity()) {
                        message.getReceivedStatus().setRead();
                        RongIMClient.getInstance().setMessageReceivedStatus(message.getMessageId(), message.getReceivedStatus(), null);
                    }
                }
                mProcessor.onReceived(MessageViewModel.this, uiMessage, left, hasPackage, offline);
                mState.onReceived(MessageViewModel.this, uiMessage, left, hasPackage, offline);
            }
            return false;
        }
    };

    private final RongIMClient.ReadReceiptListener mReadReceiptListener = new RongIMClient.ReadReceiptListener() {
        @Override
        public void onReadReceiptReceived(Message message) {
            if (mCurConversationType == null || TextUtils.isEmpty(mCurTargetId)) {
                return;
            }
            if (mCurConversationType == Conversation.ConversationType.PRIVATE && message.getConversationType() == mCurConversationType && Objects.equals(mCurTargetId, message.getTargetId())
                    && message.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
                ReadReceiptMessage content = (ReadReceiptMessage) message.getContent();
                long ntfTime = content.getLastMessageSendTime();
                int count = 0;
                for (int i = mUiMessages.size() - 1; i >= 0; i--) {
                    UiMessage uiMessage = mUiMessages.get(i);
                    //如果消息已读，则之前的消息不会重新赋值直接跳出循环
                    if (Message.MessageDirection.SEND.equals(uiMessage.getMessage().getMessageDirection()) && uiMessage.getMessage().getSentStatus() == Message.SentStatus.READ) {
                        break;
                    }
                    count++;
                    if (Message.MessageDirection.SEND.equals(uiMessage.getMessage().getMessageDirection())
                            && (Message.SentStatus.SENT.equals(uiMessage.getMessage().getSentStatus()))
                            && ntfTime >= uiMessage.getMessage().getSentTime()) {
                        uiMessage.setSentStatus(Message.SentStatus.READ);
                    }
                }
                //需要刷新
                if (count > 0) {
                    mUiMessageLiveData.setValue(mUiMessages);
                }
            }
        }

        @Override
        public void onMessageReceiptRequest(Conversation.ConversationType conversationType, String targetId, String messageUId) {
            if (!Objects.equals(mCurConversationType, conversationType) || !Objects.equals(mCurTargetId, targetId)) {
                return;
            }
            if (!(Conversation.ConversationType.GROUP.equals(mCurConversationType) || Conversation.ConversationType.DISCUSSION.equals(mCurConversationType))) {
                return;
            }
            if (!RongConfigCenter.conversationConfig().isShowReadReceiptRequest(mCurConversationType)) {
                return;
            }

            for (final UiMessage item : mUiMessages) {
                String uid = item.getMessage().getUId();
                if (uid != null && uid.equals(messageUId)) {
                    ReadReceiptInfo readReceiptInfo = item.getMessage().getReadReceiptInfo();
                    if (readReceiptInfo == null) {
                        readReceiptInfo = new ReadReceiptInfo();
                        item.setReadReceiptInfo(readReceiptInfo);
                    }
                    if (readReceiptInfo.isReadReceiptMessage() && readReceiptInfo.hasRespond()) {
                        return;
                    }
                    readReceiptInfo.setIsReadReceiptMessage(true);
                    readReceiptInfo.setHasRespond(false);
                    List<Message> messageList = new ArrayList<>();
                    messageList.add(item.getMessage());
                    RongIMClient.getInstance().sendReadReceiptResponse(mCurConversationType, mCurTargetId, messageList, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            item.getMessage().getReadReceiptInfo().setHasRespond(true);
                            refreshSingleMessage(item);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            RLog.e(TAG, "sendReadReceiptResponse failed, errorCode = " + errorCode);
                        }
                    });
                    break;
                }
            }
        }

        @Override
        public void onMessageReceiptResponse(Conversation.ConversationType conversationType, String targetId, String messageUid, HashMap<String, Long> respondUserIdList) {
            if (mCurConversationType == null || TextUtils.isEmpty(mCurTargetId)) {
                return;
            }
            if (!(mCurConversationType.equals(Conversation.ConversationType.GROUP) || mCurConversationType.equals(Conversation.ConversationType.DISCUSSION))) {
                return;
            }
            if (!(conversationType.equals(Conversation.ConversationType.GROUP) || conversationType.equals(Conversation.ConversationType.DISCUSSION) || conversationType.equals(Conversation.ConversationType.PRIVATE))) {
                return;
            }
            if (!conversationType.equals(mCurConversationType) || !targetId.equals(mCurTargetId)) {
                return;
            }
            for (final UiMessage item : mUiMessages) {
                if (item.getMessage().getUId() != null && item.getMessage().getUId().equals(messageUid)) {
                    ReadReceiptInfo readReceiptInfo = item.getMessage().getReadReceiptInfo();
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

    /**
     * 消息被撤回时的回调。
     */
    private RongIMClient.OnRecallMessageListener mRecallMessageListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            if (mCurConversationType == null || TextUtils.isEmpty(mCurTargetId)) {
                return false;
            }
            RLog.d(TAG, "onRecallMessage");
            Conversation.ConversationType conversationType = message.getConversationType();
            String targetId = message.getTargetId();
            if (!conversationType.equals(mCurConversationType) || !targetId.equals(mCurTargetId)) {
                return false;
            }
            UiMessage uiMessage = findUIMessage(message.getMessageId());
            removeRecallMentionMsg(message);
            if (uiMessage != null) {
                MessageContent content = uiMessage.getMessage().getContent();
                if (content instanceof VoiceMessage) {
                    Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                    if (playingUri.equals(((VoiceMessage) content).getUri())) {
                        AudioPlayManager.getInstance().stopPlay();
                    }
                } else if (content instanceof HQVoiceMessage) {
                    Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                    if (playingUri.equals(((HQVoiceMessage) content).getLocalPath())) {
                        AudioPlayManager.getInstance().stopPlay();
                    }
                } else if (content instanceof MediaMessageContent) {
                    RongIMClient.getInstance().cancelDownloadMediaMessage(uiMessage.getMessage(), null);
                }
                uiMessage.setContent(recallNotificationMessage);
                refreshSingleMessage(uiMessage);
                UiMessage newUnreadMessage = findNewUnreadMessage(message.getMessageId());
                if (newUnreadMessage != null) {
                    mNewUnReadMessages.remove(newUnreadMessage);
                    processNewMessageUnread(true);
                }
            }
            return false;
        }
    };

    private void removeRecallMentionMsg(Message message) {
        //遍历 @消息未读列表，如果存在撤回消息则移除，刷新 @ Bar
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
            processNewMentionMessageUnread(true);
        }
    }


    private RongIMClient.ConnectionStatusListener mConnectionStatusListener = new RongIMClient.ConnectionStatusListener() {
        @Override
        public void onChanged(ConnectionStatus connectionStatus) {
            if (mProcessor != null) {
                mProcessor.onConnectStatusChange(MessageViewModel.this, connectionStatus);
            }
        }
    };

    private BaseConversationEventListener mConversationEventListener = new BaseConversationEventListener() {
        @Override
        public void onClearedMessage(Conversation.ConversationType type, String targetId) {
            if (mCurConversationType == null || TextUtils.isEmpty(mCurTargetId)) {
                return;
            }
            if (type.equals(mCurConversationType) && targetId.equals(mCurTargetId)) {
                mUiMessages.clear();
                mFirstUnreadMessage = null;
                mRemoteMessageLoadFinish = false;
                mState = IMessageState.normalState;
                mUiMessageLiveData.setValue(mUiMessages);
                mNewUnReadMentionMessages.clear();
                processNewMentionMessageUnread(true);
                hideHistoryBar();
            }
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();
        IMCenter.getInstance().removeOnReceiveMessageListener(mOnReceiveMessageListener);
        IMCenter.getInstance().removeConnectionStatusListener(mConnectionStatusListener);
        IMCenter.getInstance().removeReadReceiptListener(mReadReceiptListener);
        IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
        IMCenter.getInstance().removeMessageEventListener(this);
        IMCenter.getInstance().removeConversationEventListener(mConversationEventListener);
    }

    public boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }


    public void onViewClick(int clickType, UiMessage data) {
        IMessageViewModelProcessor viewModelProcessor = RongConfigCenter.conversationConfig().getViewModelProcessor();
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
                    onUserPortraitClick(getApplication(), data.getMessage().getConversationType(), data.getUserInfo(), data.getMessage().getTargetId());
                    break;
                case MessageClickType.EDIT_CLICK:
                    boolean selected = data.isSelected();
                    int preSize = mSelectedUiMessage.size();
                    if (selected) {
                        mSelectedUiMessage.remove(data);
                        if (mSelectedUiMessage.size() <= 0) {
                            mPageEventLiveData.postValue(new InputBarEvent(InputBarEvent.Type.InactiveMoreMenu, null));
                        }
                        data.setSelected(false);
                        refreshSingleMessage(data);
                    } else {
                        if (mSelectedUiMessage.size() < RongConfigCenter.conversationConfig().rc_max_message_selected_count) {
                            mSelectedUiMessage.add(data);
                            if (mSelectedUiMessage.size() > 0 && preSize <= 0) {
                                mPageEventLiveData.setValue(new InputBarEvent(InputBarEvent.Type.ActiveMoreMenu, null));
                            }
                            data.setSelected(true);
                            refreshSingleMessage(data);
                        } else {
                            if (RongConfigCenter.conversationConfig().rc_max_message_selected_count == 100) {
                                executePageEvent(new ToastEvent(getApplication().getString(R.string.rc_exceeded_max_limit_100)));
                            }
                        }
                    }
                    break;
            }
        }
    }

    public boolean onViewLongClick(int clickType, UiMessage data) {
        IMessageViewModelProcessor viewModelProcessor = RongConfigCenter.conversationConfig().getViewModelProcessor();
        boolean isProcess = false;
        if (viewModelProcessor != null) {
            isProcess = viewModelProcessor.onViewLongClick(this, clickType, data);
        }
        if (!isProcess) {
            switch (clickType) {
                case MessageClickType.CONTENT_LONG_CLICK:
                    return onItemLongClick(data);
                case MessageClickType.USER_PORTRAIT_LONG_CLICK:
                    return onUserPortraitLongClick(getApplication(), data.getMessage().getConversationType(), data.getUserInfo(), data.getMessage().getTargetId());
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

    public IMessageState getState() {
        return mState;
    }


    public void setState(IMessageState state) {
        mState = state;
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

    public void hideHistoryBar() {
        setFirstUnreadMessage(null);
        mHistoryMessageUnreadLiveData.setValue(0);
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

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        //判断是否滑动到底部
        if (!recyclerView.canScrollVertically(1)) {
            setScrollToBottom(true);
            mState.onScrollToBottom(this);
        } else {
            setScrollToBottom(false);
        }
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (RongConfigCenter.conversationConfig().isShowHistoryMessageBar(mCurConversationType) && getFirstUnreadMessage() != null) {
            int firstPosition = findPositionByMessageId(getFirstUnreadMessage().getMessageId());
            if (layoutManager instanceof LinearLayoutManager) {
                int firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                if (firstVisibleItemPosition <= firstPosition) {
                    hideHistoryBar();
                }
            }
        }

        if (RongConfigCenter.conversationConfig().isShowHistoryMessageBar(mCurConversationType) && mNewUnReadMentionMessages != null && mNewUnReadMentionMessages.size() > 0 && getUiMessages().size() > 0) {
            int firstVisibleItemPosition = 0;
            int lastPosition = 0;
            if (layoutManager instanceof LinearLayoutManager) {
                firstVisibleItemPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                lastPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
            }
            UiMessage firstMessage = getUiMessages().get(Math.max(firstVisibleItemPosition, 0));
            UiMessage lastMessage = getUiMessages().get(lastPosition < getUiMessages().size() ? lastPosition : getUiMessages().size() - 1);
            long topTime = firstMessage.getSentTime();
            long bottomTime = lastMessage.getSentTime();
            int size = mNewUnReadMentionMessages.size();
            for (int i = size - 1; i >= 0; i--) {
                if (mNewUnReadMentionMessages.get(i).getSentTime() >= topTime && mNewUnReadMentionMessages.get(i).getSentTime() <= bottomTime) {
                    mNewUnReadMentionMessages.remove(mNewUnReadMentionMessages.get(i));
                }
            }
        }
        processNewMentionMessageUnread(true);
    }


    /**
     * 清理未读状态
     */
    public void cleanUnreadStatus() {
        if (isInitUnreadMessageFinish() && isInitMentionedMessageFinish()) {
            IMCenter.getInstance().clearMessagesUnreadStatus(getCurConversationType(), getCurTargetId(), null);
        }
    }

    public void onExistUnreadMessage(Conversation conversation, int unreadMessageCount) {
        if (mProcessor != null)
            mProcessor.onExistUnreadMessage(this, conversation, unreadMessageCount);
    }

    private void clearAllNotification() {
        RongNotificationManager.getInstance().clearAllNotification();
    }

    public void onResume() {
        mIsForegroundActivity = true;
        if (mProcessor != null)
            mProcessor.onResume(this);
        cleanUnreadStatus();
        if (RongConfigCenter.featureConfig().rc_wipe_out_notification_message) {
            clearAllNotification();
        }
    }

    public void onPause() {
        stopPlay();
    }

    public void onStop() {
        mIsForegroundActivity = false;
    }

    public void onDestroy() {
        stopPlay();
        MessageItemLongClickActionManager.getInstance().removeMessageItemLongClickAction(mMoreAction);
        mMoreAction = null;
        if (mProcessor != null)
            mProcessor.onDestroy(this);
    }
}
