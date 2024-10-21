package io.rong.imkit.usermanage.handler;

import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.UserProfile;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class UserProfileHandler extends MultiDataHandler {

    public static final DataKey<UserProfile> KEY_GET_USER_PROFILE =
            DataKey.obtain("KEY_GET_USER_PROFILE", UserProfile.class);

    public static final DataKey<UserProfile> KEY_GET_MY_USER_PROFILE =
            DataKey.obtain("KEY_GET_MY_USER_PROFILE", UserProfile.class);

    /** 获取我的用户信息 */
    public void getMyUserProfile() {
        RongCoreClient.getInstance()
                .getMyUserProfile(
                        new IRongCoreCallback.ResultCallback<UserProfile>() {
                            @Override
                            public void onSuccess(UserProfile userProfile) {
                                notifyDataChange(KEY_GET_MY_USER_PROFILE, userProfile);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GET_MY_USER_PROFILE, e);
                            }
                        });
    }

    /**
     * 获取用户信息
     *
     * @param id 用户ID
     */
    public void getUserProfile(String id) {
        ArrayList<String> idList = new ArrayList<>(1);
        idList.add(id);
        RongCoreClient.getInstance()
                .getUserProfiles(
                        idList,
                        new IRongCoreCallback.ResultCallback<List<UserProfile>>() {
                            @Override
                            public void onSuccess(List<UserProfile> userProfiles) {
                                if (userProfiles != null && !userProfiles.isEmpty()) {
                                    notifyDataChange(KEY_GET_USER_PROFILE, userProfiles.get(0));
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                notifyDataError(KEY_GET_USER_PROFILE, e);
                            }
                        });
    }
}
