package io.rong.imkit.feature.reference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.MessageItemLongClickAction;
import io.rong.imkit.MessageItemLongClickActionManager;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.event.actionevent.ClearEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.event.actionevent.InsertEvent;
import io.rong.imkit.event.actionevent.MessageEventListener;
import io.rong.imkit.event.actionevent.RecallEvent;
import io.rong.imkit.event.actionevent.RefreshEvent;
import io.rong.imkit.event.actionevent.SendEvent;
import io.rong.imkit.event.actionevent.SendMediaEvent;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;

public class ReferenceManager implements IExtensionModule, IExtensionEventWatcher {
    private final String TAG = ReferenceManager.class.getSimpleName();
    private ReferenceMessage mReferenceMessage;
    private WeakReference<RongExtension> mRongExtension;
    private WeakReference<Fragment> mFragment;
    private UiMessage mUiMessage;
    private Stack<ReferenceInstance> stack = new Stack<>();
    private WeakReference<RongExtensionViewModel> messageViewModel;
    private List<ReferenceStatusListener> mReferenceStatusListenerList = new CopyOnWriteArrayList<>();

    private MessageItemLongClickAction mClickActionReference = new MessageItemLongClickAction.Builder()
            .titleResId(R.string.rc_dialog_item_message_reference)
            .actionListener(new MessageItemLongClickAction.MessageItemLongClickListener() {
                @Override
                public boolean onMessageItemLongClick(Context context, UiMessage uiMessage) {
                    if (mRongExtension == null) {
                        return false;
                    }
                    RongExtension rongExtension = mRongExtension.get();
                    Fragment fragment = mFragment.get();
                    if (rongExtension == null || fragment == null) {
                        return false;
                    }
                    mUiMessage = uiMessage;
                    ReferenceView reference = new ReferenceView(context, rongExtension.getContainer(RongExtension.ContainerType.ATTACH), uiMessage);
                    rongExtension.setAttachedInfo(reference.getReferenceView());
                    final RongExtensionViewModel extensionViewModel = new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
                    extensionViewModel.getInputModeLiveData().postValue(InputMode.TextInput);
                    rongExtension.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            extensionViewModel.setSoftInputKeyBoard(true);
                        }
                    }, 100);

