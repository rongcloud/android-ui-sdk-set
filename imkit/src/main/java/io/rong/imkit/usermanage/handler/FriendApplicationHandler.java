package io.rong.imkit.usermanage.handler;

import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.FriendApplicationInfo;
import io.rong.imlib.model.FriendApplicationStatus;
import io.rong.imlib.model.FriendApplicationType;
import io.rong.imlib.model.PagingQueryOption;
import io.rong.imlib.model.PagingQueryResult;

/**
 * 好友申请处理类
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class FriendApplicationHandler extends MultiDataHandler {

    public static final DataKey<PagingQueryResult> KEY_GET_FRIEND_APPLICATIONS =
            MultiDataHandler.DataKey.obtain("KEY_GET_FRIEND_APPLICATIONS", PagingQueryResult.class);
    public static final DataKey<Boolean> KEY_ACCEPT_FRIEND_APPLICATIONS =
            MultiDataHandler.DataKey.obtain("KEY_ACCEPT_FRIEND_APPLICATIONS", Boolean.class);

    public static final DataKey<Boolean> KEY_REJECT_FRIEND_APPLICATIONS =
            MultiDataHandler.DataKey.obtain("KEY_REJECT_FRIEND_APPLICATIONS", Boolean.class);

    public void getFriendApplications(
            PagingQueryOption option,
            FriendApplicationType[] type,
            FriendApplicationStatus[] status) {

        RongCoreClient.getInstance()
                .getFriendApplications(
                        option,
                        type,
                        status,
                        new IRongCoreCallback.PageResultCallback<FriendApplicationInfo>() {
                            @Override
                            public void onSuccess(PagingQueryResult<FriendApplicationInfo> result) {
                                notifyDataChange(KEY_GET_FRIEND_APPLICATIONS, result);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {}
                        });
    }

    public void acceptFriendApplication(String userId, OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_ACCEPT_FRIEND_APPLICATIONS, listener);
        RongCoreClient.getInstance()
                .acceptFriendApplication(
                        userId,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_ACCEPT_FRIEND_APPLICATIONS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_ACCEPT_FRIEND_APPLICATIONS, false);
                            }
                        });
    }

    public void refuseFriendApplication(String userId, OnDataChangeListener<Boolean> listener) {
        replaceDataChangeListener(KEY_REJECT_FRIEND_APPLICATIONS, listener);
        RongCoreClient.getInstance()
                .refuseFriendApplication(
                        userId,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_REJECT_FRIEND_APPLICATIONS, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_REJECT_FRIEND_APPLICATIONS, false);
                            }
                        });
    }
}
