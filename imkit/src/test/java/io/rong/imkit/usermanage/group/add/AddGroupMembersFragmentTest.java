package io.rong.imkit.usermanage.group.add;

import static org.junit.Assert.assertEquals;

import io.rong.imkit.R;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import org.junit.Test;

public class AddGroupMembersFragmentTest {

    @Test
    public void groupMemberCountLimitUsesInvitationFailureMessage() {
        assertEquals(
                R.string.rc_group_members_count_exceed_limit_invite,
                AddGroupMembersFragment.getInviteGroupLimitMessageResId(
                        CoreErrorCode.RC_GROUP_MEMBERS_COUNT_EXCEED_LIMIT));
    }

    @Test
    public void groupJoinLimitKeepsExistingFailedUserHandling() {
        assertEquals(
                0,
                AddGroupMembersFragment.getInviteGroupLimitMessageResId(
                        CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT));
    }
}
