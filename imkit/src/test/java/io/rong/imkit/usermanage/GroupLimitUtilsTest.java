package io.rong.imkit.usermanage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.rong.imkit.model.ContactModel;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class GroupLimitUtilsTest {

    @Test
    public void failedUserNamesPreferPageNameThenNicknameNameAndUserId() {
        GroupMemberInfo pageNamed = new GroupMemberInfo("user-1", "Server Name");
        pageNamed.setNickname("Server Nickname");
        GroupMemberInfo nickname = new GroupMemberInfo("user-2", "Server Name 2");
        nickname.setNickname("Server Nickname 2");
        GroupMemberInfo name = new GroupMemberInfo("user-3", "Server Name 3");
        GroupMemberInfo userId = new GroupMemberInfo("user-4", null);
        Map<String, String> pageNames = new HashMap<>();
        pageNames.put("user-1", "Friend Remark");

        List<String> names =
                GroupLimitUtils.resolveFailedUserNames(
                        Arrays.asList(pageNamed, nickname, name, userId), pageNames);

        assertEquals(
                Arrays.asList("Friend Remark", "Server Nickname 2", "Server Name 3", "user-4"),
                names);
    }

    @Test
    public void friendDisplayNameReturnsOnlyRealPageNames() {
        FriendInfo remark = new FriendInfo();
        remark.setRemark("Friend Remark");
        remark.setName("Friend Name");
        FriendInfo name = new FriendInfo();
        name.setName("Friend Name");
        FriendInfo missing = new FriendInfo();

        assertEquals("Friend Remark", GroupLimitUtils.getFriendDisplayName(remark));
        assertEquals("Friend Name", GroupLimitUtils.getFriendDisplayName(name));
        assertEquals(null, GroupLimitUtils.getFriendDisplayName(missing));
    }

    @Test
    public void pageDisplayNamesStayAssociatedWithTheirInviteeIds() {
        Map<String, String> names =
                GroupLimitUtils.mapDisplayNames(
                        Arrays.asList("user-1", "user-2", "user-3"),
                        Arrays.asList("Friend Remark", "", "Friend Name"));

        assertEquals("Friend Remark", names.get("user-1"));
        assertFalse(names.containsKey("user-2"));
        assertEquals("Friend Name", names.get("user-3"));
    }

    @Test
    public void listFormattingUsesLocaleProvidedPatterns() {
        assertEquals(
                "Alice、Bob、Carol",
                GroupLimitUtils.formatNameList(
                        Arrays.asList("Alice", "Bob", "Carol"),
                        "%1$s、%2$s",
                        "%1$s、%2$s",
                        "%1$s、%2$s"));
        assertEquals(
                "Alice and Bob",
                GroupLimitUtils.formatNameList(
                        Arrays.asList("Alice", "Bob"),
                        "%1$s and %2$s",
                        "%1$s, %2$s",
                        "%1$s, and %2$s"));
        assertEquals(
                "Alice, Bob, and Carol",
                GroupLimitUtils.formatNameList(
                        Arrays.asList("Alice", "Bob", "Carol"),
                        "%1$s and %2$s",
                        "%1$s, %2$s",
                        "%1$s, and %2$s"));
    }

    @Test
    public void failedContactStateKeepsSelectionAndCanBeCleared() {
        FriendInfo friendInfo = new FriendInfo();
        friendInfo.setUserId("user-1");
        ContactModel<FriendInfo> contact =
                ContactModel.obtain(
                        friendInfo, ContactModel.ItemType.CONTENT, ContactModel.CheckType.CHECKED);

        GroupLimitUtils.updateFailedContactStates(
                Collections.singletonList(contact), Collections.singleton("user-1"));
        assertEquals(ContactModel.CheckType.FAILED, contact.getCheckType());
        assertTrue(GroupLimitUtils.isSelected(contact));

        GroupLimitUtils.updateFailedContactStates(
                Collections.singletonList(contact), Collections.emptySet());
        assertEquals(ContactModel.CheckType.CHECKED, contact.getCheckType());
    }

    @Test
    public void groupApplicationOnlyUpdatesAdapterForSuccessResults() {
        assertTrue(GroupLimitUtils.isGroupApplicationSuccess(CoreErrorCode.SUCCESS));
        assertTrue(
                GroupLimitUtils.isGroupApplicationSuccess(
                        CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT));
        assertFalse(
                GroupLimitUtils.isGroupApplicationSuccess(
                        CoreErrorCode.RC_GROUP_JOIN_COUNT_EXCEED_LIMIT));
    }
}
