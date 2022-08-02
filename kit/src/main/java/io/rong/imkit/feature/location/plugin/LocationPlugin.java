package io.rong.imkit.feature.location.plugin;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.feature.location.LocationManager;
import io.rong.imlib.model.Message;
import io.rong.imlib.location.message.LocationMessage;

public class LocationPlugin implements IPluginModule {
    public LocationPlugin() {

    }

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ext_plugin_location_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_location_title);
    }

    @Override
    public void onClick(Fragment currentFragment, final RongExtension extension, int index) {
        if (LocationManager.getInstance().getLocationProvider() != null) {
            if(currentFragment.getActivity() != null) {
                LocationManager.getInstance().getLocationProvider().onStartLocation(currentFragment.getActivity().getApplicationContext(), new LocationManager.LocationProvider.LocationCallback() {
                    @Override
                    public void onSuccess(LocationMessage locationMessage) {
                        Message message = Message.obtain(extension.getTargetId(), extension.getConversationType(), locationMessage);
                        IMCenter.getInstance().sendMessage(message, null, null, null);
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                });
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }
}
