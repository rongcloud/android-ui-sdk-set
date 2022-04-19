package io.rong.imkit.feature.location;

import android.content.Context;
import android.content.res.Resources;
import androidx.fragment.app.Fragment;
import io.rong.common.RLog;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.feature.location.plugin.CombineLocationPlugin;
import io.rong.imkit.feature.location.plugin.DefaultLocationPlugin;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import java.util.ArrayList;
import java.util.List;

public class LocationExtensionModule implements IExtensionModule {

    private static final String TAG = "LocationExtensionModule";
    private String[] types = null;

    public LocationExtensionModule() {}

    @Override
    public void onInit(Context context, String appKey) {
        Resources resources = context.getResources();
        try {
            types =
                    resources.getStringArray(
                            resources.getIdentifier(
                                    "rc_realtime_support_conversation_types",
                                    "array",
                                    context.getPackageName()));
        } catch (Resources.NotFoundException e) {
            RLog.i(TAG, "not config rc_realtime_support_conversation_types in rc_config.xml");
        }
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
    public void onReceivedMessage(Message message) {}

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        List<IPluginModule> pluginModuleList = new ArrayList<>();
        try {
            String clsName = "com.amap.api.netlocation.AMapNetworkLocationClient";
            Class<?> locationCls = Class.forName(clsName);

            IPluginModule combineLocation = new CombineLocationPlugin();
            IPluginModule locationPlugin = new DefaultLocationPlugin();
            boolean typesDefined = false;
            if (types != null && types.length > 0) {
                for (String type : types) {
                    if (conversationType.getName().equals(type)) {
                        typesDefined = true;
                        break;
                    }
                }
            }

            if (typesDefined) {
                pluginModuleList.add(combineLocation);
            } else {
                if (types == null
                        && conversationType.equals(
                                Conversation.ConversationType.PRIVATE)) { // 配置文件中没有类型定义且会话类型为私聊
                    pluginModuleList.add(combineLocation);
                } else {
                    pluginModuleList.add(locationPlugin);
                }
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
    public void onDisconnect() {}
}
