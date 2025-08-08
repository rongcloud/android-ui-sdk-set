package io.rong.imkit.usermanage.friend.apply;

import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.StringRes;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.model.UiFriendApplicationInfo;
import io.rong.imkit.usermanage.handler.FriendApplicationHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.model.FriendApplicationInfo;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 申请好友列表页面ViewModel
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class ApplyFriendViewModel extends BaseViewModel {
    private final MutableLiveData<List<UiFriendApplicationInfo>> mLivedata =
            new MutableLiveData<>();
    private final FriendApplicationHandler friendApplicationHandler;
    private FriendApplicationType[] types = {
        FriendApplicationType.Received, FriendApplicationType.Sent
    };

    FriendApplicationStatus[] status = {
        FriendApplicationStatus.UnHandled,
        FriendApplicationStatus.Expired,
        FriendApplicationStatus.Refused,
        FriendApplicationStatus.Accepted
    };
    private List<UiFriendApplicationInfo> mData = new ArrayList<>();
    private String token;

    private volatile boolean onLoad = false;

    public ApplyFriendViewModel(Bundle bundle) {
        super(bundle);
        friendApplicationHandler = new FriendApplicationHandler();
        friendApplicationHandler.addDataChangeListener(
                FriendApplicationHandler.KEY_GET_FRIEND_APPLICATIONS,
                new OnDataChangeListener<PagingQueryResult>() {
                    @Override
                    public void onDataChange(PagingQueryResult result) {
                        token = result.getPageToken();
                        List<FriendApplicationInfo> data = result.getData();
                        List<UiFriendApplicationInfo> uiData = new ArrayList<>();
                        for (FriendApplicationInfo item : data) {
                            uiData.add(
                                    new UiFriendApplicationInfo(
                                            item, getTimeLabel(item.getOperationTime())));
                        }
                        onLoad = false;
                        mData.addAll(uiData);
                        mLivedata.postValue(mData);
                    }
                });
    }

    /**
     * 获取时间标签
     *
     * @param timestamp 时间戳
     * @return 时间标签
     */
    protected @StringRes int getTimeLabel(long timestamp) {
        long currentTimeMillis = System.currentTimeMillis();
        long result = currentTimeMillis - timestamp;
        if (result <= 86400000) {
            return R.string.rc_just_now;
        } else if (result < 259200000) {
            return R.string.rc_within_three_days;
        } else {
            return R.string.rc_three_days_ago;
        }
    }

    public MutableLiveData<List<UiFriendApplicationInfo>> getFriendApplicationsLiveData() {
        return mLivedata;
    }

    /**
     * @param type 0=all,1=received,2=sent
     */
    public void loadFriendApplications(int type) {
        if (type == 0) {
            types =
                    new FriendApplicationType[] {
                        FriendApplicationType.Received, FriendApplicationType.Sent
                    };
        } else if (type == 1) {
            types = new FriendApplicationType[] {FriendApplicationType.Received};
        } else if (type == 2) {
            types = new FriendApplicationType[] {FriendApplicationType.Sent};
        }
        loadFriendApplications(false);
    }

    public void loadFriendApplications(boolean isLoadMore) {
        if (isLoadMore && TextUtils.isEmpty(token)) {
            mLivedata.postValue(mData);
            return;
        }
        if (onLoad) {
            return;
        }
        onLoad = true;
        PagingQueryOption option;
        if (!isLoadMore) {
            mData.clear();
            option = new PagingQueryOption(null, 20, false);
        } else {
            option = new PagingQueryOption(token, 20, false);
        }
        friendApplicationHandler.getFriendApplications(option, types, status);
    }

    public void acceptFriendApplication(String userId, OnDataChangeListener<Boolean> callback) {
        friendApplicationHandler.acceptFriendApplication(userId, callback);
    }

    public void refuseFriendApplication(String userId, OnDataChangeListener<Boolean> callback) {
        friendApplicationHandler.refuseFriendApplication(userId, callback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendApplicationHandler.stop();
    }
}
