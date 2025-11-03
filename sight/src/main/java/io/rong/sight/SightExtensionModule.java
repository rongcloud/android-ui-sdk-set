package io.rong.sight;

import android.content.Context;
import androidx.fragment.app.Fragment;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.messgelist.provider.SightMessageItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.SightMessage;
import java.util.ArrayList;
import java.util.List;

public class SightExtensionModule implements IExtensionModule {

    @Override
    public void onInit(Context context, String appKey) {
        IMKitThemeManager.addTheme(
                IMKitThemeManager.TRADITION_THEME,
                R.style.RCTraditionLightTheme_SightKit,
                R.style.RCTraditionLightTheme_SightKit);
        IMKitThemeManager.addTheme(
                IMKitThemeManager.LIVELY_THEME,
                R.style.RCLivelyLightTheme_SightKit,
                R.style.RCLivelyDarkTheme_SightKit);
        RongIMClient.getInstance().registerMessageType(SightMessage.class);
        RongConfigCenter.conversationConfig().addMessageProvider(new SightMessageItemProvider());
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        // default implementation ignoredq
    }

    @Override
    public void onDetachedFromExtension() {
        // default implementation ignored
    }

    @Override
    public void onReceivedMessage(Message message) {
        // default implementation ignored
    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        List<IPluginModule> pluginModules = new ArrayList<>();
        SightPlugin sightPlugin = new SightPlugin();
        pluginModules.add(sightPlugin);
        return pluginModules;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {
        // default implementation ignored
    }
}
