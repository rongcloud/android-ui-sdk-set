package io.rong.imkit.usermanage.adapter;

import static org.junit.Assert.assertEquals;

import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationStatus;
import io.rong.imlib.model.GroupApplicationType;
import java.util.Collections;
import org.junit.Test;

public class GroupApplicationsAdapterTest {

    @Test
    public void completionUpdatesRefreshedInstanceOfSameApplication() {
        GroupApplicationInfo original = application("group-1");
        GroupApplicationInfo refreshed = application("group-1");

        int position =
                GroupApplicationsAdapter.applyCompletedStatus(
                        Collections.singletonList(refreshed),
                        original,
                        GroupApplicationStatus.Joined);

        assertEquals(0, position);
        assertEquals(GroupApplicationStatus.Joined, refreshed.getStatus());
    }

    @Test
    public void joinLimitFailureRestoresInviteePendingStatus() {
        GroupApplicationInfo original = application("group-1");
        original.setStatus(GroupApplicationStatus.InviteeUnHandled);
        GroupApplicationInfo refreshed = application("group-1");
        refreshed.setStatus(GroupApplicationStatus.Joined);

        int position =
                GroupApplicationsAdapter.applyAcceptResult(
                        Collections.singletonList(refreshed),
                        original,
                        GroupApplicationStatus.InviteeUnHandled,
                        CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT);

        assertEquals(0, position);
        assertEquals(GroupApplicationStatus.InviteeUnHandled, refreshed.getStatus());
    }

    @Test
    public void joinLimitFailureRestoresManagerPendingStatus() {
        GroupApplicationInfo original = application("group-1");
        original.setStatus(GroupApplicationStatus.ManagerUnHandled);
        GroupApplicationInfo refreshed = application("group-1");
        refreshed.setStatus(GroupApplicationStatus.Joined);

        int position =
                GroupApplicationsAdapter.applyAcceptResult(
                        Collections.singletonList(refreshed),
                        original,
                        GroupApplicationStatus.ManagerUnHandled,
                        CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT);

        assertEquals(0, position);
        assertEquals(GroupApplicationStatus.ManagerUnHandled, refreshed.getStatus());
    }

    @Test
    public void groupCountLimitFailureRestoresInviteePendingStatus() {
        assertAcceptFailureRestoresStatus(
                GroupApplicationStatus.InviteeUnHandled, CoreErrorCode.RC_GROUP_COUNT_EXCEED_LIMIT);
    }

    @Test
    public void groupMemberCountLimitFailureRestoresManagerPendingStatus() {
        assertAcceptFailureRestoresStatus(
                GroupApplicationStatus.ManagerUnHandled,
                CoreErrorCode.RC_GROUP_MEMBERS_COUNT_EXCEED_LIMIT);
    }

    @Test
    public void successfulAcceptStillMarksApplicationAsJoined() {
        GroupApplicationInfo original = application("group-1");
        GroupApplicationInfo refreshed = application("group-1");

        int position =
                GroupApplicationsAdapter.applyAcceptResult(
                        Collections.singletonList(refreshed),
                        original,
                        GroupApplicationStatus.ManagerUnHandled,
                        CoreErrorCode.SUCCESS);

        assertEquals(0, position);
        assertEquals(GroupApplicationStatus.Joined, refreshed.getStatus());
    }

    @Test
    public void acceptRequiringInviteeConfirmationKeepsInviteePendingStatus() {
        GroupApplicationInfo original = application("group-1");
        GroupApplicationInfo refreshed = application("group-1");

        int position =
                GroupApplicationsAdapter.applyAcceptResult(
                        Collections.singletonList(refreshed),
                        original,
                        GroupApplicationStatus.ManagerUnHandled,
                        CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT);

        assertEquals(0, position);
        assertEquals(GroupApplicationStatus.InviteeUnHandled, refreshed.getStatus());
    }

    private static GroupApplicationInfo application(String groupId) {
        GroupApplicationInfo application = new GroupApplicationInfo();
        application.setGroupId(groupId);
        application.setType(GroupApplicationType.Application);
        application.setStatus(GroupApplicationStatus.ManagerUnHandled);
        return application;
    }

    private static void assertAcceptFailureRestoresStatus(
            GroupApplicationStatus pendingStatus, CoreErrorCode errorCode) {
        GroupApplicationInfo original = application("group-1");
        original.setStatus(pendingStatus);
        GroupApplicationInfo refreshed = application("group-1");
        refreshed.setStatus(GroupApplicationStatus.Joined);

        int position =
                GroupApplicationsAdapter.applyAcceptResult(
                        Collections.singletonList(refreshed), original, pendingStatus, errorCode);

        assertEquals(0, position);
        assertEquals(pendingStatus, refreshed.getStatus());
    }
}
