package io.rong.sight;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.IMCenter;
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

public class SightExtensionModule implements IExtensionModule {

    @Override
    public void onInit(Context context, String appKey) {
        RongIMClient.getInstance().registerMessageType(SightMessage.class);
        RongConfigCenter.conversationConfig().addMessageProvider(new SightMessageItemProvider());
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {

    }

    @Override
    public void onDetachedFromExtension() {

    }

    @Override
    public void onReceivedMessage(Message message) {

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

    }
}
