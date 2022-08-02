package io.rong.imkit.feature.location;

import android.content.Context;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.location.message.LocationMessage;

public class LocationManager implements IRealTimeLocationStateListener {
    private MapMode mMapMode = MapMode.Map_3D;
    private LocationProvider mLocationProvider;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private IRealTimeLocationChangedCallback mRTLocationChangedCallback;

    public void setOnRTLocationChangedCallback(IRealTimeLocationChangedCallback callback) {
        this.mRTLocationChangedCallback = callback;

    }

    private static class SingletonHolder {
        private static final LocationManager INSTANCE = new LocationManager();
    }

    private LocationManager() {
    }

    public static LocationManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    void init(RongExtension extension) {
        mConversationType = extension.getConversationType();
        mTargetId = extension.getTargetId();
        mMapMode = extension.getContext().getResources().getBoolean(R.bool.rc_location_2D) ? MapMode.Map_2D : MapMode.Map_3D;
        if (mMapMode.equals(MapMode.Map_2D)) {
            LocationDelegate2D.getInstance().bindConversation(extension.getContext(), extension.getConversationType(), extension.getTargetId());
            LocationDelegate2D.getInstance().setParticipantChangedListener(this);
        } else {
            LocationDelegate3D.getInstance().bindConversation(extension.getContext(), extension.getConversationType(), extension.getTargetId());
            LocationDelegate3D.getInstance().setParticipantChangedListener(this);
        }
    }

    void deinit() {
        mRTLocationChangedCallback = null;
        RongIMClient.getInstance().removeRealTimeLocationObserver(mConversationType, mTargetId);
    }

    @Override
    public void onParticipantChanged(List<String> userIdList) {
        //Todo 在会话页面绘制实时位置共享信息相关 view
        if (mRTLocationChangedCallback == null) {
            return;
        }

        mRTLocationChangedCallback.onParticipantChanged(userIdList);
    }

    @Override
    public void onErrorException() {

    }

    public MapMode getMapMode() {
        return mMapMode;
    }

    /**
     * 设置位置信息的提供者。
     *
     * @param locationProvider 位置信息提供者。
     */
    public void setLocationProvider(LocationProvider locationProvider) {
        mLocationProvider = locationProvider;
    }

    public LocationProvider getLocationProvider() {
        return mLocationProvider;
    }

    public enum MapMode {
        Map_2D,
        Map_3D
    }

    /**
     * 位置信息的提供者，实现后获取用户位置信息。
     */
    public interface LocationProvider {
        void onStartLocation(Context context, LocationCallback callback);

        interface LocationCallback {
            void onSuccess(LocationMessage message);

            void onFailure(String msg);
        }
    }

    public interface IRealTimeLocationChangedCallback {
        void onParticipantChanged(List<String> userIdList);
    }
}
