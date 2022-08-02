package io.rong.imkit.conversation.extension;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.util.List;

import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public interface IExtensionModule {
    /**
     * SDK 初始化。
     * 用户可以在该方法中注册自定义消息、注册消息模板、初始化自己的模块。
     *
     * @param context 上下文
     * @param appKey 应用唯一 key。
     */
    void onInit(Context context, String appKey);

    /**
     * 进入会话后，Extension 加载所有注册的 module。
     * module 可以持有并使用 Extension.
     * 注意：如果 module 持有 Extension 对象，需要在 onDetachedFromExtension 回调时释放，否则会有内存泄露。
     *
     * @param fragment Extension 对象所在的 fragment.
     * @param extension Extension 对象。
     */
    void onAttachedToExtension(Fragment fragment, RongExtension extension);

    /**
     * 退出会话，Extension 释放所有已加载的 module。
     * 注意：如果 module 持有 Extension 对象，需要在该回调时释放，否则会有内存泄露。
     */
    void onDetachedFromExtension();

    /**
     * SDK 接收到消息后，通过此方法路由给对应的模块去处理。
     * 用户可以根据自己注册的消息，有选择性的去处理接收到的消息。
     *
     * @param message 消息实体。
     */
    void onReceivedMessage(Message message);

    /**
     * 用户可以根据不同的会话，配置 “+” 号区域插件。
     * 可以配置一个插件，也可以同时配置多个插件。extension 展示所有返回的插件列表。
     * 注意：如果用户没有配置插件，此方法可以不用实现。
     *
     * @param conversationType 会话类型。
     * @return 插件列表。
     */
    List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType);

    /**
     * 在会话中可以配置多个表情 tab，也可以配置单个表情 tab。
     * 配置后，所有的会话中都会显示此 tab。
     * 注意：如果用户没有配置表情，此方法可以不用实现。
     *
     * @return 表情 tab 列表。
     */
    List<IEmoticonTab> getEmoticonTabs();

    /**
     * SDK 断开连接。
     */
    void onDisconnect();
}
