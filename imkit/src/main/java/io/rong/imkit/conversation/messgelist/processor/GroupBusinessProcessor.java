package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;
import java.util.ArrayList;
import java.util.List;

public class GroupBusinessProcessor extends BaseBusinessProcessor {
    private static final String TAG = "GroupBusinessProcessor";

    @Override
    public void init(final MessageViewModel messageViewModel, Bundle bundle) {
        super.init(messageViewModel, bundle);
    }

    @Override
    public boolean onReceived(
            MessageViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        if (left == 0 && !hasPackage) {
            if (RongConfigCenter.conversationConfig()
                            .isEnableMultiDeviceSync(viewModel.getCurConversationType())
                    && !RongConfigCenter.conversationConfig()
                            .isShowReadReceipt(viewModel.getCurConversationType())) {
                IMCenter.getInstance()
                        .syncConversationReadStatus(
                                ConversationIdentifier.obtain(message.getMessage()),
                                message.getSentTime(),
                                null);

                if (Conversation.ConversationType.ULTRA_GROUP.equals(
                        viewModel.getCurConversationType())) {
                    RLog.e(
                            TAG,
                            "onReceived syncUltraGroupReadStatus"
                                    + "，t:"
                                    + message.getTargetId()
                                    + "，c:"
                                    + message.getMessage().getChannelId()
                                    + "，type:"
                                    + message.getClass().getSimpleName());
                    ChannelClient.getInstance()
                            .syncUltraGroupReadStatus(
                                    message.getTargetId(),
                                    message.getMessage().getChannelId(),
                                    message.getSentTime(),
                                    new IRongCoreCallback.OperationCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(
                                                            viewModel.getApplication(),
                                                            "超级群已读状态同步成功",
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                        }

                                        @Override
                                        public void onError(
                                                IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                            Toast.makeText(
                                                            viewModel.getApplication(),
                                                            "超级群已读状态同步失败",
                                                            Toast.LENGTH_LONG)
                                                    .show();
                                        }
                                    });
                }
            }
        }
        return super.onReceived(viewModel, message, left, hasPackage, offline);
    }

    @Override
    public void onExistUnreadMessage(
            MessageViewModel viewModel, Conversation conversation, int unreadMessageCount) {
        boolean syncReadStatus =
                RongConfigCenter.conversationConfig()
                        .isEnableMultiDeviceSync(viewModel.getCurConversationType());
        if (syncReadStatus) {
            IMCenter.getInstance()
                    .syncConversationReadStatus(
                            viewModel.getConversationIdentifier(),
                            conversation.getSentTime(),
                            null);
            if (Conversation.ConversationType.ULTRA_GROUP.equals(
                    viewModel.getCurConversationType())) {
                RLog.e(
                        TAG,
                        "onExistUnreadMessage syncUltraGroupReadStatus"
                                + "，t:"
                                + viewModel.getCurTargetId()
                                + "，c:"
                                + viewModel.getConversationIdentifier().getChannelId());
                ChannelClient.getInstance()
                        .syncUltraGroupReadStatus(
                                viewModel.getCurTargetId(),
                                viewModel.getConversationIdentifier().getChannelId(),
                                conversation.getSentTime(),
                                new IRongCoreCallback.OperationCallback() {
                                    @Override
                                    public void onSuccess() {
                                        //                                        Toast.makeText(
                                        //
                                        // viewModel.getApplication(),
                                        //
                                        // "超级群已读状态同步成功",
                                        //
                                        // Toast.LENGTH_LONG)
                                        //                                                .show();
                                    }

                                    @Override
                                    public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                        //                                        Toast.makeText(
                                        //
                                        // viewModel.getApplication(),
                                        //
                                        // "超级群已读状态同步失败",
                                        //
                                        // Toast.LENGTH_LONG)
                                        //                                                .show();
                                    }
                                });
            }
        }
    }

    @Override
    public boolean onUserPortraitLongClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo userInfo,
            String targetId) {
        if (userInfo != null
                && !userInfo.getUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
            RongMentionManager.getInstance()
                    .mentionMember(conversationType, targetId, userInfo.getUserId());
            return true;
        }
        return false;
    }

    /** 当加载完消息，群组发送已读回执 */
    @Override
    public void onLoadMessage(MessageViewModel viewModel, List<Message> messages) {
        if (!RongConfigCenter.conversationConfig()
                .isShowReadReceiptRequest(viewModel.getCurConversationType())) {
            return;
        }
        ExecutorHelper.getInstance()
                .networkIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<io.rong.imlib.model.Message> responseMessageList =
                                        new ArrayList<>();
                                for (io.rong.imlib.model.Message message : messages) {
                                    ReadReceiptInfo readReceiptInfo = message.getReadReceiptInfo();
                                    if (readReceiptInfo == null) {
                                        continue;
                                    }
                                    if (readReceiptInfo.isReadReceiptMessage()
                                            && !readReceiptInfo.hasRespond()) {
                                        responseMessageList.add(message);
                                    }
                                }
                                if (responseMessageList.size() > 0) {
                                    RongCoreClient.getInstance()
                                            .sendReadReceiptResponse(
                                                    viewModel.getCurConversationType(),
                                                    viewModel.getCurTargetId(),
                                                    responseMessageList,
                                                    new IRongCoreCallback.OperationCallback() {
                                                        @Override
                                                        public void onSuccess() {
                                                            for (io.rong.imlib.model.Message
                                                                    message : messages) {
                                                                updateMessageHadRespond(message);
                                                            }
                                                        }

                                                        @Override
                                                        public void onError(
                                                                IRongCoreEnum.CoreErrorCode
                                                                        coreErrorCode) {}
                                                    });
                                }
                            }
                        });
    }

    @Override
    public void onResume(MessageViewModel viewModel) {
        if (!RongConfigCenter.conversationConfig()
                .isShowReadReceiptRequest(viewModel.getCurConversationType())) {
            return;
        }
        ExecutorHelper.getInstance()
                .networkIO()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                List<io.rong.imlib.model.Message> responseMessageList =
                                        new ArrayList<>();
                                final List<UiMessage> uiMessages = viewModel.getUiMessages();
                                for (UiMessage uiMessage : uiMessages) {
                                    ReadReceiptInfo readReceiptInfo =
                                            uiMessage.getMessage().getReadReceiptInfo();
                                    if (readReceiptInfo == null) {
                                        continue;
                                    }
                                    if (readReceiptInfo.isReadReceiptMessage()
                                            && !readReceiptInfo.hasRespond()) {
                                        responseMessageList.add(uiMessage.getMessage());
                                    }
                                }
                                if (responseMessageList.size() > 0) {
                                    RongCoreClient.getInstance()
                                            .sendReadReceiptResponse(
                                                    viewModel.getCurConversationType(),
                                                    viewModel.getCurTargetId(),
                                                    responseMessageList,
                                                    new IRongCoreCallback.OperationCallback() {
                                                        @Override
                                                        public void onSuccess() {
                                                            for (UiMessage uiMessage : uiMessages) {
                                                                updateMessageHadRespond(
                                                                        uiMessage.getMessage());
                                                            }
                                                        }

                                                        @Override
                                                        public void onError(
                                                                IRongCoreEnum.CoreErrorCode
                                                                        coreErrorCode) {}
                                                    });
                                }
                            }
                        });
    }

    @Override
    public void onMessageReceiptRequest(
            final MessageViewModel viewModel,
            Conversation.ConversationType conversationType,
            String targetId,
            String messageUId) {
        if (!RongConfigCenter.conversationConfig()
                .isShowReadReceiptRequest(viewModel.getCurConversationType())) {
            return;
        }
        for (final UiMessage item : viewModel.getUiMessages()) {
            if (item.getMessage().getUId().equals(messageUId)) {
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
                if (viewModel.isForegroundActivity()) {
                    List<Message> messageList = new ArrayList<>();
                    messageList.add(item.getMessage());
                    RongCoreClient.getInstance()
                            .sendReadReceiptResponse(
                                    viewModel.getCurConversationType(),
                                    viewModel.getCurTargetId(),
                                    messageList,
                                    new IRongCoreCallback.OperationCallback() {
                                        @Override
                                        public void onSuccess() {
                                            updateMessageHadRespond(item.getMessage());
                                        }

                                        @Override
                                        public void onError(IRongCoreEnum.CoreErrorCode errorCode) {
                                            RLog.e(
                                                    TAG,
                                                    "sendReadReceiptResponse failed, errorCode = "
                                                            + errorCode);
                                        }
                                    });
                    break;
                }
            }
        }
    }

    private void updateMessageHadRespond(Message message) {
        if (message == null) {
            return;
        }
        ReadReceiptInfo readReceiptInfo = message.getReadReceiptInfo();
        if (readReceiptInfo == null) {
            readReceiptInfo = new ReadReceiptInfo();
        }
        readReceiptInfo.setHasRespond(true);
        message.setReadReceiptInfo(readReceiptInfo);
    }
}
