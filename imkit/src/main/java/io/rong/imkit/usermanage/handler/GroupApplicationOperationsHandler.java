package io.rong.imkit.usermanage.handler;

import androidx.annotation.NonNull;
import io.rong.imkit.base.MultiDataHandler;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群组申请操作
 *
 * <p>注意：使用完毕后需要调用 {@link #stop()} 方法释放资源
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationOperationsHandler extends MultiDataHandler {

    private final Set<ListenerHolder<?>> requestListeners = ConcurrentHashMap.newKeySet();

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

    @Override
    public void stop() {
        super.stop();
        synchronized (requestListeners) {
            for (ListenerHolder<?> listenerHolder : requestListeners) {
                listenerHolder.clear();
            }
            requestListeners.clear();
        }
    }

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

    /** 接受群邀请，并将结果返回给当前请求对应的监听器。 @since 5.46.0 */
    public void acceptGroupInviteWithResult(
            String groupId,
            String inviterId,
            @NonNull OnDataChangeListener<IRongCoreEnum.CoreErrorCode> listener) {
        RongCoreClient.getInstance()
                .acceptGroupInvite(groupId, inviterId, createAcceptGroupInviteCallback(listener));
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

    /** 拒绝群邀请，并将结果返回给当前请求对应的监听器。 @since 5.46.0 */
    public void refuseGroupInvite(
            String groupId,
            String inviterId,
            String reason,
            @NonNull OnDataChangeListener<Boolean> listener) {
        RongCoreClient.getInstance()
                .refuseGroupInvite(
                        groupId, inviterId, reason, createRefuseGroupInviteCallback(listener));
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

    /** 接受群申请，并将结果返回给当前请求对应的监听器。 @since 5.46.0 */
    public void acceptGroupApplication(
            String groupId,
            String inviterId,
            String applicantId,
            @NonNull OnDataChangeListener<IRongCoreEnum.CoreErrorCode> listener) {
        RongCoreClient.getInstance()
                .acceptGroupApplication(
                        groupId,
                        inviterId,
                        applicantId,
                        createAcceptGroupApplicationCallback(listener));
    }

    IRongCoreCallback.OperationCallback createAcceptGroupInviteCallback(
            @NonNull OnDataChangeListener<IRongCoreEnum.CoreErrorCode> listener) {
        ListenerHolder<IRongCoreEnum.CoreErrorCode> listenerHolder = registerListener(listener);
        return new IRongCoreCallback.OperationCallback() {
            @Override
            public void onSuccess() {
                notifyRequestResult(listenerHolder, IRongCoreEnum.CoreErrorCode.SUCCESS);
            }

            @Override
            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                notifyRequestResult(listenerHolder, coreErrorCode);
            }
        };
    }

    IRongCoreCallback.ResultCallback<IRongCoreEnum.CoreErrorCode>
            createAcceptGroupApplicationCallback(
                    @NonNull OnDataChangeListener<IRongCoreEnum.CoreErrorCode> listener) {
        ListenerHolder<IRongCoreEnum.CoreErrorCode> listenerHolder = registerListener(listener);
        return new IRongCoreCallback.ResultCallback<IRongCoreEnum.CoreErrorCode>() {
            @Override
            public void onSuccess(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                notifyRequestResult(listenerHolder, coreErrorCode);
            }

            @Override
            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                notifyRequestResult(listenerHolder, coreErrorCode);
            }
        };
    }

    IRongCoreCallback.OperationCallback createRefuseGroupInviteCallback(
            @NonNull OnDataChangeListener<Boolean> listener) {
        return createBooleanOperationCallback(listener);
    }

    IRongCoreCallback.OperationCallback createRefuseGroupApplicationCallback(
            @NonNull OnDataChangeListener<Boolean> listener) {
        return createBooleanOperationCallback(listener);
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

    /** 拒绝群申请，并将结果返回给当前请求对应的监听器。 @since 5.46.0 */
    public void refuseGroupApplication(
            String groupId,
            String inviterId,
            String applicantId,
            String reason,
            @NonNull OnDataChangeListener<Boolean> listener) {
        RongCoreClient.getInstance()
                .refuseGroupApplication(
                        groupId,
                        inviterId,
                        applicantId,
                        reason,
                        createRefuseGroupApplicationCallback(listener));
    }

    private IRongCoreCallback.OperationCallback createBooleanOperationCallback(
            @NonNull OnDataChangeListener<Boolean> listener) {
        ListenerHolder<Boolean> listenerHolder = registerListener(listener);
        return new IRongCoreCallback.OperationCallback() {
            @Override
            public void onSuccess() {
                notifyRequestResult(listenerHolder, true);
            }

            @Override
            public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                notifyRequestResult(listenerHolder, false);
            }
        };
    }

    private <T> ListenerHolder<T> registerListener(@NonNull OnDataChangeListener<T> listener) {
        ListenerHolder<T> listenerHolder = new ListenerHolder<>(listener);
        synchronized (requestListeners) {
            if (isAlive()) {
                requestListeners.add(listenerHolder);
            } else {
                listenerHolder.clear();
            }
        }
        return listenerHolder;
    }

    private <T> void notifyRequestResult(ListenerHolder<T> listenerHolder, T result) {
        OnDataChangeListener<T> listener = listenerHolder.take();
        requestListeners.remove(listenerHolder);
        if (listener != null && isAlive()) {
            listener.onDataChange(result);
        }
    }

    int getPendingRequestListenerCount() {
        return requestListeners.size();
    }

    private static final class ListenerHolder<T> {
        private volatile OnDataChangeListener<T> listener;

        ListenerHolder(OnDataChangeListener<T> listener) {
            this.listener = listener;
        }

        synchronized OnDataChangeListener<T> take() {
            OnDataChangeListener<T> currentListener = listener;
            listener = null;
            return currentListener;
        }

        synchronized void clear() {
            listener = null;
        }
    }
}
