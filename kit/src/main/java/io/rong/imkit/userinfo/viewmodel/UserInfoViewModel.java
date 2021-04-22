package io.rong.imkit.userinfo.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import io.rong.imkit.IMCenter;

import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public class UserInfoViewModel extends AndroidViewModel {
    private final String TAG = UserInfoViewModel.class.getSimpleName();

    public UserInfoViewModel(@NonNull Application application) {
        super(application);
        IMCenter.getInstance().addOnReceiveMessageListener(new RongIMClient.OnReceiveMessageWrapperListener() {
            @Override
            public boolean onReceived(Message message, int left, boolean hasPackage, boolean offline) {
                onReceivedMessage(message, left, hasPackage, offline);
                return false;
            }
        });
    }

    public LiveData<List<User>> getAllUsers() {
        return RongUserInfoManager.getInstance().getAllUsersLiveData();
    }

    private void onReceivedMessage(Message message, int left, boolean hasPackage, boolean offline) {
        if (message != null && message.getContent() != null && message.getContent().getUserInfo() != null
                && RongUserInfoManager.getInstance().getUserDatabase() != null) {
            final UserInfo userInfo = message.getContent().getUserInfo();
            ExecutorHelper.getInstance().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    RongUserInfoManager.getInstance().getUserDatabase().getUserDao().insertUser(new User(userInfo));
                }
            });
        }
    }
}
