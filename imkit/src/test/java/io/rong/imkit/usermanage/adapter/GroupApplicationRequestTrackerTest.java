package io.rong.imkit.usermanage.adapter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationType;
import org.junit.Test;

public class GroupApplicationRequestTrackerTest {

    @Test
    public void blocksDuplicateRequestUntilOriginalRequestFinishes() {
        GroupApplicationRequestTracker tracker = new GroupApplicationRequestTracker();
        GroupApplicationInfo application = new GroupApplicationInfo();

        assertTrue(tracker.tryStart(application));
        assertTrue(tracker.isPending(application));
        assertFalse(tracker.tryStart(application));

        tracker.finish(application);

        assertFalse(tracker.isPending(application));
        assertTrue(tracker.tryStart(application));
    }

    @Test
    public void allowsDifferentApplicationsToRunConcurrently() {
        GroupApplicationRequestTracker tracker = new GroupApplicationRequestTracker();
        GroupApplicationInfo first = new GroupApplicationInfo();
        first.setGroupId("group-1");
        GroupApplicationInfo second = new GroupApplicationInfo();
        second.setGroupId("group-2");

        assertTrue(tracker.tryStart(first));
        assertTrue(tracker.tryStart(second));
    }

    @Test
    public void tracksRefreshedInstanceOfSameApplicationByStableKey() {
        GroupApplicationRequestTracker tracker = new GroupApplicationRequestTracker();
        GroupApplicationInfo original = new GroupApplicationInfo();
        original.setGroupId("group-1");
        original.setType(GroupApplicationType.Application);
        GroupApplicationInfo refreshed = new GroupApplicationInfo();
        refreshed.setGroupId("group-1");
        refreshed.setType(GroupApplicationType.Application);

        assertTrue(tracker.tryStart(original));
        assertTrue(tracker.isPending(refreshed));

        tracker.finish(original);

        assertFalse(tracker.isPending(refreshed));
    }

    @Test
    public void allowsDifferentApplicationTypesToRunConcurrently() {
        GroupApplicationRequestTracker tracker = new GroupApplicationRequestTracker();
        GroupApplicationInfo application = new GroupApplicationInfo();
        application.setGroupId("group-1");
        application.setType(GroupApplicationType.Application);
        GroupApplicationInfo invitation = new GroupApplicationInfo();
        invitation.setGroupId("group-1");
        invitation.setType(GroupApplicationType.Invitation);

        assertTrue(tracker.tryStart(application));
        assertTrue(tracker.tryStart(invitation));
    }
}
