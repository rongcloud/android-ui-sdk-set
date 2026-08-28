package io.rong.imkit.notification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RongNotificationManagerTest {
    @Test
    public void shouldUseAppNameWhenUserNameIsUnavailable() {
        assertEquals(
                "SealTalk", RongNotificationManager.resolveNotificationTitle(null, "SealTalk"));
        assertEquals(
                "User name",
                RongNotificationManager.resolveNotificationTitle("User name", "SealTalk"));
    }
}
