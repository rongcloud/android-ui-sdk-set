package io.rong.imkit.usermanage.handler;

import static org.junit.Assert.assertEquals;

import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class GroupApplicationOperationsHandlerTest {

    @Test
    public void acceptInviteCallbacksStayBoundToTheirOriginatingRequests() {
        AtomicReference<CoreErrorCode> firstResult = new AtomicReference<>();
        AtomicReference<CoreErrorCode> secondResult = new AtomicReference<>();
        GroupApplicationOperationsHandler handler = new GroupApplicationOperationsHandler();

        IRongCoreCallback.OperationCallback firstCallback =
                handler.createAcceptGroupInviteCallback(firstResult::set);
        IRongCoreCallback.OperationCallback secondCallback =
                handler.createAcceptGroupInviteCallback(secondResult::set);

        secondCallback.onError(CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT);
        firstCallback.onSuccess();

        assertEquals(CoreErrorCode.SUCCESS, firstResult.get());
        assertEquals(CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT, secondResult.get());
    }

    @Test
    public void acceptApplicationCallbacksStayBoundToTheirOriginatingRequests() {
        AtomicReference<CoreErrorCode> firstResult = new AtomicReference<>();
        AtomicReference<CoreErrorCode> secondResult = new AtomicReference<>();
        GroupApplicationOperationsHandler handler = new GroupApplicationOperationsHandler();
        OnDataChangeListener<CoreErrorCode> firstListener = firstResult::set;
        OnDataChangeListener<CoreErrorCode> secondListener = secondResult::set;

        IRongCoreCallback.ResultCallback<CoreErrorCode> firstCallback =
                handler.createAcceptGroupApplicationCallback(firstListener);
        IRongCoreCallback.ResultCallback<CoreErrorCode> secondCallback =
                handler.createAcceptGroupApplicationCallback(secondListener);

        firstCallback.onError(CoreErrorCode.RC_NET_UNAVAILABLE);
        secondCallback.onSuccess(CoreErrorCode.SUCCESS);

        assertEquals(CoreErrorCode.RC_NET_UNAVAILABLE, firstResult.get());
        assertEquals(CoreErrorCode.SUCCESS, secondResult.get());
    }

    @Test
    public void refuseInviteCallbacksStayBoundToTheirOriginatingRequests() {
        AtomicReference<Boolean> firstResult = new AtomicReference<>();
        AtomicReference<Boolean> secondResult = new AtomicReference<>();
        GroupApplicationOperationsHandler handler = new GroupApplicationOperationsHandler();

        IRongCoreCallback.OperationCallback firstCallback =
                handler.createRefuseGroupInviteCallback(firstResult::set);
        IRongCoreCallback.OperationCallback secondCallback =
                handler.createRefuseGroupInviteCallback(secondResult::set);

        secondCallback.onError(CoreErrorCode.RC_NET_UNAVAILABLE);
        firstCallback.onSuccess();

        assertEquals(Boolean.TRUE, firstResult.get());
        assertEquals(Boolean.FALSE, secondResult.get());
    }

    @Test
    public void refuseApplicationCallbacksStayBoundToTheirOriginatingRequests() {
        AtomicReference<Boolean> firstResult = new AtomicReference<>();
        AtomicReference<Boolean> secondResult = new AtomicReference<>();
        GroupApplicationOperationsHandler handler = new GroupApplicationOperationsHandler();

        IRongCoreCallback.OperationCallback firstCallback =
                handler.createRefuseGroupApplicationCallback(firstResult::set);
        IRongCoreCallback.OperationCallback secondCallback =
                handler.createRefuseGroupApplicationCallback(secondResult::set);

        firstCallback.onError(CoreErrorCode.RC_NET_UNAVAILABLE);
        secondCallback.onSuccess();

        assertEquals(Boolean.FALSE, firstResult.get());
        assertEquals(Boolean.TRUE, secondResult.get());
    }

    @Test
    public void callbacksDoNotNotifyAfterHandlerStops() {
        AtomicInteger notificationCount = new AtomicInteger();
        GroupApplicationOperationsHandler handler = new GroupApplicationOperationsHandler();
        IRongCoreCallback.OperationCallback acceptInviteCallback =
                handler.createAcceptGroupInviteCallback(
                        result -> notificationCount.incrementAndGet());
        IRongCoreCallback.ResultCallback<CoreErrorCode> acceptApplicationCallback =
                handler.createAcceptGroupApplicationCallback(
                        result -> notificationCount.incrementAndGet());
        IRongCoreCallback.OperationCallback refuseInviteCallback =
                handler.createRefuseGroupInviteCallback(
                        result -> notificationCount.incrementAndGet());
        IRongCoreCallback.OperationCallback refuseApplicationCallback =
                handler.createRefuseGroupApplicationCallback(
                        result -> notificationCount.incrementAndGet());

        assertEquals(4, handler.getPendingRequestListenerCount());

        handler.stop();
        acceptInviteCallback.onSuccess();
        acceptApplicationCallback.onSuccess(CoreErrorCode.SUCCESS);
        refuseInviteCallback.onSuccess();
        refuseApplicationCallback.onError(CoreErrorCode.RC_NET_UNAVAILABLE);

        assertEquals(0, notificationCount.get());
        assertEquals(0, handler.getPendingRequestListenerCount());
    }

    @Test
    public void terminalCallbackReleasesListenerAndOnlyNotifiesOnce() {
        AtomicReference<CoreErrorCode> result = new AtomicReference<>();
        GroupApplicationOperationsHandler handler = new GroupApplicationOperationsHandler();
        IRongCoreCallback.OperationCallback callback =
                handler.createAcceptGroupInviteCallback(result::set);

        callback.onSuccess();
        callback.onError(CoreErrorCode.RC_NET_UNAVAILABLE);

        assertEquals(CoreErrorCode.SUCCESS, result.get());
        assertEquals(0, handler.getPendingRequestListenerCount());
    }
}
