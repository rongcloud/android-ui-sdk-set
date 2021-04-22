package io.rong.imkit.feature.location;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.model.Marker;

import io.rong.imkit.R;


/**
 * 位置消息 marker 信息窗口适配器
 */
public class AmapInfoWindowAdapter2D implements AMap.InfoWindowAdapter {

    private Context context;

    public AmapInfoWindowAdapter2D(Context context) {
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_location_marker_info_window, null);
        setViewContent(marker, view);
        return view;
    }

    private void setViewContent(Marker marker, View view) {
        TextView markerTitle = view.findViewById(R.id.rc_location_marker_title);
        markerTitle.setText(marker.getTitle());
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}