package io.rong.imkit.feature.location.plugin;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.common.LibStorageUtils;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.feature.location.AMapLocationActivity;
import io.rong.imkit.feature.location.AMapLocationActivity2D;
import io.rong.imkit.feature.location.AMapRealTimeActivity;
import io.rong.imkit.feature.location.AMapRealTimeActivity2D;
import io.rong.imkit.feature.location.LocationDelegate2D;
import io.rong.imkit.feature.location.LocationDelegate3D;
import io.rong.imkit.feature.location.LocationManager;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.location.message.LocationMessage;


public class CombineLocationPlugin implements IPluginModule, IPluginRequestPermissionResultCallback {

    private final static int REQUEST_CODE_FOREGROUND_PERMISSION_PLUGIN = 254;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ext_plugin_location_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_location_title);
    }

    @Override
    public void onClick(final Fragment currentFragment, final RongExtension extension, int index) {
        if (extension == null) {
            return;
        }

        mConversationType = extension.getConversationType();
        mTargetId = extension.getTargetId();

        //ACCESS_BACKGROUND_LOCATION后台定位权限自API 29添加.
        //Android 11 对前后台位置权限申请顺序有要求,先前台再后台,否则无法得到后台权限授权.
        //适配方案：1、target>=30 先前台位置再后台位置权限 2、target<30 前后台权限一起申请
        String[] permissionsWithBackground = new String[]{Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE};

        String[] permissions = new String[]{Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE};

        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? permissionsWithBackground : permissions)) {
            sendOrShareLocation(currentFragment, extension);
        } else {
            if (LibStorageUtils.isOsAndTargetForR(currentFragment.getActivity())) {
                //os>=30 and target>=30
                extension.requestPermissionForPluginResult(permissions,
                        REQUEST_CODE_FOREGROUND_PERMISSION_PLUGIN,
                        this);
            } else if (LibStorageUtils.isBuildAndTargetForQ(currentFragment.getActivity())) {
                //os>=29 and target>=29
                extension.requestPermissionForPluginResult(permissionsWithBackground,
                        IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                        this);
            } else {
                extension.requestPermissionForPluginResult(permissions,
                        IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                        this);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null == data) {
            return;
        }
        double lat = data.getDoubleExtra("lat", 0);
        double lng = data.getDoubleExtra("lng", 0);
        String poi = data.getStringExtra("poi");
        String thumb = data.getStringExtra("thumb");
        LocationMessage locationMessage = LocationMessage.obtain(lat, lng, poi, Uri.parse(thumb));
        io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, locationMessage);
        IMCenter.getInstance().sendMessage(message, null, null, null);
        if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
            RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:LBSMsg");
        }
    }

    private void sendOrShareLocation(final Fragment currentFragment, final RongExtension extension) {
        String[] items = {
                currentFragment.getString(R.string.rc_plugin_location_message),
                currentFragment.getString(R.string.rc_plugin_location_sharing)
        };

        OptionsPopupDialog.newInstance(currentFragment.getActivity(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
            @Override
            public void onOptionsItemClicked(int which) {
                if (which == 0) {
                    Intent intent;
                    if (LocationManager.getInstance().getMapMode().equals(LocationManager.MapMode.Map_2D)) {
                        intent = new Intent(currentFragment.getActivity(), AMapLocationActivity2D.class);
                    } else {
                        intent = new Intent(currentFragment.getActivity(), AMapLocationActivity.class);
                    }
                    extension.startActivityForPluginResult(intent, 1, CombineLocationPlugin.this);
                } else if (which == 1) {
                    int result;
                    if (LocationManager.getInstance().getMapMode().equals(LocationManager.MapMode.Map_2D)) {
                        result = LocationDelegate2D.getInstance().joinLocationSharing();
                    } else {
                        result = LocationDelegate3D.getInstance().joinLocationSharing();
                    }
                    if (result == 0) {
                        Intent intent;
                        if (LocationManager.getInstance().getMapMode().equals(LocationManager.MapMode.Map_2D)) {
                            intent = new Intent(currentFragment.getActivity(), AMapRealTimeActivity2D.class);
                        } else {
                            intent = new Intent(currentFragment.getActivity(), AMapRealTimeActivity.class);
                        }
                        if (currentFragment.getActivity() != null) {
                            currentFragment.getActivity().startActivity(intent);
                        }
                    } else if (result == 1) {
                        Toast.makeText(currentFragment.getActivity(), R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
                    } else if (result == 2) {
                        Toast.makeText(currentFragment.getActivity(), R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }).show();
    }

    @Override
    public boolean onRequestPermissionResult(Fragment fragment, final RongExtension extension, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_FOREGROUND_PERMISSION_PLUGIN) {
            if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
                new AlertDialog.Builder(fragment.getActivity()).setMessage(fragment.getResources().getString(R.string.rc_permission_background_location_grant_tip)).setPositiveButton(R.string.rc_dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        extension.requestPermissionForPluginResult(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                                CombineLocationPlugin.this);
                    }
                }).create().show();
            } else {
                if (fragment.getActivity() != null) {
                    PermissionCheckUtil.showRequestPermissionFailedAlter(fragment.getActivity(), permissions, grantResults);
                }
            }
        } else {
            if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
                sendOrShareLocation(fragment, extension);
            } else {
                if (fragment.getActivity() != null) {
                    PermissionCheckUtil.showRequestPermissionFailedAlter(fragment.getActivity(), permissions, grantResults);
                }
            }
        }
        return true;
    }
}
