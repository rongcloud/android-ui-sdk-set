package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import io.rong.common.RLog;
import io.rong.imkit.feature.resend.ResendManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public abstract class BaseUiConversation {
    private final String TAG = this.getClass().getSimpleName();
    protected final String COLON_SPLIT = ": ";
    Context mContext;
    public Conversation mCore;
    public Spannable mConversationContent;

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
}
