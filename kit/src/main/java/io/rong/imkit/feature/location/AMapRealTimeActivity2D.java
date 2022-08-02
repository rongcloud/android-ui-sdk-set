package io.rong.imkit.feature.location;

import static android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
import static android.provider.Settings.ACTION_SETTINGS;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.widget.dialog.PromptPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

public class AMapRealTimeActivity2D extends RongBaseNoActionbarActivity implements
        ILocationChangedListener, RongUserInfoManager.UserDataObserver {

    private static final String TAG = "AMapRealTimeActivity";
    private static final int REQUEST_OPEN_LOCATION_SERVICE = 50;

    private MapView mAMapView;
    private ViewGroup mTitleBar;
    private TextView mUserText;
    private Handler mHandler;
    private AMap mAMap;
    private Map<String, UserTarget> mUserTargetMap;
    private ArrayList<String> mParticipants;
    private boolean mHasAnimate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LocationDelegate2D.getInstance().isBindedConversation()) {
            finish();
            return;
        }
        setContentView(R.layout.rc_location_real_time_activity_2d);
        mHandler = new Handler();
        mUserTargetMap = new HashMap<>();
        mAMapView = findViewById(R.id.rc_ext_amap);
        mAMapView.onCreate(savedInstanceState);
        View exitView = findViewById(R.id.rc_toolbar_close);
        exitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PromptPopupDialog dialog = PromptPopupDialog.newInstance(v.getContext(), "",
                        getString(R.string.rc_location_exit_location_sharing),
                        getString(R.string.rc_location_exit_sharing_confirm));
                dialog.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                    @Override
                    public void onPositiveButtonClicked() {
                        LocationDelegate2D.getInstance().quitLocationSharing();
                        finish();
                    }
                });
                dialog.show();
            }
        });
        View closeView = findViewById(R.id.rc_toolbar_hide);
        closeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mTitleBar = findViewById(R.id.rc_user_icons);
        mUserText = findViewById(R.id.rc_user_text);

        mParticipants = getIntent().getStringArrayListExtra("participants");
        if (mParticipants == null) {
            mParticipants = new ArrayList<>();
            mParticipants.add(RongIMClient.getInstance().getCurrentUserId());
        }

        checkMapPermission();
        RongUserInfoManager.getInstance().addUserDataObserver(this);
    }

    private void initMap() {
        mAMap = mAMapView.getMap();
        mAMap.getUiSettings().setMyLocationButtonEnabled(false);
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);

        for (String userId : mParticipants) {
            UserTarget userTarget = createUserTargetById(userId);
            mUserTargetMap.put(userId, userTarget);
            UserInfo user = RongUserInfoManager.getInstance().getUserInfo(userId);
            if (user != null) {
                updateUserInfo(user);
            }
            updateParticipantTitleText();
        }
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(17f);
        mAMap.animateCamera(zoom, null);

        LocationDelegate2D.getInstance().setLocationChangedListener(this);
        LocationDelegate2D.getInstance().setMyLocationChangedListener(new IMyLocationChangedListener() {
            @Override
            public void onMyLocationChanged(AMapLocationInfo locInfo) {
                updateParticipantMarker(locInfo.getLat(), locInfo.getLng(), RongIMClient.getInstance().getCurrentUserId());
            }
        });
        LocationDelegate2D.getInstance().updateMyLocationInLoop(5);
    }

    public void updateUserInfo(UserInfo userInfo) {
        String userId = userInfo.getUserId();
        UserTarget userTarget = mUserTargetMap.get(userId);
        if (userTarget != null) {
            setAvatar(userTarget.targetView, userInfo.getPortraitUri().toString(), null);
            View iconView = LayoutInflater.from(AMapRealTimeActivity2D.this).inflate(R.layout.rc_location_realtime_marker_icon, null);
            ImageView imageView = iconView.findViewById(android.R.id.icon);
            ImageView locImageView = iconView.findViewById(android.R.id.icon1);
            if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
                locImageView.setImageResource(R.drawable.rc_location_rt_loc_myself);
            } else {
                locImageView.setImageResource(R.drawable.rc_location_rt_loc_other);
            }
            setAvatar(imageView, userInfo.getPortraitUri().toString(), new PictureRequestListener(userTarget, iconView));
        }
    }

    @Override
    public void onLocationChanged(double latitude, double longitude, String userId) {
        updateParticipantMarker(latitude, longitude, userId);
    }

    @Override
    public void onParticipantJoinSharing(String userId) {
        if (mUserTargetMap.get(userId) != null) {
            return;
        }
        if (!mParticipants.contains(userId)) {
            mParticipants.add(userId);
        }
        UserTarget userTarget = createUserTargetById(userId);
        mUserTargetMap.put(userId, userTarget);
        UserInfo user = RongUserInfoManager.getInstance().getUserInfo(userId);
        if (user != null) {
            updateUserInfo(user);
        }
        updateParticipantTitleText();
    }

    @Override
    public void onParticipantQuitSharing(String userId) {
        UserTarget userTarget = mUserTargetMap.get(userId);
        mParticipants.remove(userId);
        if (userTarget != null) {
            mUserTargetMap.remove(userId);
            removeParticipantTitleIcon(userTarget);
            updateParticipantTitleText();
            removeParticipantMarker(userTarget);
        }
    }

    @Override
    public void onError(RealTimeLocationConstant.RealTimeLocationErrorCode code) {
        Toast.makeText(this, R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
        LocationDelegate2D.getInstance().quitLocationSharing();
        finish();
    }

    @Override
    public void onSharingTerminated() {

    }


    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        Bitmap bitmap = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return bitmap;
    }

    private void setAvatar(ImageView imageView, String url, PictureRequestListener listener) {
        Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.rc_location_realtime_default_avatar)
                .fallback(R.drawable.rc_location_realtime_default_avatar)
                .error(R.drawable.rc_location_realtime_default_avatar)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .addListener(listener)
                .into(imageView);
    }

    @Override
    protected void onDestroy() {
        RLog.d(TAG, "onDestroy()");
        if (mAMapView != null) {
            mAMapView.onDestroy();
        }
        LocationDelegate2D.getInstance().setLocationChangedListener(null);
        RongUserInfoManager.getInstance().removeUserDataObserver(this);
        super.onDestroy();
    }

    private UserTarget createUserTargetById(final String userId) {
        if (mUserTargetMap.get(userId) != null) {
            return mUserTargetMap.get(userId);
        }
        UserTarget userTarget = new UserTarget();

        // set target icon view.
        userTarget.targetView = new ImageView(this);
        userTarget.targetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserTarget user = mUserTargetMap.get(userId);
                LatLng latLng = null;
                if (user != null) {
                    latLng = user.targetMarker.getPosition();
                }
                if (latLng != null) {
                    CameraUpdate update = CameraUpdateFactory.changeLatLng(latLng);
                    mAMap.animateCamera(update, null);
                }
            }
        });
        float scale = Resources.getSystem().getDisplayMetrics().density;
        int hw = (int) (40 * scale + 0.5f);
        int pd = (int) (2 * scale + 0.5f);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(hw, hw);
        userTarget.targetView.setLayoutParams(lp);
        userTarget.targetView.setPadding(pd, pd, pd, pd);
        mTitleBar.addView(userTarget.targetView);
        setAvatar(userTarget.targetView, null, null);

        // set target aMap marker.
        View iconView = LayoutInflater.from(AMapRealTimeActivity2D.this).inflate(R.layout.rc_location_realtime_marker_icon, null);
        ImageView locImageView = iconView.findViewById(android.R.id.icon1);

        ImageView avatar = iconView.findViewById(android.R.id.icon);
        setAvatar(avatar, null, null);
        if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
            locImageView.setImageResource(R.drawable.rc_location_rt_loc_myself);
        } else {
            locImageView.setImageResource(R.drawable.rc_location_rt_loc_other);
        }
        MarkerOptions markerOptions = new MarkerOptions().anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromView(iconView));
        userTarget.targetMarker = mAMap.addMarker(markerOptions);
        return userTarget;
    }

    private void removeParticipantTitleIcon(final UserTarget userTarget) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTitleBar.removeView(userTarget.targetView);
            }
        });
    }

    private void updateParticipantTitleText() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mUserTargetMap.size() == 0) {
                    RLog.e(TAG, "mUserTargetMap size is 0 ");
                } else {
                    mUserText.setText(mUserTargetMap.size() + getResources().getString(R.string.rc_location_others_sharing));
                }
            }
        });
    }

    private void updateParticipantMarker(final double latitude, final double longitude, final String userId) {
        UserTarget target = mUserTargetMap.get(userId);
        if (target == null) {
            target = createUserTargetById(userId);
            mUserTargetMap.put(userId, target);
            UserInfo user = RongUserInfoManager.getInstance().getUserInfo(userId);
            if (user != null) {
                updateUserInfo(user);
            }
            if (!mParticipants.contains(userId)) {
                mParticipants.add(userId);
            }
        }

        target.targetMarker.setPosition(new LatLng(latitude, longitude));
        updateParticipantTitleText();
        if (userId.equals(RongIMClient.getInstance().getCurrentUserId())
                && !mHasAnimate
                && latitude != 0
                && longitude != 0) {
            CameraUpdate update = CameraUpdateFactory.changeLatLng(new LatLng(latitude, longitude));
            mAMap.animateCamera(update, null);
            mHasAnimate = true;
        }
    }

    private void removeParticipantMarker(final UserTarget userTarget) {
        userTarget.targetMarker.remove();
    }

    @Override
    public void onUserUpdate(UserInfo user) {
        if (user != null) {
            if (mParticipants.contains(user.getUserId())) {
                updateUserInfo(user);
            }
        }
    }

    @Override
    public void onGroupUpdate(Group group) {

    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {

    }

    private class UserTarget {
        public ImageView targetView;
        public Marker targetMarker;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_LOCATION_SERVICE) {
            initMap();
        }
    }

    class PictureRequestListener implements RequestListener<Drawable> {
        UserTarget userTarget;
        View iconView;

        public PictureRequestListener(UserTarget userTarget, View iconView) {
            this.userTarget = userTarget;
            this.iconView = iconView;
        }

        @Override
        public boolean onLoadFailed(GlideException e, Object o, Target<Drawable> target, boolean b) {
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable drawable, Object o, Target<Drawable> target, DataSource dataSource, boolean b) {
            // 加载成功
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    userTarget.targetMarker.setIcon(BitmapDescriptorFactory.fromView(iconView));
                }
            }, 500);
            return false;
        }
    }

    /**
     * 检查位置服务是否开启、后台位置权限引导
     */
    private void checkMapPermission() {
        if (RongUtils.isLocationServiceEnabled(this)) {
            initMap();
        } else {
            new android.app.AlertDialog.Builder(this).
                    setTitle(getString(R.string.rc_location_sevice_dialog_title))
                    .setMessage(getString(R.string.rc_location_sevice_dialog_messgae))
                    .setPositiveButton(getString(R.string.rc_location_sevice_dialog_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                Intent intent = new Intent(ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivityForResult(intent, REQUEST_OPEN_LOCATION_SERVICE);
                            } catch (Exception e) {
                                Intent intent = new Intent(ACTION_SETTINGS);
                                startActivityForResult(intent, REQUEST_OPEN_LOCATION_SERVICE);
                            }

                        }
                    }).create().show();

            return;
        }

        boolean isEnableBackgroundLocation = getResources().getBoolean(R.bool.rc_enable_background_location_permission);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && isEnableBackgroundLocation
                && !PermissionCheckUtil.checkPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION})) {

            new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                    .setMessage("应用在后台仍想访问您的位置信息，在设置中选择始终允许")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + AMapRealTimeActivity2D.this.getPackageName()));
                            startActivity(intent);
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
        }
    }
}
