package io.rong.imkit.conversation.extension;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.rong.common.RLog;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.feature.destruct.DestructExtensionModule;
import io.rong.imkit.feature.forward.ForwardExtensionModule;
import io.rong.imkit.feature.location.LocationExtensionModule;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.feature.publicservice.PublicServiceManager;
import io.rong.imkit.feature.quickreply.QuickReplyExtensionModule;
import io.rong.imkit.feature.reference.ReferenceManager;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public class RongExtensionManager {

    private final static String TAG = "RongExtensionManager";
    private final static String DEFAULT_RC_STICKER = "io.rong.sticker.StickerExtensionModule";
    private final static String DEFAULT_CALL_MODULE = "io.rong.callkit.RongCallModule";
    private static String mAppKey;
    private static Context mApplicationContext;
    private static List<IExtensionModule> mExtModules = new CopyOnWriteArrayList<>();
    private static List<IExtensionEventWatcher> mExtensionEventWatcher = new CopyOnWriteArrayList<>();
    private static IExtensionConfig mExtensionConfig;

    private RongExtensionManager() {
        addExtensionEventWatcher(RongMentionManager.getInstance());
    }

    private static class SingletonHolder {
        static RongExtensionManager sInstance = new RongExtensionManager();
    }

    public static RongExtensionManager getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 初始化，SDK 在初始化时已调用此方法，用户不需要再调用。
     *
     * @param context 应用上下文.
     * @param appKey  应用 key.
     */
    public static void init(Context context, String appKey) {
        RLog.d(TAG, "init");
        AndroidEmoji.init(context);
        RongUtils.init(context);
        mAppKey = appKey;
        mApplicationContext = context;
        mExtensionConfig = new DefaultExtensionConfig();
        mExtModules.clear();
        mExtModules.add(new LocationExtensionModule());
        mExtModules.add(new ForwardExtensionModule());

        if (RongConfigCenter.featureConfig().isReferenceEnable()) {
            mExtModules.add(ReferenceManager.getInstance());
        }
        if (RongConfigCenter.featureConfig().isQuickReplyEnable()) {
            mExtModules.add(new QuickReplyExtensionModule());
        }
        if (RongConfigCenter.featureConfig().isDestructEnable()) {
            mExtModules.add(new DestructExtensionModule());
        }
        Conversation.ConversationType[] types = RongConfigCenter.conversationListConfig().getDataProcessor().supportedTypes();
        List<Conversation.ConversationType> typeList = Arrays.asList(types);
        if (typeList.contains(Conversation.ConversationType.PUBLIC_SERVICE)
                || typeList.contains(Conversation.ConversationType.APP_PUBLIC_SERVICE)) {
            mExtModules.add(PublicServiceManager.getInstance().getExtensionModule());
        }
        checkCallModule();
        checkRCBQ();

        for (IExtensionModule module : mExtModules) {
            module.onInit(mApplicationContext, mAppKey);
        }
    }

    /**
     * 设置输入栏相关配置。
     *
     * @param extensionConfig
     */
    public void setExtensionConfig(IExtensionConfig extensionConfig) {
        mExtensionConfig = extensionConfig;
    }

    public IExtensionConfig getExtensionConfig() {
        return mExtensionConfig;
    }

    /**
     * 注册自定义的 {@link IExtensionModule},注册后，可以通过 {@link #getExtensionModules()} 获取已注册的 module
     * <pre>
     * 注意：
     * 1. 请在 SDK 初始化后 {@link io.rong.imkit.RongIM#init(Application, String, boolean)}，调用此方法注册自定义 {@link IExtensionModule}
     * 2. 一定要在进入会话界面之前调此方法
     * </pre>
     *
     * @param extensionModule 自定义模块。
     * @throws IllegalArgumentException IExtensionModule 参数非法时，抛出异常
     */
    public void registerExtensionModule(IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "registerExtensionModule " + extensionModule.getClass().getSimpleName());
        //当集成了红包，表情美美或融云表情的时候，需要把EMOJI置于list的最前面；
        if (mExtModules.size() > 0 && (mExtModules.get(0).getClass().getCanonicalName().equals(DEFAULT_RC_STICKER))) {
            mExtModules.add(0, extensionModule);
        } else {
            mExtModules.add(extensionModule);
        }
        extensionModule.onInit(mApplicationContext, mAppKey);
    }

    public void registerExtensionModule(int index, IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "registerExtensionModule " + extensionModule.getClass().getSimpleName());
        mExtModules.add(index, extensionModule);
        extensionModule.onInit(mApplicationContext, mAppKey);
    }

    /**
     * 添加自定义的 {@link IExtensionModule},添加后，可以通过 {@link #getExtensionModules()} 获取已注册的 module
     * <pre>
     * 注意：
     * 1. 此方法只是把自定义IExtensionModule加入到IExtensionModule列表,不会调用{@link IExtensionModule#onInit(Context, String)}}
     * 2. 注册请使用{@link #registerExtensionModule(IExtensionModule)}
     * 3. 此方法适用于IExtensionModule的排序
     * </pre>
     *
     * @param extensionModule 自定义模块。
     * @throws IllegalArgumentException IExtensionModule 参数非法时，抛出异常
     */
    public void addExtensionModule(IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "addExtensionModule " + extensionModule.getClass().getSimpleName());
        mExtModules.add(extensionModule);
    }

    /**
     * 注销 {@link IExtensionModule} 模块
     * <pre>
     * 注意：
     * 1. 请在 SDK 初始化后 {@link io.rong.imkit.IMCenter#init(Application, String, boolean)} )}，调用此方法反注册注册 {@link IExtensionModule}
     * 2. 一定要在进入会话界面之前调次方法
     * </pre>
     *
     * @param extensionModule 已注册的 IExtensionModule 模块
     * @throws IllegalArgumentException IExtensionModule 参数非法时，抛出异常
     */
    public void unregisterExtensionModule(IExtensionModule extensionModule) {
        if (mExtModules == null) {
            RLog.e(TAG, "Not init in the main process.");
            return;
        }
        if (extensionModule == null || !mExtModules.contains(extensionModule)) {
            RLog.e(TAG, "Illegal extensionModule.");
            return;
        }
        RLog.i(TAG, "unregisterExtensionModule " + extensionModule.getClass().getSimpleName());
        Iterator<IExtensionModule> iterator = mExtModules.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().equals(extensionModule)) {
                iterator.remove();
            }
        }
    }

    /**
     * 获取已注册的模块。
     *
     * @return 已注册的模块列表
     */
    public List<IExtensionModule> getExtensionModules() {
        return mExtModules;
    }

    public void addExtensionEventWatcher(IExtensionEventWatcher watcher) {
        if (!mExtensionEventWatcher.contains(watcher)) {
            mExtensionEventWatcher.add(watcher);
        }
    }

    public void removeExtensionEventWatcher(IExtensionEventWatcher watcher) {
        if (mExtensionEventWatcher.contains(watcher)) {
            mExtensionEventWatcher.remove(watcher);
        }
    }

    public List<IExtensionEventWatcher> getExtensionEventWatcher() {
        return mExtensionEventWatcher;
    }

    /**
     * SDK 断开连接时，已调用此方法，用户不需要再次调用。
     */
    public void disconnect() {
        if (mExtModules == null) {
            return;
        }
        for (IExtensionModule extensionModule : mExtModules) {
            extensionModule.onDisconnect();
        }
    }

    /**
     * SDK 接收到消息时，已调用此方法，用户不需要再次调用。
     * RongExtModuleManage 会将消息路由到各个 {@link IExtensionModule} 模块。
     *
     * @param message 接收到的消息实体。
     */
    void onReceivedMessage(Message message) {
        for (IExtensionModule extensionModule : mExtModules) {
            extensionModule.onReceivedMessage(message);
        }
    }

    /**
     * 检查融云表情是否存在
     */
    private static void checkRCBQ() {
        try {
            Class<?> cls = Class.forName(DEFAULT_RC_STICKER);
            Constructor<?> constructor = cls.getConstructor();
            IExtensionModule rcbq = (IExtensionModule) constructor.newInstance();
            RLog.i(TAG, "add module " + rcbq.getClass().getSimpleName());
            mExtModules.add(rcbq);
        } catch (Exception e) {
            RLog.i(TAG, "Can't find " + DEFAULT_RC_STICKER);
        }
    }

    private static void checkCallModule() {
        try {
            Class<?> cls = Class.forName(DEFAULT_CALL_MODULE);
            Constructor<?> constructor = cls.getConstructor();
            IExtensionModule callModule = (IExtensionModule) constructor.newInstance();
            RLog.i(TAG, "add module " + callModule.getClass().getSimpleName());
            mExtModules.add(callModule);
        } catch (Exception e) {
            RLog.i(TAG, "Can't find" + DEFAULT_CALL_MODULE);
        }
    }
}
