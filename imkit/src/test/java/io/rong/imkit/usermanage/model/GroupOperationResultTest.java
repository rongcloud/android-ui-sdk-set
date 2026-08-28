package io.rong.imkit.usermanage.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.model.CreateGroupErrorInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.InviteUsersToGroupErrorInfo;
import java.util.Collections;
import org.junit.Test;

public class GroupOperationResultTest {

    @Test
    public void createAndInviteUseIndependentResultTypes() {
        assertNotEquals(CreateGroupOperationResult.class, InviteUsersToGroupOperationResult.class);
    }

    @Test
    public void createResultSeparatesSuccessProcessCodeFromFailureDetail() {
        CreateGroupOperationResult success =
                CreateGroupOperationResult.success(CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT);
        CreateGroupErrorInfo errorInfo =
                new CreateGroupErrorInfo(Collections.singletonList("groupName"), null);
        CreateGroupOperationResult failure =
                CreateGroupOperationResult.error(
                        CoreErrorCode.RC_SERVICE_INFORMATION_AUDIT_FAILED, errorInfo);

        assertTrue(success.isSuccess());
        assertEquals(CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT, success.getCode());
        assertNull(success.getErrorInfo());
        assertFalse(failure.isSuccess());
        assertEquals(CoreErrorCode.RC_SERVICE_INFORMATION_AUDIT_FAILED, failure.getCode());
        assertEquals(errorInfo, failure.getErrorInfo());
    }

    @Test
    public void inviteFailureCarriesOnlyInviteErrorInfo() {
        InviteUsersToGroupErrorInfo errorInfo =
                new InviteUsersToGroupErrorInfo(
                        Collections.singletonList(new GroupMemberInfo("user-1", "Alice")));
        InviteUsersToGroupOperationResult result =
                InviteUsersToGroupOperationResult.error(
                        CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT, errorInfo);

        assertFalse(result.isSuccess());
        assertEquals(CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT, result.getCode());
        assertEquals(errorInfo, result.getErrorInfo());
    }
}
