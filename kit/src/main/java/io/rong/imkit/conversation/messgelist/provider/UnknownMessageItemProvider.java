package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UnknownMessage;

public class UnknownMessageItemProvider extends BaseNotificationMessageItemProvider<UnknownMessage> {
    private static final String TAG = "UnknownMessageItemProvider";


    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_group_information_notification_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder,ViewHolder parentHolder, UnknownMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.rc_msg, holder.getContext().getString(R.string.rc_message_unknown));
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof UnknownMessage;
    }


    @Override
    public Spannable getSummarySpannable(Context context, UnknownMessage unknownMessage) {
       return new SpannableString(context.getResources().getString(R.string.rc_message_unknown));
    }
}
