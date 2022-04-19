package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import io.rong.common.RLog;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
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

    abstract void buildConversationContent();

    public abstract void onUserInfoUpdate(UserInfo user);

    public abstract void onGroupInfoUpdate(Group group);

    public abstract void onGroupMemberUpdate(GroupUserInfo groupMember);

    public abstract void onConversationUpdate(Conversation conversation);
}
