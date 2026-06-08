package io.rong.imkit.feature.reference;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.activity.FilePreviewActivity;
import io.rong.imkit.activity.PicturePagerActivity;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imkit.widget.ILinkClickListener;
import io.rong.imkit.widget.LinkTextViewMovementMethod;
import io.rong.imkit.widget.ReferenceDialog;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.FileMessage;
import io.rong.message.ImageMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.SightMessage;
import io.rong.message.StreamMessage;
import io.rong.message.TextMessage;
import java.util.List;

/**
 * V1 引用消息 cell。
 *
 * <p>引用区使用 {@link QuoteCardView}，与 V2 视觉一致（对齐 iOS RCReferencedContentView 的两端共用模式）。 Provider
 * 仅负责发送方文本渲染、引用点击交互、及高 DPI 下的容器宽度调整。
 */
public class ReferenceMessageItemProvider extends BaseMessageItemProvider<ReferenceMessage> {
    private static final int MAX_DENSITY_DPI = 500;
    private static final int STANDARD_DEFAULT_DENSITY_DPI = 440;
    private static final int DATUM_DENSITY_DPI = 160;

    private static final String TAG = "ReferenceMessageItemProvider";

    public ReferenceMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showEditState = true;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_reference_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        // 1. 发送方文本（用户自己回复的内容）
        TextView referenceSendContent = holder.getView(R.id.rc_msg_tv_reference_send_content);
        if (referenceSendContent != null && referenceMessage.getEditSendText() != null) {
            setTextContent(referenceSendContent, uiMessage, referenceMessage.getEditSendText());
            setMovementMethod(uiMessage, referenceSendContent);
            referenceSendContent.setOnLongClickListener(
                    v -> parentHolder.getView(R.id.rc_content).performLongClick());
        }

        // 2. 引用区交给 QuoteCardView 渲染（与 V2 风格一致）
        QuoteCardView quoteCard = holder.getView(R.id.rc_reference_quote_card);
        if (quoteCard != null) {
            Message wrappingMessage = uiMessage.getMessage();
            quoteCard.setReferenceMessage(wrappingMessage);
            quoteCard.setOnClickListener(
                    v -> {
                        if (isInvalidReferenceStatus(referenceMessage)) {
                            return;
                        }
                        handleQuoteClick(
                                v.getContext(), wrappingMessage, referenceMessage, uiMessage);
                    });
            quoteCard.setOnLongClickListener(
                    v -> parentHolder.getView(R.id.rc_content).performLongClick());
        }

        setMaximumDisplaySize(holder);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean onItemLongClick(
            ViewHolder holder,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof ReferenceMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, ReferenceMessage referenceMessage) {
        if (referenceMessage != null && !TextUtils.isEmpty(referenceMessage.getEditSendText())) {
            return new SpannableString(referenceMessage.getEditSendText());
        }
        return null;
    }

    /** 把发送方文本（含 emoji/链接 spannable 化）写入 TextView，并缓存 spannable 到 UiMessage 上。 */
    private void setTextContent(final TextView textView, final UiMessage data, String content) {
        textView.setTag(data.getMessageId());
        if (data.getContentSpannable() == null) {
            Runnable textViewRunnable =
                    () -> setTextMessageContent(textView, data, data.getContentSpannable());
            SpannableStringBuilder spannable =
                    TextViewUtils.getSpannable(
                            content,
                            false,
                            new TextViewUtils.RegularCallBack() {
                                @Override
                                public void finish(SpannableStringBuilder spannable) {
                                    data.setContentSpannable(spannable);
                                    if (textView.getTag().equals(data.getMessageId())) {
                                        textView.post(textViewRunnable);
                                    }
                                }
                            });
            data.setContentSpannable(spannable);
        }
        setTextMessageContent(textView, data, data.getContentSpannable());
    }

