package io.rong.imkit.conversationlist.provider;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;

public class PrivateConversationProvider extends BaseConversationProvider {
    @Override
    public boolean isItemViewType(BaseUiConversation item) {
        return (Conversation.ConversationType.PRIVATE.equals(item.mCore.getConversationType()));
    }

    @Override
    public void bindViewHolder(ViewHolder holder, BaseUiConversation uiConversation, int position, List<BaseUiConversation> list, IViewProviderListener<BaseUiConversation> listener) {
        super.bindViewHolder(holder, uiConversation, position, list, listener);
        //已读回执功能打开且最后一条消息为发送消息且状态为已读时，单聊会话里需要显示已读标记。
        if (RongConfigCenter.featureConfig().isReadReceiptConversationType(Conversation.ConversationType.PRIVATE)
                && uiConversation.mCore.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())
                && uiConversation.mCore.getSentStatus().getValue() == Message.SentStatus.READ.getValue()
                && !(uiConversation.mCore.getLatestMessage() instanceof RecallNotificationMessage)) {
            holder.setVisible(R.id.rc_conversation_read_receipt, true);
        } else {
            holder.setVisible(R.id.rc_conversation_read_receipt, false);
        }
    }
}
