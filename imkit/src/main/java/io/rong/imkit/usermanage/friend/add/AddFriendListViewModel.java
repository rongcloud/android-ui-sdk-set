package io.rong.imkit.usermanage.friend.add;

import static io.rong.imkit.usermanage.handler.FriendInfoHandler.KEY_SEARCH_USER;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import io.rong.imkit.base.BaseViewModel;
import io.rong.imkit.usermanage.handler.FriendInfoHandler;
import io.rong.imlib.model.UserProfile;

/**
 * 功能描述: 创建群组ViewModel
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class AddFriendListViewModel extends BaseViewModel {
    private final MutableLiveData<UserProfile> userProfileLiveData = new MutableLiveData<>();
    private final FriendInfoHandler friendInfoHandler;

    public AddFriendListViewModel(@NonNull Bundle arguments) {
        super(arguments);
        friendInfoHandler = new FriendInfoHandler();
        friendInfoHandler.addDataChangeListener(
                KEY_SEARCH_USER,
                new SafeDataHandler<UserProfile>() {
                    @Override
                    public void onDataChange(UserProfile data) {
                        userProfileLiveData.postValue(data);
                    }
                });
    }

    public MutableLiveData<UserProfile> getUserProfileLiveData() {
        return userProfileLiveData;
    }

    public void findUser(String uniqueId) {
        friendInfoHandler.findUser(uniqueId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        friendInfoHandler.stop();
    }
}
