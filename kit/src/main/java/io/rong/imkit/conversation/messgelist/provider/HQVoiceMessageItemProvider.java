package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.text.TextUtilsCompat;

import java.util.List;
import java.util.Locale;

import io.rong.imkit.R;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.model.State;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.HQVoiceMessage;

public class HQVoiceMessageItemProvider extends BaseMessageItemProvider<HQVoiceMessage> {
    public HQVoiceMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_hq_voice_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder,ViewHolder parentHolder, final HQVoiceMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        boolean isSender = uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);
        holder.setBackgroundRes(R.id.rc_voice_bg, isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        int minWidth = 70, maxWidth = 204;
        float scale = holder.getContext().getResources().getDisplayMetrics().density;
        minWidth = (int) (minWidth * scale + 0.5f);
        maxWidth = (int) (maxWidth * scale + 0.5f);
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();
        ;
        holder.getView(R.id.rc_voice_bg).getLayoutParams().width = minWidth + (maxWidth - minWidth) / duration * message.getDuration();
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == LayoutDirection.RTL) {
            holder.setText(R.id.rc_duration, String.format("\"%s", message.getDuration()));
        } else {
            holder.setText(R.id.rc_duration, String.format("%s\"", message.getDuration()));
        }

        if (uiMessage.getMessage().getMessageDirection() == Message.MessageDirection.SEND) {
            AnimationDrawable animationDrawable = (AnimationDrawable) holder.getContext().getResources().getDrawable(R.drawable.rc_an_voice_send);
            holder.setVisible(R.id.rc_voice, false);
            holder.setVisible(R.id.rc_voice_send, true);
            ((TextView) holder.getView(R.id.rc_duration)).setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.getView(R.id.rc_duration).getLayoutParams();
            lp.setMarginEnd(12);
            holder.getView(R.id.rc_duration).setLayoutParams(lp);
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.rc_voice_send, animationDrawable);
                if (animationDrawable != null)
                    animationDrawable.start();
            } else {
                holder.setImageResource(R.id.rc_voice_send, R.drawable.rc_voice_send_play3);
            }
            holder.setVisible(R.id.rc_voice_unread, false);
            holder.setVisible(R.id.rc_voice_download_error, false);
            holder.setVisible(R.id.rc_download_progress, false);
        } else {
            AnimationDrawable animationDrawable = (AnimationDrawable) holder.getContext().getResources().getDrawable(R.drawable.rc_an_voice_receive);
            holder.setVisible(R.id.rc_voice, true);
            holder.setVisible(R.id.rc_voice_send, false);
            ((TextView) holder.getView(R.id.rc_duration)).setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.getView(R.id.rc_duration).getLayoutParams();
            lp.setMarginStart(12);
            holder.getView(R.id.rc_duration).setLayoutParams(lp);
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.rc_voice, animationDrawable);
                if (animationDrawable != null)
                    animationDrawable.start();
            } else {
                holder.setImageResource(R.id.rc_voice, R.drawable.rc_voice_receive_play3);
            }

            if (message.getLocalPath() != null) {
                holder.setVisible(R.id.rc_voice_download_error, false);
                holder.setVisible(R.id.rc_download_progress, false);
                holder.setVisible(R.id.rc_voice_unread, !uiMessage.getMessage().getReceivedStatus().isListened());
            } else {
                if (uiMessage.getState() == State.ERROR || !NetUtils.isNetWorkAvailable(holder.getContext())) {
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
    protected boolean onItemClick(ViewHolder holder, HQVoiceMessage message, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        if (listener != null) {
            listener.onViewClick(MessageClickType.AUDIO_CLICK,uiMessage);
            return true;
        }
        return false;
    }


    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof HQVoiceMessage && !messageContent.isDestruct();
    }


    @Override
    public Spannable getSummarySpannable(Context context, HQVoiceMessage hqVoiceMessage) {
        return new SpannableString(context.getString(R.string.rc_conversation_summary_content_voice));
    }
}
