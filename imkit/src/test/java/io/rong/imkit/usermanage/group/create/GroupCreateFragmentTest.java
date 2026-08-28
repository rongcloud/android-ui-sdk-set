package io.rong.imkit.usermanage.group.create;

import static org.junit.Assert.assertEquals;

import io.rong.imkit.R;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import org.junit.Test;

public class GroupCreateFragmentTest {

    @Test
    public void createGroupLimitErrorsUseDedicatedMessages() {
        assertEquals(
                R.string.rc_group_app_count_exceed_limit,
                GroupCreateFragment.getCreateGroupLimitMessageResId(
                        CoreErrorCode.RC_GROUP_COUNT_EXCEED_LIMIT));
        assertEquals(
                R.string.rc_group_members_count_exceed_limit,
                GroupCreateFragment.getCreateGroupLimitMessageResId(
                        CoreErrorCode.RC_GROUP_MEMBERS_COUNT_EXCEED_LIMIT));
    }

    @Test
    public void unrelatedErrorDoesNotUseGroupLimitMessage() {
        assertEquals(
                0,
                GroupCreateFragment.getCreateGroupLimitMessageResId(
                        CoreErrorCode.RC_NETWORK_IS_DOWN_OR_UNREACHABLE));
    }
}
