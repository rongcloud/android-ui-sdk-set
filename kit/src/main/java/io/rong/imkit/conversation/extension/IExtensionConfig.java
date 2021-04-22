package io.rong.imkit.conversation.extension;

import java.util.List;
import java.util.Map;

import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;

public interface IExtensionConfig {
    /**
     * 返回输入框 “+” 号区域配置的插件列表。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 Id
     * @return 插件列表。
     */
    List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType, String targetId);

    /**
     * 返回输入框表情区域配置字的表情 tab 列表。
     *
     * @param conversationType 会话类型。
     * @param targetId 会话 Id
     * @return 表情 tab 列表。
     */
    Map<String, List<IEmoticonTab>> getEmoticonTabs(Conversation.ConversationType conversationType, String targetId);
}
