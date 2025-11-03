package io.rong.imkit.conversation.extension.component.moreaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import java.util.List;

/** Created by zwfang on 2018/3/30. */
public class DeleteClickActions implements IClickActions {

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources()
                .getDrawable(
                        IMKitThemeManager.getAttrResId(
                                context, R.attr.rc_conversation_menu_item_combine_delete_img));
    }

    @Override
    public void onClick(Fragment curFragment) {
        MessageViewModel messageViewModel =
                new ViewModelProvider(curFragment).get(MessageViewModel.class);
        List<UiMessage> messages = messageViewModel.getSelectedUiMessages();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        deleteRemoteMessage(
                curFragment.getContext(),
                messageViewModel.getCurConversationType(),
                messageViewModel.getCurTargetId(),
                messages);
        messageViewModel.quitEditMode();
    }

    private void deleteRemoteMessage(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            List<UiMessage> uiMessages) {
        // 是否全部是本地消息
        boolean isAllMessagesLocal = true;
        int[] messageIds = new int[uiMessages.size()];
        for (int i = 0; i < uiMessages.size(); ++i) {
            messageIds[i] = uiMessages.get(i).getMessage().getMessageId();
            if (!isLocalSendMessage(uiMessages.get(i))) {
                isAllMessagesLocal = false;
            }
        }
        // 不需要删除远端，那就只把本地删除
        if (!RongConfigCenter.conversationConfig().isNeedDeleteRemoteMessage()) {
            deleteLocalMessage(conversationType, targetId, messageIds);
            return;
        }
        // 全部是未发送成功的消息，删除本地
        if (isAllMessagesLocal) {
            deleteLocalMessage(conversationType, targetId, messageIds);
            return;
        }
        // 聊天室不支持删除远端消息，只能删除本地消息
        if (conversationType == Conversation.ConversationType.CHATROOM) {
            deleteLocalMessage(conversationType, targetId, messageIds);
            return;
        }
        // 无网络则删除失败，本地消息也不会删除
        String errorTxt = context.getString(R.string.rc_dialog_item_message_delete_failed_msg);
        if (!NetUtils.isNetWorkAvailable(context)) {
            ToastUtils.show(context, errorTxt, Toast.LENGTH_SHORT);
            return;
        }
        // 先删远端，远端删除成功才删本地
        Message[] messages = new Message[uiMessages.size()];
        for (int i = 0; i < uiMessages.size(); i++) {
            messages[i] = uiMessages.get(i).getMessage();
        }
        IMCenter.getInstance()
                .deleteRemoteMessages(
                        ConversationIdentifier.obtain(uiMessages.get(0).getMessage()),
                        messages,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                deleteLocalMessage(conversationType, targetId, messageIds);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                ToastUtils.show(context, errorTxt, Toast.LENGTH_SHORT);
                            }
                        });
    }

    private void deleteLocalMessage(
            Conversation.ConversationType conversationType, String targetId, int[] messageIds) {
        IMCenter.getInstance()
                .deleteMessages(
                        conversationType,
                        targetId,
                        messageIds,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                if (result) {
                                    ResendManager.getInstance().removeResendMessages(messageIds);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {}
                        });
    }

    private boolean isLocalSendMessage(UiMessage uiMessage) {
        if (uiMessage == null) {
            return false;
        }
        return Message.MessageDirection.SEND == uiMessage.getMessageDirection()
                && (uiMessage.getState() == State.ERROR || uiMessage.getState() == State.PROGRESS)
                && TextUtils.isEmpty(uiMessage.getUId());
    }

    @Override
    public boolean filter(UiMessage message) {
        return false;
    }
}
