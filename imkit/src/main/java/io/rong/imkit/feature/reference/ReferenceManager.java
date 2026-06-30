package io.rong.imkit.feature.reference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import io.rong.common.rlog.RLog;
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
import io.rong.imkit.feature.editmessage.EditMessageManager;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.ExecutorFactory;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.QuoteInfo;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

public class ReferenceManager implements IExtensionModule, IExtensionEventWatcher {
    private final String TAG = ReferenceManager.class.getSimpleName();
    private ReferenceMessage mReferenceMessage;
    private WeakReference<RongExtension> mRongExtension;
    private WeakReference<Fragment> mFragment;
    private UiMessage mUiMessage;
    private volatile QuoteInfo pendingPluginQuoteInfo;
    private volatile String pendingPluginQuoteObjectName;
    private Stack<ReferenceInstance> stack = new Stack<>();
    private WeakReference<RongExtensionViewModel> messageViewModel;
    private List<ReferenceStatusListener> mReferenceStatusListenerList =
            new CopyOnWriteArrayList<>();

    // 返回值上层没有使用
    private MessageItemLongClickAction mClickActionReference =
            new MessageItemLongClickAction.Builder()
                    .titleResId(R.string.rc_reference)
                    .iconResId(R.attr.rc_conversation_menu_item_reference_img)
                    .actionListener(this::showReferenceViewInEditMode)
                    .showFilter(
                            new MessageItemLongClickAction.Filter() {
                                @Override
                                public boolean filter(UiMessage uiMessage) {
                                    // 过滤失败消息
                                    if (mRongExtension == null) {
                                        return false;
                                    }
                                    RongExtension rongExtension = mRongExtension.get();
                                    if (rongExtension == null) {
                                        return false;
                                    }
                                    Message message = uiMessage.getMessage();
                                    boolean isSuccess =
                                            message.getSentStatus() != Message.SentStatus.CANCELED
                                                    && message.getSentStatus()
                                                            != Message.SentStatus.FAILED
                                                    && message.getSentStatus()
                                                            != Message.SentStatus.SENDING;
                                    boolean forbidConversationType =
                                            message.getConversationType()
                                                            .equals(
                                                                    Conversation.ConversationType
                                                                            .ENCRYPTED)
                                                    || message.getConversationType()
                                                            .equals(
                                                                    Conversation.ConversationType
                                                                            .APP_PUBLIC_SERVICE)
                                                    || message.getConversationType()
                                                            .equals(
                                                                    Conversation.ConversationType
                                                                            .PUBLIC_SERVICE)
                                                    || message.getConversationType()
                                                            .equals(
                                                                    Conversation.ConversationType
                                                                            .SYSTEM)
                                                    || message.getConversationType()
                                                            .equals(
                                                                    Conversation.ConversationType
                                                                            .CUSTOMER_SERVICE);
                                    boolean isFireMsg = message.getContent().isDestruct();
                                    boolean isFireMode =
                                            RongExtensionCacheHelper.isDestructMode(
                                                    rongExtension.getContext(),
                                                    rongExtension.getConversationType(),
                                                    rongExtension.getTargetId());
                                    boolean isEnableReferenceMsg =
                                            RongConfigCenter.featureConfig().isReferenceEnable();
                                    boolean hasValidQuoteV2Uid =
                                            !RongConfigCenter.featureConfig().isQuoteV2Enable()
                                                    || !TextUtils.isEmpty(message.getUId());
                                    boolean shouldShowByFilter = false;
                                    try {
                                        shouldShowByFilter =
                                                RongConfigCenter.conversationConfig()
                                                        .getReferenceMenuItemFilter()
                                                        .shouldShowReferenceMenuItem(uiMessage);
                                    } catch (Exception e) {
                                        RLog.e(TAG, "ReferenceMenuItemFilter error", e);
                                    }
                                    boolean isReferenceable;
                                    if (RongConfigCenter.featureConfig().isQuoteV2Enable()) {
                                        isReferenceable = hasValidQuoteV2Uid && shouldShowByFilter;
                                    } else {
                                        isReferenceable = shouldShowByFilter;
                                    }
                                    return isSuccess
                                            && isEnableReferenceMsg
                                            && isReferenceable
                                            && !forbidConversationType
                                            && !isFireMsg & !isFireMode;
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
        // do nothing
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, final RongExtension extension) {
        if (fragment == null
                || fragment.isDetached()
                || fragment.getContext() == null
                || fragment.getFragmentManager() == null) {
            return;
        }
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
        messageViewModel =
                new WeakReference<>(
                        new ViewModelProvider(fragment).get(RongExtensionViewModel.class));
        RongExtensionViewModel extensionViewModel = messageViewModel.get();
        if (extensionViewModel != null) {
            extensionViewModel
                    .getInputModeLiveData()
                    .observe(
                            fragment,
                            new Observer<InputMode>() {
                                @Override
                                public void onChanged(InputMode inputMode) {
                                    // V2 开启时切换到语音/扩展面板仍保持引用条
                                    if (inputMode.equals(InputMode.VoiceInput)
                                            && !RongConfigCenter.featureConfig()
                                                    .isQuoteV2Enable()) {
                                        hideReferenceView();
                                    }
                                }
                            });
        }

        MessageItemLongClickActionManager.getInstance()
                .addMessageItemLongClickAction(mClickActionReference);

        RongExtensionManager.getInstance().addExtensionEventWatcher(this);
        IMCenter.getInstance().addOnRecallMessageListener(mRecallMessageListener);
        IMCenter.getInstance().addMessageEventListener(mMessageEventListener);
    }

    private MessageEventListener mMessageEventListener =
            new MessageEventListener() {

                @Override
                public void onSendMessage(SendEvent event) {
                    // do nothing
                }

                @Override
                public void onSendMediaMessage(SendMediaEvent event) {
                    // do nothing
                }

                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    // do nothing
                }

                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    if (mUiMessage != null
                            && mUiMessage.getMessage() != null
                            && event != null
                            && event.getMessageIds() != null
                            && mUiMessage
                                    .getMessage()
                                    .getConversationType()
                                    .equals(event.getConversationType())
                            && mUiMessage.getMessage().getTargetId().equals(event.getTargetId())) {
                        int messageId = mUiMessage.getMessage().getMessageId();
                        for (int id : event.getMessageIds()) {
                            if (id == messageId) {
                                hideReferenceView();
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onRecallEvent(RecallEvent event) {
                    if (mUiMessage != null
                            && event != null
                            && mUiMessage.getMessage() != null
                            && mUiMessage
                                    .getMessage()
                                    .getConversationType()
                                    .equals(event.getConversationType())
                            && mUiMessage.getMessage().getTargetId().equals(event.getTargetId())
                            && mUiMessage.getMessage().getMessageId() == event.getMessageId()) {
                        hideReferenceView();
                    }
                }

                @Override
                public void onRefreshEvent(RefreshEvent event) {
                    // do nothing
                }

                @Override
                public void onInsertMessage(InsertEvent event) {
                    // do nothing
                }

                @Override
                public void onClearMessages(ClearEvent event) {
                    // do nothing
                }
            };

    private RongIMClient.OnRecallMessageListener mRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (mFragment == null) {
                        return false;
                    }
                    Fragment fragment = mFragment.get();
                    if (fragment == null) {
                        return false;
                    }
                    if (mUiMessage != null
                            && message != null
                            && !TextUtils.isEmpty(mUiMessage.getUId())
                            && mUiMessage.getMessage().getUId().equals(message.getUId())) {
                        if (fragment.getActivity() == null || fragment.getContext() == null) {
                            return false;
                        }
                        new AlertDialog.Builder(
                                        fragment.getActivity(),
                                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                .setMessage(
                                        fragment.getContext().getString(R.string.rc_recall_success))
                                .setPositiveButton(
                                        fragment.getContext().getString(R.string.rc_dialog_ok),
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                        hideReferenceView();
                        if (messageViewModel != null) {
                            RongExtensionViewModel viewModel = messageViewModel.get();
                            if (viewModel != null) {
                                viewModel.collapseExtensionBoard();
                            }
                        }
                    }
                    return false;
                }
            };

    @Override
    public void onDetachedFromExtension() {
        // do nothing
    }

    @Override
    public void onReceivedMessage(Message message) {
        // do nothing
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
        // do nothing
    }

    @Override
    public void onTextChanged(
            Context context,
            Conversation.ConversationType type,
            String targetId,
            int cursorPos,
            int count,
            String text) {
        // do nothing
    }

    @Override
    public void onSendToggleClick(Message message) {
        if (mUiMessage == null || mReferenceMessage == null) {
            return;
        }
        // V2 路径：在 Message 上设置 quoteInfo，不替换消息类型
        if (RongConfigCenter.featureConfig().isQuoteV2Enable()) {
            // 白名单校验：不在白名单中的消息类型不附加 quoteInfo，引用面板保持不消失
            if (isQuoteReplySupportedForObjectName(resolveObjectName(message))) {
                applyQuoteInfoToMessage(message);
                hideReferenceView();
            }
            return;
        }
        // V1 路径：包装为 ReferenceMessage
        if (!(message.getContent() instanceof TextMessage)) {
            RLog.e(TAG, "primary message content must be TextMessage");
            return;
        }
        String primaryString = ((TextMessage) message.getContent()).getContent();
        mReferenceMessage.buildSendText(primaryString);
        mReferenceMessage.setMentionedInfo(message.getContent().getMentionedInfo());
        message.setContent(mReferenceMessage);
        hideReferenceView();
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
            IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
            MessageItemLongClickActionManager.getInstance()
                    .removeMessageItemLongClickAction(mClickActionReference);
            IMCenter.getInstance().removeMessageEventListener(mMessageEventListener);
            RongExtensionManager.getInstance().removeExtensionEventWatcher(this);
            mRongExtension = null;
            mFragment = null;
        }
    }

    /**
     * 显示引用消息栏
     *
     * @param context 上下文
     * @param uiMessage 消息实体
     * @return 是否显示成功
     */
    public boolean showReferenceView(Context context, UiMessage uiMessage) {
        if (EditMessageManager.getInstance().isEditMessageState()) {
            return false;
        }
        // 如果context为空，则尝试使用mFragment的Context
        if (context == null && mFragment != null) {
            Fragment frag = mFragment.get();
            if (frag != null && !frag.isDetached()) {
                context = frag.getContext();
            }
        }
        return showReferenceView(context, uiMessage, true);
    }

    private boolean showReferenceViewInEditMode(Context context, UiMessage uiMessage) {
        if (EditMessageManager.getInstance().isEditMessageState()) {
            // 编辑模式下，需延迟退出编辑模式、展示引用UI，否则软键盘会快速落下再弹起。
            postDelayed(
                    () -> {
                        EditMessageManager.getInstance().exitEditMode();
                        postDelayed(() -> showReferenceView(context, uiMessage, false));
                    });
        } else {
            showReferenceView(context, uiMessage);
        }
        return true;
    }

    /**
     * 显示引用消息栏
     *
     * @param context 上下文
     * @param uiMessage 消息实体
     * @return 是否显示成功
     */
    public boolean showReferenceView(Context context, UiMessage uiMessage, boolean showKeyBoard) {
        if (mRongExtension == null
                || mFragment == null
                || context == null
                || uiMessage == null
                || uiMessage.getMessage() == null) {
            return false;
        }
        // 如果是提示消息，不显示引用消息
        if (Objects.equals(uiMessage.getMessage().getObjectName(), "RC:RcNtf")) {
            return false;
        }
        RongExtension rongExtension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (rongExtension == null
                || fragment == null
                || fragment.isDetached()
                || fragment.getContext() == null
                || fragment.getFragmentManager() == null) {
            return false;
        }
        mUiMessage = uiMessage;
        mReferenceMessage =
                ReferenceMessage.obtainMessage(
                        uiMessage.getMessage().getSenderUserId(),
                        uiMessage.getMessage().getContent(),
                        uiMessage.getUId());
        if (uiMessage.getMessage().isHasChanged()) {
            mReferenceMessage.setReferMsgStatus(ReferenceMessage.ReferenceMessageStatus.MODIFIED);
        }
        View attachedInfo = createCustomReferenceInputBarView(context, rongExtension, uiMessage);
        if (attachedInfo == null) {
            ReferenceView reference =
                    new ReferenceView(
                            context,
                            rongExtension.getContainer(RongExtension.ContainerType.ATTACH),
                            uiMessage);
            reference.setReferenceCancelListener(this::hideReferenceView);
            attachedInfo = reference.getReferenceView();
        }
        rongExtension.setAttachedInfo(attachedInfo);
        final RongExtensionViewModel extensionViewModel =
                new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        extensionViewModel.getInputModeLiveData().postValue(InputMode.TextInput);
        if (showKeyBoard) {
            rongExtension.postDelayed(() -> extensionViewModel.setSoftInputKeyBoard(true), 100);
        }
        return true;
    }

    private View createCustomReferenceInputBarView(
            Context context, RongExtension rongExtension, UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        ReferenceContentInputBarProvider provider =
                RongConfigCenter.conversationConfig()
                        .getReferenceContentInputBarProvider(
                                resolveObjectName(message), message.getContent());
        if (provider == null) {
            return null;
        }
        try {
            ViewGroup parent = rongExtension.getContainer(RongExtension.ContainerType.ATTACH);
            View customView = provider.onCreateView(context, parent, this::hideReferenceView);
            if (customView == null) {
                return null;
            }
            provider.onBindView(customView, message, message.getContent());
            return customView;
        } catch (Exception e) {
            RLog.e(TAG, "createCustomReferenceInputBarView error", e);
            return null;
        }
    }

    private void postDelayed(Runnable r) {
        ExecutorFactory.getInstance().getMainHandler().postDelayed(r, 100);
    }

    /**
     * V2 引用回复开启时，将当前引用态附加到待发送消息上。 供媒体/插件发送路径在 sendMessage 前调用。
     *
     * @param message 待发送的消息
     * @return 是否成功附加了引用信息
     */
    public boolean applyQuoteInfoIfActive(Message message) {
        if (!RongConfigCenter.featureConfig().isQuoteV2Enable()) {
            return false;
        }
        if (message == null) {
            return false;
        }
        QuoteInfo quoteInfo = pendingPluginQuoteInfo;
        boolean usePendingQuoteInfo = quoteInfo != null;
        if (!usePendingQuoteInfo) {
            quoteInfo = buildQuoteInfoFromUiMessage(mUiMessage);
        }
        if (quoteInfo == null) {
            return false;
        }
        String objectName = resolveObjectName(message);
        if (usePendingQuoteInfo && !TextUtils.equals(objectName, pendingPluginQuoteObjectName)) {
            return false;
        }
        // 白名单校验：待发送消息类型不在白名单中则不附加 quoteInfo
        if (!isQuoteReplySupportedForObjectName(objectName)) {
            if (usePendingQuoteInfo) {
                clearPendingPluginQuoteInfo();
            }
            return false;
        }
        message.setQuoteInfo(quoteInfo);
        if (usePendingQuoteInfo) {
            clearPendingPluginQuoteInfo();
        } else {
            hideReferenceViewAfterQuotedPluginSend();
        }
        return true;
    }

    private void hideReferenceViewAfterQuotedPluginSend() {
        Runnable task =
                () -> {
                    hideReferenceView();
                    collapsePluginBoardAfterQuotedPluginSend();
                };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            ExecutorFactory.getInstance().getMainHandler().post(task);
        }
    }

    public boolean prepareQuotedFilePluginSend() {
        if (!RongConfigCenter.featureConfig().isQuoteV2Enable()
                || !isQuoteReplySupportedForObjectName("RC:FileMsg")) {
            return false;
        }
        QuoteInfo quoteInfo = buildQuoteInfoFromUiMessage(mUiMessage);
        if (quoteInfo == null) {
            return false;
        }
        pendingPluginQuoteInfo = quoteInfo;
        pendingPluginQuoteObjectName = "RC:FileMsg";
        Runnable task =
                () -> {
                    hideReferenceView();
                    collapsePluginBoardAfterQuotedPluginSend();
                };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            ExecutorFactory.getInstance().getMainHandler().post(task);
        }
        return true;
    }

    public void cancelPendingQuotedPluginSend() {
        clearPendingPluginQuoteInfo();
    }

    private void clearPendingPluginQuoteInfo() {
        pendingPluginQuoteInfo = null;
        pendingPluginQuoteObjectName = null;
    }

    private void collapsePluginBoardAfterQuotedPluginSend() {
        if (messageViewModel == null) {
            return;
        }
        RongExtensionViewModel viewModel = messageViewModel.get();
        if (viewModel == null || viewModel.getInputModeLiveData() == null) {
            return;
        }
        InputMode inputMode = viewModel.getInputModeLiveData().getValue();
        if (inputMode == InputMode.PluginMode) {
            viewModel.setSoftInputKeyBoard(false);
            viewModel.getExtensionBoardState().setValue(false);
            viewModel.getInputModeLiveData().setValue(InputMode.NormalMode);
        }
    }

    /**
     * 从 Message 中获取 objectName，对齐 iOS quoteObjectNameForMessage。 Message.obtain 创建的消息 objectName
     * 可能为 null，需要从 MessageContent 的 {@link io.rong.imlib.MessageTag} 注解回退获取。
     */
    private String resolveObjectName(Message message) {
        if (message == null) {
            return null;
        }
        String objectName = message.getObjectName();
        if (!isEmpty(objectName)) {
            return objectName;
        }
        if (message.getContent() != null) {
            io.rong.imlib.MessageTag tag =
                    message.getContent().getClass().getAnnotation(io.rong.imlib.MessageTag.class);
            if (tag != null) {
                return tag.value();
            }
        }
        return null;
    }

    /**
     * 判断指定消息类型是否在 V2 引用白名单中，对齐 iOS isQuoteReplySupportedForObjectName 逻辑。 语音和高清语音视为等价：配置任一项即两者均可发送。
     */
    private boolean isQuoteReplySupportedForObjectName(String objectName) {
        if (isEmpty(objectName)) {
            return false;
        }
        List<String> whiteList = RongConfigCenter.featureConfig().getQuoteMessageTypeWhiteList();
        if (whiteList == null || whiteList.isEmpty()) {
            return false;
        }
        if (whiteList.contains(objectName)) {
            return true;
        }
        // 语音与高清语音互认：配置其中一个即同时放行另一个
        if ("RC:HQVCMsg".equals(objectName)) {
            return whiteList.contains("RC:VcMsg");
        }
        if ("RC:VcMsg".equals(objectName)) {
            return whiteList.contains("RC:HQVCMsg");
        }
        return false;
    }

    /** 将当前引用态的 quoteInfo 设置到待发送消息上。 */
    private boolean applyQuoteInfoToMessage(Message message) {
        QuoteInfo quoteInfo = buildQuoteInfoFromUiMessage(mUiMessage);
        if (quoteInfo == null) {
            return false;
        }
        message.setQuoteInfo(quoteInfo);
        return true;
    }

    private QuoteInfo buildQuoteInfoFromUiMessage(UiMessage uiMessage) {
        if (uiMessage == null || uiMessage.getMessage() == null) {
            return null;
        }
        Message quotedMsg = uiMessage.getMessage();
        String uid = quotedMsg.getUId();
        if (isEmpty(uid)) {
            return null;
        }
        String senderId = quotedMsg.getSenderUserId();
        String objectName = resolveObjectName(quotedMsg);
        return new QuoteInfo(uid, senderId, objectName);
    }

    private static boolean isEmpty(CharSequence value) {
        return value == null || value.length() == 0;
    }

    public void hideReferenceView() {
        if (mReferenceMessage == null && mUiMessage == null) {
            return;
        }
        mReferenceMessage = null;
        RongExtension rongExtension = null;

        if (mRongExtension != null) {
            rongExtension = mRongExtension.get();
        }

        if (rongExtension != null) {
            rongExtension.setAttachedInfo(null);
        }
        mUiMessage = null;
        for (ReferenceStatusListener listener : mReferenceStatusListenerList) {
            listener.onHide();
        }
    }

    @Override
    public void onDeleteClick(
            Conversation.ConversationType type, String targetId, EditText editText, int cursorPos) {
        // default implementation ignored
    }

    public UiMessage getUiMessage() {
        return mUiMessage;
    }

    public interface ReferenceStatusListener {
        /** 引用消息栏隐藏时回调 */
        void onHide();
    }
}
