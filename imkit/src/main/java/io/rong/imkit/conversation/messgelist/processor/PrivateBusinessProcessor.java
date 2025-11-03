package io.rong.imkit.conversation.messgelist.processor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.RongIM;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.handler.AppSettingsHandler;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.DeviceUtils;
import io.rong.imlib.common.SharedPreferencesUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;

public class PrivateBusinessProcessor extends BaseBusinessProcessor {
    private static final String TAG = "PrivateBusinessProcessor";

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
            Conversation.ConversationType type = viewModel.getCurConversationType();
            boolean readV5Enabled = AppSettingsHandler.getInstance().isReadReceiptV5Enabled(type);
            boolean isShowReadReceipt =
                    RongConfigCenter.conversationConfig().isShowReadReceipt(type);
            // 如果开启已读回执配置，且没开启V5，则发送V1回执
            if (isShowReadReceipt && !readV5Enabled && !TextUtils.isEmpty(message.getUId())) {
                // 是否是前台
                if ((viewModel.isForegroundActivity())) {
                    sendReadReceiptMessage(
                            viewModel.getApplication(),
                            viewModel.getCurTargetId(),
                            type,
                            message.getSentTime(),
                            true);
                } else {
                    addSendReadReceiptStatusToSp(
                            viewModel.getApplication(),
                            viewModel.getCurTargetId(),
                            type,
                            true,
                            message.getSentTime());
                }
            }
            if (RongConfigCenter.conversationConfig().isEnableMultiDeviceSync(type)) {
                // 如果未开启已读回执配置，或者开启了V5，则发送V1回执
                if (!isShowReadReceipt || readV5Enabled) {
                    IMCenter.getInstance()
                            .syncConversationReadStatus(
                                    ConversationIdentifier.obtain(message.getMessage()),
                                    message.getSentTime(),
                                    null);
                }
            }
        }
        // 本地插入的消息(Uid 为空)不需要发送已读回执
        return super.onReceived(viewModel, message, left, hasPackage, offline);
    }

    @Override
    public void onExistUnreadMessage(
            MessageViewModel viewModel, Conversation conversation, int unreadMessageCount) {
        Conversation.ConversationType type = viewModel.getCurConversationType();
        boolean readV5Enabled = AppSettingsHandler.getInstance().isReadReceiptV5Enabled(type);
        boolean isShowReadReceipt = RongConfigCenter.conversationConfig().isShowReadReceipt(type);
        // 如果开启已读回执配置，且没开启V5，则发送V1回执
        if (isShowReadReceipt && !readV5Enabled) {
            sendReadReceiptMessage(
                    viewModel.getApplication(),
                    viewModel.getCurTargetId(),
                    type,
                    conversation.getSentTime(),
                    true);
        }
        if (RongConfigCenter.conversationConfig().isEnableMultiDeviceSync(type)) {
            // 如果未开启已读回执配置，或者开启了V5，则发送V1回执
            if (!isShowReadReceipt || readV5Enabled) {
                IMCenter.getInstance()
                        .syncConversationReadStatus(
                                viewModel.getConversationIdentifier(),
                                conversation.getSentTime(),
                                null);
            }
        }
    }

    @Override
    public void onDestroy(MessageViewModel viewModel) {
        checkReadStatus(viewModel);
    }

    @Override
    public void onConnectStatusChange(
            MessageViewModel viewModel,
            RongIMClient.ConnectionStatusListener.ConnectionStatus status) {
        if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
            checkReadStatus(viewModel);
        }
    }

    @Override
    public void onResume(MessageViewModel viewModel) {
        checkReadStatus(viewModel);
    }

    private void checkReadStatus(MessageViewModel viewModel) {
        SharedPreferences sp =
                SharedPreferencesUtils.get(
                        viewModel.getApplication(),
                        ConversationConfig.SP_NAME_READ_RECEIPT_CONFIG,
                        Context.MODE_PRIVATE);
        long sendReadReceiptTime =
                sp == null
                        ? 0
                        : sp.getLong(
                                getSavedReadReceiptTimeName(
                                        viewModel.getCurTargetId(),
                                        viewModel.getCurConversationType()),
                                0);
        if (sendReadReceiptTime > 0) {
            sendReadReceiptMessage(
                    viewModel.getApplication(),
                    viewModel.getCurTargetId(),
                    viewModel.getCurConversationType(),
                    sendReadReceiptTime,
                    false);
        }
    }

    private void sendReadReceiptMessage(
            final Context context,
            final String targetId,
            final Conversation.ConversationType type,
            final long sendReadReceiptTime,
            final boolean processError) {
        if (type == null || TextUtils.isEmpty(targetId)) {
            return;
        }
        if (AppSettingsHandler.getInstance().isReadReceiptV5Enabled(type)) {
            return;
        }
        IMCenter.getInstance()
                .sendReadReceiptMessage(
                        type,
                        targetId,
                        sendReadReceiptTime,
                        new IRongCallback.ISendMessageCallback() {
                            @Override
                            public void onAttached(Message message) {
                                // do nothing
                            }

                            @Override
                            public void onSuccess(Message message) {
                                removeSendReadReceiptStatusToSp(context, targetId, type);
                            }

                            @Override
                            public void onError(Message message, RongIMClient.ErrorCode errorCode) {
                                RLog.e(
                                        TAG,
                                        "sendReadReceiptMessage:onError:errorCode"
                                                + errorCode.getValue());
                                if (processError) {
                                    addSendReadReceiptStatusToSp(
                                            context, targetId, type, true, sendReadReceiptTime);
                                }
                            }
                        });
    }

    private void removeSendReadReceiptStatusToSp(
            Context context, String targetId, Conversation.ConversationType type) {
        SharedPreferences preferences =
                SharedPreferencesUtils.get(
                        context,
                        ConversationConfig.SP_NAME_READ_RECEIPT_CONFIG,
                        Context.MODE_PRIVATE);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(getSavedReadReceiptStatusName(targetId, type));
            editor.remove(getSavedReadReceiptTimeName(targetId, type));
            editor.apply();
        }
    }

    private void addSendReadReceiptStatusToSp(
            Context context,
            String targetId,
            Conversation.ConversationType type,
            boolean status,
            long time) {
        SharedPreferences preferences =
                SharedPreferencesUtils.get(
                        context,
                        ConversationConfig.SP_NAME_READ_RECEIPT_CONFIG,
                        Context.MODE_PRIVATE);
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(getSavedReadReceiptStatusName(targetId, type), status);
            editor.putLong(getSavedReadReceiptTimeName(targetId, type), time);
            editor.apply();
        }
    }

    private String getSavedReadReceiptStatusName(
            String targetId, Conversation.ConversationType type) {
        if (!TextUtils.isEmpty(targetId) && type != null) {
            String savedId =
                    DeviceUtils.ShortMD5(
                            Base64.DEFAULT,
                            RongIM.getInstance().getCurrentUserId(),
                            targetId,
                            type.getName());
            return "ReadReceipt" + savedId + "Status";
        }
        return "";
    }

    private String getSavedReadReceiptTimeName(
            String targetId, Conversation.ConversationType type) {
        if (!TextUtils.isEmpty(targetId) && type != null) {
            String savedId =
                    DeviceUtils.ShortMD5(
                            Base64.DEFAULT,
                            RongIM.getInstance().getCurrentUserId(),
                            targetId,
                            type.getName());
            return "ReadReceipt" + savedId + "Time";
        }
        return "";
    }
}
