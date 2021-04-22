package io.rong.imkit.feature.location;

import android.content.Context;
import android.text.TextUtils;

import com.amap.api.maps2d.CoordinateConverter;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.netlocation.AMapNetworkLocationClient;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.location.RealTimeLocationType;
import io.rong.imlib.model.Conversation;

public class LocationDelegate2D {

    private static final String TAG = "LocationDelegate2D";

    private Context mContext;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private IRealTimeLocationStateListener mParticipantChangedListener;
    private IMyLocationChangedListener mMyLocationChangedListener;
    private ILocationChangedListener mLocationChangedListener;

    private AMapLocationParser mLocationParser;

    private AMapNetworkLocationClient mLocationClient;
    private LatLng mLatLng;
    private AMapLocationInfo mMyLastLatLng;

    private LoopThread mLoopThread;


    private static class SingletonHolder {
        private static final LocationDelegate2D INSTANCE = new LocationDelegate2D();
    }

    private LocationDelegate2D() {
    }

    public static LocationDelegate2D getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public boolean isBindedConversation() {
        if (mContext == null) {
            return false;
        }
        return true;
    }

    public void bindConversation(Context context, Conversation.ConversationType conversationType, String targetId) {
        mContext = context.getApplicationContext();
        mConversationType = conversationType;
        mTargetId = targetId;
        mLocationParser = new AMapLocationParser();
        RongIMClient.getInstance().addRealTimeLocationListener(mConversationType, mTargetId, new RongIMClient.RealTimeLocationListener() {
            @Override
            public void onStatusChange(RealTimeLocationConstant.RealTimeLocationStatus status) {
                RLog.d(TAG, "onStatusChange status = " + status.getMessage());
                sendOnParticipantChanged();
            }

            @Override
            public void onReceiveLocationWithType(double latitude, double longitude, RealTimeLocationType type, String userId) {
                RLog.d(TAG, "onReceiveLocationWithType userId = " + userId + "; latitude = " + latitude + "; longitude = " + longitude + ",type = " + type);
                if (type == RealTimeLocationType.WGS84 && (latitude != 0 || longitude != 0)) {
                    LatLng sourceLatLng = new LatLng(latitude, longitude);
                    CoordinateConverter converter = new CoordinateConverter();
                    converter.from(CoordinateConverter.CoordType.GPS);
                    converter.coord(sourceLatLng);
                    LatLng desLatLng = converter.convert();
                    if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
                        mLatLng = new LatLng(desLatLng.latitude, desLatLng.longitude);
                    }
                    sendOnLocationChanged(desLatLng.latitude, desLatLng.longitude, userId);
                } else {
                    if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
                        mLatLng = new LatLng(latitude, longitude);
                    }
                    sendOnLocationChanged(latitude, longitude, userId);
                }

            }

            @Override
            public void onReceiveLocation(double latitude, double longitude, String userId) {
                RLog.d(TAG, "onReceiveLocation userId = " + userId + "; latitude = " + latitude + "; longitude = " + longitude);
                if (userId.equals(RongIMClient.getInstance().getCurrentUserId())) {
                    mLatLng = new LatLng(latitude, longitude);
                }
                sendOnLocationChanged(latitude, longitude, userId);
            }

            @Override
            public void onParticipantsJoin(String userId) {
                RLog.d(TAG, "onParticipantsJoin userId = " + userId);
                sendOnParticipantChanged();
                sendOnParticipantJoinSharing(userId);
            }

            @Override
            public void onParticipantsQuit(String userId) {
                RLog.d(TAG, "onParticipantsQuit userId = " + userId);
                sendOnParticipantChanged();
                sendOnParticipantQuitSharing(userId);
            }

            @Override
            public void onError(RealTimeLocationConstant.RealTimeLocationErrorCode errorCode) {
                RLog.d(TAG, "RealTimeLocationErrorCode errorCode = " + errorCode);
                stopMyLocationInLoop();
                sendOnRealTimeLocationError(errorCode);
            }
        });
    }

    public void unBindConversation() {
        RongIMClient.getInstance().removeRealTimeLocationObserver(mConversationType, mTargetId);
    }

    public void setParticipantChangedListener(IRealTimeLocationStateListener listener) {
        mParticipantChangedListener = listener;
        if (mParticipantChangedListener != null) {
            mParticipantChangedListener.onParticipantChanged(RongIMClient.getInstance().getRealTimeLocationParticipants(mConversationType, mTargetId));
        }
    }

    public void setMyLocationChangedListener(IMyLocationChangedListener listener) {
        mMyLocationChangedListener = listener;
        if (mMyLocationChangedListener != null) {
            updateMyLocation();
            if (mMyLastLatLng != null) {
                listener.onMyLocationChanged(mMyLastLatLng);
            }
        }
    }

    public void setLocationChangedListener(ILocationChangedListener listener) {
        mLocationChangedListener = listener;
        if (mLocationChangedListener != null) {
            if (mLatLng != null) {
                mLocationChangedListener.onLocationChanged(mLatLng.latitude, mLatLng.longitude, RongIMClient.getInstance().getCurrentUserId());
            }
        }
    }

    public int joinLocationSharing() {
        RongIMClient.ConnectionStatusListener.ConnectionStatus state = RongIMClient.getInstance().getCurrentConnectionStatus();
        if (state != RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            return 1;
        }
        RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
        switch (status) {
            case RC_REAL_TIME_LOCATION_STATUS_IDLE:
                RongIMClient.getInstance().startRealTimeLocation(mConversationType, mTargetId);
                break;
            case RC_REAL_TIME_LOCATION_STATUS_INCOMING:
                int errorCode = RongIMClient.getInstance().joinRealTimeLocation(mConversationType, mTargetId).getValue();
                if (errorCode == RealTimeLocationConstant.RealTimeLocationErrorCode.RC_REAL_TIME_LOCATION_EXCEED_MAX_PARTICIPANT.getValue()) {
                    return 2;
                }
                break;
            default:
                break;
        }
        return 0;
    }

    public void quitLocationSharing() {
        RLog.d(TAG, "quitLocationSharing");
        RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
        if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_CONNECTED
                || status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_OUTGOING) {
            RongIMClient.getInstance().quitRealTimeLocation(mConversationType, mTargetId);
            stopMyLocationInLoop();
            mLatLng = null;
        }
    }

    public void updateMyLocation() {
        MyLocationThread thread = new MyLocationThread();
        thread.start();
    }

    public void updateMyLocationInLoop(int sec) {
        if (mLoopThread != null) {
            mLoopThread.stopLooping();
        }
        mLoopThread = new LoopThread(sec);
        mLoopThread.start();
    }

    public void stopMyLocationInLoop() {
        if (mLoopThread != null) {
            mLoopThread.stopLooping();
            mLoopThread = null;
        }
    }

    public boolean isSharing() {
        RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
        return status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_CONNECTED
                || status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_OUTGOING;
    }

    private void sendOnParticipantChanged() {
        if (mContext != null && mParticipantChangedListener != null) {
            List<String> userIdList = RongIMClient.getInstance().getRealTimeLocationParticipants(mConversationType, mTargetId);
            mParticipantChangedListener.onParticipantChanged(userIdList);
        }
    }

    private void sendOnMyLocationChanged(AMapLocationInfo locInfo) {
        if (mContext != null && mMyLocationChangedListener != null && locInfo != null) {
            mMyLocationChangedListener.onMyLocationChanged(locInfo);
        }
    }

    private void sendOnRealTimeLocationError(RealTimeLocationConstant.RealTimeLocationErrorCode code) {
        if (mContext != null && mLocationChangedListener != null) {
            mLocationChangedListener.onError(code);
        }
        if (mParticipantChangedListener != null) {
            mParticipantChangedListener.onErrorException();
        }
    }

    private void sendOnLocationChanged(double latitude, double longitude, String userId) {
        if (mContext != null && mLocationChangedListener != null) {
            mLocationChangedListener.onLocationChanged(latitude, longitude, userId);
        }
    }

    private void sendOnParticipantJoinSharing(String userId) {
        if (mContext != null && mLocationChangedListener != null) {
            mLocationChangedListener.onParticipantJoinSharing(userId);
        }
    }

    private void sendOnParticipantQuitSharing(String userId) {
        if (mContext != null && mLocationChangedListener != null) {
            mLocationChangedListener.onParticipantQuitSharing(userId);
        }
    }

    private class LoopThread extends Thread {
        private boolean mLooping;
        private int mMilSec;

        public LoopThread(int sec) {
            mMilSec = sec * 1000;
        }

        public void stopLooping() {
            mLooping = false;
        }

        @Override
        public void run() {
            mLooping = true;
            while (mLooping) {
                if (mLocationClient == null) {
                    mLocationClient = new AMapNetworkLocationClient(mContext);
                    mLocationClient.setApiKey("8bc77f14ab831d62baaf2ed17fb798a4");
                }
                String locStr = mLocationClient.getNetworkLocation();
                RLog.d(TAG, "LoopThread location: " + locStr);
                if (!TextUtils.isEmpty(locStr)) {
                    AMapLocationInfo locInfo = mLocationParser.parserApsJsonResp(locStr);
                    RealTimeLocationType realTimeLocationType;
                    if (locInfo.getCoord() == 1) {
                        realTimeLocationType = RealTimeLocationType.GCJ02;
                    } else {
                        realTimeLocationType = RealTimeLocationType.WGS84;
                    }
                    RongIMClient.getInstance().updateRealTimeLocationStatus(mConversationType, mTargetId,
                            locInfo.getLat(), locInfo.getLng(), realTimeLocationType);
                    mMyLastLatLng = locInfo;
                }
                try {
                    sleep(mMilSec);
                } catch (InterruptedException e) {
                    RLog.e(TAG, "LoopThread run", e);
                    // Restore interrupted state...
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private class MyLocationThread extends Thread {
        @Override
        public void run() {
            if (mLocationClient == null) {
                mLocationClient = new AMapNetworkLocationClient(mContext);
                mLocationClient.setApiKey("8bc77f14ab831d62baaf2ed17fb798a4");
            }
            String locStr = mLocationClient.getNetworkLocation();
            RLog.d(TAG, "MyLocationThread location: " + locStr);
            AMapLocationInfo locInfo = mLocationParser.parserApsJsonResp(locStr);
            sendOnMyLocationChanged(locInfo);
            mMyLastLatLng = locInfo;
        }
    }
}
