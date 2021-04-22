package io.rong.imkit.config;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.GlideKitImageEngine;
import io.rong.imkit.IMCenter;
import io.rong.imkit.KitImageEngine;
import io.rong.imkit.R;
import io.rong.imkit.feature.quickreply.IQuickReplyProvider;
import io.rong.imlib.model.Conversation;

public class FeatureConfig {

    private final static String TAG = "FeatureConfig";
    private static String KIT_VERSION = "4.1.0.98";
    //<!--是否支持消息引用功能，默认打开，聊天页面长按消息支持引用（目前仅支持文本消息、文件消息、图文消息、图片消息、引用消息的引用）-->
    private boolean isReferenceEnable; //引用
    private boolean isDestructEnable; //阅后即焚
    private boolean isQuickReplyEnable; //快捷回复
    private IQuickReplyProvider quickReplyProvider;
    private IMCenter.VoiceMessageType voiceMessageType;
    private List<Conversation.ConversationType> readReceiptSupportTypes;
    //设置 AMR_NB 语音消息的码率 (单位 bps)[rc_audio_encoding_bit_rate]
    private int audioNBEncodingBitRate;
    //设置 AMR_WB 语音消息的码率 (单位 bps)[rc_audio_wb_encoding_bit_rate]
    private int audioWBEncodingBitRate;

    private KitImageEngine mKitImageEngine;

    public boolean rc_wipe_out_notification_message = true;
    public boolean rc_set_java_script_enabled = true;
    // 在前台非会话页面时，接收到新消息是否响铃
    public boolean rc_sound_in_foreground = true;

    public FeatureConfig() {
        isReferenceEnable = true;
        isDestructEnable = false;
        voiceMessageType = IMCenter.VoiceMessageType.HighQuality;
        readReceiptSupportTypes = new ArrayList<>();
        readReceiptSupportTypes.add(Conversation.ConversationType.PRIVATE);
        readReceiptSupportTypes.add(Conversation.ConversationType.GROUP);
        isQuickReplyEnable = false;
        audioNBEncodingBitRate = 7950;
        audioWBEncodingBitRate = 12650;
        mKitImageEngine = new GlideKitImageEngine();
    }

    public void initConfig(Context context) {
        if (context != null) {
            Resources resources = context.getResources();
            try {
                rc_wipe_out_notification_message = resources.getBoolean(R.bool.rc_wipe_out_notification_message);
            } catch (Exception e) {
                RLog.e(TAG, "rc_wipe_out_notification_message not get value", e);
            }
            try {
                rc_set_java_script_enabled = resources.getBoolean(R.bool.rc_set_java_script_enabled);
            } catch (Exception e) {
                RLog.e(TAG, "rc_set_java_script_enabled not get value", e);
            }
            try {
                isDestructEnable = resources.getBoolean(R.bool.rc_open_destruct_plugin);
            } catch (Exception e) {
                RLog.e(TAG, "rc_open_destruct_plugin not get value", e);
            }
            try {
                rc_sound_in_foreground = resources.getBoolean(R.bool.rc_sound_in_foreground);
            } catch (Exception e) {
                RLog.e(TAG, "rc_sound_in_foreground not get value", e);
            }
        }
    }


    public boolean isReferenceEnable() {
        return isReferenceEnable;
    }

    public boolean isDestructEnable() {
        return isDestructEnable;
    }

    public boolean isQuickReplyEnable() {
        return isQuickReplyEnable;
    }

    public boolean isQuickReplyType() {
        return isQuickReplyEnable;
    }

    public void setAudioNBEncodingBitRate(int audioNBEncodingBitRate) {
        this.audioNBEncodingBitRate = audioNBEncodingBitRate;
    }

    public void setAudioWBEncodingBitRate(int audioWBEncodingBitRate) {
        this.audioWBEncodingBitRate = audioWBEncodingBitRate;
    }

    public void setKitImageEngine(KitImageEngine engine) {
        if (engine != null)
            this.mKitImageEngine = engine;
    }

    public void setVoiceMessageType(IMCenter.VoiceMessageType type) {
        this.voiceMessageType = type;
    }

    public int getAudioNBEncodingBitRate() {
        return audioNBEncodingBitRate;
    }

    public KitImageEngine getKitImageEngine() {
        return mKitImageEngine;
    }

    public int getAudioWBEncodingBitRate() {
        return audioWBEncodingBitRate;
    }

    public IQuickReplyProvider getQuickReplyProvider() {
        return quickReplyProvider;
    }

    public boolean isReadReceiptConversationType(Conversation.ConversationType type) {
        if (readReceiptSupportTypes != null) {
            return readReceiptSupportTypes.contains(type);
        }
        return false;
    }

    public IMCenter.VoiceMessageType getVoiceMessageType() {
        return voiceMessageType;
    }

    public void enableReadReceipt(Conversation.ConversationType... supportedTypes) {
        if (supportedTypes != null) {
            readReceiptSupportTypes.clear();
            readReceiptSupportTypes.addAll(Arrays.asList(supportedTypes));
        }
    }

    public void enableReference(Boolean value) {
        isReferenceEnable = value;
    }

    public void enableDestruct(Boolean value) {
        isDestructEnable = value;
    }

    /**
     * 开启快捷回复功能。需要在{@link IMCenter#init(Application, String, boolean)} 之前调用。
     * @param provider 快捷回复短语的内容提供模板。
     */
    public void enableQuickReply(IQuickReplyProvider provider) {
        isQuickReplyEnable = true;
        quickReplyProvider = provider;
    }
}
