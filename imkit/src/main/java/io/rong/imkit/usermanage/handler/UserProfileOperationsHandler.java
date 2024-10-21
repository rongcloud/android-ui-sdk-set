package io.rong.imkit.usermanage.handler;

import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.UserProfile;
import java.util.Map;

/**
 * 用户详情
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class UserProfileOperationsHandler extends MultiDataHandler {

    public static final DataKey<Boolean> KEY_UPDATE_MY_USER_PROFILE =
            DataKey.obtain("KEY_UPDATE_MY_USER_PROFILE", Boolean.class);

    public static final DataKey<Boolean> KEY_SET_FRIEND_INFO =
            DataKey.obtain("KEY_SET_FRIEND_INFO", Boolean.class);

    /**
     * 更新用户信息
     *
     * @param userProfile 用户信息
     */
    public void updateMyUserProfile(UserProfile userProfile) {
        RongCoreClient.getInstance()
                .updateMyUserProfile(
                        userProfile,
                        new IRongCoreCallback.UpdateUserProfileCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_UPDATE_MY_USER_PROFILE, true);
                            }

                            @Override
                            public void onError(int errorCode, String errorData) {
                                notifyDataChange(KEY_UPDATE_MY_USER_PROFILE, false);
                                notifyDataError(
                                        KEY_UPDATE_MY_USER_PROFILE,
                                        IRongCoreEnum.CoreErrorCode.valueOf(errorCode),
                                        errorData);
                            }
                        });
    }

    /**
     * 设置好友信息
     *
     * @param userId 用户ID
     * @param remark 备注
     * @param extProfile 扩展信息
     */
    public void setFriendInfo(
            final String userId, final String remark, final Map<String, String> extProfile) {
        RongCoreClient.getInstance()
                .setFriendInfo(
                        userId,
                        remark,
                        extProfile,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_SET_FRIEND_INFO, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_SET_FRIEND_INFO, false);
                                notifyDataError(KEY_SET_FRIEND_INFO, coreErrorCode);
                            }
                        });
    }

    @Override
    public void stop() {
        super.stop();
    }
}
