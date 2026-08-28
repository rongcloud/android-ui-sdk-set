package io.rong.imkit.usermanage.group.application;

import static org.junit.Assert.assertEquals;

import io.rong.imkit.R;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import org.junit.Test;

public class GroupApplicationsFragmentTest {

    @Test
    public void groupCountLimitUsesCreationFailureMessage() {
        assertEquals(
                R.string.rc_group_app_count_exceed_limit,
                GroupApplicationsFragment.getAcceptFailureMessageResId(
                        CoreErrorCode.RC_GROUP_COUNT_EXCEED_LIMIT));
    }

    @Test
    public void groupMemberCountLimitUsesJoinFailureMessage() {
        assertEquals(
                R.string.rc_group_members_count_exceed_limit_join,
                GroupApplicationsFragment.getAcceptFailureMessageResId(
                        CoreErrorCode.RC_GROUP_MEMBERS_COUNT_EXCEED_LIMIT));
    }

    @Test
    public void unrelatedErrorUsesExistingAcceptFailureMessage() {
        assertEquals(
                R.string.rc_invite_confirm_failed,
                GroupApplicationsFragment.getAcceptFailureMessageResId(
                        CoreErrorCode.RC_NETWORK_IS_DOWN_OR_UNREACHABLE));
    }
}
