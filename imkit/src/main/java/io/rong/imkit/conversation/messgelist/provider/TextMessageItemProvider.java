package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.LayoutDirection;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.TextUtilsCompat;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;
import java.util.List;
import java.util.Locale;

public class TextMessageItemProvider extends BaseMessageItemProvider<TextMessage> {

    public TextMessageItemProvider() {
        mConfig.showReadState = true;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_translate_text_message_item, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            TextMessage message,
            final UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        final TextView textView = holder.getView(R.id.rc_text);
        final TextView translatedView = holder.getView(R.id.rc_translated_text);
        final ProgressBar progressBar = holder.getView(R.id.rc_pb_translating);

        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        textView.setTag(uiMessage.getMessageId());
        if (uiMessage.getContentSpannable() == null) {
            SpannableStringBuilder spannable =
                    TextViewUtils.getSpannable(
                            message.getContent(),
                            new TextViewUtils.RegularCallBack() {
                                @Override
                                public void finish(SpannableStringBuilder spannable) {
                                    uiMessage.setContentSpannable(spannable);
                                    if (textView.getTag().equals(uiMessage.getMessageId())) {
                                        textView.post(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        textView.setText(
                                                                uiMessage.getContentSpannable());
                                                    }
                                                });
                                    }
                                }
                            });
            uiMessage.setContentSpannable(spannable);
        }
        textView.setText(uiMessage.getContentSpannable());
        textView.setMovementMethod(
                new LinkTextViewMovementMethod(
                        new ILinkClickListener() {
                            @Override
                            public boolean onLinkClick(String link) {
                                boolean result = false;
                                if (RongConfigCenter.conversationConfig()
                                                .getConversationClickListener()
                                        != null) {
                                    result =
                                            RongConfigCenter.conversationConfig()
                                                    .getConversationClickListener()
                                                    .onMessageLinkClick(
                                                            holder.getContext(),
                                                            link,
                                                            uiMessage.getMessage());
                                }
                                if (result) return true;
                                String str = link.toLowerCase();
                                if (str.startsWith("http") || str.startsWith("https")) {
                                    RouteUtils.routeToWebActivity(textView.getContext(), link);
                                    result = true;
                                }

                                return result;
                            }
                        }));
        textView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ViewParent parent = view.getParent();
                        if (parent instanceof View) {
                            ((View) parent).performClick();
                        }
                    }
                });

        textView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        ViewParent parent = view.getParent();
                        if (parent instanceof View) {
                            return ((View) parent).performLongClick();
                        }
                        return false;
                    }
                });

        boolean isSender =
                uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);

        if (mConfig.showContentBubble) {
            holder.setBackgroundRes(
                    R.id.rc_text,
                    isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        }
        if (uiMessage.getTranslateStatus() == State.SUCCESS
                && !TextUtils.isEmpty(uiMessage.getTranslatedContent())) {
            translatedView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            translatedView.setText(uiMessage.getTranslatedContent());
            holder.setBackgroundRes(
                    R.id.rc_translated_text,
                    isSender
                            ? R.drawable.rc_ic_translation_bubble_right
                            : R.drawable.rc_ic_translation_bubble_left);
        } else if (uiMessage.getTranslateStatus() == State.PROGRESS) {
            translatedView.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            translatedView.setVisibility(View.GONE);
            holder.setBackgroundRes(
                    R.id.rc_pb_translating,
                    isSender
                            ? R.drawable.rc_ic_translation_bubble_right
                            : R.drawable.rc_ic_translation_bubble_left);
        } else {
            translatedView.setText(null);
            translatedView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            translatedView.setBackground(null);
        }

        setDirection(textView, isSender);
        setDirection(translatedView, isSender);
        setDirection(progressBar, isSender);

        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewParent parent = v.getParent();
                        if (parent instanceof View) {
                            ((View) parent).performClick();
                        }
                    }
                });
        holder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        ViewParent parent = v.getParent();
                        if (parent instanceof View) {
                            return ((View) parent).performLongClick();
                        }
                        return false;
                    }
                });
    }

    private void setDirection(View view, boolean isSender) {
        ConstraintLayout.LayoutParams lp = ((ConstraintLayout.LayoutParams) view.getLayoutParams());
        if (isSender) {
            lp.startToStart = ConstraintLayout.LayoutParams.UNSET;
            lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        } else {
            lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            lp.endToEnd = ConstraintLayout.LayoutParams.UNSET;
        }
        view.setLayoutParams(lp);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            TextMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof TextMessage && !messageContent.isDestruct();
    }

    @Override
    public Spannable getSummarySpannable(Context context, TextMessage message) {
        if (message != null && !TextUtils.isEmpty(message.getContent())) {
            String content = message.getContent();
            content = content.replace("\n", " ");
            if (content.length() > 100) {
                content = content.substring(0, 100);
            }
            return new SpannableString(AndroidEmoji.ensure(content));
        } else {
            return new SpannableString("");
        }
    }

    @Override
    public boolean showBubble() {
        return false;
    }
}
