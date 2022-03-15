package io.rong.imkit.conversation.extension;

import android.content.Context;
import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import java.util.List;

public interface IExtensionModule {
    /**
     * /~chinese SDK 初始化。 用户可以在该方法中注册自定义消息、注册消息模板、初始化自己的模块。
     *
     * @param context 上下文
     * @param appKey 应用唯一 key。
     */

    /**
     * /~english SDK initialization. In this method, users can register custom messages, register
     * message templates, and initialize their own modules.
     *
     * @param context Context
     * @param appKey Application’s unique key.
     */
    void onInit(Context context, String appKey);

    /**
     * /~chinese 进入会话后，Extension 加载所有注册的 module。 module 可以持有并使用 Extension. 注意：如果 module 持有 Extension
     * 对象，需要在 onDetachedFromExtension 回调时释放，否则会有内存泄露。
     *
     * @param fragment Extension 对象所在的 fragment.
     * @param extension Extension 对象。
     */

    /**
     * /~english After entering the conversation, Extension loads all registered module. Module can
     * hold and use Extension. Note: If the module holds the Extension object, it shall be released
     * during the onDetachedFromExtension callback, otherwise there will be a memory leak.
     *
     * @param fragment Fragment where the Extension object resides.
     * @param extension Extension object.
     */
    void onAttachedToExtension(Fragment fragment, RongExtension extension);

    /**
     * /~chinese 退出会话，Extension 释放所有已加载的 module。 注意：如果 module 持有 Extension 对象，需要在该回调时释放，否则会有内存泄露。
     */

    /**
     * /~english After exiting the conversation, Extension releases all loaded module. Note: If
     * module holds an Extension object, it shall be released on this callback, otherwise there will
     * be a memory leak. OnReceivedMessage.
     */
    void onDetachedFromExtension();

    /**
     * /~chinese SDK 接收到消息后，通过此方法路由给对应的模块去处理。 用户可以根据自己注册的消息，有选择性的去处理接收到的消息。
     *
     * @param message 消息实体。
     */

    /**
     * /~english After the SDK receives the message, it is routed to the corresponding module for
     * processing by this method. Users can selectively process received messages according to their
     * registered messages.
     *
     * @param message Message entity.
     */
    void onReceivedMessage(Message message);

    /**
     * /~chinese 用户可以根据不同的会话，配置 “+” 号区域插件。 可以配置一个插件，也可以同时配置多个插件。extension 展示所有返回的插件列表。
     * 注意：如果用户没有配置插件，此方法可以不用实现。
     *
     * @param conversationType 会话类型。
     * @return 插件列表。
     */

    /**
     * /~english Users can configure the "+" zone plug-in according to different conversations. You
     * can configure one plug-in or multiple plug-ins at the same time. Extension shows a list of
     * all returned plug-ins. Note: If the user does not configure the plug-in, this method does not
     * have to be implemented.
     *
     * @param conversationType Conversation type
     * @return List of plug-ins.
     */
    List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType);

    /**
     * /~chinese 在会话中可以配置多个表情 tab，也可以配置单个表情 tab。 配置后，所有的会话中都会显示此 tab。 注意：如果用户没有配置表情，此方法可以不用实现。
     *
     * @return 表情 tab 列表。
     */

    /**
     * /~english You can configure multiple emoji tab, or a single emoji tab in a conversation. Once
     * configured, this tab appears in all conversations. Note: If the user does not configure
     * emoticons, this method does not have to be implemented.
     *
     * @return Emoji tab list.
     */
    List<IEmoticonTab> getEmoticonTabs();

    /** /~chinese SDK 断开连接。 */

    /** /~english The SDK is disconnected. */
    void onDisconnect();
}
