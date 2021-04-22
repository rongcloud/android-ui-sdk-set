package io.rong.imkit;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.message.RealTimeLocationStartMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UnknownMessage;
import io.rong.imlib.publicservice.message.PublicServiceMultiRichContentMessage;
import io.rong.imlib.publicservice.message.PublicServiceRichContentMessage;
import io.rong.message.HQVoiceMessage;
import io.rong.message.HandshakeMessage;
import io.rong.message.NotificationMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;
import io.rong.message.VoiceMessage;

import static android.widget.Toast.makeText;

/**
 * Created by jiangecho on 2017/3/17.
 */

public class MessageItemLongClickActionManager {

    private static final String TAG = MessageItemLongClickActionManager.class.getSimpleName();

    private OptionsPopupDialog mDialog;
    private Message mLongClickMessage;

    private static class Holder {
        static MessageItemLongClickActionManager instance = new MessageItemLongClickActionManager();
    }

    public static MessageItemLongClickActionManager getInstance() {
        return Holder.instance;
    }

    private MessageItemLongClickActionManager() {
        if (messageItemLongClickActions == null) {
            messageItemLongClickActions = new ArrayList<>();
            initCommonMessageItemLongClickActions();
        }
    }


    public void setLongClickMessage(Message message) {
        mLongClickMessage = message;
    }

    public Message getLongClickMessage() {
        return mLongClickMessage;
    }

    public void setLongClickDialog(OptionsPopupDialog dialog) {
        mDialog = dialog;
    }

    public OptionsPopupDialog getLongClickDialog() {
        return mDialog;
    }


    private List<MessageItemLongClickAction> messageItemLongClickActions;


    public List<MessageItemLongClickAction> getMessageItemLongClickActions() {
        return messageItemLongClickActions;
    }

    public void addMessageItemLongClickAction(MessageItemLongClickAction action) {
        addMessageItemLongClickAction(action, -1);
    }

    public void addMessageItemLongClickAction(MessageItemLongClickAction action, int index) {
        messageItemLongClickActions.remove(action);

        if (index < 0) {
            messageItemLongClickActions.add(action);
        } else {
            messageItemLongClickActions.add(index, action);
        }
    }

    public void removeMessageItemLongClickAction(MessageItemLongClickAction action) {
        messageItemLongClickActions.remove(action);
    }

    /**
     * 本方法应当只能被ItemProvider调用, 如果想修改默认的长按弹出菜单，请调用getMessageItemLongClickActions()
     *
     * @param uiMessage
     * @return
     */
    public List<MessageItemLongClickAction> getMessageItemLongClickActions(UiMessage uiMessage) {
        List<MessageItemLongClickAction> actions = new ArrayList<>();
        for (MessageItemLongClickAction action : messageItemLongClickActions) {
            if (action.filter(uiMessage)) {
                actions.add(action);
            }
        }
        Collections.sort(actions, new Comparator<MessageItemLongClickAction>() {
            @Override
            public int compare(MessageItemLongClickAction t1, MessageItemLongClickAction t2) {
                if (t1.priority > t2.priority) {
                    return 1;
                }
                if (t1.priority == t2.priority) {
                    return 0;
                }
                return -1;
            }
        });
        return actions;
    }

