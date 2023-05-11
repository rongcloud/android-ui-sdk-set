package io.rong.imkit.feature.destruct;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;

public class DestructPlugin implements IPluginModule {
    private static final String TAG = "DestructPlugin";

    @Override
    public Drawable obtainDrawable(Context context) {
        return ContextCompat.getDrawable(context, R.drawable.rc_ext_plugin_fire_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_ext_plugin_destruct);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "onClick extension null");
            return;
        }
        if (currentFragment.getContext() == null) {
            RLog.e(TAG, "onClick getContext null");
            return;
        }
        FragmentActivity activity = currentFragment.getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            RLog.e(TAG, "onClick activity null");
            return;
        }
        if (RongExtensionCacheHelper.isDestructFirstUsing(currentFragment.getContext())) {
            if (currentFragment.isAdded()) {
                new DestructHintDialog().show(currentFragment.getParentFragmentManager());
            }
        }
        DestructManager.getInstance().safeAttacheToExtension(currentFragment, extension);
        DestructManager.getInstance().activeDestructMode(currentFragment.getContext());
        RongExtensionCacheHelper.saveVoiceInputMode(
                currentFragment.getContext(),
                extension.getConversationType(),
                extension.getTargetId(),
                false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // do nothing
    }
}
