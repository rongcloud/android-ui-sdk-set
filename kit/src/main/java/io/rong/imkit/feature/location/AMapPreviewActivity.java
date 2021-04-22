package io.rong.imkit.feature.location;

import android.content.Intent;
import android.os.Bundle;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseActivity;
import io.rong.imlib.location.message.LocationMessage;

public class AMapPreviewActivity extends RongBaseActivity {
    private MapView mAMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_location_preview_activity);
        initStatusBar(R.color.app_color_white);
        mTitleBar.setRightIconDrawableVisibility(false);
        mAMapView = findViewById(R.id.rc_ext_amap);
        mAMapView.onCreate(savedInstanceState);
        mTitleBar.setTitle(R.string.rc_location_title);
        initMap();
    }

    private void initMap() {
        AMap amap;

        amap = mAMapView.getMap();
        amap.setMyLocationEnabled(false);
        amap.getUiSettings().setTiltGesturesEnabled(false);
        amap.getUiSettings().setZoomControlsEnabled(false);
        amap.getUiSettings().setMyLocationButtonEnabled(false);

        Intent intent = getIntent();
        LocationMessage locationMessage = intent.getParcelableExtra("location");
        double lat = locationMessage.getLat();
        double lng = locationMessage.getLng();
        String poi = locationMessage.getPoi();
        MarkerOptions markerOptions = new MarkerOptions().anchor(0.5f, 0.5f)
                .position(new LatLng(lat, lng)).title(poi)
                .snippet(lat + "," + lng).draggable(false);
        Marker marker = amap.addMarker(markerOptions);
        amap.setInfoWindowAdapter(new AmapInfoWindowAdapter(AMapPreviewActivity.this));
        marker.showInfoWindow();
        amap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(new LatLng(lat, lng)).zoom(16).bearing(0).build()));
    }

    @Override
    protected void onDestroy() {
        mAMapView.onDestroy();
        super.onDestroy();
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
}