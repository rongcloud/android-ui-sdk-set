package io.rong.contactcard;

import android.content.Context;
import androidx.fragment.app.Fragment;
import io.rong.contactcard.message.ContactMessage;
import io.rong.contactcard.message.ContactMessageItemProvider;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import java.util.ArrayList;
import java.util.List;

/** Created by Beyond on 2017/4/14. */
public class ContactCardExtensionModule implements IExtensionModule {

    private IContactCardClickListener iContactCardClickListener;

    public ContactCardExtensionModule(
            IContactCardSelectListProvider iContactCardSelectListProvider,
            IContactCardInfoProvider iContactCardInfoProvider,
            IContactCardClickListener iContactCardClickListener) {
        ContactCardContext.getInstance()
                .setContactCardSelectListProvider(iContactCardSelectListProvider);
        ContactCardContext.getInstance().setContactCardInfoProvider(iContactCardInfoProvider);
        this.iContactCardClickListener = iContactCardClickListener;
    }

    public ContactCardExtensionModule(
            IContactCardInfoProvider iContactCardInfoProvider,
            IContactCardClickListener iContactCardClickListener) {
        ContactCardContext.getInstance().setContactCardInfoProvider(iContactCardInfoProvider);
        this.iContactCardClickListener = iContactCardClickListener;
    }

    @Override
    public void onInit(Context context, String appKey) {
        RongIMClient.registerMessageType(ContactMessage.class); // 注册名片消息

        // Todo
        RongConfigCenter.conversationConfig()
                .addMessageProvider(new ContactMessageItemProvider(iContactCardClickListener));
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        // do nothing
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
        List<IPluginModule> pluginModules = new ArrayList<>();
        if (Conversation.ConversationType.PRIVATE.equals(conversationType)
                || Conversation.ConversationType.GROUP.equals(conversationType)) {
            pluginModules.add(new ContactCardPlugin());
        }
        return pluginModules;
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
