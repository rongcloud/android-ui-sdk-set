package io.rong.imkit.notification;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import io.rong.imlib.model.Message;

public class NotificationConfig {
    private NotificationChannel mChannel;
    private PendingIntent mPendingIntent;
    private TitleType mTitleType;
    private ForegroundOtherPageAction mOtherPageAction;
    private Interceptor mInterceptor;

    public NotificationConfig() {
        mTitleType = TitleType.TARGET_NAME;
        mOtherPageAction = ForegroundOtherPageAction.Sound;
    }

    public NotificationChannel getNotificationChannel() {
        return mChannel;
    }

    public void setNotificationChannel(NotificationChannel channel) {
        this.mChannel = channel;
    }

    public Interceptor getInterceptor() {
        return mInterceptor;
    }

    /**
     * 本地通知拦截器
     *
     * @param interceptor 通知拦截器 @Deprecated 5.1.4版本废弃，请使用{{@link
     *     #setInterceptor(DefaultInterceptor)}}
     */
    @Deprecated
    public void setInterceptor(Interceptor interceptor) {
        this.mInterceptor = interceptor;
    }

    /**
     * 本地通知拦截器
     *
     * @param interceptor 默认的抽象类实现，用户可以继承 DefaultInterceptor，复写方法，进行拦截处理
     */
    public void setInterceptor(DefaultInterceptor interceptor) {
        this.mInterceptor = interceptor;
    }

    public TitleType getTitleType() {
        return mTitleType;
    }

    public void setTitleType(TitleType type) {
        this.mTitleType = type;
    }

    public ForegroundOtherPageAction getForegroundOtherPageAction() {
        return mOtherPageAction;
    }

    public void setForegroundOtherPageAction(ForegroundOtherPageAction action) {
        this.mOtherPageAction = action;
    }

    /** 通知标题的类型 */
    public enum TitleType {
        APP_NAME, // 应用名称
        TARGET_NAME; // 消息目标 Id 的名称
    }

    /** 在前台非会话页面，接受到消息时的行为 */
    public enum ForegroundOtherPageAction {
        Silent, // 静默
        Sound, // 震动或响铃，根据系统设置决定
        Notification // 弹通知，和后台时的行为一致。
    }

    public interface Interceptor {
        /**
         * /~chiense 是否拦截此本地通知，一般用于自定义本地通知的显示。
         *
         * @param message 本地通知对应的消息
         * @return 是否拦截。true 拦截本地通知，SDK 不弹出通知，需要用户自己处理。false 不拦截，由 SDK 展示本地通知。
         */

        /**
         * /~english Generally whether to intercept this local notification is used to customize the
         * display of local notifications.
         *
         * @param message Message corresponding to local notification
         * @return Whether to intercept. True indicates to intercept local notifications. SDK does
         *     not pop up notifications and it shall be handled by users themselves. False indicates
         *     not to intercept, and local notifications are displayed by SDK.
         */
        boolean isNotificationIntercepted(Message message);

        /**
         * /~chinese 设置本地通知 PendingIntent 时的回调。 应用层可通过此方法更改 PendingIntent 里的设置，以便自定义本地通知的点击行为。
         * 点击本地通知时，SDK 默认跳转到对应会话页面。
         *
         * @param pendingIntent SDK 默认 PendingIntent
         * @param intent pendingIntent 里携带的 intent。 可通过 intent 获取以下信息:
         *     intent.getStringExtra(RouteUtils.CONVERSATION_TYPE);
         *     intent.getStringExtra(RouteUtils.TARGET_ID);
         *     intent.getIntExtra(RouteUtils.MESSAGE_ID, -1);
         * @return 本地通知里需配置的 PendingIntent.
         */

        /**
         * /~english Callback for setting local notification PendingIntent. In this way, the
         * application layer can change the settings in PendingIntent to customize the click
         * behavior of local notifications. When you click local notification, SDK jumps to the
         * corresponding conversation page by default.
         *
         * @param pendingIntent SDK default PendingIntent
         * @param intent Intent carried in pendingIntent The following information is available
         *     through intent: intent.GettringExtra(RouteUtils.CONVERSATION_TYPE);
         *     intent.GettringExtra(RouteUtils.TARGET_ID);
         *     intent.getParcelableExtra(RouteUtils.MESSAGE);
         * @return PendingIntent to be configured in local notification
         */
        PendingIntent onPendingIntent(PendingIntent pendingIntent, Intent intent);

        /**
         * /~chinese 是否为高优先级消息。高优先级消息不受全局静默时间和会话免打扰控制，比如 @ 消息。
         *
         * @param message 接收到的消息
         * @return 是否为高优先级消息
         */

        /**
         * /~english Whether it is a high priority message. High-priority messages are not
         * controlled by global silence time and conversation do not Disturb, e.g. @ messages.
         *
         * @param message Messages received
         * @return Whether it is a high priority message
         */
        boolean isHighPriorityMessage(Message message);

        /**
         * /~chinese 注册默认 channel 之前的回调。可以通过此方法拦截并修改默认 channel 里的配置，将修改后的 channel 返回。
         *
         * @param defaultChannel 默认通知频道
         * @return 修改后的通知频道。
         */

        /**
         * /~english Callback before the default channel is registered You can use this method to
         * intercept and modify the configuration in the default channel, and return the modified
         * channel.
         *
         * @param defaultChannel Default notification channel
         * @return The modified notification channel
         */
        @TargetApi(Build.VERSION_CODES.O)
        NotificationChannel onRegisterChannel(NotificationChannel defaultChannel);
    }
}
