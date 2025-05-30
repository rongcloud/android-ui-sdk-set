package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;
import java.util.HashSet;
import java.util.Set;

public class GroupConversation extends BaseUiConversation {
    private String TAG = GroupConversation.class.getSimpleName();
    private Set<String> mNicknameIds;
    private SpannableString mPreString; // 前缀字符，如【有人@我】【草稿】

    public GroupConversation(Context context, Conversation conversation) {
        super(context, conversation);
        RLog.d(TAG, "new group conversation.");
        mNicknameIds = new HashSet<>();
        onConversationUpdate(conversation);
    }

    @Override
    void buildConversationContent() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String preStr;
        String senderName =
                TextUtils.isEmpty(mCore.getSenderUserName()) ? "" : mCore.getSenderUserName();
        boolean isShowName =
                RongConfigCenter.conversationConfig().showSummaryWithName(mCore.getLatestMessage());

        if (mCore.getUnreadMentionedCount() > 0) {
            if (mContext != null) {
                preStr = mContext.getString(R.string.rc_conversation_summary_content_mentioned);
                mPreString = new SpannableString(preStr);
                mPreString.setSpan(
                        new ForegroundColorSpan(
                                mContext.getResources().getColor(R.color.rc_warning_color)),
                        0,
                        preStr.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(mPreString);
            }
            if (!TextUtils.isEmpty(senderName) && isShowName) {
                builder.append(senderName).append(COLON_SPLIT);
            }
            Spannable messageSummary =
                    RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore);
            if (!TextUtils.isEmpty(messageSummary)) {
                builder.append(messageSummary);
            }
        } else if (!TextUtils.isEmpty(getDraft())) {
            if (mContext != null) {
                preStr = mContext.getString(R.string.rc_conversation_summary_content_draft);
                mPreString = new SpannableString(preStr);
                mPreString.setSpan(
                        new ForegroundColorSpan(
                                mContext.getResources().getColor(R.color.rc_warning_color)),
                        0,
                        preStr.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(mPreString);
            }
            String draft = getDraft();
            draft = draft.replace("\n", "");
            builder.append(draft);
        } else {
            mPreString = new SpannableString("");
            Spannable messageSummary =
                    RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore);
            if (mCore.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                if (!TextUtils.isEmpty(messageSummary)) {
                    builder.append(messageSummary);
                }
            } else {
                if (!TextUtils.isEmpty(senderName)
                        && !TextUtils.isEmpty(messageSummary)
                        && isShowName) {
                    builder.append(senderName).append(COLON_SPLIT).append(messageSummary);
                } else if (!TextUtils.isEmpty(senderName) && isShowName) {
                    builder.append(senderName);
                } else if (!TextUtils.isEmpty(messageSummary)) {
                    builder.append(messageSummary);
                }
            }
        }
        mConversationContent = builder;
    }

    @Override
    public void onUserInfoUpdate(UserInfo user) {
        if (!TextUtils.isEmpty(getDraft()) || user == null) {
            return; // 有草稿时，会话内容里显示草稿，不需要处理用户信息
        }
        if (mCore.getSenderUserId().equals(user.getUserId())
                && !mNicknameIds.contains(user.getUserId())) {
            mCore.setSenderUserName(RongUserInfoManager.getInstance().getUserDisplayName(user));
            buildConversationContent();
        }
    }

    @Override
    public void onGroupInfoUpdate(Group group) {
        if (group == null) {
            return;
        }
        if (group.getId().equals(mCore.getTargetId())) {
            RLog.d(TAG, "onGroupInfoUpdate. name:" + group.getName());
            mCore.setConversationTitle(group.getName());
            mCore.setPortraitUrl(
                    group.getPortraitUri() != null ? group.getPortraitUri().toString() : null);
        }
    }

    @Override
    public void onGroupMemberUpdate(GroupUserInfo groupMember) {
        if (groupMember == null) {
            return;
        }
        if (groupMember.getGroupId().equals(mCore.getTargetId())
                && groupMember.getUserId().equals(mCore.getSenderUserId())) {
            mNicknameIds.add(groupMember.getUserId());
            mCore.setSenderUserName(groupMember.getNickname());
            buildConversationContent();
        }
    }

    @Override
    public void onConversationUpdate(Conversation conversation) {
        processResending(conversation);
        mCore = conversation;
        String groupId = conversation.getTargetId();
        if (conversation.getConversationType() == Conversation.ConversationType.ULTRA_GROUP) {
            groupId = conversation.getTargetId() + conversation.getChannelId();
        }
        io.rong.imlib.model.Group group = RongUserInfoManager.getInstance().getGroupInfo(groupId);
        if (group != null) {
            RLog.d(TAG, "onConversationUpdate. name:" + group.getName());
        } else {
            RLog.d(TAG, "onConversationUpdate. group info is null");
        }
        mCore.setConversationTitle(group == null ? "" : group.getName());
        mCore.setPortraitUrl(
                group == null || group.getPortraitUri() == null
                        ? ""
                        : group.getPortraitUri().toString());
        GroupUserInfo groupUserInfo =
                RongUserInfoManager.getInstance()
                        .getGroupUserInfo(groupId, conversation.getSenderUserId());
        UserInfo userInfo = getUserInfo(conversation.getSenderUserId(), mCore);
        if (groupUserInfo != null) {
            mNicknameIds.add(groupUserInfo.getUserId());
            mCore.setSenderUserName(
                    RongUserInfoManager.getInstance()
                            .getUserDisplayName(userInfo, groupUserInfo.getNickname()));
        } else {
            if (userInfo != null) {
                mCore.setSenderUserName(
                        RongUserInfoManager.getInstance().getUserDisplayName(userInfo));
            }
        }
        buildConversationContent();
    }
}
