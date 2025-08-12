package io.rong.imkit.feature.editmessage;

import android.app.Activity;
import android.content.Context;
import android.text.Spannable;
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
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imkit.utils.StringUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.utils.keyboard.KeyboardHeightObserver;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.MessageModifyInfo;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.AppSettings;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.params.ModifyMessageParams;
import io.rong.message.FileMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class EditMessageManager implements IExtensionEventWatcher, IExtensionModule {
    public static final String TAG = "EditMessageManager";
    private static final int MAX_MESSAGE_LENGTH_TO_SEND = 5500;
    private WeakReference<RongExtension> mRongExtension;
    private WeakReference<Fragment> mFragment;
    private final Stack<EditMessageState> stack = new Stack<>();
    private final MessageItemLongClickAction mClickActionEditMessage =
            new MessageItemLongClickAction.Builder()
                    .titleResId(R.string.rc_dialog_item_message_edit)
                    .actionListener(
                            (context, uiMessage) -> {
                                activeEditMode(
                                        ActiveType.OnLongClickMessage,
                                        uiMessage.getUId(),
                                        getInputContent(uiMessage),
                                        getReferContent(context, uiMessage),
                                        uiMessage.getSentTime(),
                                        getMentionBlocks(uiMessage),
                                        true);
                                return true;
                            })
                    .showFilter(this::isFilter)
                    .build();
    // 观察软键盘高度
    private final List<KeyboardHeightObserver> mObservers = new ArrayList<>();
    private AppSettings settings = new AppSettings();
    private DraftHelper draftHelper = new DraftHelper();
    private IRongCoreListener.ConnectionStatusListener connectionStatusListener =
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
        // 添加监听
        MessageItemLongClickActionManager.getInstance()
                .addMessageItemLongClickAction(mClickActionEditMessage);
        RongExtensionManager.getInstance().addExtensionEventWatcher(this);

        // 保存Fragment、RongExtension、RongExtensionViewModel
        mFragment = new WeakReference<>(fragment);
        mRongExtension = new WeakReference<>(extension);
        // 保存到Stack
        EditMessageState editMessageInstance = new EditMessageState();
        editMessageInstance.mFragment = mFragment;
        editMessageInstance.mRongExtension = mRongExtension;
        stack.add(editMessageInstance);
        // 根据上次保存的编辑配置，来决定是否激活编辑组件
        EditMessageConfig config =
                RongExtensionCacheHelper.getEditMessageConfig(
                        extension.getContext(),
                        extension.getConversationType(),
                        extension.getTargetId());
        if (EditMessageConfig.isInvalid(config)) {
            exitEditMode();
        } else {
            editMessageInstance.config = config;
            activeEditMode(
                    ActiveType.OnAttachedToExtension,
                    config.uid,
                    config.content,
                    config.referContent,
                    config.sentTime,
                    config.mentionBlocks,
                    true);
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

    private void initConfig() {
        if (mRongExtension == null || mFragment == null) {
            if (!stack.isEmpty()) {
                EditMessageState topState = stack.peek();
                mRongExtension = topState.mRongExtension;
                mFragment = topState.mFragment;
            }
        }
    }

    /**
     * 激活编辑消息模式
     *
     * @param config 编辑消息的配置
     */
    public void activeEditMode(EditMessageConfig config, boolean showKeyBoard) {
        if (config == null) {
            return;
        }
        activeEditMode(
                ActiveType.OnCancelMultiSelectStatus,
                config.uid,
                config.content,
                config.referContent,
                config.sentTime,
                config.mentionBlocks,
                showKeyBoard);
    }

    /**
     * 激活编辑消息模式
     *
     * @param type 激活类型
     * @param uid 编辑消息的uid
     * @param content 编辑消息的内容
     * @param referContent 编辑消息的被引用消息内容
     * @param sentTime 编辑消息的sentTime
     * @param mentionBlocks 编辑消息的mentionBlocks
     */
    public void activeEditMode(
            ActiveType type,
            String uid,
            String content,
            String referContent,
            long sentTime,
            List<MentionBlock> mentionBlocks,
            boolean showKeyBoard) {
        initConfig();
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
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(content)) {
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
            if (type == ActiveType.OnLongClickMessage && TextUtils.equals(lastUid, uid)) {
                return;
            }
            // 不同，则弹出确认对话框
            if (!TextUtils.equals(lastUid, uid)) {
                EditMessageDialog.OnClickListener listener =
                        (v, b) -> {
                            extension.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            activeEditModeReally(
                                                    uid,
                                                    content,
                                                    referContent,
                                                    sentTime,
                                                    mentionBlocks,
                                                    showKeyBoard);
                                        }
                                    },
                                    100);
                        };
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
        extension.postDelayed(
                () ->
                        activeEditModeReally(
                                uid, content, referContent, sentTime, mentionBlocks, showKeyBoard),
                100);
    }

    private void activeEditModeReally(
            String uid,
            String content,
            String referContent,
            long sentTime,
            List<MentionBlock> mentionBlocks,
            boolean showKeyBoard) {
        initConfig();
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
        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(content)) {
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
                        fragment, container, extension.getConversationIdentifier());
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
        addMentionBlocks(mEditMessageInputPanel.getEditText(), mentionBlocks);
        // 延时弹起键盘，避免键盘快速收起弹起动画突兀
        mEditMessageInputPanel
                .getEditText()
                .postDelayed(
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
                                mEditMessageInputPanel.setContent(
                                        content, referContent, showKeyBoard);
                            }
                        },
                        300);

        // 保存激活状态到内存中
        EditMessageState lastState = stack.peek();
        EditMessageConfig config = new EditMessageConfig();
        config.uid = uid;
        config.content = content;
        config.referContent = referContent;
        config.sentTime = sentTime;
        config.mentionBlocks = mentionBlocks;
        lastState.config = config;
        // 保存编辑消息状态
        RongExtensionCacheHelper.setEditMessageConfig(
                activity, extension.getConversationType(), extension.getTargetId(), config);
        // 查询是否可以编辑，根据结果刷新UI
        mEditMessageInputPanel.setCheckMessageModifiableResult(checkMessageModifiable(sentTime));
        // 清除引用消息组件，必须要放到 setEditMessageConfig 后面
        ReferenceManager.getInstance().hideReferenceView();
    }

    // 退出编辑消息状态，清空编辑消息配置。
    public void exitEditMode() {
        initConfig();
        RongExtension extension = mRongExtension.get();
        if (extension == null) {
            return;
        }
        // 清空内存状态
        if (!stack.isEmpty()) {
            EditMessageState lastState = stack.peek();
            lastState.config = null;
        }
        // 清空激活状态
        RongExtensionCacheHelper.clearEditMessageConfig(
                extension.getContext(), extension.getConversationType(), extension.getTargetId());
        extension.resetToDefaultView(null, InputMode.TextInput, true);
        extension.getInputPanel().getDraft();
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
        initConfig();
        if (mRongExtension == null) {
            return null;
        }
        RongExtension extension = mRongExtension.get();
        if (extension == null) {
            return null;
        }
        EditMessageConfig config =
                RongExtensionCacheHelper.getEditMessageConfig(
                        extension.getContext(),
                        extension.getConversationType(),
                        extension.getTargetId());
        return config;
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
        // 重新编辑
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
        if (params == null) {
            RLog.e(TAG, "editMessage params " + message.getUId());
            return;
        }
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
        ExecutorHelper.getInstance()
                .mainThread()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<Message> messages = new ArrayList<>();
                                messages.add(message);
                                IMCenter.getInstance()
                                        .refreshMessage(new RefreshEvent(messages, true));
                            }
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
        MessageContent messageContent = message.getContent();
        if (messageContent instanceof TextMessage && isEditMessageState()) {
            int length = ((TextMessage) messageContent).getContent().length();
            long time;
            if (length <= 20) {
                time = 10;
            } else {
                time = Math.round((length - 20) * 0.5 + 10);
            }
            messageContent.setDestructTime(time);
            messageContent.setDestruct(true);
            message.setContent(messageContent);
        }
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
        if (!stack.isEmpty()) {
            updateCurrentEditConfig(stack.peek());
        }
    }

    public void onResume() {
        if (!stack.isEmpty()) {
            updateCurrentEditConfig(stack.peek());
        }
    }

    // 更新内存中的编辑配置到SP
    private void updateCurrentEditConfig(EditMessageState currentState) {
        initConfig();
        if (mFragment == null || mRongExtension == null) {
            return;
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return;
        }
        if (currentState != null
                && currentState.config != null
                && !TextUtils.isEmpty(currentState.config.content)) {
            RongExtensionCacheHelper.setEditMessageConfig(
                    extension.getContext(),
                    extension.getConversationType(),
                    extension.getTargetId(),
                    currentState.config);
        } else {
            RongExtensionCacheHelper.clearEditMessageConfig(
                    extension.getContext(),
                    extension.getConversationType(),
                    extension.getTargetId());
        }
    }

    private String getInputContent(UiMessage uiMessage) {
        if (uiMessage.getContent() instanceof TextMessage) {
            return ((TextMessage) uiMessage.getContent()).getContent();
        }
        if (uiMessage.getContent() instanceof ReferenceMessage) {
            return ((ReferenceMessage) uiMessage.getContent()).getEditSendText();
        }
        return "";
    }

    private String getReferContent(Context context, UiMessage uiMessage) {
        if (uiMessage.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) uiMessage.getContent();
            MessageContent referenceContent = referenceMessage.getReferenceContent();
            String name = getDisplayName(uiMessage, referenceMessage);
            Spannable messageSummary =
                    RongConfigCenter.conversationConfig()
                            .getMessageSummary(context, referenceContent);
            String content = "";
            ReferenceMessage.ReferenceMessageStatus status = referenceMessage.getReferMsgStatus();
            if (ReferenceMessage.ReferenceMessageStatus.DELETE == status) {
                content = context.getString(R.string.rc_reference_status_delete);
            } else if (ReferenceMessage.ReferenceMessageStatus.RECALLED == status) {
                content = context.getString(R.string.rc_reference_status_recall);
            } else if (referenceContent instanceof FileMessage) {
                content = messageSummary.toString();
            } else if (referenceContent instanceof RichContentMessage) {
                String fileTile = ((RichContentMessage) referenceContent).getTitle();
                content = messageSummary.toString() + fileTile;
            } else {
                content = StringUtils.getStringNoBlank(messageSummary.toString());
            }
            return name + ":" + content;
        }
        return "";
    }

    // Cursor生成，修改建议也用Cursor
    private List<MentionBlock> getMentionBlocks(UiMessage uiMessage) {
        initConfig();
        if (mFragment == null || mRongExtension == null) {
            return new ArrayList<>();
        }
        RongExtension extension = mRongExtension.get();
        Fragment fragment = mFragment.get();
        if (extension == null || fragment == null) {
            return new ArrayList<>();
        }
        String content = "";
        if (uiMessage.getContent() instanceof TextMessage) {
            content = ((TextMessage) uiMessage.getContent()).getContent();
        } else if (uiMessage.getContent() instanceof ReferenceMessage) {
            content = ((ReferenceMessage) uiMessage.getContent()).getEditSendText();
        }
        if (TextUtils.isEmpty(content)) {
            return new ArrayList<>();
        }

        MentionedInfo mentionedInfo = uiMessage.getContent().getMentionedInfo();
        if (mentionedInfo == null) {
            return new ArrayList<>();
        }
        List<String> mentionedUserIdList = new ArrayList<>();
        if (mentionedInfo.getMentionedUserIdList() != null
                && !mentionedInfo.getMentionedUserIdList().isEmpty()) {
            mentionedUserIdList.addAll(mentionedInfo.getMentionedUserIdList());
        }
        if (mentionedInfo.getType() == MentionedInfo.MentionedType.ALL) {
            boolean hasAll = false;
            if (!mentionedUserIdList.isEmpty()) {
                for (String uid : mentionedUserIdList) {
                    if (TextUtils.equals(uid, "-1")) {
                        hasAll = true;
                    }
                }
            }
            if (!hasAll) {
                mentionedUserIdList.add("-1");
            }
        }
        if (mentionedUserIdList.isEmpty()) {
            return new ArrayList<>();
        }
        String targetId = uiMessage.getTargetId();
        Conversation.ConversationType type = uiMessage.getConversationType();
        List<MentionBlock> mentionBlocks = new ArrayList<>();

        // 记录已处理的区间，避免重复匹配 - 使用二维数组存储[start, end]
        List<int[]> processedRanges = new ArrayList<>();

        for (String uid : mentionedUserIdList) {
            // 获取@的用户名
            String mentionUserName = getMentionUserName(type, targetId, uid);
            // 检查用户名是否为空
            if (TextUtils.isEmpty(mentionUserName)) {
                RLog.e(TAG, "Empty user name for uid: " + uid);
                continue;
            }

            String searchPattern = "@" + mentionUserName + " ";
            int searchIndex = 0;

            // 查找所有匹配的位置
            while (searchIndex < content.length()) {
                int foundIndex = content.indexOf(searchPattern, searchIndex);
                if (foundIndex == -1) {
                    break; // 没有找到更多匹配
                }

                int foundEnd = foundIndex + searchPattern.length();

                // 检查这个位置是否已经被处理过（区间重叠检查）
                boolean alreadyProcessed = false;
                for (int[] processedRange : processedRanges) {
                    int processedStart = processedRange[0];
                    int processedEnd = processedRange[1];
                    // 检查两个区间是否重叠：foundIndex < processedEnd && foundEnd > processedStart
                    if (foundIndex < processedEnd && foundEnd > processedStart) {
                        alreadyProcessed = true;
                        break;
                    }
                }

                if (!alreadyProcessed) {
                    // 创建新的MentionBlock
                    MentionBlock block = new MentionBlock();
                    block.userId = uid;
                    block.name = mentionUserName;
                    block.offset = true;
                    block.start = foundIndex;
                    block.end = foundEnd;
                    mentionBlocks.add(block);

                    // 记录已处理的区间
                    processedRanges.add(new int[] {foundIndex, foundEnd});
                }

                // 移动到下一个搜索位置，避免重复匹配
                searchIndex = foundIndex + searchPattern.length();
            }
        }

        return mentionBlocks;
    }

    // 返回Mention中userID对应的名字，@所有人则返回"所有人"。
    private String getMentionUserName(
            Conversation.ConversationType type, String targetId, String uid) {
        if (type == Conversation.ConversationType.GROUP) {
            if (TextUtils.equals(uid, "-1")) {
                return "";
            }
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance().getGroupUserInfo(targetId, uid);
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                return groupUserInfo.getNickname();
            }
        }
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(uid);
        if (userInfo == null
                || TextUtils.isEmpty(userInfo.getUserId())
                || userInfo.getName() == null) {
            return "";
        }
        return userInfo.getName();
    }

    private String getDisplayName(UiMessage uiMessage, ReferenceMessage referenceMessage) {
        if (uiMessage.getMessage().getSenderUserId() != null) {
            UserInfo userInfo =
                    getUserInfo(
                            referenceMessage.getUserId(), referenceMessage.getReferenceContent());
            String groupMemberName = "";
            if (uiMessage
                    .getMessage()
                    .getConversationType()
                    .equals(Conversation.ConversationType.GROUP)) {
                GroupUserInfo groupUserInfo =
                        RongUserInfoManager.getInstance()
                                .getGroupUserInfo(
                                        uiMessage.getMessage().getTargetId(),
                                        referenceMessage.getUserId());
                groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
            }
            return RongUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
        }
        return "";
    }

    private UserInfo getUserInfo(String userId, MessageContent messageContent) {
        boolean isInfoManagement =
                RongUserInfoManager.getInstance().getDataSourceType()
                        == RongUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && messageContent != null
                && messageContent.getUserInfo() != null
                && messageContent.getUserInfo().getUserId() != null
                && messageContent.getUserInfo().getUserId().equals(userId)) {
            return messageContent.getUserInfo();
        }
        return RongUserInfoManager.getInstance().getUserInfo(userId);
    }

    // 是否过滤
    private boolean isFilter(UiMessage uiMessage) {
        initConfig();
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
        initConfig();
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

    public enum ActiveType {
        // 长按消息。
        OnLongClickMessage,
        // onAttachedToExtension判断如果保存了编辑信息，要恢复。
        OnAttachedToExtension,
        // 消息列表取消多选状态
        OnCancelMultiSelectStatus
    }
}
