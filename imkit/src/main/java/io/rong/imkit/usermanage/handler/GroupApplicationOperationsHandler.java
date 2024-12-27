package io.rong.imkit.usermanage.handler;

import io.rong.imkit.base.MultiDataHandler;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;

/**
 * 群组申请操作
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationOperationsHandler extends MultiDataHandler {

    /** 用于同意入群邀请的操作 */
    public static final DataKey<Boolean> KEY_ACCEPT_GROUP_INVITE =
            DataKey.obtain("KEY_ACCEPT_GROUP_INVITE", Boolean.class);

    /** 用于拒绝入群邀请的操作 */
    public static final DataKey<Boolean> KEY_REFUSE_GROUP_INVITE =
            DataKey.obtain("KEY_REFUSE_GROUP_INVITE", Boolean.class);

    /** 用于同意入群申请的操作 */
    public static final DataKey<IRongCoreEnum.CoreErrorCode> KEY_ACCEPT_GROUP_APPLICATION =
            DataKey.obtain("KEY_ACCEPT_GROUP_APPLICATION", IRongCoreEnum.CoreErrorCode.class);

    /** 用于拒绝入群申请的操作 */
    public static final DataKey<Boolean> KEY_REFUSE_GROUP_APPLICATION =
            DataKey.obtain("KEY_REFUSE_GROUP_APPLICATION", Boolean.class);

    public GroupApplicationOperationsHandler() {}

    /**
     * 接受群邀请
     *
     * @param groupId 群组ID
     * @param inviterId 邀请人ID
     */
    public void acceptGroupInvite(String groupId, String inviterId) {
        RongCoreClient.getInstance()
                .acceptGroupInvite(
                        groupId,
                        inviterId,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_ACCEPT_GROUP_INVITE, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_ACCEPT_GROUP_INVITE, false);
                                notifyDataError(KEY_ACCEPT_GROUP_INVITE, coreErrorCode);
                            }
                        });
    }

    /**
     * 拒绝群邀请
     *
     * @param groupId 群组ID
     * @param inviterId 邀请人ID
     * @param reason 拒绝原因
     */
    public void refuseGroupInvite(String groupId, String inviterId, String reason) {
        RongCoreClient.getInstance()
                .refuseGroupInvite(
                        groupId,
                        inviterId,
                        reason,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_REFUSE_GROUP_INVITE, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_REFUSE_GROUP_INVITE, false);
                                notifyDataError(KEY_REFUSE_GROUP_INVITE, coreErrorCode);
                            }
                        });
    }

    /**
     * 接受群申请
     *
     * @param groupId 群组ID
     * @param inviterId 邀请人ID
     * @param applicantId 申请人ID
     */
    public void acceptGroupApplication(String groupId, String inviterId, String applicantId) {
        RongCoreClient.getInstance()
                .acceptGroupApplication(
                        groupId,
                        inviterId,
                        applicantId,
                        new IRongCoreCallback.ResultCallback<IRongCoreEnum.CoreErrorCode>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.CoreErrorCode errorCode) {
                                // 同意加入群组请求成功
                                notifyDataChange(KEY_ACCEPT_GROUP_APPLICATION, errorCode);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_ACCEPT_GROUP_APPLICATION, coreErrorCode);
                                notifyDataError(KEY_ACCEPT_GROUP_APPLICATION, coreErrorCode);
                            }
                        });
    }

    /**
     * 拒绝群申请
     *
     * @param groupId 群组ID
     * @param inviterId 邀请人ID
     * @param applicantId 申请人ID
     * @param reason 拒绝原因
     */
    public void refuseGroupApplication(
            String groupId, String inviterId, String applicantId, String reason) {
        RongCoreClient.getInstance()
                .refuseGroupApplication(
                        groupId,
                        inviterId,
                        applicantId,
                        reason,
                        new IRongCoreCallback.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                notifyDataChange(KEY_REFUSE_GROUP_APPLICATION, true);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                notifyDataChange(KEY_REFUSE_GROUP_APPLICATION, false);
                                notifyDataError(KEY_REFUSE_GROUP_APPLICATION, coreErrorCode);
                            }
                        });
    }
}
