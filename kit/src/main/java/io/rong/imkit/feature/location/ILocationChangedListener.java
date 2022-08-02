package io.rong.imkit.feature.location;

import io.rong.imlib.location.RealTimeLocationConstant;

public interface ILocationChangedListener {

    void onLocationChanged(double latitude, double longitude, String userId);

    void onParticipantJoinSharing(String userId);

    void onParticipantQuitSharing(String userId);

    void onSharingTerminated();

    void onError(RealTimeLocationConstant.RealTimeLocationErrorCode code);
}

