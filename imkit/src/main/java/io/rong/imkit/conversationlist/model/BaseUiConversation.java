package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseUiConversation {
    private final String TAG = this.getClass().getSimpleName();
    protected final String COLON_SPLIT = ": ";
    Context mContext;
    public Conversation mCore;
    public Spannable mConversationContent;
    // 已读回执人数
    private int readReceiptCount = 0;

    public BaseUiConversation(Context context, Conversation conversation) {
        if (context == null || conversation == null) {
            RLog.e(TAG, "Context or conversation can't be null.");
            return;
        }
        mContext = context;
        mCore = conversation;
    }

    public void onDraftUpdate(String draft) {
        mCore.setDraft(draft);
        mCore.setSentTime(System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime());
        buildConversationContent();
    }

    public ConversationIdentifier getConversationIdentifier() {
        if (mCore == null) {
            return null;
        }
        if (TextUtils.isEmpty(mCore.getTargetId()) || mCore.getConversationType() == null) {
            return null;
        }
        return ConversationIdentifier.obtain(
                mCore.getConversationType(), mCore.getTargetId(), mCore.getChannelId());
    }

    // 拼接ConversationType + targetID + ChannelID，返回能代表会话唯一的Key
    public String getConversationKey() {
        if (mCore == null) {
            return "";
        }
        Conversation.ConversationType type =
                mCore.getConversationType() != null
                        ? mCore.getConversationType()
                        : Conversation.ConversationType.NONE;
        String targetId = mCore.getTargetId() != null ? mCore.getTargetId() : "";
        String channelId = mCore.getChannelId() != null ? mCore.getChannelId() : "";
        return "type=" + type + "&tid=" + targetId + "&cid=" + channelId;
    }

    // 如果会话的lastMsg在重发列表中，则需要更新成sending状态
    public void processResending(Conversation conversation) {
        if (ResendManager.getInstance().needResend(conversation.getLatestMessageId())) {
            conversation.setSentStatus(Message.SentStatus.SENDING);
        }
    }

    public Conversation currentConversation(String targetId) {
        return this.mCore;
    }

    public int getUnreadMessageCount() {
        if (this.mCore == null) {
            return -1;
        }
        return this.mCore.getUnreadMessageCount();
    }

    abstract void buildConversationContent();

    public String getDraft() {
        if (mCore == null || TextUtils.isEmpty(mCore.getDraft())) {
            return "";
        }
        // 尝试解析为 JSON 格式
        String draftContent = "";
        String draft = mCore.getDraft();
        try {
            JSONObject draftJson = new JSONObject(draft);
            draftContent = draftJson.optString("draftContent", "");
        } catch (JSONException e) {
            // 如果不是 JSON 格式，兼容原有的字符串草稿内容
            draftContent = draft;
        }
        return draftContent == null ? "" : draftContent;
    }

    /**
     * 用户信息更新
     *
     * @param user {@link UserInfo}
     */
    public abstract void onUserInfoUpdate(UserInfo user);

    /**
     * 群组信息更新
     *
     * @param group {@link Group}
     */
    public abstract void onGroupInfoUpdate(Group group);

    /**
     * 群组成员更新
     *
     * @param groupMember {@link GroupUserInfo}
     */
    public abstract void onGroupMemberUpdate(GroupUserInfo groupMember);

    /**
     * 会话更新
     *
     * @param conversation {@link Conversation}
     */
    public abstract void onConversationUpdate(Conversation conversation);

    UserInfo getUserInfo(String userId, Conversation conversation) {
        boolean isInfoManagement =
                RongUserInfoManager.getInstance().getDataSourceType()
                        == RongUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && conversation != null
                && conversation.getLatestMessage() != null
                && conversation.getLatestMessage().getUserInfo() != null
                && conversation.getLatestMessage().getUserInfo().getUserId() != null
                && conversation.getLatestMessage().getUserInfo().getUserId().equals(userId)) {
            return conversation.getLatestMessage().getUserInfo();
        }
        return RongUserInfoManager.getInstance().getUserInfo(userId);
    }

    /**
     * 获取已读回执人数
     *
     * @return 已读回执人数
     */
    public int getReadReceiptCount() {
        return readReceiptCount;
    }

    /**
     * 设置已读回执人数
     *
     * @param readReceiptCount 已读回执人数
     */
    public void setReadReceiptCount(int readReceiptCount) {
        this.readReceiptCount = readReceiptCount;
    }
}
