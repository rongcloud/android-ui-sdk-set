package io.rong.imkit.conversation.extension;

import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;
import java.util.List;
import java.util.Map;

public interface IExtensionConfig {
    /**
     * /~chinese 返回输入框 “+” 号区域配置的插件列表。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 Id
     * @return 插件列表。
     */

    /**
     * /~english Return a list of plug-ins configured in the "+" area of the input box.
     *
     * @param conversationType Conversation type
     * @param targetId Conversation Id
     * @return List of plug-ins.
     */
    List<IPluginModule> getPluginModules(
            Conversation.ConversationType conversationType, String targetId);

    /**
     * /~chinese 返回输入框表情区域配置字的表情 tab 列表。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 Id
     * @return 表情 tab 列表。
     */

    /**
     * /~english Returns the emoji tab list of configured words in the emoji area of the input box.
     *
     * @param conversationType Conversation type
     * @param targetId Conversation Id
     * @return Emoji tab list.
     */
    Map<String, List<IEmoticonTab>> getEmoticonTabs(
            Conversation.ConversationType conversationType, String targetId);
}
