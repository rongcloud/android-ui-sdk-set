package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.message.InformationNotificationMessage;

public class InformationNotificationMessageItemProvider extends BaseNotificationMessageItemProvider<InformationNotificationMessage> {

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_information_notification_message, parent, false);
        return new ViewHolder(parent.getContext(), rootView);
    }

    @Override
    public boolean isItemViewType(UiMessage item) {
        return item.getMessage().getContent() instanceof InformationNotificationMessage;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof InformationNotificationMessage;
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder,ViewHolder parentHolder, InformationNotificationMessage content, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.rc_msg, content.getMessage());
    }



    @Override
    public Spannable getSummarySpannable(Context context, InformationNotificationMessage data) {
        if (data != null && !TextUtils.isEmpty(data.getMessage()))
            return new SpannableString(data.getMessage());
        return null;
    }
}
