package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.ConversationClickListener;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RongDateUtils;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.HistoryDividerMessage;
import io.rong.message.TextMessage;

public abstract class BaseMessageItemProvider<T extends MessageContent> implements IMessageProvider<T> {
    private static final String TAG = "BaseMessageItemProvider";
    protected MessageItemProviderConfig mConfig = new MessageItemProviderConfig();

    /**
     * 创建 ViewHolder
     * @param parent 父 ViewGroup
     * @param viewType 视图类型
     * @return ViewHolder
     */
    protected abstract ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType);

    /**
     * 设置消息视图里各 view 的值
     * @param holder ViewHolder
     * @param parentHolder 父布局的 ViewHolder
     * @param t 此展示模板对应的消息
     * @param uiMessage {@link UiMessage}
     * @param position 消息位置
     * @param list 列表
     * @param listener ViewModel 的点击事件监听器。如果某个子 view 的点击事件需要 ViewModel 处理，可通过此监听器回调。
     */
    protected abstract void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, T t, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener);

    /**
     * @param holder ViewHolder
     * @param t 自定义消息
     * @param uiMessage {@link UiMessage}
     * @param position 位置
     * @param list 列表数据
     * @param listener ViewModel 的点击事件监听器。如果某个子 view 的点击事件需要 ViewModel 处理，可通过此监听器回调。
     * @return 点击事件是否被消费
     */
    protected abstract boolean onItemClick(ViewHolder holder, T t, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener);

    protected boolean onItemLongClick(ViewHolder holder, T t, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        return false;
    }


    /**
     * 根据消息内容，判断是否为本模板需要展示的消息类型
     *
     * @param messageContent 消息内容
     * @return 本模板是否处理。
     */
    protected abstract boolean isMessageViewType(MessageContent messageContent);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_message_item, parent, false);
        FrameLayout contentView = rootView.findViewById(R.id.rc_content);
        ViewHolder contentViewHolder = onCreateMessageContentViewHolder(contentView, viewType);
        if (contentViewHolder != null) {
            if (contentView.getChildCount() == 0) {
                contentView.addView(contentViewHolder.itemView);
            }
        }
        return new MessageViewHolder(rootView.getContext(), rootView, contentViewHolder);
    }

    @Override
    public void bindViewHolder(final ViewHolder holder, final UiMessage uiMessage, final int position, final List<UiMessage> list, final IViewProviderListener<UiMessage> listener) {
        if (uiMessage != null && uiMessage.getMessage() != null && listener != null) {
            Message message = uiMessage.getMessage();
            holder.setVisible(R.id.rc_selected, uiMessage.isEdit());
            holder.setVisible(R.id.rc_v_edit, uiMessage.isEdit());
            if (uiMessage.isEdit()) {
                holder.setSelected(R.id.rc_selected, uiMessage.isSelected());
                holder.setOnClickListener(R.id.rc_v_edit, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onViewClick(MessageClickType.EDIT_CLICK, uiMessage);
                    }
                });
            }
            boolean isSender = uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);
            initTime(holder, position, list, message);
            initUserInfo(holder, uiMessage, position, listener, isSender);
            initContent(holder, isSender, uiMessage, position, listener, list);
            initStatus(holder, uiMessage, position, listener, message, isSender, list);

            if (holder instanceof MessageViewHolder) {
                bindMessageContentViewHolder(((MessageViewHolder) holder).getMessageContentViewHolder(), holder, (T) uiMessage.getMessage().getContent(), uiMessage, position, list, listener);
            } else {
                RLog.e(TAG, "holder is not MessageViewHolder");
            }
            uiMessage.setChange(false);
        } else {
            RLog.e(TAG, "uiMessage is null");
        }
    }

    private void initStatus(ViewHolder holder, final UiMessage uiMessage, final int position, final IViewProviderListener<UiMessage> listener, Message message, boolean isSender, List<UiMessage> list) {
        if (mConfig.showWarning && !ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
            if (isSender && uiMessage.getState() == State.ERROR) {
                holder.setVisible(R.id.rc_warning, true);
                holder.setOnClickListener(R.id.rc_warning, new View.OnClickListener() {
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
            } else if (isSender && uiMessage.getState() == State.ERROR && ResendManager.getInstance().needResend(uiMessage.getMessage().getMessageId())) {
                holder.setVisible(R.id.rc_progress, true);
            } else {
                holder.setVisible(R.id.rc_progress, false);
            }
        } else {
            holder.setVisible(R.id.rc_progress, false);
        }

        initReadStatus(holder, uiMessage, position, listener, message, isSender, list);
    }

    private void initReadStatus(ViewHolder holder, final UiMessage uiMessage, int position, final IViewProviderListener<UiMessage> listener, final Message message, boolean isSender, List<UiMessage> list) {
        //单聊已读状态
        if (RongConfigCenter.conversationConfig().isShowReadReceipt(message.getConversationType()) &&
                mConfig.showReadState &&
                isSender &&
                message.getSentStatus() == Message.SentStatus.READ) {
            holder.setVisible(R.id.rc_read_receipt, true);
        } else {
            holder.setVisible(R.id.rc_read_receipt, false);
        }
        //群聊和讨论组已读状态
        if (RongConfigCenter.conversationConfig().isShowReadReceiptRequest(message.getConversationType()) &&
                showReadReceiptRequest(message) &&
                isSender &&
                !TextUtils.isEmpty(message.getUId())) {

            boolean isLastSentMessage = true;
            for (int i = position + 1; i < list.size(); i++) {
                if (list.get(i).getMessage().getMessageDirection() == Message.MessageDirection.SEND) {
                    isLastSentMessage = false;
                    break;
                }
            }
            long serverTime = System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime();
            if ((serverTime - message.getSentTime() < RongConfigCenter.conversationConfig().rc_read_receipt_request_interval * 1000)
                    && isLastSentMessage
                    && (message.getReadReceiptInfo() == null || !message.getReadReceiptInfo().isReadReceiptMessage())) {
                holder.setVisible(R.id.rc_read_receipt_request, true);
                holder.setOnClickListener(R.id.rc_read_receipt_request, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onViewClick(MessageClickType.READ_RECEIPT_REQUEST_CLICK, uiMessage);
                    }
                });
            } else {
                holder.setVisible(R.id.rc_read_receipt_request, false);
            }

            if (message.getReadReceiptInfo() != null
                    && message.getReadReceiptInfo().isReadReceiptMessage()) {
                if (message.getReadReceiptInfo().getRespondUserIdList() != null) {
                    holder.setText(R.id.rc_read_receipt_status, message.getReadReceiptInfo().getRespondUserIdList().size() + " " + holder.getContext().getString(R.string.rc_read_receipt_status));
                } else {
                    holder.setText(R.id.rc_read_receipt_status, 0 + " " + holder.getContext().getString(R.string.rc_read_receipt_status));
                }
                holder.setVisible(R.id.rc_read_receipt_status, true);
                holder.setOnClickListener(R.id.rc_read_receipt_status, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ConversationClickListener conversationClickListener = RongConfigCenter.conversationConfig().getConversationClickListener();
                        if (conversationClickListener != null
                                && conversationClickListener.onReadReceiptStateClick(v.getContext(), uiMessage.getMessage())) {
                            return;
                        }
                        listener.onViewClick(MessageClickType.READ_RECEIPT_STATE_CLICK, uiMessage);

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


    private void initContent(final ViewHolder holder, boolean isSender, final UiMessage uiMessage, final int position, final IViewProviderListener<UiMessage> listener, final List<UiMessage> list) {
        if (mConfig.showContentBubble) {
            holder.setBackgroundRes(R.id.rc_content, isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
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


        holder.setOnClickListener(R.id.rc_content, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = false;
                /**
                 * 点击事件分发策略：应用 -> 各消息模板实现类 -> Processor
                 */
                if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                    result = RongConfigCenter.conversationConfig().getConversationClickListener().onMessageClick(holder.getContext(), v, uiMessage.getMessage());
                }
                if (!result) {
                    result = onItemClick(((MessageViewHolder) holder).getMessageContentViewHolder(), (T) uiMessage.getMessage().getContent(), uiMessage, position, list, listener);
                    if (!result) {
                        listener.onViewClick(MessageClickType.CONTENT_CLICK, uiMessage);
                    }
                }
            }
        });

        holder.setOnLongClickListener(R.id.rc_content, new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                boolean result = false;
                if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                    result = RongConfigCenter.conversationConfig().getConversationClickListener().onMessageLongClick(holder.getContext(), v, uiMessage.getMessage());
                }
                if (!result) {
                    result = onItemLongClick(((MessageViewHolder) holder).getMessageContentViewHolder(), (T) uiMessage.getMessage().getContent(), uiMessage, position, list, listener);
                    if (!result) {
                        return listener.onViewLongClick(MessageClickType.CONTENT_LONG_CLICK, uiMessage);
                    }
                }
                return false;
            }
        });
    }

    private void initTime(ViewHolder holder, int position, List<UiMessage> data, Message message) {
        String time = RongDateUtils.getConversationFormatDate(message.getSentTime(), holder.getContext());
        holder.setText(R.id.rc_time, time);
        if (position == 0) {
            holder.setVisible(R.id.rc_time, !(message.getContent() instanceof HistoryDividerMessage));
        } else {
            UiMessage pre = data.get(position - 1);
            if (pre.getMessage() != null && RongDateUtils.isShowChatTime(message.getSentTime(), pre.getMessage().getSentTime(), 180)) {
                holder.setVisible(R.id.rc_time, true);
            } else {
                holder.setVisible(R.id.rc_time, false);
            }
        }
    }

    private void initUserInfo(final ViewHolder holder, final UiMessage uiMessage, final int position, final IViewProviderListener<UiMessage> listener, boolean isSender) {
        if (mConfig.showPortrait) {
            holder.setVisible(R.id.rc_left_portrait, !isSender);
            holder.setVisible(R.id.rc_right_portrait, isSender);
            ImageView view = holder.getView(isSender ? R.id.rc_right_portrait : R.id.rc_left_portrait);
            if (uiMessage.getUserInfo().getPortraitUri() != null) {
                RongConfigCenter.featureConfig().getKitImageEngine().loadConversationPortrait(holder.getContext(), uiMessage.getUserInfo().getPortraitUri().toString(), view);
            }
            holder.setOnClickListener(R.id.rc_left_portrait, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                        boolean result = RongConfigCenter.conversationConfig().getConversationClickListener().onUserPortraitClick(holder.getContext(), uiMessage.getMessage().getConversationType(), uiMessage.getUserInfo(), uiMessage.getMessage().getTargetId());
                        if (!result) {
                            listener.onViewClick(MessageClickType.USER_PORTRAIT_CLICK, uiMessage);
                        }
                    }
                }
            });

            holder.setOnClickListener(R.id.rc_right_portrait, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                        boolean result = RongConfigCenter.conversationConfig().getConversationClickListener().onUserPortraitClick(holder.getContext(), uiMessage.getMessage().getConversationType(), uiMessage.getUserInfo(), uiMessage.getMessage().getTargetId());
                        if (!result) {
                            listener.onViewClick(MessageClickType.USER_PORTRAIT_CLICK, uiMessage);
                        }
                    }
                }
            });

            holder.setOnLongClickListener(R.id.rc_left_portrait, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                        boolean result = RongConfigCenter.conversationConfig().getConversationClickListener().onUserPortraitLongClick(holder.getContext(), uiMessage.getMessage().getConversationType(), uiMessage.getUserInfo(), uiMessage.getMessage().getTargetId());
                        if (!result) {
                            return listener.onViewLongClick(MessageClickType.USER_PORTRAIT_LONG_CLICK, uiMessage);
                        }
                    }
                    return false;
                }
            });

            holder.setOnLongClickListener(R.id.rc_right_portrait, new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (RongConfigCenter.conversationConfig().getConversationClickListener() != null) {
                        boolean result = RongConfigCenter.conversationConfig().getConversationClickListener().onUserPortraitLongClick(holder.getContext(), uiMessage.getMessage().getConversationType(), uiMessage.getUserInfo(), uiMessage.getMessage().getTargetId());
                        if (!result) {
                            return listener.onViewLongClick(MessageClickType.USER_PORTRAIT_LONG_CLICK, uiMessage);
                        }
                    }
                    return false;
                }
            });
            if (!RongConfigCenter.conversationConfig().isShowReceiverUserTitle(uiMessage.getMessage().getConversationType())) {
                holder.setVisible(R.id.rc_title, false);
            } else {
                if (!isSender) {
                    holder.setVisible(R.id.rc_title, true);
                    holder.setText(R.id.rc_title, uiMessage.getUserInfo().getName());
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

    /**
     * @return 群组或讨论组是否展示消息已读回执, 默认只有文本消息展示
     * 子类可以重写此方法
     */
    protected boolean showReadReceiptRequest(Message message) {
        return message != null && message.getContent() != null &&
                (message.getContent() instanceof TextMessage);
    }

    private static class MessageViewHolder extends ViewHolder {
        private ViewHolder mMessageContentViewHolder;

        public MessageViewHolder(Context context, View itemView, ViewHolder messageViewHolder) {
            super(context, itemView);
            mMessageContentViewHolder = messageViewHolder;
        }

        public ViewHolder getMessageContentViewHolder() {
            return mMessageContentViewHolder;
        }

    }

    @Override
    public boolean isSummaryType(MessageContent messageContent) {
        return isMessageViewType(messageContent);
    }

    @Override
    public boolean isItemViewType(UiMessage item) {
        return isMessageViewType(item.getMessage().getContent());
    }

    @Override
    public boolean showSummaryWithName() {
        return mConfig.showSummaryWithName;
    }
}
