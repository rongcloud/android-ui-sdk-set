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
import android.widget.TextView;

import androidx.core.text.TextUtilsCompat;

import java.util.List;
import java.util.Locale;

import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;

public class TextMessageItemProvider extends BaseMessageItemProvider<TextMessage> {

    public TextMessageItemProvider() {
        mConfig.showReadState = true;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View textView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_text_message_item, parent, false);
        return new ViewHolder(parent.getContext(), textView);
    }

    @Override
    protected void bindMessageContentViewHolder(final ViewHolder holder, ViewHolder parentHolder, TextMessage message, final UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        final TextView view = holder.getView(R.id.rc_text);

        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == LayoutDirection.RTL) {
            view.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        view.setTag(uiMessage.getMessageId());
        if (uiMessage.getContentSpannable() == null) {
            SpannableStringBuilder spannable = TextViewUtils.getSpannable(message.getContent(), new TextViewUtils.RegularCallBack() {
                @Override
                public void finish(SpannableStringBuilder spannable) {
                    uiMessage.setContentSpannable(spannable);
                    if (view.getTag().equals(uiMessage.getMessageId())) {
                        view.post(new Runnable() {
                            @Override
                            public void run() {
                                view.setText(uiMessage.getContentSpannable());
                            }
                        });
                    }
                }
            });
            uiMessage.setContentSpannable(spannable);
        }
        view.setText(uiMessage.getContentSpannable());
        view.setMovementMethod(new LinkTextViewMovementMethod(new ILinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                boolean result = false;
                if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                    result = RongConfigCenter.conversationConfig().getConversationClickListener().onMessageLinkClick(holder.getContext(), link, uiMessage.getMessage());
                }
                if (result)
                    return true;
                String str = link.toLowerCase();
                if (str.startsWith("http") || str.startsWith("https")) {
                    RouteUtils.routeToWebActivity(view.getContext(), link);
                    result = true;
                }

                return result;
            }
        }));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewParent parent = view.getParent();
                if (parent instanceof View) {
                    ((View) parent).performClick();
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ViewParent parent = view.getParent();
                if (parent instanceof View) {
                    return ((View) parent).performLongClick();
                }
                return false;
            }
        });
    }

    @Override
    protected boolean onItemClick(ViewHolder holder, TextMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
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
}
