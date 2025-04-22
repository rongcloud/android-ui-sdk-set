package io.rong.imkit.usermanage.handler;

import io.rong.imkit.IMCenter;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.UserProfile;
import java.util.List;
import java.util.Map;

/**
 * 用户信息操作处理类
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UserProfileOperationsHandler extends MultiDataHandler {

    public static final DataKey<Boolean> KEY_UPDATE_MY_USER_PROFILE =
            DataKey.obtain("KEY_UPDATE_MY_USER_PROFILE", Boolean.class);

    public static final DataKey<Boolean> KEY_UPDATE_MY_USER_PROFILE_EXAMINE =
            DataKey.obtain("KEY_UPDATE_MY_USER_PROFILE_EXAMINE", Boolean.class);

    public static final DataKey<Boolean> KEY_SET_FRIEND_INFO =
            DataKey.obtain("KEY_SET_FRIEND_INFO", Boolean.class);

    public static final DataKey<Boolean> KEY_SET_FRIEND_INFO_EXMAINE =
            DataKey.obtain("KEY_SET_FRIEND_INFO_EXMAINE", Boolean.class);

    /**
     * 更新用户信息
     *
     * @param userProfile 用户信息
     */
    @Deprecated
    public void updateMyUserProfile(UserProfile userProfile) {
        IMCenter.getInstance()
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
     * 更新用户信息
     *
     * @param userProfile 用户信息
     */
    public void updateMyUserProfileExamine(UserProfile userProfile) {
        IMCenter.getInstance()
                .updateMyUserProfile(
                        userProfile,
                        new IRongCoreCallback.UpdateUserProfileEnhancedCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_UPDATE_MY_USER_PROFILE_EXAMINE, true);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode errorCode, List<String> errorData) {
                                notifyDataError(
                                        KEY_UPDATE_MY_USER_PROFILE_EXAMINE, errorCode, errorData);
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
    @Deprecated
    public void setFriendInfo(
            final String userId, final String remark, final Map<String, String> extProfile) {
        IMCenter.getInstance()
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

    /**
     * 设置好友信息
     *
     * @param userId 用户ID
     * @param remark 备注
     * @param extProfile 扩展信息
     */
    public void setFriendInfoExamine(
            final String userId, final String remark, final Map<String, String> extProfile) {
        IMCenter.getInstance()
                .setFriendInfo(
                        userId,
                        remark,
                        extProfile,
                        new IRongCoreCallback.ExamineOperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_SET_FRIEND_INFO_EXMAINE, true);
                            }

                            @Override
                            public void onError(
                                    IRongCoreEnum.CoreErrorCode coreErrorCode,
                                    List<String> errorKeys) {
                                notifyDataError(
                                        KEY_SET_FRIEND_INFO_EXMAINE, coreErrorCode, errorKeys);
                            }
                        });
    }
}
