package io.rong.imkit.handler;

import android.content.Context;
import android.text.TextUtils;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.feature.editmessage.EditMessageConfig;
import io.rong.imkit.feature.editmessage.EditMessageManager;
import io.rong.imkit.feature.reference.ReferenceManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.MessageModifyInfo;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.common.ExecutorFactory;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.MessageResult;
import io.rong.imlib.params.RefreshReferenceMessageParams;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * EditMessageHandler
 *
 * <p>处理消息编辑相关的数据请求
 *
 * @since 5.26.0
 */
public class EditMessageHandler extends MultiDataHandler {

    private static final String TAG = "EditMessageHandler";

    public static final DataKey<List<Message>> KEY_ON_MESSAGE_MODIFIED =
            DataKey.obtain("KEY_ON_MESSAGE_MODIFIED", (Class<List<Message>>) (Class<?>) List.class);

    public static final DataKey<Message> KEY_ON_MESSAGE_REFRESH =
            DataKey.obtain("KEY_ON_MESSAGE_REFRESH", Message.class);

    private final CopyOnWriteArraySet<String> runningMsgSet = new CopyOnWriteArraySet<>();

    private final IRongCoreListener.MessageModifiedListener mMessageModifiedListener =
            new IRongCoreListener.MessageModifiedListener() {
                @Override
                public void onMessageModified(List<Message> messages) {
                    if (messages != null && !messages.isEmpty()) {
                        notifyDataChange(KEY_ON_MESSAGE_MODIFIED, messages);
                    }
                }

                @Override
                public void onModifiedMessageSyncCompleted() {}
            };

    public EditMessageHandler() {
        super();
        RongCoreClient.getInstance().addMessageModifiedListener(mMessageModifiedListener);
    }

    /**
     * 处理消息的编辑状态和引用了此消息的引用消息状态
     *
     * @param editMessageList 编辑消息列表
     * @param uiMessageList 页面中的消息列表
     * @return 处理后的消息列表，用户UI刷新
     */
    public List<UiMessage> processMessageEditStatusAndReferMsgStatus(
            List<Message> editMessageList, List<UiMessage> uiMessageList) {
        if (editMessageList == null
                || editMessageList.isEmpty()
                || uiMessageList == null
                || uiMessageList.isEmpty()) {
            return uiMessageList;
        }
        // 刷新引用了被编辑消息的引用消息的被引用消息状态
        List<Message> result = new ArrayList<>();
        HashMap<String, Message> uIdMap = new HashMap<>();
        for (Message message : editMessageList) {
            uIdMap.put(message.getUId(), message);
        }
        for (UiMessage item : uiMessageList) {
            if (item.getMessage().getContent() instanceof ReferenceMessage) {
                ReferenceMessage referMsg = (ReferenceMessage) item.getMessage().getContent();
                Message msg = uIdMap.get(referMsg.getReferMsgUid());
                if (msg != null) {
                    referMsg.setReferMsgStatus(ReferenceMessage.ReferenceMessageStatus.MODIFIED);
                    MessageContent referenceContent = referMsg.getReferenceContent();
                    MessageContent sourceContent = msg.getContent();
                    if (referenceContent instanceof TextMessage
                            && sourceContent instanceof TextMessage) {
                        String newContent = ((TextMessage) msg.getContent()).getContent();
                        ((TextMessage) referenceContent).setContent(newContent);
                    } else if (referenceContent instanceof ReferenceMessage
                            && sourceContent instanceof ReferenceMessage) {
                        String newContent = ((ReferenceMessage) msg.getContent()).getEditSendText();
                        ((ReferenceMessage) referenceContent).setEditSendText(newContent);
                    }
                    result.add(item.getMessage());
                }
            }
        }
        // 编辑的消息列表需要刷新 + 引用了这条编辑消息的引用消息也需要刷新合并成1次刷新
        if (!result.isEmpty()) {
            editMessageList.addAll(result);
        }
        return processMessageEditStatus(editMessageList, uiMessageList);
    }

    /**
     * 处理引用了此消息的引用消息状态
     *
     * @param message 消息
     * @param status 引用消息状态 撤回状态、删除状态
     * @param uiMessageList 页面中的消息列表
     * @return 处理后的消息列表，用户UI刷新
     */
    public List<UiMessage> processMessageReferMsgStatus(
            Message message,
            ReferenceMessage.ReferenceMessageStatus status,
            List<UiMessage> uiMessageList) {
        if (message != null && !TextUtils.isEmpty(message.getUId())) {
            return processMessageReferMsgStatus(new Message[] {message}, status, uiMessageList);
        }
        return uiMessageList;
    }

    /**
     * 处理引用了此消息的引用消息状态
     *
     * @param messages 消息列表
     * @param status 引用消息状态 撤回状态、删除状态
     * @param uiMessageList 页面中的消息列表
     * @return 处理后的消息列表，用户UI刷新
     */
    public List<UiMessage> processMessageReferMsgStatus(
            Message[] messages,
            ReferenceMessage.ReferenceMessageStatus status,
            List<UiMessage> uiMessageList) {
        if (uiMessageList == null || uiMessageList.isEmpty()) {
            return uiMessageList;
        }
        List<Message> result = new ArrayList<>();
        HashSet<String> uIdSet = new HashSet<>();
        for (Message message : messages) {
            uIdSet.add(message.getUId());
        }
        for (UiMessage item : uiMessageList) {
            if (item.getMessage().getContent() instanceof ReferenceMessage) {
                ReferenceMessage referMsg = (ReferenceMessage) item.getMessage().getContent();
                if (uIdSet.contains(referMsg.getReferMsgUid())) {
                    referMsg.setReferMsgStatus(status);
                    result.add(item.getMessage());
                }
            }
        }
        return processMessageEditStatus(result, uiMessageList);
    }

