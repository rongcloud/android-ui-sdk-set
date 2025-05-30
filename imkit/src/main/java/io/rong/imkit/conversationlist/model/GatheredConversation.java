package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.TimeUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GatheredConversation extends BaseUiConversation {
    public Conversation.ConversationType mGatheredType;
    private String mLastTargetId; // 聚合会话里最新一条会话的 targetId. 聚会会话内容里需要拼接此 targetId 对应的名称。
    private final Map<String, Conversation> gatheredConversationMap =
            new ConcurrentHashMap<>(); // 存储当前聚合会话中 所有的Conversation

    public GatheredConversation(Context context, Conversation conversation) {
        super(context, conversation);
        gatheredConversationMap.put(conversation.getTargetId(), conversation);
        mGatheredType = conversation.getConversationType();
        mLastTargetId = conversation.getTargetId();
        setConversationTitle();
        setConversationPortrait();
        buildConversationContent();
    }

    /**
     * 会话聚合后，会话列表中显示信息如下： - 会话默认头像 - 聚合会话中最近一条消息的会话名称（如：单聊为用户名、群聊为群名称）、消息内容、消息发送时间 -
     * 显示最近一条消息状态包括：发送中、发送失败、消息已读 - 如会话中最后一条为草稿消息时，则显示为[草稿]
     */
    @Override
    void buildConversationContent() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String targetName;
        String preStr;
        // 前缀字符，如【有人@我】【草稿】
        SpannableString mPreString;
        if (mContext == null) {
            mPreString = new SpannableString("");
        } else if (mCore.getMentionedCount() > 0) {
            preStr = mContext.getString(R.string.rc_conversation_summary_content_mentioned);
            mPreString = new SpannableString(preStr);
            mPreString.setSpan(
                    new ForegroundColorSpan(
                            mContext.getResources().getColor(R.color.rc_warning_color)),
                    0,
                    preStr.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (!TextUtils.isEmpty(getDraft())) {
            preStr = mContext.getString(R.string.rc_conversation_summary_content_draft);
            mPreString = new SpannableString(preStr);
            mPreString.setSpan(
                    new ForegroundColorSpan(
                            mContext.getResources().getColor(R.color.rc_warning_color)),
                    0,
                    preStr.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            mPreString = new SpannableString("");
        }
        if (mGatheredType.equals(Conversation.ConversationType.GROUP)) {
            Group group = RongUserInfoManager.getInstance().getGroupInfo(mLastTargetId);
            targetName = group == null ? mLastTargetId : group.getName();
        } else {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(mLastTargetId);
            targetName = userInfo == null ? mLastTargetId : userInfo.getName();
        }

        builder.append(mPreString)
                .append(targetName)
                .append(COLON_SPLIT)
                .append(
                        TextUtils.isEmpty(getDraft())
                                ? RongConfigCenter.conversationConfig()
                                        .getMessageSummary(mContext, mCore)
                                : getDraft());
        mConversationContent = builder;
    }

    @Override
    public void onUserInfoUpdate(UserInfo user) {
        if (user != null && !mGatheredType.equals(Conversation.ConversationType.GROUP)) {
            if (user.getUserId().equals(mLastTargetId)) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(RongUserInfoManager.getInstance().getUserDisplayName(user))
                        .append(COLON_SPLIT)
                        .append(
                                RongConfigCenter.conversationConfig()
                                        .getMessageSummary(mContext, mCore));
                mConversationContent = builder;
            }
        }
    }

    @Override
    public void onGroupInfoUpdate(Group group) {
        if (group != null && mGatheredType.equals(Conversation.ConversationType.GROUP)) {
            if (group != null && group.getId().equals(mLastTargetId)) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(group.getName())
                        .append(COLON_SPLIT)
                        .append(
                                RongConfigCenter.conversationConfig()
                                        .getMessageSummary(mContext, mCore));
                mConversationContent = builder;
            }
        }
    }

    @Override
    public void onGroupMemberUpdate(GroupUserInfo groupMember) {
        // do nothing
    }

    @Override
    public void onConversationUpdate(Conversation conversation) {

        if (conversation != null && conversation.getConversationType().equals(mGatheredType)) {
            gatheredConversationMap.put(conversation.getTargetId(), conversation);

            if (TimeUtils.getLatestTime(conversation) >= TimeUtils.getLatestTime(mCore)) {
                mCore = conversation;
                mLastTargetId = conversation.getTargetId();
                buildConversationContent();
                setConversationTitle();
            }
        }
    }

    @Override
    public int getUnreadMessageCount() {
        int count = 0;
        for (String targetId : gatheredConversationMap.keySet()) {
            Conversation conversation = gatheredConversationMap.get(targetId);
            if (conversation != null) {
                count += conversation.getUnreadMessageCount();
            }
        }
        return count;
    }

    @Override
    public Conversation currentConversation(String targetId) {
        Conversation conversation = gatheredConversationMap.get(targetId);
        if (conversation != null) {
            return conversation;
        }
        return super.currentConversation(targetId);
    }

    private void setConversationTitle() {
        Conversation.ConversationType type = mCore.getConversationType();
        String title = "";
        if (mContext == null) {
            mCore.setConversationTitle(title);
            return;
        }
        Integer titleId = RongConfigCenter.gatheredConversationConfig().getConversationTitle(type);
        if (titleId != null) {
            title = mContext.getString(titleId);
        }
        if (TextUtils.isEmpty(title)) {
            if (type.equals(Conversation.ConversationType.PRIVATE)) {
                title = mContext.getString(R.string.rc_gathered_conversation_private_title);
            } else if (type.equals(Conversation.ConversationType.GROUP)) {
                title = mContext.getString(R.string.rc_gathered_conversation_group_title);
            } else if (type.equals(Conversation.ConversationType.SYSTEM)) {
                title = mContext.getString(R.string.rc_gathered_conversation_system_title);
            } else if (type.equals(Conversation.ConversationType.CUSTOMER_SERVICE)) {
                title = mContext.getString(R.string.rc_gathered_conversation_custom_title);
            } else if (type.equals(Conversation.ConversationType.CHATROOM)) {
                title = mContext.getString(R.string.rc_gathered_conversation_chatroom_title);
            } else {
                title = mContext.getString(R.string.rc_gathered_conversation_unkown_title);
            }
        }
        mCore.setConversationTitle(title);
    }

    private void setConversationPortrait() {
        Conversation.ConversationType type = mCore.getConversationType();
        Uri uri = RongConfigCenter.gatheredConversationConfig().getGatherConversationPortrait(type);
        if (uri != null) {
            mCore.setPortraitUrl(uri.toString());
            return;
        }
        if (mContext != null) {
            uri = RongUtils.getUriFromDrawableRes(mContext, R.drawable.rc_default_portrait);
            mCore.setPortraitUrl(uri.toString());
        }
    }
}
