package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.feature.reference.QuoteCardView;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.utils.FileTypeUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.EllipsizeTextView;
import io.rong.imkit.widget.FileRectangleProgress;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.FileMessage;
import java.util.List;

public class FileMessageItemProvider extends BaseMessageItemProvider<FileMessage> {

    private int progress = 0;

    @Override
    protected int getQuoteCardWrapperMinWidth(Context context) {
        return 0;
    }

    public FileMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
        mConfig.showProgress = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_file_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            FileMessage fileMessage,
            final UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        EllipsizeTextView tvFileName = holder.getView(R.id.rc_msg_tv_file_name);
        FileRectangleProgress fileProgress = holder.getView(R.id.rc_msg_pb_file_upload_progress);
        if (!checkViewsValid(tvFileName, fileProgress)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
            return;
        }
        boolean hasQuoteCard = QuoteCardView.shouldShowQuoteCard(uiMessage.getMessage());
        if (hasQuoteCard) {
            tvFileName.setSingleLine(true);
            tvFileName.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            tvFileName.setText(fileMessage.getName());
        } else {
            tvFileName.setSingleLine(false);
            tvFileName.setMaxLines(2);
            tvFileName.setEllipsize(null);
            tvFileName.setAdaptiveText(fileMessage.getName());
        }
        long fileSizeBytes = fileMessage.getSize();
        holder.setText(R.id.rc_msg_tv_file_size, FileTypeUtils.formatFileSize(fileSizeBytes));
        holder.setImageResource(
                R.id.rc_msg_iv_file_type_image,
                FileTypeUtils.fileTypeImageId(holder.getContext(), fileMessage.getName()));
        boolean isSender =
                uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);
        holder.setBackgroundRes(
                R.id.rc_message,
                IMKitThemeManager.dynamicResource(
                        holder.getContext(),
                        R.attr.rc_conversation_msg_special_background,
                        isSender
                                ? R.drawable.rc_bg_file_message_send
                                : R.drawable.rc_bg_file_message_receive));
        applyFileLayoutForQuote(holder, hasQuoteCard);
        applyQuoteFileCardWidth(parentHolder, hasQuoteCard);
        if (uiMessage.getMessage().getSentStatus().equals(Message.SentStatus.SENDING)
                && fileMessage.progress < 100) {
            holder.setVisible(R.id.rc_msg_pb_file_upload_progress, true);

            if (ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
                if (uiMessage.getProgress() == progress) {
                    holder.setVisible(R.id.rc_progress, true);
                    holder.setVisible(R.id.rc_btn_cancel, false);
                } else {
                    holder.setVisible(R.id.rc_progress, false);
                    holder.setVisible(R.id.rc_btn_cancel, true);
                }
            } else {
                progress = uiMessage.getProgress();
                if (progress > 0) {
                    holder.setVisible(R.id.rc_btn_cancel, true);
                    holder.setVisible(R.id.rc_progress, false);
                } else {
                    holder.setVisible(R.id.rc_btn_cancel, false);
                    holder.setVisible(R.id.rc_progress, true);
                }
            }
            holder.setHoldVisible(R.id.rc_msg_canceled, false);
            fileProgress.setProgress(uiMessage.getProgress());
        } else if (uiMessage.getMessage().getSentStatus().equals(Message.SentStatus.FAILED)
                && ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
            holder.setVisible(R.id.rc_msg_pb_file_upload_progress, true);
            holder.setVisible(R.id.rc_btn_cancel, false);
            holder.setHoldVisible(R.id.rc_msg_canceled, false);
            holder.setVisible(R.id.rc_progress, true);
            fileProgress.setProgress(uiMessage.getProgress());
        } else {
            if (uiMessage.getMessage().getSentStatus().equals(Message.SentStatus.CANCELED)) {
                holder.setHoldVisible(R.id.rc_msg_canceled, true);
            } else {
                holder.setHoldVisible(R.id.rc_msg_canceled, false);
            }
            holder.setHoldVisible(R.id.rc_msg_pb_file_upload_progress, false);
            holder.setVisible(R.id.rc_btn_cancel, false);
            holder.setVisible(R.id.rc_progress, false);
        }

        View fileRoot = holder.itemView;
        ViewGroup.LayoutParams rootLp = fileRoot.getLayoutParams();
        if (rootLp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) rootLp;
            if (hasQuoteCard) {
                int horizontalMargin = ScreenUtils.dip2px(fileRoot.getContext(), 12);
                int topMargin = ScreenUtils.dip2px(fileRoot.getContext(), 10);
                int bottomMargin = ScreenUtils.dip2px(fileRoot.getContext(), 10);
                llp.setMargins(horizontalMargin, topMargin, horizontalMargin, bottomMargin);
            } else {
                llp.setMargins(0, 0, 0, 0);
            }
            fileRoot.setLayoutParams(llp);
        }

        holder.setOnClickListener(
                R.id.rc_btn_cancel,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        IMCenter.getInstance()
                                .cancelSendMediaMessage(
                                        uiMessage.getMessage(),
                                        new RongIMClient.OperationCallback() {
                                            @Override
                                            public void onSuccess() {
                                                ResendManager.getInstance()
                                                        .removeResendMessage(
                                                                uiMessage
                                                                        .getMessage()
                                                                        .getMessageId());
                                                holder.setVisible(R.id.rc_msg_canceled, true);
                                                holder.setHoldVisible(
                                                        R.id.rc_msg_pb_file_upload_progress, false);
                                                holder.setVisible(R.id.rc_btn_cancel, false);
                                            }

                                            @Override
                                            public void onError(RongIMClient.ErrorCode errorCode) {
                                                // do nothing
                                            }
                                        });
                    }
                });
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            FileMessage fileMessage,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        RouteUtils.routeToFilePreviewActivity(
                holder.getContext(), uiMessage.getMessage(), fileMessage, uiMessage.getProgress());
        return true;
    }

    private void applyFileLayoutForQuote(ViewHolder holder, boolean hasQuoteCard) {
        Context context = holder.getContext();
        View messageView = holder.getView(R.id.rc_message);
        View contentRow = holder.getView(R.id.rc_file_content_row);
        View iconContainer = holder.getView(R.id.rc_file_icon_container);
        View infoContainer = holder.getView(R.id.rc_file_info_container);
        View fileName = holder.getView(R.id.rc_msg_tv_file_name);
        View fileSize = holder.getView(R.id.rc_msg_tv_file_size);

        int messageHeight =
                context.getResources()
                        .getDimensionPixelSize(
                                hasQuoteCard
                                        ? R.dimen.rc_quote_v2_file_card_height
                                        : R.dimen.rc_file_item_height);
        updateHeight(messageView, messageHeight);
        if (hasQuoteCard) {
            holder.setBackgroundRes(R.id.rc_message, R.drawable.rc_quote_v2_file_card_bg);
        }

        int rowInset =
                context.getResources()
                        .getDimensionPixelSize(
                                hasQuoteCard
                                        ? R.dimen.rc_quote_v2_file_card_padding
                                        : R.dimen.rc_margin_size_12);
        updateRowLayout(
                contentRow,
                hasQuoteCard
                        ? ViewGroup.LayoutParams.MATCH_PARENT
                        : ViewGroup.LayoutParams.WRAP_CONTENT,
                rowInset,
                hasQuoteCard ? rowInset : 0,
                hasQuoteCard
                        ? 0
                        : context.getResources().getDimensionPixelSize(R.dimen.rc_margin_size_12));

        int iconSize =
                context.getResources()
                        .getDimensionPixelSize(
                                hasQuoteCard
                                        ? R.dimen.rc_quote_v2_file_card_icon_size
                                        : R.dimen.rc_file_item_icon_size);
        int iconTextSpacing =
                context.getResources()
                        .getDimensionPixelSize(
                                hasQuoteCard
                                        ? R.dimen.rc_quote_v2_file_card_icon_text_spacing
                                        : R.dimen.rc_margin_size_12);
        updateSizeAndEndMargin(iconContainer, iconSize, iconSize, iconTextSpacing);

        int infoHeight =
                context.getResources()
                        .getDimensionPixelSize(
                                hasQuoteCard
                                        ? R.dimen.rc_quote_v2_file_body_content_height
                                        : R.dimen.rc_file_item_content_height);
        updateHeight(infoContainer, infoHeight);

        updateTopMargin(fileName, hasQuoteCard ? 0 : -ScreenUtils.dip2px(context, 3));
        updateBottomMargin(fileSize, hasQuoteCard ? 0 : -ScreenUtils.dip2px(context, 2));
    }

    private void applyQuoteFileCardWidth(ViewHolder parentHolder, boolean hasQuoteCard) {
        QuoteCardView quoteCard = findQuoteCard(parentHolder);
        if (quoteCard == null) {
            return;
        }
        int width =
                hasQuoteCard
                        ? parentHolder
                                .getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.rc_file_item_width)
                        : 0;
        quoteCard.setFileCardMinWidthOverride(width);
    }

    private void updateHeight(View view, int height) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null || lp.height == height) {
            return;
        }
        lp.height = height;
        view.setLayoutParams(lp);
    }

    private void updateRowLayout(
            View view, int height, int marginStart, int marginEnd, int topMargin) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
            llp.height = height;
            llp.setMarginStart(marginStart);
            llp.setMarginEnd(marginEnd);
            llp.topMargin = topMargin;
            view.setLayoutParams(llp);
        }
    }

    private void updateSizeAndEndMargin(View view, int width, int height, int marginEnd) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams llp = (LinearLayout.LayoutParams) lp;
            llp.width = width;
            llp.height = height;
            llp.setMarginEnd(marginEnd);
            view.setLayoutParams(llp);
        }
    }

    private void updateTopMargin(View view, int topMargin) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.topMargin = topMargin;
            view.setLayoutParams(mlp);
        }
    }

    private void updateBottomMargin(View view, int bottomMargin) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.bottomMargin = bottomMargin;
            view.setLayoutParams(mlp);
        }
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof FileMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, FileMessage fileMessage) {
        if (fileMessage != null && !TextUtils.isEmpty(fileMessage.getName())) {
            return new SpannableString(
                    context.getString(R.string.rc_conversation_summary_content_file)
                            + fileMessage.getName());
        }
        return new SpannableString(
                context.getString(R.string.rc_conversation_summary_content_file));
    }
}
