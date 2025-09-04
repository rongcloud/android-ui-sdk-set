package io.rong.imkit.conversation.messgelist.provider;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.text.TextUtilsCompat;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.handler.SpeechToTextHandler;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.LoadingDotsView;
import io.rong.imkit.widget.SpeechToTextPopup;
import io.rong.imkit.widget.TextAnimationHelper;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.SpeechToTextInfo;
import io.rong.message.VoiceMessage;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class VoiceMessageItemProvider extends BaseMessageItemProvider<VoiceMessage> {
    private static final String TAG = "VoiceMessageItemProvider";

    public VoiceMessageItemProvider() {
        mConfig.showReadState = true;
        mConfig.showContentBubble = false;
    }

    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_voice_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(
            ViewHolder holder,
            ViewHolder parentHolder,
            VoiceMessage message,
            UiMessage uiMessage,
            int position,
            List<UiMessage> list,
            IViewProviderListener<UiMessage> listener) {
        boolean isSender =
                uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);
        holder.setBackgroundRes(
                R.id.rc_voice_bg,
                isSender ? R.drawable.rc_ic_bubble_right : R.drawable.rc_ic_bubble_left);
        int minWidth = 70, maxWidth = 204;
        float scale = holder.getContext().getResources().getDisplayMetrics().density;
        minWidth = (int) (minWidth * scale + 0.5f);
        maxWidth = (int) (maxWidth * scale + 0.5f);
        int duration = AudioRecordManager.getInstance().getMaxVoiceDuration();

        View rcVoiceBgView = holder.getView(R.id.rc_voice_bg);
        TextView rcDuration = holder.getView(R.id.rc_duration);
        if (!checkViewsValid(rcVoiceBgView, rcDuration)) {
            RLog.e(TAG, "checkViewsValid error," + uiMessage.getObjectName());
            return;
        }

        rcVoiceBgView.getLayoutParams().width =
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
            rcDuration.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) rcDuration.getLayoutParams();
            lp.setMarginEnd(12);
            rcDuration.setLayoutParams(lp);
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.rc_voice_send, animationDrawable);
                if (animationDrawable != null) animationDrawable.start();
            } else {
                holder.setImageResource(R.id.rc_voice_send, R.drawable.rc_voice_send_play3);
            }
            holder.setVisible(R.id.rc_voice_unread, false);
        } else {
            AnimationDrawable animationDrawable =
                    (AnimationDrawable)
                            holder.getContext()
                                    .getResources()
                                    .getDrawable(R.drawable.rc_an_voice_receive);
            holder.setVisible(R.id.rc_voice, true);
            holder.setVisible(R.id.rc_voice_send, false);
            rcDuration.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) rcDuration.getLayoutParams();
            lp.setMarginStart(12);
            rcDuration.setLayoutParams(lp);
            if (uiMessage.isPlaying()) {
                holder.setImageDrawable(R.id.rc_voice, animationDrawable);
                if (animationDrawable != null) animationDrawable.start();
            } else {
                holder.setImageResource(R.id.rc_voice, R.drawable.rc_voice_receive_play3);
            }
            holder.setVisible(
                    R.id.rc_voice_unread, !uiMessage.getMessage().getReceivedStatus().isListened());
        }

        // 设置语音转文字UI
        setupSpeechToTextUI(holder, message, uiMessage, listener);
    }

    @Override
    protected boolean onItemClick(
            ViewHolder holder,
            VoiceMessage message,
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
        return messageContent instanceof VoiceMessage && !messageContent.isDestruct();
    }

    @Override
    public Spannable getSummarySpannable(Context context, VoiceMessage message) {
        return new SpannableString(context.getString(R.string.rc_message_content_voice));
    }

    /** 设置语音转文字UI 根据sttInfo的状态和可见性控制UI显示 */
    private void setupSpeechToTextUI(
            @NonNull ViewHolder holder,
            @NonNull VoiceMessage message,
            UiMessage uiMessage,
            IViewProviderListener<UiMessage> listener) {

        SpeechToTextInfo sttInfo = message.getSttInfo();

        // 获取UI组件
        SpeechToTextViews views = getSpeechToTextViews(holder);
        if (views == null) {
            RLog.e(TAG, "getSpeechToTextViews failed," + uiMessage.getObjectName());
            return;
        }

        // 如果没有语音转文字信息，隐藏相关UI
        if (sttInfo == null) {
            hideSpeechToTextViews(views);
            return;
        }

        // 如果消息发送状态是正在发送、发送失败或取消，则不显示语音转文字UI
        Message.SentStatus sentStatus = uiMessage.getMessage().getSentStatus();
        if (sentStatus == Message.SentStatus.SENDING
                || sentStatus == Message.SentStatus.FAILED
                || sentStatus == Message.SentStatus.CANCELED) {
            hideSpeechToTextViews(views);
            return;
        }

        // 设置对齐
        views.loadingDots.setVisibility(View.GONE);
        setViewAlignment(views, uiMessage);

        // 统一设置点击事件拦截，防止传递给外层View
        views.sttContainer.setOnClickListener(v -> {});
        views.sttContainer.setOnLongClickListener(v -> false);

        // 如果处于隐藏状态，直接隐藏语音转文字UI
        if (Objects.equals(
                uiMessage.getBusinessState(), SpeechToTextHandler.SPEECH_TO_TEXT_HIDDEN_STATE)) {
            hideSpeechToTextViews(views);
            return;
        }

        // 如果处于 Loading 状态，显示加载中UI
        if (Objects.equals(
                uiMessage.getBusinessState(), SpeechToTextHandler.SPEECH_TO_TEXT_LOADING_STATE)) {
            showConvertingState(views);
            return;
        }

        // 如果不可见，隐藏语音转文字UI
        if (!sttInfo.isVisible()) {
            hideSpeechToTextViews(views);
            return;
        }

        // 根据状态显示UI
        String transcribedText = sttInfo.getText();

        switch (sttInfo.getStatus()) {
            case CONVERTING:
                showConvertingState(views);
                break;
            case SUCCESS:
                showSuccessState(views, transcribedText, uiMessage, listener);
                break;
            case FAILED:
                showFailedState(views);
                break;
            default:
                hideSpeechToTextViews(views);
                break;
        }
    }

    /** 隐藏语音转文字Views */
    private void hideSpeechToTextViews(SpeechToTextViews views) {
        if (views.sttContainer != null) {
            views.sttContainer.setVisibility(View.GONE);
            views.sttContainer.setClipBounds(null);
        }
        if (views.loadingDots != null) {
            views.loadingDots.setVisibility(View.GONE);
        }
        if (views.sttText != null) {
            // 清除图标（隐藏状态清除所有图标）
            views.sttText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    /** 设置View对齐方式 */
    private void setViewAlignment(SpeechToTextViews views, UiMessage uiMessage) {
        boolean isSender =
                uiMessage.getMessage().getMessageDirection().equals(Message.MessageDirection.SEND);
        int gravity = isSender ? Gravity.END : Gravity.START;

        // 设置容器对齐
        if (views.sttContainer != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) views.sttContainer.getLayoutParams();
            if (params != null) {
                params.gravity = gravity;
                views.sttContainer.setLayoutParams(params);
            }
        }

        if (views.rcLayout != null) {
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) views.rcLayout.getLayoutParams();
            if (params != null) {
                params.gravity = gravity;
                views.rcLayout.setLayoutParams(params);
            }
        }
    }

    /** 显示转换中状态 */
    private void showConvertingState(SpeechToTextViews views) {
        views.sttContainer.setVisibility(View.VISIBLE);
        views.sttContainer.setClipBounds(null);

        if (views.sttText != null) {
            views.sttText.setVisibility(View.GONE);
            // 清除图标（转换中状态不显示图标）
            views.sttText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        if (views.loadingDots != null) {
            views.loadingDots.setVisibility(View.VISIBLE);
            views.loadingDots.startAnimation();
        }
    }

    /** 显示转换失败状态 */
    private void showFailedState(SpeechToTextViews views) {
        views.sttContainer.setVisibility(View.VISIBLE);
        views.sttContainer.setClipBounds(null);

        if (views.sttText != null) {
            views.sttText.setVisibility(View.VISIBLE);
            views.sttText.setText(
                    views.sttText.getContext().getString(R.string.rc_speech_to_text_failed));
            views.sttText.setTextColor(
                    views.sttText.getContext().getResources().getColor(R.color.rc_secondary_color));

            // 设置失败图标到文字左侧，间距为4dp
            views.sttText.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.rc_ic_warning, 0, 0, 0);
            views.sttText.setCompoundDrawablePadding(
                    (int)
                            (4
                                    * views.sttText
                                            .getContext()
                                            .getResources()
                                            .getDisplayMetrics()
                                            .density));
        }
    }

    /** 显示转换成功状态 */
    private void showSuccessState(
            SpeechToTextViews views,
            String transcribedText,
            UiMessage uiMessage,
            IViewProviderListener<UiMessage> listener) {
        // 如果为空, 则添加一个空格, 展示给用户
        if (TextUtils.isEmpty(transcribedText)) {
            transcribedText = " ";
        }

        // 设置文本样式并启动动画
        if (views.sttText != null) {
            views.sttText.setTextColor(
                    views.sttText.getContext().getResources().getColor(R.color.rc_text_main_color));

            // 清除图标（成功状态不显示图标）
            views.sttText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

            String messageUid = uiMessage.getMessage().getUId();
            boolean isLeftToRight =
                    !uiMessage
                            .getMessage()
                            .getMessageDirection()
                            .equals(Message.MessageDirection.SEND);

            TextAnimationHelper.startWithUidCheck(
                    views.sttContainer, views.sttText, transcribedText, messageUid, isLeftToRight);
        }

        // 设置长按事件
        String finalTranscribedText = transcribedText;
        views.sttContainer.setOnLongClickListener(
                v -> {
                    showSpeechToTextDialog(
                            v.getContext(),
                            finalTranscribedText,
                            listener,
                            uiMessage,
                            views.sttContainer);
                    return true;
                });
    }

    /** 显示语音转文字操作弹窗 */
    private void showSpeechToTextDialog(
            Context context,
            String text,
            IViewProviderListener<UiMessage> listener,
            UiMessage uiMessage,
            View anchorView) {
        SpeechToTextPopup popup = new SpeechToTextPopup(context);
        popup.setOnActionClickListener(
                new SpeechToTextPopup.OnActionClickListener() {
                    @Override
                    public void onCopyTextClick() {
                        copyTextToClipboard(context, text);
                    }

                    @Override
                    public void onCancelSpeechToTextClick() {
                        if (listener != null) {
                            listener.onViewClick(MessageClickType.SPEECH_TO_TEXT, uiMessage);
                        }
                    }
                });

        popup.showAboveView(anchorView);
    }

    private void copyTextToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(null, text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            RLog.e(TAG, "copyTextToClipboard stt text", e);
        }
    }

    /** 语音转文字UI组件封装 */
    private static class SpeechToTextViews {
        final TextView sttText;
        final LinearLayout sttContainer;
        final LoadingDotsView loadingDots;
        final LinearLayout rcLayout;

        SpeechToTextViews(
                TextView sttText,
                LinearLayout sttContainer,
                LoadingDotsView loadingDots,
                LinearLayout rcLayout) {
            this.sttText = sttText;
            this.sttContainer = sttContainer;
            this.loadingDots = loadingDots;
            this.rcLayout = rcLayout;
        }
    }

    /** 获取语音转文字相关的UI组件 */
    private SpeechToTextViews getSpeechToTextViews(ViewHolder holder) {
        TextView sttText = holder.getView(R.id.rc_stt_text);
        LinearLayout sttContainer = holder.getView(R.id.rc_stt_container);
        LoadingDotsView loadingDots = holder.getView(R.id.rc_loading_dots);
        LinearLayout rcLayout = holder.getView(R.id.rc_layout);

        if (!checkViewsValid(sttText, sttContainer, loadingDots, rcLayout)) {
            return null;
        }
        return new SpeechToTextViews(sttText, sttContainer, loadingDots, rcLayout);
    }
}
