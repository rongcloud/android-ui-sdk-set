package io.rong.imkit.handler;

import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.IRongCoreListener;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.AppSettings;

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

    /** 静态内部类持有单例实例 利用JVM的类加载机制保证线程安全和懒加载 */
    private static class Holder {
        private static final AppSettingsHandler INSTANCE = new AppSettingsHandler();
    }

    private AppSettings appSettings = new AppSettings();

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
        if (hasInit) {
            return appSettings;
        }
        appSettings = RongCoreClient.getInstance().getAppSettings();
        hasInit = true;
        return appSettings;
    }

    /** 是否初始化完成，从Lib获取成功过AppSettings */
    public boolean hasInit() {
        return hasInit;
    }

    /** 内部方法：异步获取并更新应用设置 */
    private void getInnerAppSettings() {
        ExecutorHelper.getInstance()
                .compressExecutor()
                .execute(
                        new Runnable() {
                            @Override
                            public void run() {
                                appSettings = RongCoreClient.getInstance().getAppSettings();
                                hasInit = true;
                            }
                        });
    }

    /** 停止应用设置处理器并清理资源 */
    private void stop() {
        RongCoreClient.removeConnectionStatusListener(connectionStatusListener);
    }
}
