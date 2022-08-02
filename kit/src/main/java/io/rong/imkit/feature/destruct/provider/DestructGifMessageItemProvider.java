package io.rong.imkit.feature.destruct.provider;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.activity.GIFPreviewActivity;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.GIFMessage;

public class DestructGifMessageItemProvider extends BaseMessageItemProvider<GIFMessage> {
    private static final String TAG = DestructGifMessageItemProvider.class.getSimpleName();

    public DestructGifMessageItemProvider() {
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_destruct_image_message, parent, false);
        return new ViewHolder(view.getContext(), view);
    }


    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, GIFMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        boolean isSender = uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND;
        holder.getConvertView().setTag(uiMessage.getMessage().getUId());
        holder.setBackgroundRes(R.id.rc_destruct_click, isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        holder.setVisible(R.id.fl_send_fire, isSender);
        holder.setVisible(R.id.fl_receiver_fire, !isSender);
        TextView clickHint = holder.getView(R.id.rc_destruct_click_hint);
        if (!isSender) {
            DestructManager.getInstance().addListener(uiMessage.getMessage().getUId(), new DestructListener(holder, uiMessage), TAG);
            boolean isRead = uiMessage.getMessage().getReadTime() > 0;
            holder.setVisible(R.id.tv_receiver_fire, isRead);
            holder.setVisible(R.id.iv_receiver_fire, !isRead);
            if (isRead) {
                String unFinishTime;
                if (TextUtils.isEmpty(uiMessage.getDestructTime())) {
                    unFinishTime = DestructManager.getInstance().getUnFinishTime(uiMessage.getMessage().getUId());
                } else {
                    unFinishTime = uiMessage.getDestructTime();
                }
                holder.setText(R.id.tv_receiver_fire, unFinishTime);
                DestructManager.getInstance().startDestruct(uiMessage.getMessage());
            }
        }
    }


    @Override
    protected boolean onItemClick(ViewHolder holder, GIFMessage imageMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        Intent intent = new Intent(holder.getContext(), GIFPreviewActivity.class);
        intent.putExtra("message", uiMessage.getMessage());
        holder.getContext().startActivity(intent);
        return true;
    }

    @Override
    public Spannable getSummarySpannable(Context context, GIFMessage imageMessage) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_burn));
    }


    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof GIFMessage && messageContent.isDestruct();
    }

    private static class DestructListener implements RongIMClient.DestructCountDownTimerListener {
        private WeakReference<ViewHolder> mHolder;
        private UiMessage mUIMessage;

        public DestructListener(ViewHolder pHolder, UiMessage pUIMessage) {
            mHolder = new WeakReference<>(pHolder);
            mUIMessage = pUIMessage;
        }

        @Override
        public void onTick(long millisUntilFinished, String messageId) {
            if (mUIMessage.getMessage().getUId().equals(messageId)) {
                ViewHolder holder = mHolder.get();
                if (holder != null && messageId.equals(holder.getConvertView().getTag())) {
                    holder.setVisible(R.id.tv_receiver_fire, true);
                    holder.setVisible(R.id.iv_receiver_fire, false);
                    String unDestructTime = String.valueOf(Math.max(millisUntilFinished, 1));
                    holder.setText(R.id.tv_receiver_fire, unDestructTime);
                    mUIMessage.setDestructTime(unDestructTime);
                }
            }
        }

        @Override
        public void onStop(String messageId) {
            if (mUIMessage.getMessage().getUId().equals(messageId)) {
                ViewHolder holder = mHolder.get();
                if (holder != null && messageId.equals(holder.getConvertView().getTag())) {
                    holder.setVisible(R.id.tv_receiver_fire, false);
                    holder.setVisible(R.id.iv_receiver_fire, true);
                    mUIMessage.setDestructTime(null);
                }
            }
        }
    }

}
