package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

public class GroupConversation extends BaseUiConversation {
    private String TAG = GroupConversation.class.getSimpleName();
    private Set<String> mNicknameIds;
    private SpannableString mPreString; //前缀字符，如【有人@我】【草稿】

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
        String senderName = TextUtils.isEmpty(mCore.getSenderUserName()) ? "" : mCore.getSenderUserName();
        boolean isShowName = RongConfigCenter.conversationConfig().showSummaryWithName(mCore.getLatestMessage());

        if (mCore.getMentionedCount() > 0) {
            preStr = mContext.getString(R.string.rc_conversation_summary_content_mentioned);
            mPreString = new SpannableString(preStr);
            mPreString.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.rc_warning_color)), 0, preStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            Spannable messageSummary = RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore.getLatestMessage());
            builder.append(mPreString);
            if (!TextUtils.isEmpty(senderName) && isShowName) {
                builder.append(senderName).append(COLON_SPLIT);
            }
            builder.append(messageSummary);
        } else if (!TextUtils.isEmpty(mCore.getDraft())) {
            preStr = mContext.getString(R.string.rc_conversation_summary_content_draft);
            mPreString = new SpannableString(preStr);
            mPreString.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.rc_warning_color)), 0, preStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(mPreString).append(mCore.getDraft());
        } else {
            mPreString = new SpannableString("");
            Spannable messageSummary = RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore.getLatestMessage());
            if (mCore.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                builder.append(messageSummary);
            } else {
                if (!TextUtils.isEmpty(senderName) && !TextUtils.isEmpty(messageSummary) && isShowName) {
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
    public void onUserInfoUpdate(List<User> users) {
        if (!TextUtils.isEmpty(mCore.getDraft()) || users == null || users.size() == 0) {
            return; //有草稿时，会话内容里显示草稿，不需要处理用户信息
        }
        for (User user : users) {
            if (user != null && mCore.getSenderUserId().equals(user.id) && !mNicknameIds.contains(user.id)) {
                mCore.setSenderUserName(user.name);
                Spannable messageSummary = RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore.getLatestMessage());
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(mPreString).append(user.name).append(COLON_SPLIT).append(messageSummary);
                mConversationContent = builder;
            }
        }
    }

    @Override
    public void onConversationUpdate(Conversation conversation) {
        mCore = conversation;
        io.rong.imlib.model.Group group = RongUserInfoManager.getInstance().getGroupInfo(conversation.getTargetId());
        if (group != null) {
            RLog.d(TAG, "onConversationUpdate. name:" + group.getName());
        } else {
            RLog.d(TAG, "onConversationUpdate. group info is null");
        }
        mCore.setConversationTitle(group == null ? conversation.getTargetId() : group.getName());
        mCore.setPortraitUrl(group == null || group.getPortraitUri() == null ? "" : group.getPortraitUri().toString());
        GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(conversation.getTargetId(), conversation.getSenderUserId());
        if (groupUserInfo != null) {
            mNicknameIds.add(groupUserInfo.getUserId());
            mCore.setSenderUserName(groupUserInfo.getNickname());
        } else {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(conversation.getSenderUserId());
            if (userInfo != null) {
                mCore.setSenderUserName(userInfo.getName());
            }
        }
        buildConversationContent();
    }

    @Override
    public void onGroupInfoUpdate(List<Group> groups) {
        if (groups == null || groups.size() == 0) {
            return;
        }
        for (Group group : groups) {
            if (group.id.equals(mCore.getTargetId())) {
                RLog.d(TAG, "onGroupInfoUpdate. name:" + group.name);
                mCore.setConversationTitle(group.name);
                mCore.setPortraitUrl(group.portraitUrl);
                break;
            }
        }
    }

    @Override
    public void onGroupMemberUpdate(List<GroupMember> groupMembers) {
        if (groupMembers == null || groupMembers.size() == 0) {
            return;
        }
        for (GroupMember groupMember : groupMembers) {
            if (groupMember != null && groupMember.groupId.equals(mCore.getTargetId())
                    && groupMember.userId.equals(mCore.getSenderUserId())) {
                mNicknameIds.add(groupMember.userId);
                mCore.setSenderUserName(groupMember.memberName);
                Spannable messageSummary = RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore.getLatestMessage());
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(mPreString).append(mCore.getSenderUserName()).append(messageSummary);
            }
        }

    }
}