    private void setMovementMethod(final UiMessage uiMessage, final TextView textView) {
        textView.setMovementMethod(
                new LinkTextViewMovementMethod(
                        new ILinkClickListener() {
                            @Override
                            public boolean onLinkClick(String link) {
                                boolean handled = false;
                                if (RongConfigCenter.conversationConfig()
                                                .getConversationClickListener()
                                        != null) {
                                    handled =
                                            RongConfigCenter.conversationConfig()
                                                    .getConversationClickListener()
                                                    .onMessageLinkClick(
                                                            textView.getContext(),
                                                            link,
                                                            uiMessage.getMessage());
                                }
                                if (handled) return true;
                                String lower = link.toLowerCase();
                                if (lower.startsWith("http") || lower.startsWith("https")) {
                                    RouteUtils.routeToWebActivity(textView.getContext(), link);
                                    return true;
                                }
                                return false;
                            }
                        }));
    }

    /** 引用区点击：根据被引用消息类型跳转预览。 */
    private void handleQuoteClick(
            Context context,
            Message wrappingMessage,
            ReferenceMessage referenceMessage,
            UiMessage uiMessage) {
        MessageContent referContent = referenceMessage.getReferenceContent();
        if (referContent == null) {
            return;
        }
        try {
            if (referContent instanceof ImageMessage) {
                Intent intent = new Intent(context, PicturePagerActivity.class);
                intent.setPackage(context.getPackageName());
                intent.putExtra("message", wrappingMessage);
                context.startActivity(intent);
            } else if (referContent instanceof SightMessage) {
                RLog.d(TAG, "Quote clicked on sight message, not handled yet");
            } else if (referContent instanceof FileMessage) {
                Intent intent = new Intent(context, FilePreviewActivity.class);
                intent.setPackage(context.getPackageName());
                intent.putExtra("FileMessage", referContent);
                intent.putExtra("Message", wrappingMessage);
                intent.putExtra("Progress", uiMessage.getProgress());
                context.startActivity(intent);
            } else if (referContent instanceof RichContentMessage) {
                String url = ((RichContentMessage) referContent).getUrl();
                if (!TextUtils.isEmpty(url)) {
                    RouteUtils.routeToWebActivity(context, url);
                }
            } else if (referContent instanceof TextMessage
                    || referContent instanceof StreamMessage
                    || referContent instanceof ReferenceMessage) {
                showPopWindow(context, uiMessage);
                hideInputKeyboard(context);
            }
        } catch (Exception e) {
            RLog.e(TAG, "handleQuoteClick", e);
        }
    }

    private void hideInputKeyboard(Context context) {
        if (!(context instanceof FragmentActivity)) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        FragmentActivity activity = (FragmentActivity) context;
        View focus = activity.getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private boolean isInvalidReferenceStatus(ReferenceMessage referenceMessage) {
        ReferenceMessage.ReferenceMessageStatus status = referenceMessage.getReferMsgStatus();
        return status == ReferenceMessage.ReferenceMessageStatus.DELETE
                || status == ReferenceMessage.ReferenceMessageStatus.RECALLED;
    }

    private void showPopWindow(Context context, UiMessage uiMessage) {
        if (context instanceof FragmentActivity) {
            new ReferenceDialog(uiMessage)
                    .show(((FragmentActivity) context).getSupportFragmentManager());
        }
    }

    /**
     * 高 DPI 设备 + 竖屏下，按 dp 重算 root 宽度，避免 UI 异常放大。
     *
     * @param holder ViewHolder
     */
    private void setMaximumDisplaySize(ViewHolder holder) {
        Resources resources = holder.getContext().getResources();
        if (resources == null) {
            return;
        }
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        if (metrics.densityDpi > MAX_DENSITY_DPI
                && config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            float dimensionValue =
                    holder.getContext().getResources().getDimension(R.dimen.rc_reference_width);
            float dbValue = dimensionValue / metrics.density;
            float viewWidthValue = dbValue * STANDARD_DEFAULT_DENSITY_DPI / DATUM_DENSITY_DPI;
            LinearLayout rootView = holder.getView(R.id.rc_reference_root_view);
            ViewGroup.LayoutParams params = rootView.getLayoutParams();
            params.width = (int) viewWidthValue;
            params.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            rootView.setLayoutParams(params);
        }
    }
}
