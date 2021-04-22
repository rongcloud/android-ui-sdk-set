package io.rong.imkit.feature.location;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.feature.location.plugin.CombineLocationPlugin;
import io.rong.imkit.feature.location.plugin.DefaultLocationPlugin;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

public class LocationExtensionModule implements IExtensionModule {

    private static final String TAG = "LocationExtensionModule";

    public LocationExtensionModule() {
    }

    @Override
    public void onInit(Context context, String appKey) {
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        if (extension == null) {
            return;
        }
        LocationManager.getInstance().init(extension);
    }

    @Override
    public void onDetachedFromExtension() {
        LocationManager.getInstance().deinit();
    }

    @Override
    public void onReceivedMessage(Message message) {

    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        List<IPluginModule> pluginModuleList = new ArrayList<>();
        try {
            String clsName = "com.amap.api.netlocation.AMapNetworkLocationClient";
            Class<?> locationCls = Class.forName(clsName);
            IPluginModule combineLocation = new CombineLocationPlugin();
            IPluginModule locationPlugin = new DefaultLocationPlugin();
            if (conversationType.equals(Conversation.ConversationType.PRIVATE)) {
                pluginModuleList.add(combineLocation);
            } else {
                pluginModuleList.add(locationPlugin);
            }
        } catch (Exception e) {
            RLog.w(TAG, "No AMap jar ！！");
        }
        return pluginModuleList;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {

    }

}
