package io.rong.imkit.feature.location;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseActivity;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongUtils;

import static android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS;
import static android.provider.Settings.ACTION_SETTINGS;

public class AMapLocationActivity2D extends RongBaseActivity implements
        LocationSource,
        AMap.OnCameraChangeListener,
        GeocodeSearch.OnGeocodeSearchListener,
        IMyLocationChangedListener,
        View.OnClickListener {

    private final static String TAG = "AMapLocationActivity2D";

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 100;
    private static final int REQUEST_SEARCH_LOCATION = 1;
    private static final int PAGE_COUNT = 20;
    private static final int REQUEST_OPEN_LOCATION_SERVICE = 50;
    ValueAnimator animator;
    private int currentPage = 1;
    private int mTouchSlop;//系统值
    private BitmapDescriptor mBitmapDescriptor;
    private MapView mAMapView;
    private AMap mAMap;
    private TextView mLocationTip;
    private Handler mHandler;
    private Marker mMarker;
    private GeocodeSearch mGeocodeSearch;
    private OnLocationChangedListener mLocationChangedListener;
    private double mMyLat;
    private double mMyLng;
    private String mMyPoi;
    private double mLatResult;
    private double mLngResult;
    private String mPoiResult;
    private ListView listViewNearby;
    private NearbyListAdapter nearbyListAdapter;
    private ProgressBar listLoadingView;
    private float Y;
    private float downY;
    private float lastY;
    private int flag = 0;
    private String cityCode = "";
    private String[] locationPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    private AbsListView.OnScrollListener onScrollListener = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (firstVisibleItem != 0 && (firstVisibleItem + visibleItemCount) == totalItemCount) {
                View lastVisibleItemView = listViewNearby.getChildAt(listViewNearby.getChildCount() - 1);
                if (lastVisibleItemView != null && lastVisibleItemView.getBottom() == listViewNearby.getHeight()) {
                    loadNextPageNearByView();
                }
            }
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LocationDelegate2D.getInstance().isBindedConversation()) {
            finish();
            return;
        }
        setContentView(R.layout.rc_location_2d_activity);
        initStatusBar(R.color.app_color_white);
        mAMapView = findViewById(R.id.rc_ext_amap);
        initNearbyView();
        mHandler = new Handler();
        mLocationTip = findViewById(R.id.rc_ext_location_marker);
        ImageView myLocationView = findViewById(R.id.rc_ext_my_location);
        myLocationView.setOnClickListener(this);
        Button ok = findViewById(R.id.rc_action_bar_ok);
        ok.setText(getResources().getString(R.string.rc_send));
        ok.setVisibility(View.VISIBLE);
        ok.setOnClickListener(this);
        mTitleBar.setTitle(R.string.rc_location_title);
        mAMapView.onCreate(savedInstanceState);
        checkMapPermission();
        initMap();

        //Bug:关闭位置服务进入到此页面,直接在设置或快捷菜单中再次开启位置服务(非位置服务引导进行的操作),查看位置没有正常加载数据.
        //监听手机位置服务或定位服务开关状态,重新开启需重新定位,进而回调onMyLocationChanged再次刷新位置页面.
        if (PermissionCheckUtil.checkPermissions(this, locationPermissions)) {
            android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0f, gpsListener);
            locationManager.addGpsStatusListener(new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    if (event == GpsStatus.GPS_EVENT_STARTED) {
                        Log.w(TAG, "Gps Started");
                        LocationDelegate3D.getInstance().updateMyLocation();
                    } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
                        Log.w(TAG, "Gps Stopped");
                    }
                }
            });
        }
    }

    private LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    /**
     * 检查位置服务是否开启
     */
    private void checkMapPermission() {
        if (!RongUtils.isLocationServiceEnabled(this)) {
            new AlertDialog.Builder(AMapLocationActivity2D.this).
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
    }

    private void initMap() {
        mAMap = mAMapView.getMap();
        mAMap.setLocationSource(AMapLocationActivity2D.this);
        mAMap.setMyLocationEnabled(true);
        mAMap.getUiSettings().setZoomControlsEnabled(false);
        mAMap.getUiSettings().setMyLocationButtonEnabled(false);
        mAMap.setMapType(AMap.MAP_TYPE_NORMAL);

        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.rc_location_my_locator));// 设置小蓝点的图标
        myLocationStyle.strokeWidth(0);
        myLocationStyle.strokeColor(R.color.rc_main_theme);
        myLocationStyle.radiusFillColor(Color.TRANSPARENT);
        mAMap.setMyLocationStyle(myLocationStyle);

        mGeocodeSearch = new GeocodeSearch(AMapLocationActivity2D.this);
        mGeocodeSearch.setOnGeocodeSearchListener(AMapLocationActivity2D.this);
        LocationDelegate2D.getInstance().setMyLocationChangedListener(this);
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        RLog.e(TAG, "onRegeocodeSearched");
        if (regeocodeResult != null) {
            RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
            mLatResult = regeocodeResult.getRegeocodeQuery().getPoint().getLatitude();
            mLngResult = regeocodeResult.getRegeocodeQuery().getPoint().getLongitude();
            String formatAddress = regeocodeResult.getRegeocodeAddress().getFormatAddress();
            mPoiResult = formatAddress.replace(regeocodeAddress.getProvince(), "").replace(regeocodeAddress.getCity(), "").replace(regeocodeAddress.getDistrict(), "");
            mLocationTip.setText(mPoiResult);

            LatLng latLng = new LatLng(mLatResult, mLngResult);
            if (mMarker != null) {
                mMarker.setPosition(latLng);
            }
            updateNearByView(mPoiResult);
        } else {
            Toast.makeText(AMapLocationActivity2D.this, getString(R.string.rc_location_fail), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    private String getMapUrl(double latitude, double longitude) {
        return "http://restapi.amap.com/v3/staticmap?location=" + longitude + "," + latitude +
                "&zoom=16&scale=2&size=408*240&markers=mid,,A:" + longitude + ","
                + latitude + "&key=" + "e09af6a2b26c02086e9216bd07c960ae";
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mLocationChangedListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        RLog.d(TAG, "onCameraChange");
        if (Build.VERSION.SDK_INT < 11) {
            mMarker.setPosition(cameraPosition.target);
        }
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        RLog.d(TAG, "onCameraChangeFinish");
        LatLonPoint point = new LatLonPoint(cameraPosition.target.latitude, cameraPosition.target.longitude);
        RegeocodeQuery query = new RegeocodeQuery(point, 50, GeocodeSearch.AMAP);
        mGeocodeSearch.getFromLocationAsyn(query);
        if (mMarker != null) {
            animMarker();
        }
    }

    private void addLocatedMarker(LatLng latLng, String poi) {
        mBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.rc_location_marker);
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).icon(mBitmapDescriptor);
        if (mMarker != null) {
            mMarker.remove();
        }
        mMarker = mAMap.addMarker(markerOptions);
        mMarker.setPositionByPixels(mAMapView.getWidth() / 2, mAMapView.getHeight() / 2);
        mLocationTip.setText(String.format("%s", poi));
    }

    @TargetApi(11)
    private void animMarker() {
        if (Build.VERSION.SDK_INT > 11) {
            if (animator != null) {
                animator.start();
                return;
            }

            animator = ValueAnimator.ofFloat(mAMapView.getHeight() / 2, mAMapView.getHeight() / 2 - 30);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(150);
            animator.setRepeatCount(1);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    RLog.d(TAG, "onAnimationUpdate");

                    Float value = (Float) animation.getAnimatedValue();
                    mMarker.setPositionByPixels(mAMapView.getWidth() / 2, Math.round(value));
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mMarker.setIcon(mBitmapDescriptor);
                }
            });
            animator.start();
        }
    }

    @Override
    protected void onDestroy() {
        if (mAMapView != null) {
            mAMapView.onDestroy();
        }
        LocationDelegate2D.getInstance().setMyLocationChangedListener(null);
        super.onDestroy();
    }

    @Override
    public void onMyLocationChanged(final AMapLocationInfo locationInfo) {
        RLog.d(TAG, "onLocationChanged");
        if (mLocationChangedListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationInfo != null) {
                        mMyLat = mLatResult = locationInfo.getLat();
                        mMyLng = mLngResult = locationInfo.getLng();
                        mMyPoi = mPoiResult = locationInfo.getStreet() + locationInfo.getPoiname();
                        cityCode = locationInfo.getCitycode();

                        updateNearByView(mMyPoi);
                        Location location = new Location("AMap");
                        location.setLatitude(locationInfo.getLat());
                        location.setLongitude(locationInfo.getLng());
                        location.setTime(locationInfo.getTime());
                        location.setAccuracy(locationInfo.getAccuracy());
                        mLocationChangedListener.onLocationChanged(location);
                        LatLng mLatLang = new LatLng(mLatResult, mLngResult);
                        addLocatedMarker(mLatLang, mPoiResult);
                        CameraUpdate update = CameraUpdateFactory.changeLatLng(mLatLang);
                        mAMap.animateCamera(update, new AMap.CancelableCallback() {
                            @Override
                            public void onFinish() {
                                mAMap.setOnCameraChangeListener(AMapLocationActivity2D.this);
                            }

                            @Override
                            public void onCancel() {
                            }
                        });
                    } else {
                        Toast.makeText(AMapLocationActivity2D.this, getString(R.string.rc_location_fail), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void initNearbyView() {
        listViewNearby = findViewById(R.id.rc_list_nearby);
        listLoadingView = findViewById(R.id.rc_ext_loading);
        listViewNearby.setVisibility(View.GONE);
        listLoadingView.setVisibility(View.VISIBLE);
        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        listViewNearby.setOnScrollListener(onScrollListener);
        listViewNearby.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (int i = 0; i < nearbyListAdapter.getCount(); i++) {
                    MapNearbyInfo mapNearbyInfo = (MapNearbyInfo) nearbyListAdapter.getItem(i);
                    if (i == position) {
                        mapNearbyInfo.setChecked(true);
                        mLngResult = mapNearbyInfo.getLongitude();
                        mLatResult = mapNearbyInfo.getLatitude();
                        mPoiResult = mapNearbyInfo.getPoi();
                        updateCheckedMapView(mLngResult, mLatResult, mPoiResult);
                    } else {
                        mapNearbyInfo.setChecked(false);
                    }
                }
                nearbyListAdapter.notifyDataSetChanged();
            }
        });
        listViewNearby.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Y = event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = Y;
                        lastY = Y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(Y - downY) > mTouchSlop) {
                            if (Y - downY >= 0 && Y - lastY >= 0) {
                                lastY = Y;
                                return handleScrollState(ScrollDirection.SCROLL_DOWN);
                            } else {
                                lastY = Y;
                                return handleScrollState(ScrollDirection.SCROLL_UP);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // 触摸抬起时的操作
                        Y = 0;
                        if (flag == 1) {
                            flag = 0;
                            return true;
                        }
                        lastY = 0;
                        break;
                }
                return false;
            }
        });
    }

    private boolean handleScrollState(ScrollDirection scrollDirection) {
        int minHeight = (int) getResources().getDimension(R.dimen.rc_location_nearby_list_min_height);
        int maxHeight = (int) getResources().getDimension(R.dimen.rc_location_nearby_list_max_height);
        if (scrollDirection == ScrollDirection.SCROLL_DOWN) {
            return updateListViewHeight(maxHeight, minHeight);
        } else {
            return updateListViewHeight(minHeight, maxHeight);
        }
    }

    private boolean updateListViewHeight(int oldHeight, int newHeight) {
        int height = listViewNearby.getHeight();
        int top;
        if (listViewNearby == null || listViewNearby.getChildAt(0) == null) {
            return false;
        }
        top = listViewNearby.getChildAt(0).getTop();
        if (top == 0 && Math.abs(Y - downY) > mTouchSlop && flag == 0 && height == oldHeight) {
            flag = 1;
        }
        if (flag == 1) {
            ViewGroup.LayoutParams layoutParams = listViewNearby.getLayoutParams();
            layoutParams.height = newHeight;
            listViewNearby.setLayoutParams(layoutParams);
            LatLng latLng = new LatLng(mLatResult, mLngResult);
            if (mMarker != null) {
                mMarker.setPosition(latLng);
            }
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) (mLocationTip.getLayoutParams());
            int marginTop;
            int marginLeft;
            int marginRight;
            if (oldHeight < newHeight) {
                //设置 title 位置
                marginTop = RongUtils.dip2px(3);
                marginLeft = RongUtils.dip2px(20);
                marginRight = RongUtils.dip2px(20);
                params.setMargins(marginLeft, marginTop, marginRight, 0);
                mLocationTip.setLayoutParams(params);
            } else {
                marginTop = RongUtils.dip2px(20);
                marginLeft = RongUtils.dip2px(20);
                marginRight = RongUtils.dip2px(20);
                params.setMargins(marginLeft, marginTop, marginRight, 0);
                mLocationTip.setLayoutParams(params);
            }
            //阻止 listViewNearby scroll 事件
            return true;
        }
        return false;
    }

    private void updateNearByView(final String poi) {
        PoiSearch.Query query = new PoiSearch.Query("", LocationConst.CATEGORY, "");
        query.setPageSize(PAGE_COUNT);// 设置每页最多返回多少条poiitem
        currentPage = 1;
        query.setPageNum(currentPage);// 设置查第一页
        PoiSearch poiSearch = new PoiSearch(this, query);

        double longitude = mLngResult, latitude = mLatResult;
        if (latitude != 0.0 && longitude != 0.0) {
            poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude,
                    longitude), LocationConst.DISTANCE));// 设置周边搜索的中心点以及区域
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult poiResult, int i) {
                    if (poiResult != null && poiResult.getPois().size() > 0) {
                        List<MapNearbyInfo> mapNearbyInfos = new ArrayList<>();
                        List<PoiItem> poiItemList = poiResult.getPois();
                        //设置列表第一项
                        MapNearbyInfo nearbyInfo;
                        nearbyInfo = new MapNearbyInfo();
                        nearbyInfo.setName(poi);
                        nearbyInfo.setChecked(true);
                        nearbyInfo.setLongitude(mLngResult);
                        nearbyInfo.setLatitude(mLatResult);
                        nearbyInfo.setPoi(poi);
                        mapNearbyInfos.add(nearbyInfo);

                        //根据搜索结果设置列表其余项
                        for (int j = 0; j < poiItemList.size(); j++) {
                            PoiItem poiItem = poiItemList.get(j);
                            nearbyInfo = new MapNearbyInfo(poiItem.getTitle(), poiItem.getSnippet());
                            nearbyInfo.setLongitude(poiItem.getLatLonPoint().getLongitude());
                            nearbyInfo.setLatitude(poiItem.getLatLonPoint().getLatitude());
                            nearbyInfo.setPoi(poiItem.getTitle());
                            mapNearbyInfos.add(nearbyInfo);
                        }

                        //设置附近位置列表 adapter
                        nearbyListAdapter = new NearbyListAdapter(AMapLocationActivity2D.this, mapNearbyInfos);
                        listViewNearby.setAdapter(nearbyListAdapter);
                        listViewNearby.setVisibility(View.VISIBLE);
                        listLoadingView.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int i) {

                }
            });// 设置数据返回的监听器
            poiSearch.searchPOIAsyn();// 开始搜索
        } else {
            listViewNearby.setVisibility(View.VISIBLE);
            listLoadingView.setVisibility(View.GONE);
            Toast.makeText(AMapLocationActivity2D.this, getResources().getString(R.string.rc_location_fail),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNextPageNearByView() {
        PoiSearch.Query query = new PoiSearch.Query("", LocationConst.CATEGORY, "");
        query.setPageSize(PAGE_COUNT);// 设置每页最多返回多少条poiitem
        query.setPageNum(++currentPage);// 设置查第一页
        PoiSearch poiSearch = new PoiSearch(this, query);
        double longitude = mLngResult, latitude = mLatResult;
        if (latitude != 0.0 && longitude != 0.0) {
            poiSearch.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude,
                    longitude), LocationConst.DISTANCE));// 设置周边搜索的中心点以及区域
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResult poiResult, int i) {
                    if (poiResult != null && poiResult.getPois().size() > 0) {
                        List<MapNearbyInfo> mapNearbyInfos = new ArrayList<>();
                        List<PoiItem> poiItemList = poiResult.getPois();
                        MapNearbyInfo nearbyInfo;
                        for (int j = 0; j < poiItemList.size(); j++) {
                            PoiItem poiItem = poiItemList.get(j);
                            nearbyInfo = new MapNearbyInfo(poiItem.getTitle(), poiItem.getSnippet());
                            nearbyInfo.setLongitude(poiItem.getLatLonPoint().getLongitude());
                            nearbyInfo.setLatitude(poiItem.getLatLonPoint().getLatitude());
                            nearbyInfo.setPoi(poiItem.getTitle());
                            mapNearbyInfos.add(nearbyInfo);
                        }
                        nearbyListAdapter.addItems(mapNearbyInfos);
                        nearbyListAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int i) {

                }
            });// 设置数据返回的监听器
            poiSearch.searchPOIAsyn();// 开始搜索
        } else {
            Toast.makeText(AMapLocationActivity2D.this, getResources().getString(R.string.rc_location_fail),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.rc_ext_my_location) {
            handleMyLocation();
        } else if (v.getId() == R.id.rc_action_bar_ok) {
            handleOkButton();
        } else if (v.getId() == R.id.rc_search) {
            Intent intent = new Intent(this, SearchLocationActivity.class);
            intent.putExtra(LocationConst.CITY_CODE, cityCode);
            startActivityForResult(intent, REQUEST_SEARCH_LOCATION);
        }
    }

    private void handleMyLocation() {
        if (mMyPoi != null) {
            mAMap.setOnCameraChangeListener(null);
            CameraUpdate update = CameraUpdateFactory.changeLatLng(new LatLng(mMyLat, mMyLng));
            mAMap.animateCamera(update, new AMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mAMap.setOnCameraChangeListener(AMapLocationActivity2D.this);
                }

                @Override
                public void onCancel() {

                }
            });
            mLocationTip.setText(mMyPoi);
            mLatResult = mMyLat;
            mLngResult = mMyLng;
            mPoiResult = mMyPoi;
            LatLng latLng = new LatLng(mLatResult, mLngResult);
            updateNearByView(mMyPoi);
            if (mMarker != null) {
                mMarker.setPosition(latLng);
            }
        } else {
            LocationDelegate2D.getInstance().updateMyLocation();
        }
    }

    private void handleOkButton() {
        if (mLatResult == 0.0d && mLngResult == 0.0d && TextUtils.isEmpty(mPoiResult)) {
            Toast.makeText(AMapLocationActivity2D.this, getString(R.string.rc_location_temp_failed), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent();
        intent.putExtra("thumb", getMapUrl(mLatResult, mLngResult));
        intent.putExtra("lat", mLatResult);
        intent.putExtra("lng", mLngResult);
        intent.putExtra("poi", mPoiResult);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SEARCH_LOCATION && data != null) {
            mLngResult = data.getDoubleExtra(LocationConst.LONGITUDE, 0);
            mLatResult = data.getDoubleExtra(LocationConst.LATITUDE, 0);
            mPoiResult = data.getStringExtra(LocationConst.POI);
            resetViewHeight();
            updateToPosition(mLngResult, mLatResult, mPoiResult, true);
        } else if (requestCode == REQUEST_OPEN_LOCATION_SERVICE) {
            LocationDelegate2D.getInstance().updateMyLocation();
        }
    }

    private void resetViewHeight() {
        //重置附近位置列表高度
        if (listViewNearby != null) {
            int minHeight = (int) getResources().getDimension(R.dimen.rc_location_nearby_list_min_height);
            ViewGroup.LayoutParams layoutParams = listViewNearby.getLayoutParams();
            layoutParams.height = minHeight;
            listViewNearby.setLayoutParams(layoutParams);
        }

        //重置 title 位置
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) (mLocationTip.getLayoutParams());
        int marginTop;
        int marginLeft;
        int marginRight;
        marginTop = RongUtils.dip2px(20);
        marginLeft = RongUtils.dip2px(20);
        marginRight = RongUtils.dip2px(20);
        params.setMargins(marginLeft, marginTop, marginRight, 0);
        mLocationTip.post(new Runnable() {
            @Override
            public void run() {
                mLocationTip.setLayoutParams(params);
            }
        });

    }

    private void updateToPosition(double longitude, double latitude, String poi, boolean updateNearby) {
        if (poi != null) {
            mLatResult = latitude;
            mLngResult = longitude;
            mPoiResult = poi;
            mLocationTip.setText(mPoiResult);
            if (updateNearby) {
                updateNearByView(mPoiResult);
            }
            final LatLng latLng = new LatLng(mLatResult, mLngResult);
            CameraUpdate update = CameraUpdateFactory.changeLatLng(latLng);
            mAMap.setOnCameraChangeListener(null);
            mAMap.animateCamera(update, new AMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mAMap.setOnCameraChangeListener(AMapLocationActivity2D.this);
                    if (mMarker != null) {
                        mMarker.setPosition(latLng);
                    }
                }

                @Override
                public void onCancel() {

                }
            });
        }
    }

    private void updateCheckedMapView(double longitude, double latitude, String poi) {
        updateToPosition(longitude, latitude, poi, false);
    }

    private enum ScrollDirection {
        SCROLL_UP,
        SCROLL_DOWN
    }

    private class NearbyListAdapter extends BaseAdapter {
        List<MapNearbyInfo> nearbyInfoList;
        Context context;

        public NearbyListAdapter(Context context, List<MapNearbyInfo> nearbyInfoList) {
            this.context = context;
            this.nearbyInfoList = nearbyInfoList;
        }

        @Override
        public int getCount() {
            return nearbyInfoList.size();
        }

        @Override
        public Object getItem(int position) {
            if (nearbyInfoList != null && nearbyInfoList.size() > 0) {
                return nearbyInfoList.get(position);
            } else {
                return null;
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            NearbyViewHolder nearbyViewHolder;
            final MapNearbyInfo mapNearbyInfo = nearbyInfoList.get(position);
            if (convertView == null) {
                nearbyViewHolder = new NearbyViewHolder();
                convertView = View.inflate(context, R.layout.rc_location_map_nearby_info_item, null);
                nearbyViewHolder.tvNearbyName = convertView.findViewById(R.id.rc_nearby_name);
                nearbyViewHolder.tvNearbyAddress = convertView.findViewById(R.id.rc_nearby_address);
                nearbyViewHolder.ivNearbyChecked = convertView.findViewById(R.id.rc_nearby_checked);
                convertView.setTag(nearbyViewHolder);
            } else {
                nearbyViewHolder = (NearbyViewHolder) convertView.getTag();
            }
            if (position == 0) {
                nearbyViewHolder.tvNearbyAddress.setVisibility(View.GONE);
                nearbyViewHolder.tvNearbyName.setText(mapNearbyInfo.getName());
            } else {
                nearbyViewHolder.tvNearbyAddress.setVisibility(View.VISIBLE);
                nearbyViewHolder.tvNearbyName.setText(mapNearbyInfo.getName());
                nearbyViewHolder.tvNearbyAddress.setText(mapNearbyInfo.getAddress());
            }
            if (mapNearbyInfo.getChecked()) {
                nearbyViewHolder.ivNearbyChecked.setVisibility(View.VISIBLE);
            } else {
                nearbyViewHolder.ivNearbyChecked.setVisibility(View.GONE);
            }
            return convertView;
        }

        public void addItems(List<MapNearbyInfo> nearbyInfoList) {
            if (this.nearbyInfoList != null) {
                this.nearbyInfoList.addAll(nearbyInfoList);
            }
        }

        class NearbyViewHolder {
            TextView tvNearbyName;
            TextView tvNearbyAddress;
            ImageView ivNearbyChecked;
        }
    }

    private class MapNearbyInfo {
        String name;
        String address;
        double latitude;
        double longitude;
        String poi;
        boolean checked;

        public MapNearbyInfo() {
        }

        public MapNearbyInfo(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public String getPoi() {
            return poi;
        }

        public void setPoi(String poi) {
            this.poi = poi;
        }

        public boolean getChecked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            this.checked = checked;
        }
    }
}
