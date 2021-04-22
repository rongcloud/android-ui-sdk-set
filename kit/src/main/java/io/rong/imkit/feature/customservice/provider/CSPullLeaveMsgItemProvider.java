package io.rong.imkit.feature.customservice.provider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.processor.ConversationProcessorFactory;
import io.rong.imkit.conversation.messgelist.provider.BaseNotificationMessageItemProvider;
import io.rong.imkit.feature.customservice.CustomServiceBusinessProcessor;
import io.rong.imkit.feature.customservice.CSLeaveMessageActivity;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.cs.CustomServiceConfig;
import io.rong.imlib.cs.message.CSPullLeaveMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;

public class CSPullLeaveMsgItemProvider extends BaseNotificationMessageItemProvider<CSPullLeaveMessage> {
    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_information_notification_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder,ViewHolder parentHolder, CSPullLeaveMessage csPullLeaveMessage, final UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (csPullLeaveMessage != null) {
            String content = csPullLeaveMessage.getContent();
            if (!TextUtils.isEmpty(content)) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                String filter = holder.getContext().getResources().getString(R.string.rc_cs_leave_message);
                int startPos = content.indexOf(filter);
                if (startPos >= 0) {
                    SpannableString filterString = new SpannableString(content.substring(startPos, startPos + filter.length()));
                    filterString.setSpan(new ForegroundColorSpan(holder.getContext().getResources().getColor(R.color.rc_voice_color)), 0, filterString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    filterString.setSpan(new Clickable(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onLeaveMessageClicked(v, uiMessage);
                        }
                    }), 0, filterString.length(), Spanned.SPAN_MARK_MARK);

                    String preText = content.substring(0, startPos);
                    String endText = content.substring(startPos + filter.length());

                    if (!preText.endsWith(" ")) {
                        builder.append(preText).append(" ").append(filterString).append(endText);
                    } else {
                        builder.append(preText).append(filterString).append(endText);
                    }
                } else {
                    builder.append(content);
                }
                TextView textView = holder.getView(R.id.rc_msg);
                textView.setText(builder);
                textView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private void onLeaveMessageClicked(View v, UiMessage message) {
        Intent intent;
        CustomServiceBusinessProcessor processor = (CustomServiceBusinessProcessor) ConversationProcessorFactory.getInstance()
                .getProcessor(Conversation.ConversationType.CUSTOMER_SERVICE);
        if (processor == null || processor.getCustomServiceConfig() == null) {
            return;
        }
        if (processor.getCustomServiceConfig() != null
                && processor.getCustomServiceConfig().leaveMessageConfigType.equals(CustomServiceConfig.CSLeaveMessageType.WEB)) {
            RouteUtils.routeToWebActivity(v.getContext(), processor.getCustomServiceConfig().uri.toString());
        } else if (processor.getCustomServiceConfig() != null) {
            intent = new Intent(v.getContext(), CSLeaveMessageActivity.class);
            intent.putExtra("targetId", message.getMessage().getTargetId());
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("itemList", processor.getCustomServiceConfig().leaveMessageNativeInfo);
            intent.putExtras(bundle);
            v.getContext().startActivity(intent);
        }
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof CSPullLeaveMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, CSPullLeaveMessage csPullLeaveMessage) {
        return null;
    }

    class Clickable extends ClickableSpan implements View.OnClickListener {
        private final View.OnClickListener mListener;

        public Clickable(View.OnClickListener listener) {
            mListener = listener;
        }

        @Override
        public void onClick(View view) {
            mListener.onClick(view);
        }
    }
}
