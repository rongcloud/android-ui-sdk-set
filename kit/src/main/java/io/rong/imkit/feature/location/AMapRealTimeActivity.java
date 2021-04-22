package io.rong.imkit.feature.location;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rong.imkit.R;
import io.rong.common.RLog;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.userinfo.viewmodel.UserInfoViewModel;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.widget.dialog.PromptPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.UserInfo;

import static android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
import static android.provider.Settings.ACTION_SETTINGS;

public class AMapRealTimeActivity extends RongBaseNoActionbarActivity implements
        ILocationChangedListener {

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
        if (!LocationDelegate3D.getInstance().isBindedConversation()) {
            finish();
            return;
        }
        setContentView(R.layout.rc_location_real_time_activity);
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
                        LocationDelegate3D.getInstance().quitLocationSharing();
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

        if (RongUtils.isLocationServiceEnabled(this)) {
            initMap();
        } else {
            new AlertDialog.Builder(this).
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
        }


        UserInfoViewModel userInfoViewModel = new ViewModelProvider(this).get(UserInfoViewModel.class);
        userInfoViewModel.getAllUsers().observe(this, new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if (users != null) {
                    for (User user : users) {
                        if (mParticipants.contains(user.id)) {
                            UserInfo info = new UserInfo(user.id, user.name, Uri.parse(user.portraitUrl));
                            info.setExtra(user.extra);
                            updateUserInfo(info);
                        }
                    }
                }
            }
        });


    }

    private void initMap() {
        mAMap = mAMapView.getMap();
        mAMap.getUiSettings().setMyLocationButtonEnabled(false);
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);

        for (String userId : mParticipants) {
            UserTarget userTarget = createUserTargetById(userId);
            mUserTargetMap.put(userId, userTarget);

            UserInfo user = RongUserInfoManager.getInstance().getUserInfo(userId);
            updateUserInfo(user);
            updateParticipantTitleText();
        }
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(17f);
        mAMap.animateCamera(zoom, null);

        LocationDelegate3D.getInstance().setLocationChangedListener(this);
        LocationDelegate3D.getInstance().setMyLocationChangedListener(new IMyLocationChangedListener() {
            @Override
            public void onMyLocationChanged(AMapLocationInfo locInfo) {
                updateParticipantMarker(locInfo.getLat(), locInfo.getLng(), RongIMClient.getInstance().getCurrentUserId());
            }
        });
        LocationDelegate3D.getInstance().updateMyLocationInLoop(5);
    }

    public void updateUserInfo(UserInfo user) {
        String userId = user.getUserId();
        UserTarget userTarget = mUserTargetMap.get(userId);
        if (userTarget != null) {
            setAvatar(userTarget.getTargetView(), user.getPortraitUri().toString());
            View iconView = LayoutInflater.from(AMapRealTimeActivity.this).inflate(R.layout.rc_location_realtime_marker_icon, null);
            ImageView imageView = iconView.findViewById(android.R.id.icon);
            ImageView locImageView = iconView.findViewById(android.R.id.icon1);
            setAvatar(imageView, user.getPortraitUri().toString());
            if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
                locImageView.setImageResource(R.drawable.rc_location_rt_loc_myself);
            } else {
                locImageView.setImageResource(R.drawable.rc_location_rt_loc_other);
            }
            userTarget.getTargetMarker().setIcon(BitmapDescriptorFactory.fromView(iconView));
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
        LocationDelegate3D.getInstance().quitLocationSharing();
        finish();
    }

    @Override
    public void onSharingTerminated() {

    }

    private void setAvatar(ImageView imageView, String url) {
        Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.rc_location_realtime_default_avatar)
                .fallback(R.drawable.rc_location_realtime_default_avatar)
                .error(R.drawable.rc_location_realtime_default_avatar)
                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(imageView);
    }

    @Override
    protected void onDestroy() {
        RLog.d(TAG, "onDestroy()");
        if (mAMapView != null) {
            mAMapView.onDestroy();
        }
        LocationDelegate3D.getInstance().setLocationChangedListener(null);
        LocationDelegate3D.getInstance().setMyLocationChangedListener(null);
        super.onDestroy();
    }

    private UserTarget createUserTargetById(final String userId) {
        if (mUserTargetMap.get(userId) != null) {
            return mUserTargetMap.get(userId);
        }
        UserTarget userTarget = new UserTarget();

        // set target icon view.
        userTarget.setTargetView(new ImageView(this));
        userTarget.getTargetView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserTarget user = mUserTargetMap.get(userId);
                LatLng latLng = null;
                if (user != null) {
                    latLng = user.getTargetMarker().getPosition();
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
        userTarget.getTargetView().setLayoutParams(lp);
        userTarget.getTargetView().setPadding(pd, pd, pd, pd);
        setAvatar(userTarget.getTargetView(), null);
        mTitleBar.addView(userTarget.getTargetView());

        // set target aMap marker.
        View iconView = LayoutInflater.from(AMapRealTimeActivity.this).inflate(R.layout.rc_location_realtime_marker_icon, null);
        ImageView locImageView = iconView.findViewById(android.R.id.icon1);

        ImageView avatar = iconView.findViewById(android.R.id.icon);
        setAvatar(avatar, null);
        if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
            locImageView.setImageResource(R.drawable.rc_location_rt_loc_myself);
        } else {
            locImageView.setImageResource(R.drawable.rc_location_rt_loc_other);
        }
        MarkerOptions markerOptions = new MarkerOptions().anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromView(iconView));
        userTarget.setTargetMarker(mAMap.addMarker(markerOptions));
        return userTarget;
    }

    private void removeParticipantTitleIcon(final UserTarget userTarget) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTitleBar.removeView(userTarget.getTargetView());
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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
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

                target.getTargetMarker().setPosition(new LatLng(latitude, longitude));
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
        });
    }

    private void removeParticipantMarker(final UserTarget userTarget) {
        userTarget.getTargetMarker().remove();
    }

    private class UserTarget {
        private ImageView targetView;
        private Marker targetMarker;

        public ImageView getTargetView() {
            return targetView;
        }

        public void setTargetView(ImageView targetView) {
            this.targetView = targetView;
        }

        public Marker getTargetMarker() {
            return targetMarker;
        }

        public void setTargetMarker(Marker targetMarker) {
            this.targetMarker = targetMarker;
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mAMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mAMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_LOCATION_SERVICE) {
            initMap();
        }
    }
}
