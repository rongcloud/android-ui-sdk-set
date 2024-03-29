package io.rong.sticker;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.fragment.app.Fragment;
import io.rong.common.rlog.RLog;
import io.rong.imkit.RongIM;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonSettingClickListener;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.sticker.businesslogic.GifImageLoader;
import io.rong.sticker.businesslogic.StickerPackageApiTask;
import io.rong.sticker.businesslogic.StickerPackageDbTask;
import io.rong.sticker.businesslogic.StickerPackageStorageTask;
import io.rong.sticker.businesslogic.StickerPackagesConfigTask;
import io.rong.sticker.businesslogic.StickerPackagesUiHandler;
import io.rong.sticker.businesslogic.StickerSendMessageTask;
import io.rong.sticker.emoticontab.RecommendTab;
import io.rong.sticker.emoticontab.StickersTab;
import io.rong.sticker.message.StickerMessage;
import io.rong.sticker.message.StickerMessageItemProvider;
import io.rong.sticker.model.StickerPackage;
import io.rong.sticker.mysticker.MyStickerActivity;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/** Created by luoyanlong on 2018/08/03. 注册 表情消息 和 表情消息提供者 */
public class StickerExtensionModule implements IExtensionModule {

    private static final String TAG = StickerExtensionModule.class.getCanonicalName();
    private String sAppKey;
    public static WeakReference<RongExtension> sRongExtensionWeakReference;
    private RecommendTab recommendTab;
    private StickerPackagesConfigTask stickerPackagesConfigTask;
    private boolean isEnabled = true;

    @Override
    public void onInit(Context context, String appKey) {
        sAppKey = appKey;
        try {
            RongIMClient.registerMessageType(StickerMessage.class);
        } catch (Exception e) {
            RLog.e(TAG, "Failed to register message.");
        }
        RongConfigCenter.conversationConfig().addMessageProvider(new StickerMessageItemProvider());
        StickerPackageApiTask.init(appKey);
        stickerPackagesConfigTask = new StickerPackagesConfigTask();
        stickerPackagesConfigTask.getConfig();
    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        if (extension
                .getConversationType()
                .getName()
                .equals(Conversation.ConversationType.CUSTOMER_SERVICE.getName())) {
            isEnabled = false; // 客服会话不显示表情
        } else {
            isEnabled = true;
            sRongExtensionWeakReference = new WeakReference<>(extension);
            Context context = extension.getContext();
            StickerPackageDbTask.init(context, sAppKey, RongIM.getInstance().getCurrentUserId());
            StickerPackagesUiHandler.init(context);
            StickerPackageStorageTask.init(
                    context, sAppKey, RongIM.getInstance().getCurrentUserId());
            stickerPackagesConfigTask.saveConfig(context);
            StickerSendMessageTask.config(extension.getTargetId(), extension.getConversationType());
        }
    }

    @Override
    public void onDetachedFromExtension() {
        if (isEnabled) {
            sRongExtensionWeakReference = null;
            recommendTab = null;
            GifImageLoader.getInstance().clear();
            StickerPackagesUiHandler.destroy();
        }
    }

    @Override
    public void onReceivedMessage(Message message) {
        // default implementation ignored
    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        return null;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        if (sRongExtensionWeakReference == null || sRongExtensionWeakReference.get() == null) {
            return null;
        }
        RongExtension rongExtension = sRongExtensionWeakReference.get();
        if (!isEnabled) {
            return null;
        }
        List<IEmoticonTab> list = new ArrayList<>();
        List<StickerPackage> packages = StickerPackageDbTask.getInstance().getStickerPackages();
        List<StickerPackage> downloadPackages = new ArrayList<>();
        List<StickerPackage> recommendPackages = new ArrayList<>();
        for (StickerPackage stickerPackage : packages) {
            if (stickerPackage.isDownload()) {
                downloadPackages.add(stickerPackage);
            } else {
                if (stickerPackage.isPreload() == 0) {
                    recommendPackages.add(stickerPackage);
                }
            }
        }

        for (StickerPackage downloadPackage : downloadPackages) {
            StickersTab tab = new StickersTab(downloadPackage);
            list.add(tab);
        }

        if (!recommendPackages.isEmpty()) {
            recommendTab = new RecommendTab(recommendPackages);
            list.add(recommendTab);
        }
        rongExtension.getEmoticonBoard().setSettingEnable(true);
        rongExtension
                .getEmoticonBoard()
                .setOnEmoticonSettingClickListener(new SettingClickListener());
        return list;
    }

    @Override
    public void onDisconnect() {
        StickerPackageDbTask.destroy();
    }

    private static class SettingClickListener implements IEmoticonSettingClickListener {
        @Override
        public void onSettingClick(View view) {
            if (StickerExtensionModule.sRongExtensionWeakReference != null) {
                RongExtension rongExtension = sRongExtensionWeakReference.get();
                if (rongExtension != null && rongExtension.getContext() != null) {
                    Intent intent = new Intent(rongExtension.getContext(), MyStickerActivity.class);
                    rongExtension.getContext().startActivity(intent);
                }
            }
        }
    }
}
