package io.rong.imkit.conversation.extension;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.rong.imkit.conversation.extension.component.emoticon.EmojiTab;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.FilePlugin;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.ImagePlugin;
import io.rong.imlib.model.Conversation;

public class DefaultExtensionConfig implements IExtensionConfig {
    private final String DEFAULT_TAG = "DefaultExtensionModule";
    private final String CALL_MODULE = "io.rong.callkit.RongCallModule";

    /**
     * 默认 plugin 列表。
     * 默认 plugin 为：照片、位置（开关打开时）、阅后即焚（开关打开时）、
     * 语音通话（集成了音视频时）、视频通话（集成了音视频时）、文件
     *
     * @param conversationType 会话类型。
     * @param targetId         会话 Id
     * @return
     */
    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType, String targetId) {
        List<IPluginModule> pluginModules = new ArrayList<>();
        pluginModules.add(new ImagePlugin());
        List<IExtensionModule> extensionModules = RongExtensionManager.getInstance().getExtensionModules();
        for (IExtensionModule module : extensionModules) {
            if ((conversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                    || conversationType.equals(Conversation.ConversationType.CHATROOM))
                    && module.getClass().getCanonicalName() != null && module.getClass().getCanonicalName().equals(CALL_MODULE)) {
                continue;
            }
            if (module.getPluginModules(conversationType) != null && module.getPluginModules(conversationType).size() > 0) {
                pluginModules.addAll(module.getPluginModules(conversationType));
            }
        }
        if (!Conversation.ConversationType.CUSTOMER_SERVICE.equals(conversationType)) {
            pluginModules.add(new FilePlugin());
        }
        return pluginModules;
    }

    /**
     * 默认 Emoticon tab 数据。KV 格式， key 为各个 ExtensionModule 的类名， value 为各 ExtensionModule 返回的 IEmoticon 列表。
     *
     * @param conversationType 会话类型。
     * @param targetId         会话 Id
     * @return 默认 Emoticon tab 数据
     */
    @Override
    public Map<String, List<IEmoticonTab>> getEmoticonTabs(Conversation.ConversationType conversationType, String targetId) {
        Map<String, List<IEmoticonTab>> emoticonTabs = new LinkedHashMap<>();
        List<IEmoticonTab> list = new ArrayList<>();
        list.add(new EmojiTab());
        emoticonTabs.put(DEFAULT_TAG, list);
        List<IExtensionModule> extensionModules = RongExtensionManager.getInstance().getExtensionModules();
        for (IExtensionModule module : extensionModules) {
            if (module.getEmoticonTabs() != null && module.getEmoticonTabs().size() > 0) {
                emoticonTabs.put(module.getClass().getSimpleName(), module.getEmoticonTabs());
            }
        }
        return emoticonTabs;
    }
}
