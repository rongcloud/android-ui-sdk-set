package io.rong.imkit.feature.location;

import java.util.List;

public interface IRealTimeLocationStateListener {

    void onParticipantChanged(List<String> userIdList);

    void onErrorException();
}
