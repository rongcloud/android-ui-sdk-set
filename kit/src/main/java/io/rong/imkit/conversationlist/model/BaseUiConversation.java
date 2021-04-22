package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

public abstract class BaseUiConversation {
    private final String TAG = this.getClass().getSimpleName();
    protected final String COLON_SPLIT = ":";
    Context mContext;
    public Conversation mCore;
    public Spannable mConversationContent;
    public BaseUiConversation(Context context, Conversation conversation) {
        if(context == null || conversation == null) {
            RLog.e(TAG, "Context or conversation can't be null.");
            return;
        }
        mContext = context;
        mCore = conversation;
    }
    public void onDraftUpdate(String draft){
        mCore.setDraft(draft);
        mCore.setSentTime(System.currentTimeMillis() - RongIMClient.getInstance().getDeltaTime());
        buildConversationContent();
    }

    abstract void buildConversationContent();
    public abstract void onUserInfoUpdate(List<User> userList);
    public abstract void onGroupInfoUpdate(List<Group> groups);
    public abstract void onGroupMemberUpdate(List<GroupMember> groupMembers);
    public abstract void onConversationUpdate(Conversation conversation);

}


