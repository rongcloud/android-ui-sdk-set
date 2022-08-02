package io.rong.imkit.feature.destruct;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public class DestructExtensionModule implements IExtensionModule {
    static WeakReference<RongExtension> sRongExtension;
    static WeakReference<Fragment> sFragment;

    @Override
    public void onInit(Context context, String appKey) {
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }
        sFragment = new WeakReference<>(fragment);
        sRongExtension = new WeakReference<>(extension);
        if (RongExtensionCacheHelper.isDestructMode(extension.getContext(), extension.getConversationType(), extension.getTargetId())) {
            DestructManager.getInstance().activeDestructMode(fragment.getContext());
        }
    }

    @Override
    public void onDetachedFromExtension() {
    }

    @Override
    public void onReceivedMessage(Message message) {

    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        ArrayList<IPluginModule> pluginModules = new ArrayList<>();
        if (RongConfigCenter.featureConfig().isDestructEnable() && conversationType.equals(Conversation.ConversationType.PRIVATE)) {
            pluginModules.add(new DestructPlugin());
        }
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
