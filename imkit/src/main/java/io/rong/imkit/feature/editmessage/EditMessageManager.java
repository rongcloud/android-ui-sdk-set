package io.rong.imkit.feature.editmessage;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
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
import io.rong.imkit.event.actionevent.RefreshEvent;
import io.rong.imkit.feature.mention.DraftHelper;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.feature.mention.MentionBlock;
import io.rong.imkit.feature.mention.MentionInstance;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.feature.reference.ReferenceManager;
import io.rong.imkit.handler.EditMessageHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.utils.keyboard.KeyboardHeightObserver;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.MessageModifyInfo;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.common.ExecutorFactory;
import io.rong.imlib.model.AppSettings;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.params.ModifyMessageParams;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

public class EditMessageManager implements IExtensionEventWatcher, IExtensionModule {
    public static final String TAG = "EditMessageManager";
    private static final int MAX_MESSAGE_LENGTH_TO_SEND = 5500;
    private WeakReference<RongExtension> mRongExtension;
    private WeakReference<Fragment> mFragment;
    private final Stack<EditMessageState> stack = new Stack<>();
    private final MessageItemLongClickAction mClickActionEditMessage =
            new MessageItemLongClickAction.Builder()
                    .titleResId(R.string.rc_edit)
                    .iconResId(R.attr.rc_conversation_menu_item_edit_img)
                    .actionListener(this::onMessageItemLongClick)
                    .showFilter(this::isFilter)
                    .build();
    // 观察软键盘高度
    private final List<KeyboardHeightObserver> mObservers = new ArrayList<>();
    private AppSettings settings = new AppSettings();
    private final DraftHelper draftHelper = new DraftHelper();
    private final IRongCoreListener.ConnectionStatusListener connectionStatusListener =
            new IRongCoreListener.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus status) {
                    if (status.equals(ConnectionStatus.CONNECTED)) {
                        ExecutorHelper.getInstance()
                                .compressExecutor()
                                .execute(
                                        () ->
                                                settings =
                                                        RongCoreClient.getInstance()
                                                                .getAppSettings());
                    }
                }
            };
    // 当前是否全屏编辑状态，并且Emoji面板展示状态是true
    private boolean isEmoticonMode = false;
    private final EditMessageHandler editMessageHandler = new EditMessageHandler();
    private List<StatusListener> mStatusListenerList = new CopyOnWriteArrayList<>();

    private EditMessageManager() {
        RongCoreClient.addConnectionStatusListener(connectionStatusListener);
    }

    private static class Holder {
        private static final EditMessageManager INSTANCE = new EditMessageManager();
    }

    public static EditMessageManager getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void onInit(Context context, String appKey) {}

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        if (fragment == null
                || fragment.isDetached()
                || fragment.getContext() == null
                || fragment.getFragmentManager() == null) {
            return;
        }
        // 保存Fragment、RongExtension、RongExtensionViewModel
        mFragment = new WeakReference<>(fragment);
        mRongExtension = new WeakReference<>(extension);
        // 保存到Stack
        EditMessageState editMessageInstance = new EditMessageState();
        editMessageInstance.mFragment = mFragment;
        editMessageInstance.mRongExtension = mRongExtension;
        stack.add(editMessageInstance);
        // 添加监听
        MessageItemLongClickActionManager.getInstance()
                .addMessageItemLongClickAction(mClickActionEditMessage);
        RongExtensionManager.getInstance().addExtensionEventWatcher(this);
        // 根据上次保存的编辑配置，来决定是否激活编辑组件
        editMessageHandler.resumeEditMode(
                extension.getConversationIdentifier(),
                EditMessageManager.ActiveType.OnAttachedToExtension);
    }

    /** 处理恢复编辑状态结果，根据不同的类型 ActiveType 处理不同的逻辑 */
    public void onResumeEditModeResult(ActiveType type, EditMessageConfig config) {
        if (type == EditMessageManager.ActiveType.OnCancelMultiSelectStatus) {
            activeEditMode(type, config, false);
        } else if (type == EditMessageManager.ActiveType.OnAttachedToExtension) {
            activeEditMode(type, config, true);
        }
    }

    @Override
    public void onDetachedFromExtension() {}

    @Override
    public void onReceivedMessage(Message message) {}

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        return Collections.emptyList();
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return Collections.emptyList();
    }

    @Override
    public void onDisconnect() {}

    // 初始化上下文，mFragment、mRongExtension
    private void initContext() {
        if ((mRongExtension == null || mFragment == null) && !stack.isEmpty()) {
            EditMessageState topState = stack.peek();
            mRongExtension = topState.mRongExtension;
            mFragment = topState.mFragment;
        }
    }

    private boolean onMessageItemLongClick(Context context, UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        EditMessageConfig config = new EditMessageConfig();
        config.uid = uiMessage.getUId();
        config.content = EditMessageUtils.getOriginalContent(message);
        String referContent = EditMessageUtils.getReferContent(message);
        if (!TextUtils.isEmpty(referContent)) {
            String name = EditMessageUtils.getDisplayName(message);
            config.referContent = name + ":" + referContent;
        }
        if (message.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
            config.referStatus = referenceMessage.getReferMsgStatus();
            config.referUid = referenceMessage.getReferMsgUid();
        }
        config.sentTime = uiMessage.getSentTime();
        config.mentionBlocks = getMentionBlocks(uiMessage);
        activeEditMode(ActiveType.OnLongClickMessage, config, true);
        return true;
    }

    /**
     * 激活编辑消息模式
     *
     * @param type 激活类型
     * @param config 编辑消息配置
     * @param showKeyBoard 是否展示软键盘
     */
    public void activeEditMode(ActiveType type, EditMessageConfig config, boolean showKeyBoard) {
        if (config == null || TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content)) {
            return;
        }
        initContext();
        if (mRongExtension == null || mFragment == null) {
            return;
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        EditMessageState lastState = stack.peek();
        String lastUid = lastState.config != null ? lastState.config.uid : "";
        // 如果正在编辑消息中，判断新编辑的消息UID和正在编辑的消息UID是否相同
        if (!TextUtils.isEmpty(lastUid)) {
            // 长按消息编辑，uid相同，忽略
            if (type == ActiveType.OnLongClickMessage && TextUtils.equals(lastUid, config.uid)) {
                return;
            }
            // 不同，则弹出确认对话框
            if (!TextUtils.equals(lastUid, config.uid)) {
                EditMessageDialog.OnClickListener listener =
                        (v, b) -> postDelayed(() -> activeEditModeReally(config, showKeyBoard));
                EditMessageDialog dialog =
                        EditMessageDialog.newInstance(activity)
                                .setTitleText(R.string.rc_prompt)
                                .setContentMessage(R.string.rc_dialog_edit_message_content)
                                .setButtonText(R.string.rc_dialog_ok, R.string.rc_back)
                                .setOnClickListener(listener);
                dialog.show();
                return;
            }
        }
        postDelayed(() -> activeEditModeReally(config, showKeyBoard));
    }

    private void activeEditModeReally(EditMessageConfig config, boolean showKeyBoard) {
        if (config == null || TextUtils.isEmpty(config.uid) || TextUtils.isEmpty(config.content)) {
            return;
        }
        initContext();
        if (mRongExtension == null || mFragment == null) {
            return;
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        Activity activity = fragment.getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        // 调用InputPanel的onPause保存草稿。
        // 必须在添加编辑UI前（EditMessageInputPanel）、隐藏引用UI前（hideReferenceView），否则引用消息被清除。
        extension.getInputPanel().onPause();
        // 添加视图
        RelativeLayout container = extension.getContainer(RongExtension.ContainerType.INPUT);
        EditMessageInputPanel mEditMessageInputPanel =
                new EditMessageInputPanel(
                        fragment,
                        container,
                        extension.getConversationIdentifier(),
                        config.referUid,
                        config.uid);
        if (mEditMessageInputPanel.getRootView() == null) {
            return;
        }
        // 移除默认的InputPanel布局
        container.removeAllViews();
        // 添加编辑消息输入框布局
        container.addView(mEditMessageInputPanel.getRootView());
        // 找到ViewModel
        RongExtensionViewModel mExtensionViewModel =
                new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        // ViewModel绑定编辑消息输入框布局的Edittext
        mExtensionViewModel.setEditTextWidget(mEditMessageInputPanel.getEditText());
        // RongMentionManager 重新绑定Edittext对应的MentionList。
        addMentionBlocks(mEditMessageInputPanel.getEditText(), config.mentionBlocks);
        // 延时弹起键盘，避免键盘快速收起弹起动画突兀
        postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mFragment == null) {
                            return;
                        }
                        Fragment fragment = mFragment.get();
                        if (fragment == null
                                || fragment.getActivity() == null
                                || fragment.getActivity().isFinishing()) {
                            return;
                        }
                        // 插入消息内容到编辑消息输入框Edittext，并设置引用消息内容
                        mEditMessageInputPanel.setContent(config, showKeyBoard);
                    }
                });
        // 保存激活状态到内存中
        EditMessageState lastState = stack.peek();
        lastState.config = config;
        // 查询是否可以编辑，根据结果刷新UI
        mEditMessageInputPanel.setCheckMessageModifiableResult(
                checkMessageModifiable(config.sentTime));
        // 清除引用消息组件，必须要放到 setEditMessageConfig 后面
        ReferenceManager.getInstance().hideReferenceView();
        // 保存编辑消息状态
        editMessageHandler.saveEditedMessageDraft(extension.getConversationIdentifier(), config);
        notifyVisibilityChanged(true);
    }

    /** 退出编辑消息状态，清空编辑消息配置，并获取草稿。 */
    public void exitEditMode() {
        initContext();
        RongExtension extension = mRongExtension.get();
        if (extension != null) {
            // 清空内存状态
            if (!stack.isEmpty()) {
                EditMessageState lastState = stack.peek();
                lastState.config = null;
            }
            // 清空激活状态
            editMessageHandler.clearEditedMessageDraft(extension.getConversationIdentifier());
            extension.resetToDefaultView(null, InputMode.TextInput, true);
            postDelayed(() -> extension.getInputPanel().getDraftReally(null));
        }
        notifyVisibilityChanged(false);
    }

    private void notifyVisibilityChanged(boolean isVisible) {
        for (StatusListener listener : this.mStatusListenerList) {
            listener.onVisibilityChanged(isVisible);
        }
    }

    public void onKeyboardHeightChange(int orientation, boolean isOpen, int keyboardHeight) {
        for (KeyboardHeightObserver observer : mObservers) {
            observer.onKeyboardHeightChanged(orientation, isOpen, keyboardHeight);
        }
    }

    /** 是否编辑消息状态 */
    public boolean isEditMessageState() {
        return !EditMessageConfig.isInvalid(getEditMessageConfig());
    }

    /** 是否编辑消息状态 */
    public EditMessageConfig getEditMessageConfig() {
        // 获取内存中的编辑状态，不实时获取Lib状态
        if (!stack.isEmpty()) {
            EditMessageState lastState = stack.peek();
            if (lastState != null && lastState.config != null) {
                return lastState.config;
            }
        }
        return null;
    }

    // 是否全屏展示输入状态，并且是emoji输入状态
    public boolean isEmoticonMode() {
        return isEmoticonMode;
    }

    public void setEmoticonMode(boolean show) {
        this.isEmoticonMode = show;
    }

    /** 修改消息 */
    public void editMessage(EditText editText, IRongCoreCallback.OperationCallback callback) {
        String text = editText.getText().toString();
        if (text.length() > MAX_MESSAGE_LENGTH_TO_SEND) {
            ToastUtils.show(
                    editText.getContext(),
                    editText.getContext().getString(R.string.rc_message_too_long),
                    Toast.LENGTH_SHORT);
            RLog.d(TAG, "The text you entered is too long to send.");
            return;
        }
        if (stack.isEmpty()) {
            RLog.d(TAG, "The stack is empty.");
            return;
        }
        EditMessageState lastState = stack.peek();
        String uid = "";
        if (lastState.config != null && !TextUtils.isEmpty(lastState.config.uid)) {
            uid = lastState.config.uid;
        }
        // 把消息中的@信息处理完，再调用编辑。
        RongCoreClient.getInstance()
                .getMessageByUid(
                        uid,
                        new IRongCoreCallback.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message message) {
                                if (message == null || message.getContent() == null) {
                                    showEditFailedDialog(R.string.rc_dialog_edit_message_is_delete);
                                    return;
                                }
                                if (message.getContent() instanceof RecallNotificationMessage) {
                                    showEditFailedDialog(R.string.rc_dialog_edit_message_is_recall);
                                    return;
                                }
                                boolean checked = checkMessageModifiable(message.getSentTime());
                                if (!checked) {
                                    callback.onError(
                                            IRongCoreEnum.CoreErrorCode
                                                    .RC_MODIFIED_MESSAGE_TIMEOUT);
                                    return;
                                }
                                // 把RongMentionManager保存的MentionedInfo设置到消息中
                                RongMentionManager.getInstance()
                                        .onClickEditMessageConfirm(message, editText);
                                // 给上层回调触发UI刷新，切换为正常输入模式UI。
                                // MentionInstance的Edittext会变为InputPanel的Edittext，
                                callback.onSuccess();
                                // 调用消息编辑接口
                                editMessage(message, text);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                showEditFailedDialog(R.string.rc_dialog_edit_message_is_delete);
                            }
                        });
    }

    public void editMessage(Message message, String editContent) {
        ModifyMessageParams params = null;
        // 重新编辑，通过消息气泡重试
        if (TextUtils.isEmpty(editContent)) {
            MessageModifyInfo modifyInfo = message.getModifyInfo();
            if (modifyInfo != null && modifyInfo.getContent() != null) {
                params = new ModifyMessageParams(message.getUId(), modifyInfo.getContent());
            } else {
                RLog.e(TAG, "editMessage modifyInfo null editContent null" + message.getUId());
            }
        } else {
            // 非重新编辑
            if (message.getContent() instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message.getContent();
                textMessage.setContent(editContent);
                params = new ModifyMessageParams(message.getUId(), textMessage);
            } else if (message.getContent() instanceof ReferenceMessage) {
                ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
                referenceMessage.setEditSendText(editContent);
                params = new ModifyMessageParams(message.getUId(), referenceMessage);
            } else {
                RLog.e(TAG, "editMessage objectName error" + message.getUId() + "," + editContent);
            }
        }
        if (params == null) {
            RLog.e(TAG, "editMessage params " + message.getUId());
            int redId =
                    TextUtils.isEmpty(editContent)
                            ? R.string.rc_edit_status_retry_failed
                            : R.string.rc_edit_status_failed;
            ToastUtils.show(
                    IMCenter.getInstance().getContext(),
                    mFragment.get().getResources().getText(redId),
                    Toast.LENGTH_SHORT);
            return;
        }
        // 更新内存中的状态为 UPDATING
        if (message.getModifyInfo() != null) {
            message.getModifyInfo().setStatus(MessageModifyInfo.MessageModifyStatus.UPDATING);
        } else {
            MessageModifyInfo modifyInfo =
                    new MessageModifyInfo(
                            System.currentTimeMillis(),
                            message.getContent(),
                            MessageModifyInfo.MessageModifyStatus.UPDATING);
            message.setModifyInfo(modifyInfo);
        }
        // MessageModifyStatus更新为 UPDATING，刷新UI
        refreshUIMessage(message);
        // modifyMessageCallback
        IRongCoreCallback.ModifyMessageCallback modifyMessageCallback =
                new IRongCoreCallback.ModifyMessageCallback() {
                    @Override
                    public void onComplete(IRongCoreEnum.CoreErrorCode code, Message result) {
                        // 根据错误码判断是否展示Toast
                        showEditErrorToast(code, editContent);
                        // 消息为空，MessageModifyStatus更新为 SUCCESS，刷新UI
                        if (result == null) {
                            if (message.getModifyInfo() != null) {
                                message.getModifyInfo()
                                        .setStatus(MessageModifyInfo.MessageModifyStatus.SUCCESS);
                            }
                            refreshUIMessage(message);
                            return;
                        }
                        refreshUIMessage(result);
                    }
                };
        RongCoreClient.getInstance().modifyMessageWithParams(params, modifyMessageCallback);
    }

    private void showEditErrorToast(IRongCoreEnum.CoreErrorCode code, String editContent) {
        if (IRongCoreEnum.CoreErrorCode.SUCCESS == code) {
            return;
        }
        int redId;
        if (IRongCoreEnum.CoreErrorCode.RC_ORIGINAL_MESSAGE_NOT_EXIST == code) {
            redId = R.string.rc_edit_failed_by_remote_msg_not_exist;
        } else if (IRongCoreEnum.CoreErrorCode.DANGEROUS_CONTENT == code
                || IRongCoreEnum.CoreErrorCode.CONTENT_REVIEW_REJECTED == code) {
            redId = R.string.rc_edit_failed_by_sensitive;
        } else if (IRongCoreEnum.CoreErrorCode.MESSAGE_OVER_MODIFY_TIME_FAIL == code
                || IRongCoreEnum.CoreErrorCode.RC_MODIFIED_MESSAGE_TIMEOUT == code) {
            redId = R.string.rc_edit_failed_by_expire;
        } else {
            redId =
                    TextUtils.isEmpty(editContent)
                            ? R.string.rc_edit_status_retry_failed
                            : R.string.rc_edit_status_failed;
        }
        Context context = IMCenter.getInstance().getContext();
        ToastUtils.show(context, context.getText(redId), Toast.LENGTH_SHORT);
    }

    private void refreshUIMessage(Message message) {
        if (message == null) {
            return;
        }
        ExecutorFactory.runOnMainThreadSafety(
                () -> {
                    List<Message> messages = new ArrayList<>();
                    messages.add(message);
                    IMCenter.getInstance().refreshMessage(new RefreshEvent(messages, true));
                });
    }

    @Override
    public void onTextChanged(
            Context context,
            Conversation.ConversationType type,
            String targetId,
            int cursorPos,
            int count,
            String text) {
        // 只要触发了内容变更，就重新同步配置
        if (stack.isEmpty()) {
            return;
        }
        EditMessageState lastState = stack.peek();
        if (lastState == null || lastState.mFragment == null || lastState.config == null) {
            return;
        }
        Fragment fragment = lastState.mFragment.get();
        if (fragment == null) {
            return;
        }
        RongExtensionViewModel extensionViewModel =
                new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        if (extensionViewModel.getEditTextWidget() == null) {
            return;
        }
        MentionInstance mentionInstance =
                RongMentionManager.getInstance()
                        .obtainMentionInstance(extensionViewModel.getEditTextWidget());
        lastState.config.mentionBlocks =
                mentionInstance != null ? mentionInstance.mentionBlocks : null;
        lastState.config.content = text;
    }

    @Override
    public void onSendToggleClick(Message message) {
        // do nothing
    }

    @Override
    public void onDeleteClick(
            Conversation.ConversationType type, String targetId, EditText editText, int cursorPos) {
        // do nothing
    }

    @Override
    public void onDestroy(Conversation.ConversationType type, String targetId) {
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (!stack.isEmpty()) {
            EditMessageState nextState = stack.peek();
            mRongExtension = nextState.mRongExtension;
            mFragment = nextState.mFragment;
        } else {
            MessageItemLongClickActionManager.getInstance()
                    .removeMessageItemLongClickAction(mClickActionEditMessage);
            RongExtensionManager.getInstance().removeExtensionEventWatcher(this);
            mRongExtension = null;
            mFragment = null;
        }
    }

    public void onPause() {
        updateCurrentEditConfig();
    }

    public void onResume() {
        updateCurrentEditConfig();
    }

    // 更新内存中的编辑配置到SP
    private void updateCurrentEditConfig() {
        if (!RongConfigCenter.featureConfig().isEditMessageEnable()) {
            return;
        }
        initContext();
        if (stack.isEmpty()) {
            return;
        }
        if (mFragment == null || mRongExtension == null) {
            return;
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        EditMessageState state = stack.peek();
        ConversationIdentifier id = extension.getConversationIdentifier();
        if (state != null && state.config != null && !TextUtils.isEmpty(state.config.content)) {
            editMessageHandler.saveEditedMessageDraft(id, state.config);
        } else {
            editMessageHandler.clearEditedMessageDraft(id);
        }
    }

    // Cursor生成，修改建议也用Cursor
    private List<MentionBlock> getMentionBlocks(UiMessage uiMessage) {
        initContext();
        if (mFragment == null || mRongExtension == null) {
            return new ArrayList<>();
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return new ArrayList<>();
        }
        return EditMessageUtils.getMentionBlocks(uiMessage);
    }

    // 是否过滤
    private boolean isFilter(UiMessage uiMessage) {
        initContext();
        if (mRongExtension == null || mFragment == null) {
            return false;
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return false;
        }
        if (!RongConfigCenter.featureConfig().isEditMessageEnable()) {
            return false;
        }
        // 过滤失败消息
        Message message = uiMessage.getMessage();
        boolean hasUid = !TextUtils.isEmpty(message.getUId());
        boolean supportSentStatus =
                message.getSentStatus() != Message.SentStatus.CANCELED
                        && message.getSentStatus() != Message.SentStatus.FAILED
                        && message.getSentStatus() != Message.SentStatus.SENDING;
        Conversation.ConversationType type = message.getConversationType();
        boolean supportConversationType =
                type.equals(Conversation.ConversationType.PRIVATE)
                        || type.equals(Conversation.ConversationType.GROUP);
        boolean supportMsgType =
                (message.getContent() instanceof TextMessage)
                        || (message.getContent() instanceof ReferenceMessage);
        boolean supportDirection = message.getMessageDirection() == Message.MessageDirection.SEND;
        boolean isFireMsg = message.getContent().isDestruct();
        boolean isFireMode =
                RongExtensionCacheHelper.isDestructMode(
                        extension.getContext(),
                        extension.getConversationType(),
                        extension.getTargetId());
        boolean messageSupport =
                supportSentStatus
                        && supportConversationType
                        && supportMsgType
                        && supportDirection
                        && hasUid
                        && !isFireMsg
                        && !isFireMode;
        if (!messageSupport) {
            return false;
        }
        return checkMessageModifiable(message.getSentTime());
    }

    public void addKeyboardHeightObserver(KeyboardHeightObserver observer) {
        mObservers.add(observer);
    }

    public void removeKeyboardHeightObserver(KeyboardHeightObserver observer) {
        mObservers.remove(observer);
    }

    private void showEditFailedDialog(int txtResId) {
        initContext();
        if (mFragment == null) {
            return;
        }
        Fragment fragment = mFragment.get();
        if (fragment == null
                || fragment.getActivity() == null
                || fragment.getActivity().isFinishing()) {
            return;
        }
        EditMessageFailedDialog dialog =
                EditMessageFailedDialog.newInstance(fragment.getActivity())
                        .setTitleText(R.string.rc_prompt)
                        .setContentMessage(txtResId)
                        .setButtonText(R.string.rc_dialog_ok);
        dialog.show();
    }

    private boolean checkMessageModifiable(long sentTime) {
        long deltaTime = RongCoreClient.getInstance().getDeltaTime();
        long normalTime = System.currentTimeMillis() - deltaTime;
        long intervalTime = normalTime - sentTime;
        // 单位分钟
        if (settings != null && settings.getMessageModifiableMinutes() > 0) {
            return intervalTime < settings.getMessageModifiableMinutes() * 60 * 1000L;
        }
        return false;
    }

    /**
     * RongMentionManager 重新绑定Edittext对应的MentionList。 MentionBlock：“@人名”对应输入框的起始位置。
     * 在RongExtensionViewModel#setEditTextWidget重新绑定Edittext之后，需要调用此接口重新绑定MentionList。
     */
    public void addMentionBlocks(EditText editText, List<MentionBlock> mentionBlocks) {
        draftHelper.addMentionBlocks(editText, mentionBlocks);
    }

    private void postDelayed(Runnable r) {
        ExecutorFactory.getInstance().getMainHandler().postDelayed(r, 100);
    }

    public void addStatusListener(StatusListener listener) {
        if (listener != null && !this.mStatusListenerList.contains(listener)) {
            this.mStatusListenerList.add(listener);
        }
    }

    public void removeStatusListener(StatusListener listener) {
        if (listener != null) {
            this.mStatusListenerList.remove(listener);
        }
    }

    public enum ActiveType {
        // 长按消息。
        OnLongClickMessage,
        // onAttachedToExtension判断如果保存了编辑信息，要恢复。
        OnAttachedToExtension,
        // 消息列表取消多选状态，恢复编辑状态
        OnCancelMultiSelectStatus
    }

    public interface StatusListener {
        /** 编辑消息栏展示或隐藏时回调 */
        void onVisibilityChanged(boolean isVisible);
    }
}
