package io.rong.imkit.feature.location.plugin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.feature.location.AMapLocationActivity;
import io.rong.imkit.feature.location.AMapLocationActivity2D;
import io.rong.imkit.feature.location.LocationManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.location.message.LocationMessage;

public class DefaultLocationPlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private static final int RESULT_CODE = 1;

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ext_plugin_location_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_location_title);
    }

    @Override
    public void onClick(final Fragment currentFragment, RongExtension extension, final int index) {
        mConversationType = extension.getConversationType();
        mTargetId = extension.getTargetId();
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE};
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            startLocationActivity(currentFragment, extension);
        } else {
            extension.requestPermissionForPluginResult(permissions, IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN, this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE && resultCode == Activity.RESULT_OK) {
            double lat = data.getDoubleExtra("lat", 0);
            double lng = data.getDoubleExtra("lng", 0);
            String poi = data.getStringExtra("poi");
            String thumb = data.getStringExtra("thumb");
            LocationMessage locationMessage = LocationMessage.obtain(lat, lng, poi, Uri.parse(thumb));
            io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, locationMessage);
            IMCenter.getInstance().sendMessage(message, null, null, null);   //Todo check 是否需要调用 sendLocationMessage()
            if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:LBSMsg");
            }
        }
    }

    private void startLocationActivity(Fragment fragment, RongExtension extension) {
        Intent intent;
        if (LocationManager.getInstance().getMapMode().equals(LocationManager.MapMode.Map_2D)) {
            intent = new Intent(fragment.getActivity(), AMapLocationActivity2D.class);
        } else {
            intent = new Intent(fragment.getActivity(), AMapLocationActivity.class);
        }
        extension.startActivityForPluginResult(intent, RESULT_CODE, this);
    }

    @Override
    public boolean onRequestPermissionResult(Fragment fragment, RongExtension extension, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            startLocationActivity(fragment, extension);
        } else {
            if (fragment.getActivity() != null)
                PermissionCheckUtil.showRequestPermissionFailedAlter(fragment.getContext(), permissions, grantResults);
        }
        return true;
    }
}
