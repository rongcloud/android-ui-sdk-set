package io.rong.imkit.feature.publicservice.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.publicservice.message.PublicServiceRichContentMessage;
import io.rong.message.RichContentItem;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PublicServiceRichContentMessageProvider
        extends BaseMessageItemProvider<PublicServiceRichContentMessage> {
    private final String TAG = getClass().getSimpleName();

    /** 公众号距右边屏幕间隔 */
    private final int RIGHT_PADDING = 14;

    public PublicServiceRichContentMessageProvider() {
        mConfig.showPortrait = false;
    }

    @Override
    protected io.rong.imkit.widget.adapter.ViewHolder onCreateMessageContentViewHolder(
            ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.rc_item_public_service_rich_content_message,
                                parent,
                                false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            io.rong.imkit.widget.adapter.ViewHolder holder,
            io.rong.imkit.widget.adapter.ViewHolder parentHolder,
            PublicServiceRichContentMessage publicServiceRichContentMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        ImageView imageView = holder.getView(R.id.rc_img);
        if (!checkViewsValid(imageView)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
            return;
        }

        if (publicServiceRichContentMessage.getMessage() != null) {
            holder.setText(R.id.rc_title, publicServiceRichContentMessage.getMessage().getTitle());
            holder.setText(
                    R.id.rc_content, publicServiceRichContentMessage.getMessage().getDigest());
            Glide.with(holder.getContext())
                    .load(publicServiceRichContentMessage.getMessage().getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(imageView);
        }
        String time =
                formatDate(
                        uiMessage.getReceivedTime(),
                        holder.getContext().getString(R.string.rc_date_format));
        holder.setText(R.id.rc_time, time);
        ViewGroup.LayoutParams params = holder.getConvertView().getLayoutParams();
        params.width = RongUtils.getScreenWidth() - RongUtils.dip2px(RIGHT_PADDING);

        holder.getConvertView().setLayoutParams(params);
        holder.getConvertView().requestLayout();
    }

    @Override
    protected boolean onItemClick(
            io.rong.imkit.widget.adapter.ViewHolder holder,
            PublicServiceRichContentMessage content,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        String url = "";
        RichContentItem richContentItem = content.getMessage();
        if (richContentItem != null) {
            url = richContentItem.getUrl();
        } else {
            RLog.e(TAG, "onItemClick RichContentItem is Null");
        }
        RouteUtils.routeToWebActivity(holder.getContext(), url);
        return true;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof PublicServiceRichContentMessage;
    }

    @Override
    public Spannable getSummarySpannable(
            Context context, PublicServiceRichContentMessage publicServiceRichContentMessage) {
        if (publicServiceRichContentMessage != null
                && publicServiceRichContentMessage.getMessage() != null) {
            return new SpannableString(publicServiceRichContentMessage.getMessage().getTitle());
        } else {
            RLog.e(TAG, "The content of the message is null! Check your message content!");
            return new SpannableString("");
        }
    }

    private String formatDate(long timeMillis, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timeMillis));
    }
}
