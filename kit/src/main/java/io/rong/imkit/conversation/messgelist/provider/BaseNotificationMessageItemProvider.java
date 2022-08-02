package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.HistoryDividerMessage;

public abstract class BaseNotificationMessageItemProvider<T extends MessageContent> implements IMessageProvider<T> {
    private static final String TAG = "BaseMessageItemProvider";


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_notification_message_item, parent, false);
        FrameLayout contentView = rootView.findViewById(R.id.rc_content);
        ViewHolder childViewHolder = onCreateMessageContentViewHolder(contentView, viewType);
        if (childViewHolder != null) {
            if (contentView.getChildCount() == 0) {
                contentView.addView(childViewHolder.itemView);
            }
        }
        return new MessageViewHolder(rootView.getContext(), rootView, childViewHolder);
    }

    protected abstract ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType);

    protected abstract void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, T t, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener);

    @Override
    public void bindViewHolder(final ViewHolder holder, final UiMessage uiMessage, final int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (uiMessage != null && uiMessage.getMessage() != null && listener != null) {
            Message message = uiMessage.getMessage();
            initTime(holder, position, list, message);
            if (holder instanceof MessageViewHolder) {
                bindMessageContentViewHolder(((MessageViewHolder) holder).getChildViewHolder(), holder, (T) uiMessage.getMessage().getContent(), uiMessage, position, list, listener);
            } else {
                RLog.e(TAG, "holder is not MessageViewHolder");
            }
            uiMessage.setChange(false);
        } else {
            RLog.e(TAG, "uiMessage is null");
        }
    }

    private void initTime(ViewHolder holder, int position, List<UiMessage> data, Message message) {
        String time = RongDateUtils.getConversationFormatDate(message.getSentTime(), holder.getContext());
        holder.setText(R.id.rc_time, time);
        if (position == 0) {
            holder.setVisible(R.id.rc_time, !(message.getContent() instanceof HistoryDividerMessage));
        } else {
            UiMessage pre = data.get(position - 1);
            if (pre.getMessage() != null && RongDateUtils.isShowChatTime(message.getSentTime(), pre.getMessage().getSentTime(), 180)) {
                holder.setVisible(R.id.rc_time, true);
            } else {
                holder.setVisible(R.id.rc_time, false);
            }
        }
    }


    private static class MessageViewHolder extends ViewHolder {
        private ViewHolder mChildViewHolder;

        public MessageViewHolder(Context context, View itemView, ViewHolder childViewHolder) {
            super(context, itemView);
            mChildViewHolder = childViewHolder;
        }

        public ViewHolder getChildViewHolder() {
            return mChildViewHolder;
        }
    }

    @Override
    public boolean isSummaryType(MessageContent messageContent) {
        return isMessageViewType(messageContent);
    }

    @Override
    public boolean isItemViewType(UiMessage item) {
        return isMessageViewType(item.getMessage().getContent());
    }


    protected abstract boolean isMessageViewType(MessageContent messageContent);

    @Override
    public boolean showSummaryWithName() {
        return false;
    }
}
