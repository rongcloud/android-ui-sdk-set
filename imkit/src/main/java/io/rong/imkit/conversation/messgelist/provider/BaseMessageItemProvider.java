package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.activity.FilePreviewActivity;
import io.rong.imkit.activity.PicturePagerActivity;
import io.rong.imkit.config.ConversationClickListener;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.editmessage.EditMessageManager;
import io.rong.imkit.feature.reaction.OnMessageReactionClickListener;
import io.rong.imkit.feature.reaction.ReactionDetailDialog;
import io.rong.imkit.feature.reaction.ReactionView;
import io.rong.imkit.feature.reference.QuoteCardView;
import io.rong.imkit.feature.reference.QuoteMessageBatchLoader;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.handler.AppSettingsHandler;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.MessageModifyInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReadReceiptInfoV5;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.HistoryDividerMessage;
import io.rong.message.ImageMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.SightMessage;
import io.rong.message.TextMessage;
import java.util.List;
import java.util.Objects;

public abstract class BaseMessageItemProvider<T extends MessageContent>
        implements IMessageProvider<T> {
    protected static final String TAG = "BaseMessageItemProvider";
    protected MessageItemProviderConfig mConfig = new MessageItemProviderConfig();

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_message_item, parent, false);
        FrameLayout contentView = rootView.findViewById(R.id.rc_content);
        ViewHolder contentViewHolder = onCreateMessageContentViewHolder(contentView, viewType);
        if (contentViewHolder != null) {
            if (contentView.getChildCount() == 0) {
                // 用竖向 LinearLayout 包装消息内容和 V2 引用卡片
                LinearLayout wrapper = new LinearLayout(parent.getContext());
                wrapper.setOrientation(LinearLayout.VERTICAL);
                wrapper.setLayoutParams(
                        new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT));

                // 子 view 通过 parent.performClick/LongClick 透传事件，
                // 需要让 wrapper 继续向上委托到 rc_content，否则事件链断裂
                final FrameLayout parentContent = contentView;
                wrapper.setOnClickListener(v -> parentContent.performClick());
                wrapper.setOnLongClickListener(v -> parentContent.performLongClick());

                // V2 引用卡片在上，默认 GONE
                QuoteCardView quoteCard = new QuoteCardView(parent.getContext());
                LinearLayout.LayoutParams quoteCardLp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                quoteCardLp.topMargin =
                        parent.getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.rc_reference_margin_top);
                quoteCard.setLayoutParams(quoteCardLp);
                quoteCard.setVisibility(View.GONE);
                quoteCard.setOnClickListener(v -> parentContent.performClick());
                quoteCard.setOnLongClickListener(v -> parentContent.performLongClick());
                wrapper.addView(quoteCard);

                // 引用卡片与消息体之间的分隔横线。
                View quoteDivider = new View(parent.getContext());
                quoteDivider.setBackgroundColor(
                        parent.getContext().getResources().getColor(R.color.rc_divider_color));
                int dividerHeight =
                        parent.getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.rc_quote_v2_divider_height);
                int dividerTopMargin =
                        parent.getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.rc_quote_v2_divider_top_spacing);
                int dividerBottomMargin =
                        parent.getContext()
                                .getResources()
                                .getDimensionPixelSize(R.dimen.rc_quote_v2_divider_bottom_spacing);
                int dividerHMargin =
                        (int)
                                (parent.getContext().getResources().getDisplayMetrics().density * 12
                                        + 0.5f);
                LinearLayout.LayoutParams dividerLp =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, dividerHeight);
                dividerLp.setMarginStart(dividerHMargin);
                dividerLp.setMarginEnd(dividerHMargin);
                dividerLp.topMargin = dividerTopMargin;
                dividerLp.bottomMargin = dividerBottomMargin;
                quoteDivider.setLayoutParams(dividerLp);
                quoteDivider.setVisibility(View.GONE);
                quoteDivider.setTag("quote_card_divider");
                wrapper.addView(quoteDivider);

                // 消息内容在下
                wrapper.addView(contentViewHolder.itemView);

                contentView.addView(wrapper);
            }
        }
        return new MessageViewHolder(rootView.getContext(), rootView, contentViewHolder);
    }

    /**
     * 创建 ViewHolder
     *
     * @param parent 父 ViewGroup
     * @param viewType 视图类型
     * @return ViewHolder
     */
    protected abstract ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType);

    @Override
    public boolean isItemViewType(UiMessage item) {
        return isMessageViewType(item.getMessage().getContent());
    }

    @Override
    public void bindViewHolder(
            final ViewHolder holder,
            final UiMessage uiMessage,
            final int position,
            final List<UiMessage> list,
            final IViewProviderListener<UiMessage> listener) {
        if (uiMessage != null && uiMessage.getMessage() != null && listener != null) {
            Message message = uiMessage.getMessage();
            holder.setVisible(R.id.rc_selected, uiMessage.isEdit());
            holder.setVisible(R.id.rc_v_edit, uiMessage.isEdit());
            if (uiMessage.isEdit()) {
                holder.setSelected(R.id.rc_selected, uiMessage.isSelected());
                holder.setOnClickListener(
                        R.id.rc_v_edit,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listener.onViewClick(MessageClickType.EDIT_CLICK, uiMessage);
                            }
                        });
            }
            boolean isSender =
                    uiMessage
                            .getMessage()
                            .getMessageDirection()
                            .equals(Message.MessageDirection.SEND);
            initTime(holder, position, list, message);
            initUserInfo(holder, uiMessage, position, listener, isSender);
            initContent(holder, isSender, uiMessage, position, listener, list);
            initStatus(holder, uiMessage, position, listener, message, isSender, list);
            bindReactionView(holder, uiMessage, listener);

            if (holder instanceof MessageViewHolder) {
                T msgContent = null;
                try {
                    msgContent = (T) uiMessage.getMessage().getContent();
                } catch (ClassCastException e) {
                    RLog.e(TAG, "bindViewHolder MessageContent cast Exception, e:" + e);
                }
                if (msgContent != null) {
                    bindMessageContentViewHolder(
                            ((MessageViewHolder) holder).getMessageContentViewHolder(),
                            holder,
                            msgContent,
                            uiMessage,
                            position,
                            list,
                            listener);
                } else {
                    RLog.e(TAG, "bindViewHolder MessageContent cast Exception");
                }
                // V2 引用卡片绑定
                bindQuoteCard(holder, uiMessage, listener, list);
            } else {
                RLog.e(TAG, "holder is not MessageViewHolder");
            }
            uiMessage.setChange(false);
        } else {
            RLog.e(TAG, "uiMessage is null");
        }
    }

    private void bindReactionView(
            ViewHolder holder, UiMessage uiMessage, IViewProviderListener<UiMessage> listener) {
        ReactionView reactionView = holder.getView(R.id.rc_reaction_view);
        if (reactionView == null) {
            return;
        }
        if (!RongConfigCenter.featureConfig().isMessageReactionEnable()) {
            reactionView.setVisibility(View.GONE);
            resetReactionContainer(holder, reactionView);
            return;
        }
        List<io.rong.imlib.model.MessageReaction> reactions = uiMessage.getReactions();
        if (reactions != null && !reactions.isEmpty()) {
            reactionView.setVisibility(View.VISIBLE);
            Message message = uiMessage.getMessage();
            reactionView.setMessageContext(
                    message == null ? null : message.getConversationType(),
                    message == null ? null : message.getTargetId());
            reactionView.setReactions(reactions);
            applyReactionContainer(holder, reactionView, uiMessage);
            final List<io.rong.imlib.model.MessageReaction> finalReactions = reactions;
            // 点击已有回应 → toggle（已回应则移除，否则添加）
            reactionView.setOnReactionItemClickListener(
                    reaction -> {
                        if (reaction == null || listener == null) {
                            return;
                        }
                        listener.onReactionClick(uiMessage, reaction);
                    });
            reactionView.setOnMoreClickListener(
                    (allReactions, selectedReaction) -> {
                        OnMessageReactionClickListener reactionClickListener =
                                RongConfigCenter.featureConfig()
                                        .getOnMessageReactionClickListener();
                        if (selectedReaction != null
                                && reactionClickListener != null
                                && reactionClickListener.onMessageReactionDetailClicked(
                                        selectedReaction, uiMessage)) {
                            return;
                        }
                        Context ctx = holder.getContext();
                        if (ctx instanceof androidx.fragment.app.FragmentActivity) {
                            String selectedReactionId =
                                    selectedReaction == null
                                            ? null
                                            : selectedReaction.getReactionId();
                            ReactionDetailDialog dialog =
                                    ReactionDetailDialog.newInstance(
                                            uiMessage.getMessage() != null
                                                    ? uiMessage.getMessage().getUId()
                                                    : "",
                                            uiMessage.getMessage() != null
                                                    ? uiMessage.getMessage().getConversationType()
                                                    : null,
                                            uiMessage.getMessage() != null
                                                    ? uiMessage.getMessage().getTargetId()
                                                    : null,
                                            selectedReactionId);
                            dialog.setUiMessage(uiMessage);
                            dialog.setReactions(finalReactions);
                            dialog.show(
                                    ((androidx.fragment.app.FragmentActivity) ctx)
                                            .getSupportFragmentManager());
                        }
                    });
        } else {
            reactionView.setVisibility(View.GONE);
            resetReactionContainer(holder, reactionView);
        }
    }

    /** 有回应时，消息体与回应区共同使用同一个圆角底板；媒体消息额外加内边距以露出卡片边界。 */
    private void applyReactionContainer(
            ViewHolder holder, ReactionView reactionView, UiMessage uiMessage) {
        View wrapper = holder.getView(R.id.rc_message_content_wrapper);
        Message message = uiMessage.getMessage();
        if (wrapper == null || message == null) {
            return;
        }
        boolean isSender = message.getMessageDirection() == Message.MessageDirection.SEND;
        MessageContent content = message.getContent();
        wrapper.setBackgroundResource(
                isSender
                        ? R.drawable.rc_lively_reaction_container_send
                        : R.drawable.rc_lively_reaction_container_receive);

        int cardPadding = dp2px(wrapper, 10);
        boolean isTextMessage =
                content instanceof TextMessage || content instanceof ReferenceMessage;
        if (!isTextMessage) {
            wrapper.setPadding(cardPadding, cardPadding, cardPadding, cardPadding);
        } else {
            wrapper.setPadding(0, 0, 0, 0);
        }

        setReactionContentDividerVisible(holder, false);
        reactionView.setBackground(null);
        updateReactionLayoutParams(reactionView, content);
    }

    private void resetReactionContainer(ViewHolder holder, ReactionView reactionView) {
        View wrapper = holder.getView(R.id.rc_message_content_wrapper);
        if (wrapper != null) {
            wrapper.setBackground(null);
            wrapper.setPadding(0, 0, 0, 0);
        }
        setReactionContentDividerVisible(holder, false);
        reactionView.setBackground(null);
        reactionView.setPadding(0, 0, 0, 0);
        updateReactionLayoutParams(reactionView, null);
    }

    private void setReactionContentDividerVisible(ViewHolder holder, boolean visible) {
        View divider = holder.getView(R.id.rc_reaction_content_divider);
        if (divider != null) {
            divider.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    protected boolean hasVisibleReactions(UiMessage uiMessage) {
        if (uiMessage == null) {
            return false;
        }
        List<io.rong.imlib.model.MessageReaction> reactions = uiMessage.getReactions();
        return reactions != null && !reactions.isEmpty();
    }

    private void updateReactionLayoutParams(ReactionView reactionView, MessageContent content) {
        ViewGroup.LayoutParams params = reactionView.getLayoutParams();
        if (!(params instanceof LinearLayout.LayoutParams)) {
            return;
        }
        boolean isTextMessage = content instanceof TextMessage;
        boolean isReferenceMessage = content instanceof ReferenceMessage;
        boolean useTightReactionLayout = isTextMessage || isReferenceMessage;
        int horizontalPaddingStart = useTightReactionLayout ? dp2px(reactionView, 12) : 0;
        int horizontalPaddingEnd = useTightReactionLayout ? dp2px(reactionView, 10) : 0;
        reactionView.setPadding(horizontalPaddingStart, 0, horizontalPaddingEnd, 0);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) params;
        lp.topMargin = dp2px(reactionView, useTightReactionLayout ? 4 : 8);
        lp.setMarginStart(0);
        lp.bottomMargin = useTightReactionLayout ? dp2px(reactionView, 10) : 0;
        reactionView.setLayoutParams(lp);
    }

    private int dp2px(View view, int dp) {
        float density = view.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private void initTime(ViewHolder holder, int position, List<UiMessage> data, Message message) {
        String time =
                RongDateUtils.getConversationFormatDate(message.getSentTime(), holder.getContext());
        holder.setText(R.id.rc_time, time);
        if (position == 0) {
            holder.setVisible(
                    R.id.rc_time, !(message.getContent() instanceof HistoryDividerMessage));
        } else {
            UiMessage pre = data.get(position - 1);
            if (pre.getMessage() != null
                    && RongDateUtils.isShowChatTime(
                            holder.getContext(),
                            message.getSentTime(),
                            pre.getMessage().getSentTime(),
                            180)) {
                holder.setVisible(R.id.rc_time, true);
            } else {
                holder.setVisible(R.id.rc_time, false);
            }
        }
    }

    private void initUserInfo(
            final ViewHolder holder,
            final UiMessage uiMessage,
            final int position,
            final IViewProviderListener<UiMessage> listener,
            boolean isSender) {
        if (mConfig.showPortrait) {
            holder.setVisible(R.id.rc_left_portrait, !isSender);
            holder.setVisible(R.id.rc_right_portrait, isSender);
            ImageView view =
                    holder.getView(isSender ? R.id.rc_right_portrait : R.id.rc_left_portrait);
            UserInfo userInfo = uiMessage.getUserInfo();
            if (userInfo != null && userInfo.getPortraitUri() != null) {
                RongConfigCenter.featureConfig()
                        .getKitImageEngine()
                        .loadConversationPortrait(
                                holder.getContext(),
                                userInfo.getPortraitUri().toString(),
                                view,
                                uiMessage.getMessage());
            }
            holder.setOnClickListener(
                    R.id.rc_left_portrait,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (RongConfigCenter.conversationConfig().getConversationClickListener()
                                    != null) {
                                boolean result =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationClickListener()
                                                .onUserPortraitClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getConversationType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage.getMessage().getTargetId());
                                if (!result) {
                                    listener.onViewClick(
                                            MessageClickType.USER_PORTRAIT_CLICK, uiMessage);
                                }
                            }
                        }
                    });

            holder.setOnClickListener(
                    R.id.rc_right_portrait,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (RongConfigCenter.conversationConfig().getConversationClickListener()
                                    != null) {
                                boolean result =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationClickListener()
                                                .onUserPortraitClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getConversationType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage.getMessage().getTargetId());
                                if (!result) {
                                    listener.onViewClick(
                                            MessageClickType.USER_PORTRAIT_CLICK, uiMessage);
                                }
                            }
                        }
                    });

            holder.setOnLongClickListener(
                    R.id.rc_left_portrait,
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (RongConfigCenter.conversationConfig().getConversationClickListener()
                                    != null) {
                                boolean result =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationClickListener()
                                                .onUserPortraitLongClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getConversationType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage.getMessage().getTargetId());
                                if (!result) {
                                    result =
                                            listener.onViewLongClick(
                                                    holder.getView(R.id.rc_content),
                                                    MessageClickType.USER_PORTRAIT_LONG_CLICK,
                                                    uiMessage);
                                }
                                return result;
                            }
                            return false;
                        }
                    });

            holder.setOnLongClickListener(
                    R.id.rc_right_portrait,
                    new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (RongConfigCenter.conversationConfig().getConversationClickListener()
                                    != null) {
                                boolean result =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationClickListener()
                                                .onUserPortraitLongClick(
                                                        holder.getContext(),
                                                        uiMessage
                                                                .getMessage()
                                                                .getConversationType(),
                                                        uiMessage.getUserInfo(),
                                                        uiMessage.getMessage().getTargetId());
                                if (!result) {
                                    listener.onViewLongClick(
                                            holder.getView(R.id.rc_content),
                                            MessageClickType.USER_PORTRAIT_LONG_CLICK,
                                            uiMessage);
                                }
                                return result;
                            }
                            return false;
                        }
                    });
            if (!RongConfigCenter.conversationConfig()
                    .isShowReceiverUserTitle(uiMessage.getMessage().getConversationType())) {
                holder.setVisible(R.id.rc_title, false);
            } else {
                if (!isSender) {
                    holder.setVisible(R.id.rc_title, true);
                    holder.setText(R.id.rc_title, uiMessage.getDisplayName());
                } else {
                    holder.setVisible(R.id.rc_title, false);
                }
            }
        } else {
            holder.setVisible(R.id.rc_left_portrait, false);
            holder.setVisible(R.id.rc_right_portrait, false);
            holder.setVisible(R.id.rc_title, false);
        }
    }

    private void initContent(
            final ViewHolder holder,
            boolean isSender,
            final UiMessage uiMessage,
            final int position,
            final IViewProviderListener<UiMessage> listener,
            final List<UiMessage> list) {
        // 带 V2 引用卡片时强制显示气泡背景（图片/语音等原本无气泡的消息也需要包裹）
        boolean needBubble =
                showBubble() || QuoteCardView.shouldShowQuoteCard(uiMessage.getMessage());
        if (needBubble) {
            holder.setBackgroundRes(
                    R.id.rc_content,
                    IMKitThemeManager.getAttrResId(
                            holder.getContext(), getBackgroundAttrId(isSender, uiMessage)));
        } else {
            holder.getView(R.id.rc_content).setBackground(null);
        }
        holder.setPadding(R.id.rc_content, 0, 0, 0, 0);

        LinearLayout layout = holder.getView(R.id.rc_layout);
        if (mConfig.centerInHorizontal) {
            layout.setGravity(Gravity.CENTER_HORIZONTAL);
        } else {
            layout.setGravity(isSender ? Gravity.END : Gravity.START);
        }

        View contentWrapper = holder.getView(R.id.rc_message_content_wrapper);
        View contentView = holder.getView(R.id.rc_content);
        if (contentWrapper != null && contentView != null) {
            contentWrapper.setOnClickListener(v -> contentView.performClick());
            contentWrapper.setOnLongClickListener(v -> contentView.performLongClick());
        }

        holder.setOnClickListener(
                R.id.rc_content,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean result = false;
                        /** 点击事件分发策略：应用 -> 各消息模板实现类 -> Processor */
                        if (RongConfigCenter.conversationConfig().getConversationClickListener()
                                != null) {
                            result =
                                    RongConfigCenter.conversationConfig()
                                            .getConversationClickListener()
                                            .onMessageClick(
                                                    holder.getContext(), v, uiMessage.getMessage());
                        }
                        if (!result) {

                            T msgContent = null;
                            try {
                                msgContent = (T) uiMessage.getMessage().getContent();
                            } catch (ClassCastException e) {
                                RLog.e(
                                        TAG,
                                        "rc_content onClick MessageContent cast Exception, e:" + e);
                            }
                            if (msgContent != null) {
                                result =
                                        onItemClick(
                                                ((MessageViewHolder) holder)
                                                        .getMessageContentViewHolder(),
                                                msgContent,
                                                uiMessage,
                                                position,
                                                list,
                                                listener);
                            }
                            if (!result) {
                                listener.onViewClick(MessageClickType.CONTENT_CLICK, uiMessage);
                            }
                        }
                    }
                });

        holder.setOnLongClickListener(
                R.id.rc_content,
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        boolean result = false;
                        if (RongConfigCenter.conversationConfig().getConversationClickListener()
                                != null) {
                            result =
                                    RongConfigCenter.conversationConfig()
                                            .getConversationClickListener()
                                            .onMessageLongClick(
                                                    holder.getContext(), v, uiMessage.getMessage());
                        }
                        if (!result) {
                            T msgContent = null;
                            try {
                                msgContent = (T) uiMessage.getMessage().getContent();
                            } catch (ClassCastException e) {
                                RLog.e(
                                        TAG,
                                        "rc_content onLongClick MessageContent cast Exception, e:"
                                                + e);
                            }
                            if (msgContent != null) {
                                result =
                                        onItemLongClick(
                                                ((MessageViewHolder) holder)
                                                        .getMessageContentViewHolder(),
                                                (T) uiMessage.getMessage().getContent(),
                                                uiMessage,
                                                position,
                                                list,
                                                listener);
                            }
                            if (!result) {
                                listener.onViewLongClick(
                                        holder.getView(R.id.rc_content),
                                        MessageClickType.CONTENT_LONG_CLICK,
                                        uiMessage);
                            }
                            return result;
                        }
                        return false;
                    }
                });
    }

    private int getBackgroundAttrId(boolean isSender, UiMessage uiMessage) {
        int backgroundAttrId =
                isSender
                        ? R.attr.rc_conversation_msg_send_background
                        : R.attr.rc_conversation_msg_receiver_background;
        boolean hasQuoteCard = QuoteCardView.shouldShowQuoteCard(uiMessage.getMessage());
        boolean isFileMessage =
                Objects.equals(uiMessage.getMessage().getObjectName(), "RC:FileMsg");
        if ((isFileMessage && !hasQuoteCard)
                || Objects.equals(uiMessage.getMessage().getObjectName(), "RC:CardMsg")
                || Objects.equals(uiMessage.getMessage().getObjectName(), "RC:LBSMsg")) {
            backgroundAttrId = R.attr.rc_conversation_msg_special_background;
        } else if (Objects.equals(uiMessage.getMessage().getObjectName(), "RC:CombineMsg")
                && !IMKitThemeManager.isTraditionTheme()) {
            // 合并转发旧主题不适用特殊 background
            backgroundAttrId = R.attr.rc_conversation_msg_special_background;
        }
        return backgroundAttrId;
    }

    private void initStatus(
            ViewHolder holder,
            final UiMessage uiMessage,
            final int position,
            final IViewProviderListener<UiMessage> listener,
            Message message,
            boolean isSender,
            List<UiMessage> list) {
        if (mConfig.showWarning
                && !ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
            if (isSender
                    && uiMessage.getState() == State.ERROR
                    && message.getSentStatus() == Message.SentStatus.FAILED) {
                holder.setVisible(R.id.rc_warning, true);
                holder.setOnClickListener(
                        R.id.rc_warning,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listener.onViewClick(MessageClickType.WARNING_CLICK, uiMessage);
                            }
                        });
            } else {
                holder.setVisible(R.id.rc_warning, false);
            }
        } else {
            holder.setVisible(R.id.rc_warning, false);
        }
        if (mConfig.showProgress) {
            if (isSender && uiMessage.getState() == State.PROGRESS) {
                holder.setVisible(R.id.rc_progress, true);
            } else if (isSender
                    && uiMessage.getState() == State.ERROR
                    && ResendManager.getInstance()
                            .needResend(uiMessage.getMessage().getMessageId())) {
                holder.setVisible(R.id.rc_progress, true);
            } else {
                holder.setVisible(R.id.rc_progress, false);
            }
        } else {
            holder.setVisible(R.id.rc_progress, false);
        }
        // 如开启已读回执V5，则回执V5的UI
        if (AppSettingsHandler.getInstance()
                .isReadReceiptV5Enabled(message.getConversationType())) {
            initReadV5Status(holder, uiMessage, listener, message, isSender);
        } else {
            initReadStatus(holder, uiMessage, position, listener, message, isSender, list);
        }
        initEditStatus(holder, message);
    }

    /**
     * 设置消息视图里各 view 的值
     *
     * @param holder ViewHolder
     * @param parentHolder 父布局的 ViewHolder
     * @param t 此展示模板对应的消息
     * @param uiMessage {@link UiMessage}
     * @param position 消息位置
     * @param list 列表
     * @param listener ViewModel 的点击事件监听器。如果某个子 view 的点击事件需要 ViewModel 处理，可通过此监听器回调。
     */
    protected abstract void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            T t,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener);

    /**
     * @param holder ViewHolder
     * @param t 自定义消息
     * @param uiMessage {@link UiMessage}
     * @param position 位置
     * @param list 列表数据
     * @param listener ViewModel 的点击事件监听器。如果某个子 view 的点击事件需要 ViewModel 处理，可通过此监听器回调。
     * @return 点击事件是否被消费
     */
    protected abstract boolean onItemClick(
            ViewHolder holder,
            T t,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener);

    protected boolean onItemLongClick(
            ViewHolder holder,
            T t,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        return false;
    }

    private void initReadStatus(
            ViewHolder holder,
            final UiMessage uiMessage,
            int position,
            final IViewProviderListener<UiMessage> listener,
            final Message message,
            boolean isSender,
            List<UiMessage> list) {
        // 单聊已读状态
        if (RongConfigCenter.conversationConfig().isShowReadReceipt(message.getConversationType())
                && mConfig.showReadState
                && isSender
                && message.getSentStatus() == Message.SentStatus.READ) {
            holder.setVisible(R.id.rc_read_receipt, true);
        } else {
            holder.setVisible(R.id.rc_read_receipt, false);
        }
        // 群聊和讨论组已读状态
        if (RongConfigCenter.conversationConfig()
                        .isShowReadReceiptRequest(message.getConversationType())
                && showReadReceiptRequest(message)
                && isSender
                && !TextUtils.isEmpty(message.getUId())) {

            boolean isLastSentMessage = true;
            for (int i = position + 1; i < list.size(); i++) {
                if (list.get(i).getMessage().getMessageDirection()
                        == Message.MessageDirection.SEND) {
                    isLastSentMessage = false;
                    break;
                }
            }
            long serverTime =
                    System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime();
            if ((serverTime - message.getSentTime()
                            < RongConfigCenter.conversationConfig().rc_read_receipt_request_interval
                                    * 1000)
                    && isLastSentMessage
                    && (message.getReadReceiptInfo() == null
                            || !message.getReadReceiptInfo().isReadReceiptMessage())) {
                holder.setVisible(R.id.rc_read_receipt_request, true);
                holder.setOnClickListener(
                        R.id.rc_read_receipt_request,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                listener.onViewClick(
                                        MessageClickType.READ_RECEIPT_REQUEST_CLICK, uiMessage);
                            }
                        });
            } else {
                holder.setVisible(R.id.rc_read_receipt_request, false);
            }

            if (message.getReadReceiptInfo() != null
                    && message.getReadReceiptInfo().isReadReceiptMessage()) {
                if (message.getReadReceiptInfo().getRespondUserIdList() != null) {
                    holder.setText(
                            R.id.rc_read_receipt_status,
                            message.getReadReceiptInfo().getRespondUserIdList().size()
                                    + " "
                                    + holder.getContext()
                                            .getString(R.string.rc_read_receipt_status));
                } else {
                    holder.setText(
                            R.id.rc_read_receipt_status,
                            0
                                    + " "
                                    + holder.getContext()
                                            .getString(R.string.rc_read_receipt_status));
                }
                holder.setVisible(R.id.rc_read_receipt_status, true);
                holder.setOnClickListener(
                        R.id.rc_read_receipt_status,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ConversationClickListener conversationClickListener =
                                        RongConfigCenter.conversationConfig()
                                                .getConversationClickListener();
                                if (conversationClickListener != null
                                        && conversationClickListener.onReadReceiptStateClick(
                                                v.getContext(), uiMessage.getMessage())) {
                                    return;
                                }
                                listener.onViewClick(
                                        MessageClickType.READ_RECEIPT_STATE_CLICK, uiMessage);
                            }
                        });
            } else {
                holder.setVisible(R.id.rc_read_receipt_status, false);
            }

        } else {
            holder.setVisible(R.id.rc_read_receipt_request, false);
            holder.setVisible(R.id.rc_read_receipt_status, false);
        }
    }

    /**
     * 已读回执V5的已读状态图片组件设置。
     * V1已读回执单聊使用的rc_read_receipt组件（ImageView），群聊使用rc_read_receipt_status、rc_read_receipt_request组件（TextView）。
     * V5的单群聊都需要使用ImageView展示，所以复用了 rc_read_receipt
     * 组件进行展示，rc_read_receipt_status、rc_read_receipt_request隐藏。
     */
    private void initReadV5Status(
            ViewHolder holder,
            final UiMessage uiMessage,
            final IViewProviderListener<UiMessage> listener,
            final Message message,
            boolean isSender) {
        // 群聊状态组件设置不可见
        holder.setVisible(R.id.rc_read_receipt_status, false);
        holder.setVisible(R.id.rc_read_receipt_request, false);

        // 接收方向消息、消息uid为空，不显示已读回执组件
        if (!isSender || TextUtils.isEmpty(message.getUId())) {
            holder.setVisible(R.id.rc_read_receipt, false);
            return;
        }
        holder.setVisible(R.id.rc_read_receipt, true);
        ReadReceiptInfoV5 readInfo = uiMessage.getReadReceiptInfoV5();
        if (Conversation.ConversationType.PRIVATE == message.getConversationType()) {
            // 单聊已读状态
            ImageView readReceipt = holder.getView(R.id.rc_read_receipt);
            boolean isRead =
                    mConfig.showReadState && readInfo != null && readInfo.getReadCount() >= 1;
            if (isRead) {
                readReceipt.setImageResource(
                        IMKitThemeManager.dynamicResource(
                                holder.getContext(),
                                R.attr.rc_conversation_list_cell_msg_read_img,
                                R.drawable.rc_lively_read_receipt));
            } else {
                readReceipt.setImageResource(
                        IMKitThemeManager.getAttrResId(
                                holder.getContext(),
                                R.attr.rc_conversation_list_cell_msg_unread_img));
            }
        } else if (Conversation.ConversationType.GROUP == message.getConversationType()) {
            // 群聊已读状态
            drawReadReceiptCircle(holder, readInfo);
            View.OnClickListener click =
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ConversationClickListener conversationClickListener =
                                    RongConfigCenter.conversationConfig()
                                            .getConversationClickListener();
                            if (conversationClickListener != null
                                    && conversationClickListener.onReadReceiptStateClick(
                                            v.getContext(), uiMessage.getMessage())) {
                                return;
                            }
                            listener.onViewClick(
                                    MessageClickType.READ_RECEIPT_STATE_CLICK, uiMessage);
                        }
                    };
            holder.setOnClickListener(R.id.rc_read_receipt, click);
        }
    }

    // 编辑状态
    private void initEditStatus(final ViewHolder holder, final Message message) {
        // 未开启消息编辑功能、消息配置不支持编辑、消息编辑状态非法或者为成功状态，隐藏编辑状态View
        if (!RongConfigCenter.featureConfig().isEditMessageEnable()
                || !mConfig.showEditState
                || message.getModifyInfo() == null) {
            holder.setVisible(R.id.rc_edit_status_layout, false);
            return;
        }
        MessageModifyInfo.MessageModifyStatus status = message.getModifyInfo().getStatus();
        TextView content = holder.getView(R.id.rc_edit_status_content);
        if (MessageModifyInfo.MessageModifyStatus.FAILED == status) {
            holder.setVisible(R.id.rc_edit_status_layout, true);
            holder.setVisible(R.id.rc_edit_status_failed, true);
            holder.setVisible(R.id.rc_edit_status_progress, false);
            content.setText(R.string.rc_edit_status_failed);
            content.setTextColor(content.getResources().getColor(R.color.rc_edit_failed));
            holder.setOnClickListener(
                    R.id.rc_edit_status_layout,
                    view -> EditMessageManager.getInstance().editMessage(message, ""));
        } else if (MessageModifyInfo.MessageModifyStatus.UPDATING == status) {
            holder.setVisible(R.id.rc_edit_status_layout, true);
            holder.setVisible(R.id.rc_edit_status_failed, false);
            holder.setVisible(R.id.rc_edit_status_progress, true);
            content.setText(R.string.rc_edit_status_progress);
            content.setTextColor(content.getResources().getColor(R.color.rc_edit_progress));
            holder.setOnClickListener(R.id.rc_edit_status_layout, null);
        } else if (MessageModifyInfo.MessageModifyStatus.SUCCESS == status) {
            holder.setVisible(R.id.rc_edit_status_layout, false);
            holder.setOnClickListener(R.id.rc_edit_status_layout, null);
        }
    }

    /** 返回带引用卡片时 wrapper 的最小宽度（px）。默认不设置最小宽度，由 wrapper 按引用区和正文 的较宽者自适应；特殊消息类型可覆写。 */
    protected int getQuoteCardWrapperMinWidth(Context context) {
        return 0;
    }

    /** 绑定 V2 引用卡片。在 rc_content 的包装器 LinearLayout 中查找 QuoteCardView， 根据消息是否携带 quoteInfo 决定显示/隐藏。 */
    private void bindQuoteCard(
            ViewHolder holder,
            UiMessage uiMessage,
            IViewProviderListener<UiMessage> listener,
            List<UiMessage> list) {
        QuoteCardView quoteCard = findQuoteCard(holder);
        if (quoteCard == null) {
            return;
        }

        final QuoteCardView finalQuoteCard = quoteCard;
        Message message = uiMessage.getMessage();
        ViewGroup wrapper = (ViewGroup) finalQuoteCard.getParent();
        // 分隔线在 onCreateViewHolder 中通过 tag 标记
        View quoteDivider = wrapper != null ? wrapper.findViewWithTag("quote_card_divider") : null;

        if (QuoteCardView.shouldShowQuoteCard(message)) {
            finalQuoteCard.setVisibility(View.VISIBLE);
            if (quoteDivider != null) {
                quoteDivider.setVisibility(View.VISIBLE);
            }
            if (wrapper != null) {
                wrapper.setMinimumWidth(getQuoteCardWrapperMinWidth(holder.getContext()));
            }
            QuoteMessageBatchLoader.getInstance().loadForUiMessages(list, null);
            finalQuoteCard.setMessage(message);
            if (finalQuoteCard.hasCustomReferenceContent()) {
                finalQuoteCard.setOnClickListener(null);
                return;
            }
            // 点击引用卡片 → 预览原消息
            finalQuoteCard.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onQuoteCardClick(v.getContext(), finalQuoteCard);
                        }
                    });
        } else {
            finalQuoteCard.setVisibility(View.GONE);
            if (quoteDivider != null) {
                quoteDivider.setVisibility(View.GONE);
            }
            finalQuoteCard.reset();
            finalQuoteCard.setOnClickListener(null);
            // 无引用卡片时重置 wrapper 的最小宽度，避免影响无引用消息的气泡大小
            if (wrapper != null) {
                wrapper.setMinimumWidth(0);
            }
        }
    }

    /** 引用卡片点击处理：根据被引用消息类型跳转到对应预览界面。 已撤回和不可用的消息不响应点击。 */
    private void onQuoteCardClick(Context context, QuoteCardView quoteCard) {
        Message quotedMsg = quoteCard.getQuotedMessage();
        if (quotedMsg == null || quotedMsg.getContent() == null) {
            return;
        }
        // 已撤回消息不可预览
        if (quotedMsg.getContent() instanceof RecallNotificationMessage) {
            return;
        }
        try {
            MessageContent content = quotedMsg.getContent();
            if (content instanceof ImageMessage) {
                Intent intent = new Intent(context, PicturePagerActivity.class);
                intent.setPackage(context.getPackageName());
                intent.putExtra("message", quotedMsg);
                context.startActivity(intent);
            } else if (content instanceof FileMessage) {
                Intent intent = new Intent(context, FilePreviewActivity.class);
                intent.setPackage(context.getPackageName());
                intent.putExtra("FileMessage", quotedMsg.getContent());
                intent.putExtra("Message", quotedMsg);
                context.startActivity(intent);
            } else if (content instanceof SightMessage) {
                RLog.d(TAG, "QuoteCard clicked on sight message, not handled yet");
            } else if (content instanceof TextMessage) {
                // 文本消息点击放大：对齐 V1 ReferenceDialog 的放大预览体验
                showQuotedTextPreview(context, ((TextMessage) content).getContent());
            }
        } catch (Exception e) {
            RLog.e(TAG, "onQuoteCardClick", e);
        }
    }

    /** 以全屏对话框展示被引用文本消息的完整内容，对齐 V1 的 showPopWindow 放大体验。 */
    private void showQuotedTextPreview(Context context, String text) {
        if (TextUtils.isEmpty(text) || !(context instanceof android.app.Activity)) {
            return;
        }
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        TextView textView = new TextView(context);
        int padding = (int) (context.getResources().getDisplayMetrics().density * 16 + 0.5f);
        textView.setPadding(padding, padding, padding, padding);
        textView.setText(text);
        textView.setTextIsSelectable(true);
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        builder.setView(textView);
        builder.setPositiveButton(R.string.rc_dialog_ok, null);
        builder.show();
    }

    protected void setTextMessageContent(
            TextView textView, UiMessage uiMessage, SpannableStringBuilder span) {
        if (uiMessage.getMessage().isHasChanged()) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder(span);
            String edited = textView.getContext().getString(R.string.rc_edit_status_success);
            SpannableStringBuilder spannable = new SpannableStringBuilder("（" + edited + "）");
            ForegroundColorSpan foregroundColorSpan =
                    new ForegroundColorSpan(getEditStatusColor(textView));
            spannable.setSpan(
                    foregroundColorSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannable);
            textView.setText(contentSpannable);
        } else {
            textView.setText(span);
        }
    }

    protected void setReferenceMessageContent(
            TextView textView, UiMessage uiMessage, SpannableStringBuilder span) {
        ReferenceMessage content = (ReferenceMessage) uiMessage.getMessage().getContent();
        ReferenceMessage.ReferenceMessageStatus referMsgStatus = content.getReferMsgStatus();
        if (referMsgStatus == ReferenceMessage.ReferenceMessageStatus.MODIFIED) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder(span);
            String text = textView.getContext().getString(R.string.rc_edit_status_success);
            SpannableStringBuilder spannable = new SpannableStringBuilder("（" + text + "）");
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(getEditStatusColor(textView));
            spannable.setSpan(colorSpan, 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannable);
            textView.setText(contentSpannable);
        } else if (referMsgStatus == ReferenceMessage.ReferenceMessageStatus.DELETE) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder();
            String text = textView.getContext().getString(R.string.rc_reference_status_delete);
            SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(getEditStatusColor(textView));
            spannableString.setSpan(
                    colorSpan, 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannableString);
            textView.setText(contentSpannable);
        } else if (referMsgStatus == ReferenceMessage.ReferenceMessageStatus.RECALLED) {
            SpannableStringBuilder contentSpannable = new SpannableStringBuilder();
            String text = textView.getContext().getString(R.string.rc_reference_status_recall);
            SpannableStringBuilder spannableString = new SpannableStringBuilder(text);
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(getEditStatusColor(textView));
            spannableString.setSpan(
                    colorSpan, 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            contentSpannable.append(spannableString);
            textView.setText(contentSpannable);
        } else {
            textView.setText(span);
        }
    }

    /**
     * @return 群组或讨论组是否展示消息已读回执, 默认只有文本消息展示 子类可以重写此方法
     */
    protected boolean showReadReceiptRequest(Message message) {
        return message != null
                && message.getContent() != null
                && (message.getContent() instanceof TextMessage);
    }

    /**
     * 根据消息内容，判断是否为本模板需要展示的消息类型
     *
     * @param messageContent 消息内容
     * @return 本模板是否处理。
     */
    protected abstract boolean isMessageViewType(MessageContent messageContent);

    @Override
    public boolean isSummaryType(MessageContent messageContent) {
        return isMessageViewType(messageContent);
    }

    @Override
    public boolean showSummaryWithName() {
        return mConfig.showSummaryWithName;
    }

    public static class MessageViewHolder extends ViewHolder {
        private ViewHolder mMessageContentViewHolder;

        public MessageViewHolder(Context context, View itemView, ViewHolder messageViewHolder) {
            super(context, itemView);
            mMessageContentViewHolder = messageViewHolder;
        }

        public ViewHolder getMessageContentViewHolder() {
            return mMessageContentViewHolder;
        }
    }

    public boolean showBubble() {
        return mConfig.showContentBubble;
    }

    // 检测View是否有效的
    protected boolean checkViewsValid(View... views) {
        if (views == null || views.length == 0) {
            return false;
        }
        for (View view : views) {
            if (view == null) {
                return false;
            }
        }
        return true;
    }

    /** 根据已读百分比绘制圆圈状态 */
    private void drawReadReceiptCircle(ViewHolder holder, ReadReceiptInfoV5 readInfo) {
        ImageView imageView = holder.getView(R.id.rc_read_receipt);
        // 计算已读百分比
        int readCount = 0, unreadCount = 0;
        if (readInfo != null) {
            readCount = readInfo.getReadCount();
            unreadCount = readInfo.getUnreadCount();
        }
        int totalCount = readCount + unreadCount;
        float readPercentage = totalCount > 0 ? (float) readCount / totalCount : 0f;
        // 全部已读，展示对钩
        if (readPercentage >= 1f) {
            imageView.setImageResource(
                    IMKitThemeManager.dynamicResource(
                            holder.getContext(),
                            R.attr.rc_conversation_list_cell_msg_read_img,
                            R.drawable.rc_lively_read_receipt));
            return;
        } else if (readPercentage <= 0f) {
            imageView.setImageResource(
                    IMKitThemeManager.getAttrResId(
                            holder.getContext(), R.attr.rc_conversation_list_cell_msg_unread_img));
            return;
        }
        // 根据百分比绘制不同的圆圈状态
        Context context = imageView.getContext();
        // 设置固定尺寸
        int size = 12; // dp
        float density = context.getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (size * density);
        // 确保最小尺寸
        if (sizeInPx < 12) {
            sizeInPx = 12;
        }
        // 创建bitmap
        Bitmap bitmap = Bitmap.createBitmap(sizeInPx, sizeInPx, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);

        float center = sizeInPx / 2f;
        float borderWidth = Math.max(1.5f, sizeInPx / 8f);
        // 计算外圆半径（考虑边框宽度）
        float outerRadius = center - (borderWidth * 0.5f);
        // 创建画笔
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(
                IMKitThemeManager.getColorFromAttrId(holder.getContext(), R.attr.rc_success_color));
        // 1. 始终绘制圆形边框
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(borderWidth);
        canvas.drawCircle(center, center, outerRadius, paint);
        // 2. 如果进度大于0，绘制填充进度
        if (readPercentage > 0f) {
            // 计算填充半径（边框内部，留出间距）
            float innerPadding = Math.max(1.5f, sizeInPx / 8f);
            float fillRadius = outerRadius - (borderWidth * 0.5f) - innerPadding;
            if (fillRadius > 0) {
                paint.setStyle(Paint.Style.FILL);
                // 创建扇形路径：从12点钟方向开始顺时针
                RectF rectF =
                        new RectF(
                                center - fillRadius,
                                center - fillRadius,
                                center + fillRadius,
                                center + fillRadius);
                // 起始角度：12点钟方向为-90度
                float startAngle = -90f;
                // 扫描角度：progress * 360度
                float sweepAngle = 360f * readPercentage;
                // 绘制扇形（从圆心开始的扇形）
                canvas.drawArc(rectF, startAngle, sweepAngle, true, paint);
            }
        }
        // 设置到ImageView
        imageView.setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
    }

    private int getEditStatusColor(TextView textView) {
        return IMKitThemeManager.getColorFromAttrId(
                textView.getContext(), R.attr.rc_text_primary_color);
    }

    protected void applyContentBackgroundEndInset(ViewHolder parentHolder, int endInsetPx) {
        if (parentHolder == null || endInsetPx <= 0) {
            return;
        }
        View contentView = parentHolder.getView(R.id.rc_content);
        if (contentView == null) {
            return;
        }
        Drawable background = contentView.getBackground();
        if (background == null) {
            return;
        }
        contentView.setBackground(new InsetDrawable(background, 0, 0, endInsetPx, 0));
    }

    protected int getVisibleDecorationSpacePx(View... decorationViews) {
        if (decorationViews == null) {
            return 0;
        }
        for (View decorationView : decorationViews) {
            if (decorationView == null || decorationView.getVisibility() != View.VISIBLE) {
                continue;
            }
            ViewGroup.LayoutParams layoutParams = decorationView.getLayoutParams();
            int width = 0;
            int startMargin = 0;
            int endMargin = 0;
            if (layoutParams != null) {
                width = Math.max(layoutParams.width, 0);
                if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams marginLayoutParams =
                            (ViewGroup.MarginLayoutParams) layoutParams;
                    startMargin = Math.max(marginLayoutParams.getMarginStart(), 0);
                    endMargin = Math.max(marginLayoutParams.getMarginEnd(), 0);
                }
            }
            return width + startMargin + endMargin;
        }
        return 0;
    }

    protected QuoteCardView findQuoteCard(ViewHolder holder) {
        if (holder == null) {
            return null;
        }
        FrameLayout contentFrame = holder.getView(R.id.rc_content);
        if (contentFrame == null || contentFrame.getChildCount() == 0) {
            return null;
        }
        View wrapperView = contentFrame.getChildAt(0);
        if (!(wrapperView instanceof LinearLayout)) {
            return null;
        }
        LinearLayout wrapper = (LinearLayout) wrapperView;
        for (int i = 0; i < wrapper.getChildCount(); i++) {
            if (wrapper.getChildAt(i) instanceof QuoteCardView) {
                return (QuoteCardView) wrapper.getChildAt(i);
            }
        }
        return null;
    }

    protected void applyQuoteCardWidth(ViewHolder parentHolder, int widthPx) {
        QuoteCardView quoteCard = findQuoteCard(parentHolder);
        if (quoteCard == null) {
            return;
        }
        ViewGroup.LayoutParams layoutParams = quoteCard.getLayoutParams();
        if (layoutParams == null || layoutParams.width == widthPx) {
            return;
        }
        layoutParams.width = widthPx;
        quoteCard.setLayoutParams(layoutParams);
    }

    protected void applyMeasuredContentBackgroundEndInset(
            ViewHolder parentHolder,
            View messageBubbleView,
            boolean hasQuoteCard,
            boolean isSender) {
        if (parentHolder == null || messageBubbleView == null) {
            return;
        }
        View contentView = parentHolder.getView(R.id.rc_content);
        if (contentView == null) {
            return;
        }
        contentView.post(
                () -> {
                    int endInsetPx =
                            QuotedVoiceBubbleInsetResolver.resolveMeasuredEndInsetPx(
                                    hasQuoteCard,
                                    isSender,
                                    contentView.getWidth(),
                                    messageBubbleView.getWidth());
                    applyContentBackgroundEndInset(parentHolder, endInsetPx);
                });
    }

    /**
     * 将红点及 HQ 语音的下载指示器恢复到语音布局内的原始位置。
     *
     * <p>{@link #relocateVoiceUnread} 可能在上次 bind 时将这些 view 移到消息项 rc_layout 中， RecyclerView 复用时语音
     * Provider 的 {@code holder.setVisible(...)} 只在 holder.itemView 子树中查找，找不到已被移出的 view。 此方法应在语音
     * Provider 设置红点 visibility <b>之前</b>调用，确保始终可被找到。
     *
     * @param holder 语音消息内容 ViewHolder
     * @param parentHolder 消息 Item ViewHolder
     */
    protected void restoreVoiceUnreadToVoiceLayout(ViewHolder holder, ViewHolder parentHolder) {
        View voiceRcLayout = holder.getView(R.id.rc_layout);
        if (!(voiceRcLayout instanceof ViewGroup)) {
            return;
        }
        // 红点 8dp，下载失败/下载进度 16dp，依次恢复到 rc_voice_bg 之后
        restoreIndicatorToVoiceLayout(
                parentHolder, (ViewGroup) voiceRcLayout, R.id.rc_voice_unread, 8);
        restoreIndicatorToVoiceLayout(
                parentHolder, (ViewGroup) voiceRcLayout, R.id.rc_voice_download_error, 16);
        restoreIndicatorToVoiceLayout(
                parentHolder, (ViewGroup) voiceRcLayout, R.id.rc_download_progress, 16);
    }

    /** 将单个指示器 view 恢复到语音布局末尾。 */
    private void restoreIndicatorToVoiceLayout(
            ViewHolder parentHolder, ViewGroup voiceRcLayout, int viewId, int sizeDp) {
        View indicator = parentHolder.getView(viewId);
        if (indicator == null) {
            return;
        }
        ViewGroup currentParent = (ViewGroup) indicator.getParent();
        if (currentParent == null || currentParent == voiceRcLayout) {
            return;
        }
        currentParent.removeView(indicator);
        float density = indicator.getContext().getResources().getDisplayMetrics().density;
        int size = (int) (sizeDp * density + 0.5f);
        int marginStart = (int) (8 * density + 0.5f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMarginStart(marginStart);
        voiceRcLayout.addView(indicator, lp);
    }

    /** 保持语音指示器在语音布局内，避免 V2 引用气泡中的未读红点脱离气泡框。 */
    protected void relocateVoiceUnread(
            ViewHolder holder,
            ViewHolder parentHolder,
            boolean hasQuoteCard,
            boolean isSender,
            boolean hasVisibleReactions) {
        View voiceRcLayout = holder.getView(R.id.rc_layout);

        // 红点 8dp，下载失败/下载进度 16dp
        relocateIndicatorView(parentHolder, voiceRcLayout, R.id.rc_voice_unread, 8);
        relocateIndicatorView(parentHolder, voiceRcLayout, R.id.rc_voice_download_error, 16);
        relocateIndicatorView(parentHolder, voiceRcLayout, R.id.rc_download_progress, 16);
    }

    /** 将单个指示器 view 恢复到语音布局内。 */
    private void relocateIndicatorView(
            ViewHolder parentHolder, View voiceRcLayout, int viewId, int sizeDp) {
        View indicator = parentHolder.getView(viewId);
        if (indicator == null) {
            return;
        }
        ViewGroup currentParent = (ViewGroup) indicator.getParent();
        if (currentParent == null) {
            return;
        }

        float density = indicator.getContext().getResources().getDisplayMetrics().density;
        int size = (int) (sizeDp * density + 0.5f);
        int marginStart = (int) (8 * density + 0.5f);

        if (voiceRcLayout instanceof ViewGroup && currentParent != voiceRcLayout) {
            currentParent.removeView(indicator);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginStart(marginStart);
            ((ViewGroup) voiceRcLayout).addView(indicator, lp);
        }
    }
}