    /**
     * 根据编辑的消息列表，去会话页面内存中的UIMessage列表刷新对应消息的编辑状态
     *
     * @param editMessageList 编辑的消息列表
     * @param uiMessageList 会话页面中的消息列表
     */
    private List<UiMessage> processMessageEditStatus(
            List<Message> editMessageList, List<UiMessage> uiMessageList) {
        if (editMessageList == null
                || editMessageList.isEmpty()
                || uiMessageList == null
                || uiMessageList.isEmpty()) {
            return uiMessageList;
        }
        for (Message message : editMessageList) {
            if (message == null || TextUtils.isEmpty(message.getUId())) {
                continue;
            }
            UiMessage uiMessage = findUIMessage(uiMessageList, message.getUId());
            if (uiMessage == null) {
                continue;
            }
            // 消息类型是引用消息，需要保存内存中的引用状态
            if (uiMessage.getContent() instanceof ReferenceMessage
                    && message.getContent() instanceof ReferenceMessage) {
                ReferenceMessage.ReferenceMessageStatus uiStatus =
                        ((ReferenceMessage) uiMessage.getContent()).getReferMsgStatus();
                ReferenceMessage.ReferenceMessageStatus messageStatus =
                        ((ReferenceMessage) message.getContent()).getReferMsgStatus();
                // 如果内存中的referMsgStatus值大于待刷新的referMsgStatus值，则使用内存中的referMsgStatus。
                // 原因：ReferenceMessageStatus代表着向前状态，较大的值不会退回到较小的值。
                if (uiStatus.getValue() > messageStatus.getValue()) {
                    ((ReferenceMessage) message.getContent()).setReferMsgStatus(uiStatus);
                }
            }
            uiMessage.setMessage(message);
            uiMessage.setContentSpannable(null);
            uiMessage.setReferenceContentSpannable(null);
            uiMessage.setChange(true);
        }
        return uiMessageList;
    }

    /** 恢复编辑消息输入UI。 */
    public void activeEditMode(Context context, ConversationIdentifier id) {
        EditMessageConfig config =
                RongExtensionCacheHelper.getEditMessageConfig(
                        context, id.getType(), id.getTargetId());
        EditMessageManager.getInstance()
                .activeEditMode(
                        EditMessageManager.ActiveType.OnCancelMultiSelectStatus, config, false);
    }

    /**
     * 收到编辑消息事件后，更新引用消息UI
     *
     * @param messages 编辑的消息
     */
    public void updateReferenceView(List<Message> messages, List<UiMessage> uiMessageList) {
        // 引用消息未显示，不用更新
        UiMessage referMessage = ReferenceManager.getInstance().getUiMessage();
        if (referMessage == null) {
            return;
        }
        if (messages == null
                || messages.isEmpty()
                || uiMessageList == null
                || uiMessageList.isEmpty()) {
            return;
        }
        for (Message message : messages) {
            MessageModifyInfo info = message.getModifyInfo();
            if (info != null && MessageModifyInfo.MessageModifyStatus.SUCCESS == info.getStatus()) {
                UiMessage uiMessage = findUIMessage(uiMessageList, message.getUId());
                if (uiMessage == null
                        || !TextUtils.equals(referMessage.getUId(), uiMessage.getUId())) {
                    continue;
                }
                ReferenceManager.getInstance().showReferenceView(null, uiMessage);
            }
        }
    }

    private UiMessage findUIMessage(List<UiMessage> uiMessageList, String messageUId) {
        if (uiMessageList == null || uiMessageList.isEmpty()) {
            return null;
        }
        for (UiMessage item : uiMessageList) {
            if (messageUId.equals(item.getMessage().getUId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 刷新引用消息状态
     *
     * @param editMsgUid 消息uid
     * @param identifier ConversationIdentifier
     */
    public void refreshReferenceMessage(String editMsgUid, ConversationIdentifier identifier) {
        // 如果是刷新中状态则返回，等待上次刷新状态。
        if (runningMsgSet.contains(editMsgUid)) {
            return;
        }
        runningMsgSet.add(editMsgUid);
        List<String> uidList = new ArrayList<>();
        uidList.add(editMsgUid);
        RongCoreClient.getInstance()
                .refreshReferenceMessageWithParams(
                        new RefreshReferenceMessageParams(identifier, uidList),
                        new IRongCoreCallback.RefreshReferenceMessageCallback() {
                            @Override
                            public void onLocalMessageBlock(List<MessageResult> msgList) {
                                onRefreshReferenceMessage(msgList, editMsgUid);
                            }

                            @Override
                            public void onRemoteMessageBlock(List<MessageResult> msgList) {
                                onRefreshReferenceMessage(msgList, editMsgUid);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                runningMsgSet.remove(editMsgUid);
                            }
                        });
    }

    private void onRefreshReferenceMessage(List<MessageResult> msgList, String editMsgUid) {
        if (msgList == null || msgList.isEmpty()) {
            return;
        }
        ExecutorFactory.runOnMainThreadSafety(
                () -> notifyDataChange(KEY_ON_MESSAGE_REFRESH, msgList.get(0).getMessage()));
        runningMsgSet.remove(editMsgUid);
    }

    @Override
    public void stop() {
        super.stop();
        RongCoreClient.getInstance().removeMessageModifiedListener(mMessageModifiedListener);
    }
}
