package io.rong.imkit.feature.destruct.provider;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.text.TextUtilsCompat;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.conversation.messgelist.provider.MessageClickType;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.HQVoiceMessage;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class DestructHQVoiceMessageItemProvider extends BaseMessageItemProvider<HQVoiceMessage> {
    private static final String TAG = DestructHQVoiceMessageItemProvider.class.getSimpleName();

    public DestructHQVoiceMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View textView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_destruct_hq_voice_message, parent, false);
        return new ViewHolder(parent.getContext(), textView);
    }

    @Override
    protected void bindMessageContentViewHolder(
            final ViewHolder holder,
            ViewHolder parentHolder,
            HQVoiceMessage message,
            final UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        holder.getConvertView().setTag(uiMessage.getMessage().getUId());
        boolean isSender =
                uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);
        holder.setBackgroundRes(
                R.id.rc_voice_bg,
                isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        holder.setVisible(R.id.fl_send_fire, isSender);
        holder.setVisible(R.id.fl_receiver_fire, !isSender);
        if (!isSender) {
            DestructManager.getInstance()
                    .addListener(
                            uiMessage.getMessage().getUId(),
                            new DestructListener(holder, uiMessage),
                            TAG);
            boolean isRead = uiMessage.getMessage().getReadTime() > 0;
            holder.setVisible(R.id.tv_receiver_fire, isRead);
            holder.setVisible(R.id.iv_receiver_fire, !isRead);
            if (isRead) {
                String unFinishTime;
                if (TextUtils.isEmpty(uiMessage.getDestructTime())) {
                    unFinishTime =
                            DestructManager.getInstance()
                                    .getUnFinishTime(uiMessage.getMessage().getUId());
                } else {
                    unFinishTime = uiMessage.getDestructTime();
                }
                holder.setText(R.id.tv_receiver_fire, unFinishTime);
                DestructManager.getInstance().startDestruct(uiMessage.getMessage());
            }
        }
        int minWidth = 70, maxWidth = 204;
        float scale = holder.getContext().getResources().getDisplayMetrics().density;
        minWidth = (int) (minWidth * scale + 0.5f);
        maxWidth = (int) (maxWidth * scale + 0.5f);
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();
        holder.getView(R.id.rc_voice_bg).getLayoutParams().width =
                minWidth + (maxWidth - minWidth) / duration * message.getDuration();
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
                == LayoutDirection.RTL) {
            holder.setText(R.id.rc_duration, String.format("\"%s", message.getDuration()));
        } else {
            holder.setText(R.id.rc_duration, String.format("%s\"", message.getDuration()));
        }
        if (uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND) {
            AnimationDrawable animationDrawable =
                    (AnimationDrawable)
                            holder.getContext()
                                    .getResources()
                                    .getDrawable(R.drawable.rc_an_voice_send);
            holder.setVisible(R.id.rc_voice, false);
            holder.setVisible(R.id.rc_voice_send, true);
            ((TextView) holder.getView(R.id.rc_duration))
                    .setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) holder.getView(R.id.rc_duration).getLayoutParams();
            lp.setMarginEnd(12);
            holder.getView(R.id.rc_duration).setLayoutParams(lp);
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.rc_voice_send, animationDrawable);
                if (animationDrawable != null) animationDrawable.start();
            } else {
                holder.setImageResource(R.id.rc_voice_send, R.drawable.rc_voice_send_play3);
            }
            holder.setVisible(R.id.rc_voice_unread, false);
            holder.setVisible(R.id.rc_voice_download_error, false);
            holder.setVisible(R.id.rc_download_progress, false);
        } else {
            AnimationDrawable animationDrawable =
                    (AnimationDrawable)
                            holder.getContext()
                                    .getResources()
                                    .getDrawable(R.drawable.rc_an_voice_receive);
            holder.setVisible(R.id.rc_voice, true);
            holder.setVisible(R.id.rc_voice_send, false);
            ((TextView) holder.getView(R.id.rc_duration))
                    .setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) holder.getView(R.id.rc_duration).getLayoutParams();
            lp.setMarginStart(12);
            holder.getView(R.id.rc_duration).setLayoutParams(lp);
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.rc_voice, animationDrawable);
                if (animationDrawable != null) animationDrawable.start();
            } else {
                holder.setImageResource(R.id.rc_voice, R.drawable.rc_voice_receive_play3);
            }

            if (message.getLocalPath() != null) {
                holder.setVisible(R.id.rc_voice_download_error, false);
                holder.setVisible(R.id.rc_download_progress, false);
                holder.setVisible(
                        R.id.rc_voice_unread,
                        !uiMessage.getMessage().getReceivedStatus().isListened());

            } else {
                if (uiMessage.getState() == State.ERROR
                        || !NetUtils.isNetWorkAvailable(holder.getContext())) {
                    holder.setVisible(R.id.rc_voice_unread, false);
                    holder.setVisible(R.id.rc_voice_download_error, true);
                    holder.setVisible(R.id.rc_download_progress, false);
                } else if (uiMessage.getState() == State.PROGRESS) {
                    holder.setVisible(R.id.rc_voice_unread, false);
                    holder.setVisible(R.id.rc_voice_download_error, false);
                    holder.setVisible(R.id.rc_download_progress, true);
                } else {
                    holder.setVisible(R.id.rc_voice_unread, true);
                    holder.setVisible(R.id.rc_voice_download_error, false);
                    holder.setVisible(R.id.rc_download_progress, false);
                }
            }
        }
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            HQVoiceMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        if (listener != null) {
            listener.onViewClick(MessageClickType.AUDIO_CLICK, uiMessage);
            return true;
        }
        return false;
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof HQVoiceMessage && messageContent.isDestruct();
    }

    @Override
    public Spannable getSummarySpannable(Context context, HQVoiceMessage message) {
        return new SpannableString(
                context.getString(R.string.rc_conversation_summary_content_burn));
    }

    @Override
    public Spannable getSummarySpannable(Context context, Conversation conversation) {
        RLog.d(TAG, "getSummarySpannable");
        SpannableString spannableString =
                new SpannableString(
                        context.getString(R.string.rc_conversation_summary_content_burn));
        MessageContent latestMessage = conversation.getLatestMessage();
        if (latestMessage != null
                && conversation.getLatestMessageDirection() == Message.MessageDirection.RECEIVE
                && !conversation.getReceivedStatus().isListened()) {
            ForegroundColorSpan foregroundColorSpan =
                    new ForegroundColorSpan(
                            context.getResources().getColor(R.color.rc_unread_message_color));
            spannableString.setSpan(
                    foregroundColorSpan,
                    0,
                    spannableString.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
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
