package io.rong.imkit.usermanage.friend.search;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.IMCenter;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.FriendInfoHandler;
import io.rong.imlib.listener.FriendEventListener;
import io.rong.imlib.model.DirectionType;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.FriendInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索好友ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class FriendSearchViewModel extends BaseViewModel {
    private final MutableLiveData<List<FriendInfo>> friendInfoLiveData = new MutableLiveData<>();
    private final FriendInfoHandler friendInfoHandler;
    private String query;
    private final FriendEventListener listener =
            new FriendEventListener() {
                @Override
                public void onFriendAdd(
                        DirectionType directionType,
                        String userId,
                        String userName,
                        String portraitUri,
                        long operationTime) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendDelete(
                        DirectionType directionType, List<String> userIds, long operationTime) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendApplicationStatusChanged(
                        String userId,
                        FriendApplicationType applicationType,
                        FriendApplicationStatus status,
                        DirectionType directionType,
                        long operationTime,
                        String extra) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendCleared(long operationTime) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }

                @Override
                public void onFriendInfoChangedSync(
                        String userId,
                        String remark,
                        Map<String, String> extProfile,
                        long operationTime) {
                    if (!TextUtils.isEmpty(query)) {
                        queryContacts(query);
                    }
                }
            };

    public FriendSearchViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                FriendInfoHandler.KEY_SEARCH_FRIENDS,
                new SafeDataHandler<List<FriendInfo>>() {
                    @Override
                    public void onDataChange(List<FriendInfo> friendInfos) {
                        friendInfoLiveData.postValue(friendInfos);
                    }
                });
        IMCenter.getInstance().addFriendEventListener(listener);
    }

    public MutableLiveData<List<FriendInfo>> getFriendInfoLiveData() {
        return friendInfoLiveData;
    }

    /**
     * 查询联系人
     *
     * @param query 查询关键字
     */
    public void queryContacts(String query) {
        this.query = query;
        if (TextUtils.isEmpty(query)) {
            friendInfoLiveData.postValue(new ArrayList<>());
            return;
        }
        friendInfoHandler.searchFriendsInfo(query);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        IMCenter.getInstance().removeFriendEventListener(listener);
        friendInfoHandler.stop();
    }
}
