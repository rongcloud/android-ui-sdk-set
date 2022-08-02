package io.rong.imkit.notification;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import io.rong.imkit.model.OperationResult;
import io.rong.imlib.RongIMClient;

public class RongNotificationViewModel extends AndroidViewModel {
    MutableLiveData<OperationResult> mOperationResult;
    MutableLiveData<NotificationQuietInfo> mQuietInfoLiveData;

    public RongNotificationViewModel(@NonNull Application application) {
        super(application);
        mOperationResult = new MutableLiveData<>();
        mQuietInfoLiveData = new MutableLiveData<>();
    }

    public void setNotificationQuietHours(String startTime, int spanMinutes) {
        RongNotificationManager.getInstance().setNotificationQuietHours(startTime, spanMinutes, new RongIMClient.OperationCallback() {
            @Override
            public void onSuccess() {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.SET_NOTIFICATION_QUIET_HOURS, OperationResult.SUCCESS));
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {
                mOperationResult.postValue(new OperationResult(OperationResult.Action.SET_NOTIFICATION_STATUS, errorCode.getValue()));
            }
        });
    }

    /**
     * 获取会话通知免打扰时间。
     *
     * @param callback 获取会话通知免打扰时间回调。
     */
    public void getNotificationQuietHours(final RongIMClient.GetNotificationQuietHoursCallback callback){
        RongNotificationManager.getInstance().getNotificationQuietHours(callback);
    }

    public MutableLiveData<OperationResult> getOperationResult(){
        return mOperationResult;
    }

    public MutableLiveData<NotificationQuietInfo> getQuietInfoLiveData() {
        return mQuietInfoLiveData;
    }

    public class NotificationQuietInfo{
        String startTime;
        int spanMinutes;

        public NotificationQuietInfo(String startTime, int spanMinutes) {
            this.startTime = startTime;
            this.spanMinutes = spanMinutes;
        }
    }
}
