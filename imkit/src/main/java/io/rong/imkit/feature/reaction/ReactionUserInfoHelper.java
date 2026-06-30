package io.rong.imkit.feature.reaction;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import io.rong.imkit.R;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.ExtendedGroupUserInfo;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.UserInfo;

final class ReactionUserInfoHelper {

    private ReactionUserInfoHelper() {}

    static String getCurrentUserId() {
        try {
            return RongIMClient.getInstance().getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isCurrentUser(String userId) {
        String currentUserId = getCurrentUserId();
        return !TextUtils.isEmpty(currentUserId) && currentUserId.equals(userId);
    }

    static DisplayInfo getDisplayInfo(
            Context context,
            Conversation.ConversationType conversationType,
            String targetId,
            String userId,
            boolean currentUserAsMe) {
        if (TextUtils.isEmpty(userId)) {
            return new DisplayInfo("", null);
        }
        boolean currentUser = currentUserAsMe && isCurrentUser(userId);
        String currentUserName =
                currentUser ? context.getString(R.string.rc_reaction_current_user) : null;

        GroupUserInfo groupUserInfo = null;
        String groupMemberName = null;
        if (conversationType == Conversation.ConversationType.GROUP
                && !TextUtils.isEmpty(targetId)) {
            groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(targetId, userId);
            groupMemberName = getGroupDisplayName(groupUserInfo);
        }

        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(userId);
        String displayName =
                RongUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
        if (TextUtils.isEmpty(displayName)) {
            displayName = userId;
        }
        return new DisplayInfo(
                currentUser ? currentUserName : displayName,
                resolvePortrait(userInfo, groupUserInfo, groupMemberName));
    }

    private static String getGroupDisplayName(GroupUserInfo groupUserInfo) {
        if (groupUserInfo == null) {
            return null;
        }
        if (!TextUtils.isEmpty(groupUserInfo.getNickname())) {
            return groupUserInfo.getNickname();
        }
        if (groupUserInfo instanceof ExtendedGroupUserInfo) {
            GroupMemberInfo groupMemberInfo =
                    ((ExtendedGroupUserInfo) groupUserInfo).getGroupMemberInfo();
            if (groupMemberInfo != null && !TextUtils.isEmpty(groupMemberInfo.getName())) {
                return groupMemberInfo.getName();
            }
        }
        return null;
    }

    private static String getGroupPortrait(GroupUserInfo groupUserInfo) {
        if (groupUserInfo instanceof ExtendedGroupUserInfo) {
            GroupMemberInfo groupMemberInfo =
                    ((ExtendedGroupUserInfo) groupUserInfo).getGroupMemberInfo();
            if (groupMemberInfo != null && !TextUtils.isEmpty(groupMemberInfo.getPortraitUri())) {
                return groupMemberInfo.getPortraitUri();
            }
        }
        return null;
    }

    private static String getPortrait(UserInfo userInfo) {
        Uri uri = userInfo == null ? null : userInfo.getPortraitUri();
        return uri == null ? null : uri.toString();
    }

    private static String resolvePortrait(
            UserInfo userInfo, GroupUserInfo groupUserInfo, String groupMemberName) {
        String groupPortrait = getGroupPortrait(groupUserInfo);
        String userPortrait = getPortrait(userInfo);
        if (userInfo != null
                && !TextUtils.isEmpty(userInfo.getAlias())
                && !TextUtils.isEmpty(userPortrait)) {
            return userPortrait;
        }
        if (!TextUtils.isEmpty(groupMemberName) && !TextUtils.isEmpty(groupPortrait)) {
            return groupPortrait;
        }
        return !TextUtils.isEmpty(userPortrait) ? userPortrait : groupPortrait;
    }

    static class DisplayInfo {
        final String name;
        final String portraitUrl;

        DisplayInfo(String name, String portraitUrl) {
            this.name = name;
            this.portraitUrl = portraitUrl;
        }
    }
}
