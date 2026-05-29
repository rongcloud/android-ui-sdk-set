package io.rong.imkit.handler;

import android.text.InputFilter;
import android.widget.EditText;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.AppSettings;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageReadReceiptVersion;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 应用设置处理器 - 全局唯一实例
 *
 * <p>负责管理应用设置的获取、缓存和更新，包括：
 *
 * <ul>
 *   <li>监听连接状态变化并自动更新设置
 *   <li>缓存应用设置信息
 *   <li>提供应用设置的访问接口
 * </ul>
 *
 * @since 5.28.0
 */
public class AppSettingsHandler {

    private static final String TAG = "AppSettingsHandler";
    private static final int DEFAULT_MESSAGE_INPUT_LIMIT = 5000;
    private static final int CONFIGURED_MESSAGE_INPUT_LIMIT = 1500;

    /** 静态内部类持有单例实例 利用JVM的类加载机制保证线程安全和懒加载 */
    private static class Holder {
        private static final AppSettingsHandler INSTANCE = new AppSettingsHandler();
    }

    private AppSettings appSettings = new AppSettings();
    private final List<WeakReference<EditText>> messageInputEditTexts = new ArrayList<>();

    private final IRongCoreListener.ConnectionStatusListener connectionStatusListener =
            new IRongCoreListener.ConnectionStatusListener() {
                @Override
                public void onChanged(ConnectionStatus status) {
                    if (status.equals(ConnectionStatus.CONNECTED)) {
                        getInnerAppSettings();
                    }
                }
            };

    /** 是否初始化完成，从Lib获取成功过AppSettings */
    private volatile boolean hasInit = false;

    // 私有构造函数，防止外部直接实例化
    private AppSettingsHandler() {
        RongCoreClient.addConnectionStatusListener(connectionStatusListener);
        getInnerAppSettings();
    }

    /**
     * 获取应用设置处理器的全局唯一实例 使用静态内部类方式实现懒加载和线程安全
     *
     * @return 应用设置处理器实例
     */
    public static AppSettingsHandler getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 获取应用设置
     *
     * @return 当前缓存的应用设置
     */
    public AppSettings getAppSettings() {
        return appSettings;
    }

    public int getMessageInputLimit() {
        return appSettings.getMessageSizeLimit() > 0
                ? CONFIGURED_MESSAGE_INPUT_LIMIT
                : DEFAULT_MESSAGE_INPUT_LIMIT;
    }

    public void applyMessageInputLimit(EditText editText) {
        if (editText == null) {
            return;
        }
        trackMessageInput(editText);
        applyMessageInputLimitInternal(editText);
    }

    private void applyMessageInputLimitInternal(EditText editText) {
        InputFilter[] filters = editText.getFilters();
        List<InputFilter> newFilters = new ArrayList<>();
        if (filters != null) {
            for (InputFilter filter : filters) {
                if (!(filter instanceof InputFilter.LengthFilter)) {
                    newFilters.add(filter);
                }
            }
        }
        newFilters.add(new InputFilter.LengthFilter(getMessageInputLimit()));
        editText.setFilters(newFilters.toArray(new InputFilter[0]));
    }

    private void trackMessageInput(EditText editText) {
        synchronized (messageInputEditTexts) {
            Iterator<WeakReference<EditText>> iterator = messageInputEditTexts.iterator();
            while (iterator.hasNext()) {
                EditText trackedEditText = iterator.next().get();
                if (trackedEditText == null) {
                    iterator.remove();
                } else if (trackedEditText == editText) {
                    return;
                }
            }
            messageInputEditTexts.add(new WeakReference<>(editText));
        }
    }

    private void refreshMessageInputLimits() {
        ExecutorHelper.getInstance()
                .mainThread()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                synchronized (messageInputEditTexts) {
                                    Iterator<WeakReference<EditText>> iterator =
                                            messageInputEditTexts.iterator();
                                    while (iterator.hasNext()) {
                                        EditText editText = iterator.next().get();
                                        if (editText == null) {
                                            iterator.remove();
                                        } else {
                                            applyMessageInputLimitInternal(editText);
                                        }
                                    }
                                }
                            }
                        });
    }

    /** 是否初始化完成，从Lib获取成功过AppSettings */
    public boolean hasInit() {
        return hasInit;
    }

    /**
     * 获取应用设置下的已读回执版本
     *
     * @return 当前缓存的已读回执版本
     */
    public MessageReadReceiptVersion getReadReceiptVersion() {
        return appSettings.getReadReceiptVersion();
    }

    /**
     * 是否支持已读V5
     *
     * @param type 会话类型
     */
    public boolean isReadReceiptV5Enabled(Conversation.ConversationType type) {
        // 非单群聊会话类型，不支持
        if (Conversation.ConversationType.GROUP != type
                && Conversation.ConversationType.PRIVATE != type) {
            return false;
        }
        // 未开启Kit回执开关，不支持
        boolean enableReadReceipt = RongConfigCenter.conversationConfig().isEnableReadReceipt();
        if (!enableReadReceipt) {
            return false;
        }
        // 不支持的已读回执类型
        Set<Conversation.ConversationType> types =
                RongConfigCenter.conversationConfig().getSupportReadReceiptConversationType();
        if (types.isEmpty() || !types.contains(type)) {
            return false;
        }
        // 导航配置非V5版本号，不支持
        return MessageReadReceiptVersion.V5 == appSettings.getReadReceiptVersion();
    }

    /**
     * 是否开启在线状态功能，影响UI是否展示。
     *
     * <p>Kit 配置打开，且“好友在线状态订阅”与“非好友在线状态订阅”有一项打开，则代表开启在线状态功能
     */
    public boolean isOnlineStatusEnable() {
        if (!RongConfigCenter.featureConfig().isUserOnlineStatusEnable()) {
            return false;
        }
        return appSettings.isFriendOnlineStatusSubscribeEnable()
                || appSettings.isOnlineStatusSubscribeEnable();
    }

    /** 是否开启好友在线状态订阅功能 */
    public boolean isFriendOnlineStatusSubscribeEnable() {
        // Kit配置和Lib配置同时开启。
        return RongConfigCenter.featureConfig().isUserOnlineStatusEnable()
                && appSettings.isFriendOnlineStatusSubscribeEnable();
    }

    /** 是否开启在线状态订阅功能 */
    public boolean isOnlineStatusSubscribeEnable() {
        // Kit配置和Lib配置同时开启。
        return RongConfigCenter.featureConfig().isUserOnlineStatusEnable()
                && appSettings.isOnlineStatusSubscribeEnable();
    }

    /** 是否开启用户信息托管 */
    public boolean isUserProfileEnabled() {
        return appSettings.isUserProfileEnabled();
    }

    /** 内部方法：异步获取并更新应用设置 */
    private void getInnerAppSettings() {
        ExecutorHelper.getInstance()
                .compressExecutor()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                AppSettings settings =
                                        RongCoreClient.getInstance().getAppSettings();
                                if (settings != null) {
                                    appSettings = settings;
                                }
                                hasInit = true;
                                refreshMessageInputLimits();
                            }
                        });
    }

    /** 停止应用设置处理器并清理资源 */
    private void stop() {
        RongCoreClient.removeConnectionStatusListener(connectionStatusListener);
    }
}
