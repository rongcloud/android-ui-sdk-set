package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.message.HistoryDividerMessage;

public class HistoryDivMessageItemProvider extends BaseNotificationMessageItemProvider<HistoryDividerMessage> {


    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_new_message_divider, parent, false);
        return new ViewHolder(parent.getContext(), rootView);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder,ViewHolder parentHolder, HistoryDividerMessage msg, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.tv_divider_message, msg.getContent());
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof HistoryDividerMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, HistoryDividerMessage historyDividerMessage) {
        return null;
    }

}
