package io.rong.imkit.feature.destruct;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;

public class DestructPlugin implements IPluginModule {

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
        if (currentFragment == null || currentFragment.getContext() == null) {
            return;
        }
        if (RongExtensionCacheHelper.isDestructFirstUsing(currentFragment.getContext())) {
            if(currentFragment.isAdded()) {
                new DestructHintDialog().show(currentFragment.getParentFragmentManager());
            }
        }
        DestructManager.getInstance().activeDestructMode(currentFragment.getContext());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

}
