package io.rong.imkit.notification;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;

import io.rong.imlib.model.Message;

public abstract class DefaultInterceptor implements NotificationConfig.Interceptor {
    /**
     * 是否拦截此本地通知，一般用于自定义本地通知的显示。
     *
     * @param message 本地通知对应的消息
     * @return 是否拦截。true 拦截本地通知，SDK 不弹出通知，需要用户自己处理。false 不拦截，由 SDK 展示本地通知。
     */
    @Override
    public boolean isNotificationIntercepted(Message message) {
        return false;
    }

    /**
     * 设置本地通知 PendingIntent 时的回调。
     * 应用层可通过此方法更改 PendingIntent 里的设置，以便自定义本地通知的点击行为。
     * 点击本地通知时，SDK 默认跳转到对应会话页面。
     *
     * @param pendingIntent SDK 默认 PendingIntent
     * @param intent        pendingIntent 里携带的 intent。
     *                      可通过 intent 获取以下信息:
     *                      intent.getStringExtra(RouteUtils.CONVERSATION_TYPE);
     *                      intent.getStringExtra(RouteUtils.TARGET_ID);
     *                      intent.getParcelableExtra(RouteUtils.MESSAGE);
     * @return 本地通知里需配置的 PendingIntent.
     */
    @Override
    public PendingIntent onPendingIntent(PendingIntent pendingIntent, Intent intent) {
        return pendingIntent;
    }

    /**
     * 是否为高优先级消息。高优先级消息不受全局静默时间和会话免打扰控制，比如 @ 消息。
     *
     * @param message 接收到的消息
     * @return 是否为高优先级消息
     */
    @Override
    public boolean isHighPriorityMessage(Message message) {
        return false;
    }

    /**
     * 注册默认 channel 之前的回调。可以通过此方法拦截并修改默认 channel 里的配置，将修改后的 channel 返回。
     *
     * @param defaultChannel 默认通知频道
     * @return 修改后的通知频道。
     */
    @Override
    public NotificationChannel onRegisterChannel(NotificationChannel defaultChannel) {
        return defaultChannel;
    }
}
