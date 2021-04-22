package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.Group;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

public class SingleConversation extends BaseUiConversation {
    private final String TAG = SingleConversation.class.getSimpleName();

    public SingleConversation(Context context, Conversation conversation) {
        super(context, conversation);
        onConversationUpdate(conversation);
    }

    @Override
    void buildConversationContent() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(mCore.getDraft())) {
            String draft = mContext.getString(R.string.rc_conversation_summary_content_draft);
            SpannableString preStr = new SpannableString(draft);
            preStr.setSpan(new ForegroundColorSpan(mContext.getResources().getColor(R.color.rc_warning_color)), 0, draft.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(preStr).append(mCore.getDraft());
        } else {
            Spannable summary = RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore.getLatestMessage());
            if (summary.length() > 0) {
                builder.append(summary);
            }
        }
        AndroidEmoji.ensure(builder);
        mConversationContent = builder;
    }


    @Override
    public void onUserInfoUpdate(List<User> userList) {
        if (!TextUtils.isEmpty(mCore.getDraft()) || userList == null) {
            return; //有草稿时，会话内容里显示草稿，不需要处理用户信息
        }
        for (User user : userList) {
            if (user.id.equals(mCore.getTargetId())) {
                mCore.setConversationTitle(user.name);
                mCore.setPortraitUrl(user.portraitUrl);
                RLog.d(TAG, "onUserInfoUpdate. name:" + user.name);
                break;
            }
        }
    }

    @Override
    public void onConversationUpdate(Conversation conversation) {
        mCore = conversation;
        UserInfo user = RongUserInfoManager.getInstance().getUserInfo(conversation.getTargetId());
        mCore.setConversationTitle(user == null ? conversation.getTargetId() : user.getName());
        mCore.setPortraitUrl(user == null || user.getPortraitUri() == null ? "" : user.getPortraitUri().toString());
        buildConversationContent();
    }

    @Override
    public void onGroupInfoUpdate(List<Group> groups) {

    }

    @Override
    public void onGroupMemberUpdate(List<GroupMember> groupMembers) {

    }
}