                    mReferenceMessage = ReferenceMessage.obtainMessage(uiMessage.getMessage().getSenderUserId(), uiMessage.getMessage().getContent());
                    reference.setReferenceCancelListener(new ReferenceView.ReferenceCancelListener() {
                        @Override
                        public void onCanceled() {
                            hideReferenceView();
                        }
                    });
                    return true;
                }
            }).showFilter(new MessageItemLongClickAction.Filter() {
                @Override
                public boolean filter(UiMessage uiMessage) {
                    //过滤失败消息
                    if (mRongExtension == null) {
                        return false;
                    }
                    RongExtension rongExtension = mRongExtension.get();
                    if (rongExtension == null) {
                        return false;
                    }
                    Message message = uiMessage.getMessage();
                    boolean isSuccess = message.getSentStatus() != Message.SentStatus.CANCELED && message.getSentStatus() != Message.SentStatus.FAILED && message.getSentStatus() != Message.SentStatus.SENDING;
                    boolean forbidConversationType = message.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)
                            || message.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE)
                            || message.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                            || message.getConversationType().equals(Conversation.ConversationType.CUSTOMER_SERVICE);
                    boolean isFireMsg = message.getContent().isDestruct();
                    boolean isFireMode = RongExtensionCacheHelper.isDestructMode(rongExtension.getContext(), rongExtension.getConversationType(), rongExtension.getTargetId());
                    boolean isEnableReferenceMsg = RongConfigCenter.featureConfig().isReferenceEnable();
                    boolean isInstanceOf = (message.getContent() instanceof TextMessage)
                            || (message.getContent() instanceof ImageMessage)
                            || (message.getContent() instanceof FileMessage)
                            || (message.getContent() instanceof RichContentMessage)
                            || (message.getContent() instanceof ReferenceMessage);
                    return isSuccess && isEnableReferenceMsg && isInstanceOf && !forbidConversationType && !isFireMsg & !isFireMode;
                }
            })
            .build();

    private static class SingletonHolder {
        static ReferenceManager instance = new ReferenceManager();
    }

    public static ReferenceManager getInstance() {
        return SingletonHolder.instance;
    }

    public void setReferenceStatusListener(ReferenceStatusListener listener) {
        mReferenceStatusListenerList.add(listener);
    }

    public void removeReferenceStatusListener(ReferenceStatusListener listener) {
        mReferenceStatusListenerList.remove(listener);
    }

    @Override
    public void onInit(Context context, String appKey) {
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, final RongExtension extension) {
        mFragment = new WeakReference<>(fragment);
        mRongExtension = new WeakReference<>(extension);
        ReferenceInstance referenceInstance;
        referenceInstance = new ReferenceInstance();
        referenceInstance.mFragment = mFragment;
        referenceInstance.mRongExtension = mRongExtension;
        stack.add(referenceInstance);
        if (!RongConfigCenter.featureConfig().isReferenceEnable()) {
            return;
        }
        messageViewModel = new WeakReference<>(new ViewModelProvider(fragment).get(RongExtensionViewModel.class));
        messageViewModel.get().getInputModeLiveData().observe(fragment, new Observer<InputMode>() {
            @Override
            public void onChanged(InputMode inputMode) {
                if (inputMode.equals(InputMode.VoiceInput)) {
                    hideReferenceView();
                }
            }
        });

        MessageItemLongClickActionManager.getInstance().addMessageItemLongClickAction(mClickActionReference);

        RongExtensionManager.getInstance().addExtensionEventWatcher(this);
        IMCenter.getInstance().addOnRecallMessageListener(mRecallMessageListener);
        IMCenter.getInstance().addMessageEventListener(mMessageEventListener);
    }

    private MessageEventListener mMessageEventListener = new MessageEventListener() {

        @Override
        public void onSendMessage(SendEvent event) {

        }

        @Override
        public void onSendMediaMessage(SendMediaEvent event) {

        }

        @Override
        public void onDownloadMessage(DownloadEvent event) {

        }

        @Override
        public void onDeleteMessage(DeleteEvent event) {

        }

        @Override
        public void onRecallEvent(RecallEvent event) {
            if (mUiMessage != null && event != null && mUiMessage.getMessage() != null
                    && mUiMessage.getMessage().getConversationType().equals(event.getConversationType())
                    && mUiMessage.getMessage().getTargetId().equals(event.getTargetId())
                    && mUiMessage.getMessage().getMessageId() == event.getMessageId()) {
                hideReferenceView();
            }
        }

        @Override
        public void onRefreshEvent(RefreshEvent event) {

        }

        @Override
        public void onInsertMessage(InsertEvent event) {

        }

        @Override
        public void onClearMessages(ClearEvent event) {

        }
    };

    private RongIMClient.OnRecallMessageListener mRecallMessageListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            Fragment fragment = mFragment.get();
            if (fragment == null) {
                return false;
            }
            if (mUiMessage != null && message != null && !TextUtils.isEmpty(mUiMessage.getUId())
                    && mUiMessage.getMessage().getUId().equals(message.getUId())) {
                if (mFragment == null || fragment.getActivity() == null || fragment.getContext() == null) {
                    return false;
                }
                new AlertDialog.Builder(fragment.getActivity(), AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setMessage(fragment.getContext().getString(R.string.rc_recall_success))
                        .setPositiveButton(fragment.getContext().getString(R.string.rc_dialog_ok), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .show();
                hideReferenceView();
                RongExtensionViewModel viewModel = messageViewModel.get();
                if (viewModel != null) {
                    viewModel.collapseExtensionBoard();
                }
            }
            return false;
        }
    };

    @Override
    public void onDetachedFromExtension() {
    }

    @Override
    public void onReceivedMessage(Message message) {

    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        return null;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {

    }


    @Override
    public void onTextChanged(Context context, Conversation.ConversationType type, String targetId, int cursorPos, int count, String text) {
    }

    @Override
    public void onSendToggleClick(Message message) {
        if (!(message.getContent() instanceof TextMessage)) {
            RLog.e(TAG, "primary message content must be TextMessage");
            return;
        }
        String primaryString = ((TextMessage) message.getContent()).getContent();
        if (mReferenceMessage != null) {
            mReferenceMessage.buildSendText(primaryString);
            mReferenceMessage.setMentionedInfo(message.getContent().getMentionedInfo());
            message.setContent(mReferenceMessage);
            hideReferenceView();
        }
    }

    @Override
    public void onDestroy(Conversation.ConversationType type, String targetId) {
        mReferenceMessage = null;
        mUiMessage = null;
        stack.pop();
        if (stack.size() > 0) {
            ReferenceInstance referenceInstance = stack.peek();
            mRongExtension = referenceInstance.mRongExtension;
            mFragment = referenceInstance.mFragment;
            referenceInstance.mRongExtension = null;
            referenceInstance.mFragment = null;
        } else {
            mRongExtension = null;
            mFragment = null;
            MessageItemLongClickActionManager.getInstance().removeMessageItemLongClickAction(mClickActionReference);
            IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
            IMCenter.getInstance().removeMessageEventListener(mMessageEventListener);
            RongExtensionManager.getInstance().removeExtensionEventWatcher(this);
        }
    }

    private void hideReferenceView() {
        mReferenceMessage = null;
        RongExtension rongExtension = null;

        if (mRongExtension != null) {
            rongExtension = mRongExtension.get();
        }

        if (rongExtension != null) {
            rongExtension.setAttachedInfo(null);
        }
        for (ReferenceStatusListener listener : mReferenceStatusListenerList) {
            listener.onHide();
        }
        mUiMessage = null;
    }

    @Override
    public void onDeleteClick(Conversation.ConversationType type, String targetId, EditText editText, int cursorPos) {

    }

    public UiMessage getUiMessage() {
        return mUiMessage;
    }

    public interface ReferenceStatusListener {
        /**
         * 引用消息栏隐藏时回调
         */
        void onHide();
    }
}
