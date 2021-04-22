package io.rong.imkit.config;

import android.content.Context;

import io.rong.imkit.notification.NotificationConfig;

/**
 * SDK 配置入口。 应用可以通过该类更改各页面的配置。
 * 注意：需要在 init() 之前调用。
 */
public class RongConfigCenter {
    private static final String TAG = RongConfigCenter.class.getSimpleName();
    private static ConversationListConfig sConversationListConfig = new ConversationListConfig();
    private static ConversationConfig sConversationConfig = new ConversationConfig();
    private static FeatureConfig sFeatureConfig = new FeatureConfig();
    private static NotificationConfig sNotificationConfig = new NotificationConfig();
    private static GatheredConversationConfig sGatheredConversationConfig = new GatheredConversationConfig();

    public static void syncFromXml(Context context) {
        sConversationConfig.initConfig(context);
        sFeatureConfig.initConfig(context);
        sConversationListConfig.initConfig(context);
    }

    /**
     * 获取会话列表配置。
     *
     * @return
     */
    public static ConversationListConfig conversationListConfig() {
        return sConversationListConfig;
    }

    /**
     * 获取会话配置。
     *
     * @return
     */
    public static ConversationConfig conversationConfig() {
        return sConversationConfig;
    }

    public static FeatureConfig featureConfig() {
        return sFeatureConfig;
    }

    public static NotificationConfig notificationConfig() {
        return sNotificationConfig;
    }

    public static GatheredConversationConfig gatheredConversationConfig() {
        return sGatheredConversationConfig;
    }
}
