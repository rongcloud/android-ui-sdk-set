package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.recallEdit.RecallEditCountDownCallBack;
import io.rong.imkit.feature.recallEdit.RecallEditManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;

public class RecallNotificationMessageItemProvider extends BaseNotificationMessageItemProvider<RecallNotificationMessage> {
    private static final String TAG = "RecallNotificationMessageItemProvider";

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View rootView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_information_notification_message, parent, false);
        return new RecallEditViewHolder(parent.getContext(), rootView);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, RecallNotificationMessage content, final UiMessage uiMessage, int position, List<UiMessage> list, final IViewProviderListener<UiMessage> listener) {
        holder.setText(R.id.rc_msg, getInformation(holder.getContext(), content));
        long validTime = RongConfigCenter.conversationConfig().rc_message_recall_edit_interval;
        long countDownTime = System.currentTimeMillis() - content.getRecallActionTime();
        RecallEditViewHolder viewHolder = (RecallEditViewHolder) holder;
//        // 判断被复用了，取消上一个 item 的倒计时
        if (!TextUtils.isEmpty(viewHolder.messageId)) {
            RecallEditManager.getInstance().cancelCountDown(viewHolder.messageId);
        }
        viewHolder.messageId = String.valueOf(uiMessage.getMessage().getMessageId());
        if (content.getRecallActionTime() > 0 && countDownTime < validTime * 1000) {
            if (uiMessage.isEdit()) {
                TextView tvEdit = holder.getView(R.id.rc_edit);
                tvEdit.setTextColor(holder.getContext().getResources().getColor(R.color.rc_text_color_primary_inverse));
                tvEdit.setEnabled(false);
            } else {
                TextView tvEdit = holder.getView(R.id.rc_edit);
                tvEdit.setTextColor(holder.getContext().getResources().getColor(R.color.rc_blue));
                tvEdit.setEnabled(true);
            }
            holder.setVisible(R.id.rc_edit, true);
            RecallEditManager.getInstance().startCountDown(uiMessage.getMessage(), validTime * 1000 - countDownTime, new RecallEditCountDownListener(viewHolder));
            holder.setOnClickListener(R.id.rc_edit, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onViewClick(MessageClickType.REEDIT_CLICK, uiMessage);
                    }
                }
            });
        } else {
            holder.setVisible(R.id.rc_edit, false);
        }
    }


    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof RecallNotificationMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, RecallNotificationMessage data) {
        if (data != null) {
            return new SpannableString(getInformation(context, data));
        }
        return null;
    }

    private String getInformation(Context context, RecallNotificationMessage content) {
        String information;
        String operatorId = content.getOperatorId();

        if (TextUtils.isEmpty(operatorId)) {
            RLog.e(TAG, "RecallMessageItemProvider bindView - operatorId is empty");
            information = context.getString(R.string.rc_recalled_a_message);
        } else if (content.isAdmin()) {
            information = context.getString(R.string.rc_admin_recalled_message);
        } else if (operatorId.equals(RongIMClient.getInstance().getCurrentUserId())) {
            information = context.getString(R.string.rc_you_recalled_a_message);
        } else {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(operatorId);
            if (userInfo != null && userInfo.getName() != null) {
                information = userInfo.getName() + context.getString(R.string.rc_recalled_a_message);
            } else {
                information = operatorId + context.getString(R.string.rc_recalled_a_message);
            }
        }

        return information;
    }

    private class RecallEditViewHolder extends ViewHolder {
        public String messageId;

        public RecallEditViewHolder(Context context, View itemView) {
            super(context, itemView);
        }
    }

    private static class RecallEditCountDownListener implements RecallEditCountDownCallBack {
        private WeakReference<RecallEditViewHolder> mHolder;

        public RecallEditCountDownListener(RecallEditViewHolder holder) {
            mHolder = new WeakReference<>(holder);
        }

        @Override
        public void onFinish(String messageId) {
            RecallEditViewHolder viewHolder = mHolder.get();
            if (viewHolder != null && messageId.equals(viewHolder.messageId)) {
                viewHolder.getConvertView().findViewById(R.id.rc_edit).setVisibility(View.GONE);
            }
        }
    }
}
