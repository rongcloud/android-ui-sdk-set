package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.message.RichContentMessage;

public class RichContentMessageItemProvider extends BaseMessageItemProvider<RichContentMessage> {

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_rich_content_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, RichContentMessage richContentMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.rc_title, richContentMessage.getTitle());
        holder.setText(R.id.rc_content, richContentMessage.getContent());
        if (!TextUtils.isEmpty(richContentMessage.getImgUrl())) {
            Glide.with(holder.getContext()).load(richContentMessage.getImgUrl())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into((ImageView) holder.getView(R.id.rc_img));
        }
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, RichContentMessage richContentMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        RouteUtils.routeToWebActivity(holder.getContext(), richContentMessage.getUrl());
        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof RichContentMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, RichContentMessage richContentMessage) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_rich_text));
    }
}
