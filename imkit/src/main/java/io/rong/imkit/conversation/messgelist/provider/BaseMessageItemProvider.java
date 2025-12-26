package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
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
import io.rong.imkit.config.ConversationClickListener;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.editmessage.EditMessageManager;
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
import io.rong.message.HistoryDividerMessage;
import io.rong.message.ReferenceMessage;
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
                contentView.addView(contentViewHolder.itemView);
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
            } else {
                RLog.e(TAG, "holder is not MessageViewHolder");
            }
            uiMessage.setChange(false);
        } else {
            RLog.e(TAG, "uiMessage is null");
        }
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
        if (showBubble()) {
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
        if (Objects.equals(uiMessage.getMessage().getObjectName(), "RC:FileMsg")
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
}
