package io.rong.imkit.feature.forward;

import android.content.Context;
import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import java.lang.ref.WeakReference;
import java.util.List;

public class ForwardExtensionModule implements IExtensionModule {
    static WeakReference<RongExtension> sRongExtension;
    static WeakReference<Fragment> sFragment;

    @Override
    public void onInit(Context context, String appKey) {
        // default implementation ignored
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        sFragment = new WeakReference<>(fragment);
        sRongExtension = new WeakReference<>(extension);
    }

    @Override
    public void onDetachedFromExtension() {
        // do nothing
    }

    @Override
    public void onReceivedMessage(Message message) {
        // do nothing
    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        return null;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {
        // do nothing
    }
}