    // T5051: 会话界面中，优化消息长按后的功能顺序为：复制、删除、撤回、引用、更多
    private void initCommonMessageItemLongClickActions() {
        MessageItemLongClickAction messageItemLongClickAction = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_dialog_item_message_copy)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(Context context, UiMessage uiMessage) {
                        Message message = uiMessage.getMessage();
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (message.getContent() instanceof RecallNotificationMessage) {
                            return false;
                        } else {
                            if (message.getContent() instanceof TextMessage) {
                                if (clipboard != null) {
                                    clipboard.setText(((TextMessage) message.getContent()).getContent());
                                }
                            } else if (message.getContent() instanceof ReferenceMessage) {
                                ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
                                if (referenceMessage == null) {
                                    return false;
                                }
                                if (clipboard != null) {
                                    clipboard.setText(referenceMessage.getEditSendText());
                                }
                            }
                            return true;
                        }
                    }
                })
                .showFilter(new MessageItemLongClickAction.Filter() {
                    @Override
                    public boolean filter(UiMessage uiMessage) {
                        Message message = uiMessage.getMessage();
                        return (message.getContent() instanceof TextMessage || message.getContent() instanceof ReferenceMessage) &&
                                !message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED) && !message.getContent().isDestruct();
                    }
                })
                .build();
        addMessageItemLongClickAction(messageItemLongClickAction);

        messageItemLongClickAction = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_dialog_item_message_delete)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(Context context, UiMessage uiMessage) {
                        Message message = uiMessage.getMessage();
                        if (message.getContent() instanceof VoiceMessage) {
                            Uri uri = ((VoiceMessage) message.getContent()).getUri();
                            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                            if (playingUri != null && playingUri == uri) {
                                AudioPlayManager.getInstance().stopPlay();
                            }
                        } else if (message.getContent() instanceof HQVoiceMessage) {
                            Uri playingUri = AudioPlayManager.getInstance().getPlayingUri();
                            if (playingUri.equals(((HQVoiceMessage) message.getContent()).getLocalPath())) {
                                AudioPlayManager.getInstance().stopPlay();
                            }
                        }
                        IMCenter.getInstance().deleteMessages(uiMessage.getMessage().getConversationType(), uiMessage.getMessage().getTargetId(), new int[]{uiMessage.getMessage().getMessageId()}, null);
                        return true;
                    }
                }).build();
        addMessageItemLongClickAction(messageItemLongClickAction);
        messageItemLongClickAction = new MessageItemLongClickAction.Builder()
                .titleResId(R.string.rc_dialog_item_message_recall)
                .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                    @Override
                    public boolean onMessageItemLongClick(final Context context, UiMessage uiMessage) {
                        if (IMCenter.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE) {
                            makeText(context, context.getResources().getString(R.string.rc_recall_failed_for_network_unavailable), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                        long deltaTime = RongIMClient.getInstance().getDeltaTime();
                        long normalTime = System.currentTimeMillis() - deltaTime;
                        int messageRecallInterval = -1;
                        boolean needRecall = false;
                        try {
                            messageRecallInterval = RongConfigCenter.conversationConfig().rc_message_recall_interval;
                        } catch (Resources.NotFoundException e) {
                            RLog.e(TAG, "rc_message_recall_interval not configure in rc_config.xml");
                            e.printStackTrace();
                        }
                        Message message = uiMessage.getMessage();
                        needRecall = (normalTime - message.getSentTime()) <= messageRecallInterval * 1000;
                        if (needRecall) {
                            IMCenter.getInstance().recallMessage(uiMessage.getMessage(), null, new RongIMClient.ResultCallback() {
                                @Override
                                public void onSuccess(Object o) {

                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    makeText(context, context.getResources().getString(R.string.rc_recall_failed_for_network_unavailable), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            new AlertDialog.Builder(context)
                                    .setMessage(R.string.rc_recall_overtime)
                                    .setPositiveButton(R.string.rc_confirm, null)
                                    .setCancelable(false)
                                    .create()
                                    .show();
                            RLog.e(TAG, "Failed to withdraw message");
                        }
                        return true;
                    }
                })
                .showFilter(new MessageItemLongClickAction.Filter() {
                    @Override
                    public boolean filter(UiMessage uiMessage) {
                        Message message = uiMessage.getMessage();
                        if (message.getContent() instanceof NotificationMessage
                                || message.getContent() instanceof HandshakeMessage
                                || message.getContent() instanceof PublicServiceRichContentMessage
                                || message.getContent() instanceof RealTimeLocationStartMessage
                                || message.getContent() instanceof UnknownMessage
                                || message.getContent() instanceof PublicServiceMultiRichContentMessage
                                || message.getSentStatus().equals(Message.SentStatus.CANCELED) ||
                                message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)) {
                            return false;
                        }

                        long deltaTime = RongIMClient.getInstance().getDeltaTime();
                        long normalTime = System.currentTimeMillis() - deltaTime;
                        boolean enableMessageRecall = false;
                        int messageRecallInterval = -1;
                        boolean hasSent = (!message.getSentStatus().equals(Message.SentStatus.SENDING)) && (!message.getSentStatus().equals(Message.SentStatus.FAILED));

                        try {
                            enableMessageRecall = RongConfigCenter.conversationConfig().rc_enable_recall_message;
                            messageRecallInterval = RongConfigCenter.conversationConfig().rc_message_recall_interval;
                        } catch (Resources.NotFoundException e) {
                            RLog.e(TAG, "rc_message_recall_interval not configure in rc_config.xml");
                            e.printStackTrace();
                        }
                        return hasSent
                                && enableMessageRecall
                                && (normalTime - message.getSentTime()) <= messageRecallInterval * 1000
                                && message.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())
                                && !message.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                                && !message.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)
                                && !message.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                                && !message.getConversationType().equals(Conversation.ConversationType.SYSTEM)
                                && !message.getConversationType().equals(Conversation.ConversationType.CHATROOM);
                    }
                })
                .build();
        addMessageItemLongClickAction(messageItemLongClickAction);
    }
}
