package io.rong.imkit.conversationlist.provider;

import android.text.TextUtils;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.message.InformationNotificationMessage;
import io.rong.message.RecallNotificationMessage;
import java.util.List;

public class PrivateConversationProvider extends BaseConversationProvider {
    @Override
    public boolean isItemViewType(BaseUiConversation item) {
        return (Conversation.ConversationType.PRIVATE.equals(item.mCore.getConversationType()));
    }

    @Override
    public void bindViewHolder(
            ViewHolder holder,
            BaseUiConversation uiConversation,
            int position,
            List<BaseUiConversation> list,
            IViewProviderListener<BaseUiConversation> listener) {
        super.bindViewHolder(holder, uiConversation, position, list, listener);
        initReadStatus(holder, uiConversation);
    }

    /** 已读回执 */
    protected void initReadStatus(ViewHolder holder, BaseUiConversation uiConversation) {
        // 已读回执功能打开且最后一条消息为发送消息且状态为已读时，单聊会话里需要显示已读标记。
        String currentUserId = RongIMClient.getInstance().getCurrentUserId();
        Conversation conversation = uiConversation.mCore;
        Conversation.ConversationType privateType = conversation.getConversationType();
        if (IMKitThemeManager.isTraditionTheme()
                && RongConfigCenter.featureConfig().isReadReceiptConversationType(privateType)
                && RongConfigCenter.conversationConfig().isShowReadReceipt(privateType)
                && uiConversation.mCore.getSenderUserId().equals(currentUserId)
                && !(uiConversation.mCore.getLatestMessage() instanceof RecallNotificationMessage)
                && !(uiConversation.mCore.getLatestMessage()
                        instanceof InformationNotificationMessage)
                && !TextUtils.isEmpty(uiConversation.mCore.getLatestMessageUId())
                && !uiConversation.isShowDraftContent()) {
            holder.setVisible(R.id.rc_conversation_read_receipt, true);
            holder.setImageResource(
                    R.id.rc_conversation_read_receipt,
                    getReadReceiptStatusResId(holder, uiConversation));
        } else {
            holder.setVisible(R.id.rc_conversation_read_receipt, false);
        }
    }
}
