package io.rong.imkit.config;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.net.http.SslCertificate;
import io.rong.common.RLog;
import io.rong.imkit.GlideKitImageEngine;
import io.rong.imkit.IMCenter;
import io.rong.imkit.KitImageEngine;
import io.rong.imkit.R;
import io.rong.imkit.feature.quickreply.IQuickReplyProvider;
import io.rong.imlib.model.Conversation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeatureConfig {

    private static final String TAG = "FeatureConfig";
    private static String KIT_VERSION = "4.1.0.98";
    // <!--是否支持消息引用功能，默认打开，聊天页面长按消息支持引用（目前仅支持文本消息、文件消息、图文消息、图片消息、引用消息的引用）-->
    private boolean isReferenceEnable; // 引用
    private boolean isDestructEnable; // 阅后即焚
    private boolean isQuickReplyEnable; // 快捷回复
    private IQuickReplyProvider quickReplyProvider;
    private IMCenter.VoiceMessageType voiceMessageType;
    private List<Conversation.ConversationType> readReceiptSupportTypes;
    // 设置 AMR_NB 语音消息的码率 (单位 bps)[rc_audio_encoding_bit_rate]
    private int audioNBEncodingBitRate;
    // 设置 AMR_WB 语音消息的码率 (单位 bps)[rc_audio_wb_encoding_bit_rate]
    private int audioWBEncodingBitRate;

    private KitImageEngine mKitImageEngine;
    private int userCacheMaxCount;
    private int groupCacheMaxCount;
    private int groupMemberCacheMaxCount;

    private boolean preLoadUserCache = true;
    public boolean rc_wipe_out_notification_message = true;
    public boolean rc_set_java_script_enabled = true;
    // 在前台非会话页面时，接收到新消息是否响铃
    public boolean soundInForeground = true;
    // 在前台非会话页面时，接收到新消息是否震动
    private boolean vibrateInForeground = true;

    // 是否需要显示融云默认表情
    private boolean enableRongEmoji = true;
    private SSLInterceptor sSSLInterceptor;
    public String rc_translation_src_language;
    public String rc_translation_target_language;

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
        userCacheMaxCount = 500;
        groupCacheMaxCount = 200;
        groupMemberCacheMaxCount = 500;
        rc_translation_src_language = "zh_CN";
        rc_translation_target_language = "en";
    }

    public void initConfig(Context context) {
        if (context != null) {
            Resources resources = context.getResources();
            try {
                rc_wipe_out_notification_message =
                        resources.getBoolean(R.bool.rc_wipe_out_notification_message);
            } catch (Exception e) {
                RLog.e(TAG, "rc_wipe_out_notification_message not get value", e);
            }
            try {
                rc_set_java_script_enabled =
                        resources.getBoolean(R.bool.rc_set_java_script_enabled);
            } catch (Exception e) {
                RLog.e(TAG, "rc_set_java_script_enabled not get value", e);
            }
            try {
                isDestructEnable = resources.getBoolean(R.bool.rc_open_destruct_plugin);
            } catch (Exception e) {
                RLog.e(TAG, "rc_open_destruct_plugin not get value", e);
            }
            try {
                soundInForeground = resources.getBoolean(R.bool.rc_sound_in_foreground);
            } catch (Exception e) {
                RLog.e(TAG, "rc_sound_in_foreground not get value", e);
            }

            try {
                vibrateInForeground = resources.getBoolean(R.bool.rc_vibrate_in_foreground);
            } catch (Exception e) {
                RLog.e(TAG, "rc_vibrate_in_foreground not get value", e);
            }

            try {
                rc_translation_src_language =
                        resources.getString(R.string.rc_translation_src_language);
            } catch (Exception e) {
                RLog.e(TAG, "rc_translation_src_language not get value", e);
            }

            try {
                rc_translation_target_language =
                        resources.getString(R.string.rc_translation_target_language);
            } catch (Exception e) {
                RLog.e(TAG, "rc_translation_target_language not get value", e);
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
        if (engine != null) this.mKitImageEngine = engine;
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
     *
     * @param provider 快捷回复短语的内容提供模板。
     */
    public void enableQuickReply(IQuickReplyProvider provider) {
        isQuickReplyEnable = true;
        quickReplyProvider = provider;
    }

    /** @return 用户信息内存最大值 */
    public int getUserCacheMaxCount() {
        return userCacheMaxCount;
    }

    /** @param userCacheMaxCount 设置用户信息最大值，sdk 初始化前有效 */
    public void setUserCacheMaxCount(int userCacheMaxCount) {
        this.userCacheMaxCount = userCacheMaxCount;
    }

    /** @return 群组信息内存最大值 */
    public int getGroupCacheMaxCount() {
        return groupCacheMaxCount;
    }

    /** @param groupCacheMaxCount 设置群组信息最大值，sdk 初始化前有效 */
    public void setGroupCacheMaxCount(int groupCacheMaxCount) {
        this.groupCacheMaxCount = groupCacheMaxCount;
    }

    /** @return 群成员信息内存最大值 */
    public int getGroupMemberCacheMaxCount() {
        return groupMemberCacheMaxCount;
    }

    /** @param groupMemberCacheMaxCount 设置群成员信息最大值，sdk 初始化前有效 */
    public void setGroupMemberCacheMaxCount(int groupMemberCacheMaxCount) {
        this.groupMemberCacheMaxCount = groupMemberCacheMaxCount;
    }

    /** @return 是否预加载用户缓存 */
    public boolean isPreLoadUserCache() {
        return preLoadUserCache;
    }

    /** @param preLoadUserCache 是否预加载用户缓存 */
    public void setPreLoadUserCache(boolean preLoadUserCache) {
        this.preLoadUserCache = preLoadUserCache;
    }

    /**
     * 消息是否震动
     *
     * @return 返回消息是否震动配置值
     */
    public boolean isVibrateInForeground() {
        return vibrateInForeground;
    }

    /**
     * 设置消息是否震动
     *
     * @param vibrateInForeground 是否震动
     */
    public void setVibrateInForeground(boolean vibrateInForeground) {
        this.vibrateInForeground = vibrateInForeground;
    }

    /**
     * 消息是否响铃
     *
     * @return 返回消息是否响铃配置值
     */
    public boolean isSoundInForeground() {
        return soundInForeground;
    }

    /**
     * 设置消息是否响铃
     *
     * @param soundInForeground 是否震动
     */
    public void setSoundInForeground(boolean soundInForeground) {
        this.soundInForeground = soundInForeground;
    }

    public SSLInterceptor getSSLInterceptor() {
        return sSSLInterceptor;
    }

    /** @param sSSLInterceptor 设置 CombineWebViewActivity 自签证书过滤器 */
    public void setSSLInterceptor(SSLInterceptor sSSLInterceptor) {
        this.sSSLInterceptor = sSSLInterceptor;
    }

    public interface SSLInterceptor {
        boolean check(SslCertificate sslCertificate);
    }
}
