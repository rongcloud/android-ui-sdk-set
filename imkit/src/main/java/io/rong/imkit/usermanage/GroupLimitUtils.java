package io.rong.imkit.usermanage;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import io.rong.imkit.R;
import io.rong.imkit.model.ContactModel;
import io.rong.imlib.IRongCoreEnum.CoreErrorCode;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 群组能力限制提示和瞬时 UI 状态工具。 @since 5.46.0 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class GroupLimitUtils {

    private GroupLimitUtils() {}

    @NonNull
    public static List<String> resolveFailedUserNames(
            List<GroupMemberInfo> failedUsers, Map<String, String> pageDisplayNames) {
        if (failedUsers == null || failedUsers.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(failedUsers.size());
        for (GroupMemberInfo user : failedUsers) {
            if (user == null) {
                continue;
            }
            String name = pageDisplayNames == null ? null : pageDisplayNames.get(user.getUserId());
            if (!hasText(name)) {
                name = user.getNickname();
            }
            if (!hasText(name)) {
                name = user.getName();
            }
            if (!hasText(name)) {
                name = user.getUserId();
            }
            if (hasText(name)) {
                names.add(name);
            }
        }
        return names;
    }

    @Nullable
    public static String getFriendDisplayName(@Nullable FriendInfo friendInfo) {
        if (friendInfo == null) {
            return null;
        }
        if (hasText(friendInfo.getRemark())) {
            return friendInfo.getRemark();
        }
        return hasText(friendInfo.getName()) ? friendInfo.getName() : null;
    }

    @NonNull
    public static Map<String, String> mapDisplayNames(
            List<String> userIds, List<String> displayNames) {
        if (userIds == null || displayNames == null) {
            return Collections.emptyMap();
        }
        Map<String, String> names = new LinkedHashMap<>();
        int count = Math.min(userIds.size(), displayNames.size());
        for (int i = 0; i < count; i++) {
            String userId = userIds.get(i);
            String displayName = displayNames.get(i);
            if (hasText(userId) && hasText(displayName)) {
                names.put(userId, displayName);
            }
        }
        return names;
    }

    @NonNull
    public static String formatNameList(@NonNull Context context, @NonNull List<String> names) {
        return formatNameList(
                names,
                context.getString(R.string.rc_group_limit_name_list_pair),
                context.getString(R.string.rc_group_limit_name_list_middle),
                context.getString(R.string.rc_group_limit_name_list_end));
    }

    @NonNull
    public static String getFailedUsersLimitMessage(
            @NonNull Context context,
            List<GroupMemberInfo> failedUsers,
            Map<String, String> pageDisplayNames) {
        List<String> names = resolveFailedUserNames(failedUsers, pageDisplayNames);
        if (names.isEmpty()) {
            return context.getString(R.string.rc_group_join_count_exceed_limit);
        }
        String nameList = formatNameList(context, names);
        int messageRes =
                names.size() == 1
                        ? R.string.rc_group_failed_users_join_count_exceed_limit_single
                        : R.string.rc_group_failed_users_join_count_exceed_limit_multiple;
        return context.getString(messageRes, nameList);
    }

    @NonNull
    public static String getGroupApplicationLimitMessage(
            @NonNull Context context, GroupMemberInfo memberInfo, String fallbackUserId) {
        List<String> names =
                resolveFailedUserNames(
                        memberInfo == null
                                ? Collections.emptyList()
                                : Collections.singletonList(memberInfo),
                        Collections.emptyMap());
        String name = names.isEmpty() ? fallbackUserId : names.get(0);
        if (!hasText(name)) {
            return context.getString(R.string.rc_group_join_count_exceed_limit);
        }
        return context.getString(R.string.rc_group_application_join_count_exceed_limit, name);
    }

    static String formatNameList(
            List<String> names, String pairPattern, String middlePattern, String endPattern) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        if (names.size() == 2) {
            return format(pairPattern, names.get(0), names.get(1));
        }
        String formatted = format(middlePattern, names.get(0), names.get(1));
        for (int i = 2; i < names.size() - 1; i++) {
            formatted = format(middlePattern, formatted, names.get(i));
        }
        return format(endPattern, formatted, names.get(names.size() - 1));
    }

    public static void updateFailedContactStates(
            List<ContactModel> contacts, @NonNull Set<String> failedUserIds) {
        if (contacts == null) {
            return;
        }
        for (ContactModel contact : contacts) {
            String userId = getUserId(contact);
            if (failedUserIds.contains(userId)
                    && contact.getCheckType() == ContactModel.CheckType.CHECKED) {
                contact.setCheckType(ContactModel.CheckType.FAILED);
            } else if (!failedUserIds.contains(userId)
                    && contact.getCheckType() == ContactModel.CheckType.FAILED) {
                contact.setCheckType(ContactModel.CheckType.CHECKED);
            }
        }
    }

    public static boolean isSelected(ContactModel contact) {
        return contact != null
                && (contact.getCheckType() == ContactModel.CheckType.CHECKED
                        || contact.getCheckType() == ContactModel.CheckType.FAILED);
    }

    public static boolean isGroupApplicationSuccess(CoreErrorCode code) {
        return code == CoreErrorCode.SUCCESS || code == CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT;
    }

    private static String getUserId(ContactModel contact) {
        if (contact == null || !(contact.getBean() instanceof FriendInfo)) {
            return null;
        }
        return ((FriendInfo) contact.getBean()).getUserId();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String format(String pattern, String first, String second) {
        return String.format(Locale.getDefault(), pattern, first, second);
    }
}
