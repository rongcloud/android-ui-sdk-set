package io.rong.imkit.conversationlist.model;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import androidx.annotation.NonNull;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
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
        if (!TextUtils.isEmpty(getDraft())) {
            if (mContext != null) {
                String draft = mContext.getString(R.string.rc_conversation_summary_content_draft);
                SpannableString preStr = new SpannableString(draft);
                preStr.setSpan(
                        new ForegroundColorSpan(
                                mContext.getResources().getColor(R.color.rc_warning_color)),
                        0,
                        draft.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append(preStr);
            }
            builder.append(getDraft());
        } else {
            Spannable summary =
                    RongConfigCenter.conversationConfig().getMessageSummary(mContext, mCore);
            if (summary.length() > 0) {
                builder.append(summary);
            }
        }
        mConversationContent = builder;
    }

    @Override
    public void onUserInfoUpdate(@NonNull UserInfo user) {
        if (user.getUserId().equals(mCore.getTargetId())) {
            mCore.setConversationTitle(RongUserInfoManager.getInstance().getUserDisplayName(user));
            mCore.setPortraitUrl(
                    user.getPortraitUri() != null ? user.getPortraitUri().toString() : null);
            RLog.d(TAG, "onUserInfoUpdate. name:" + mCore.getConversationTitle());
        }
    }

    @Override
    public void onConversationUpdate(Conversation conversation) {
        processResending(conversation);
        mCore = conversation;
        UserInfo user = RongUserInfoManager.getInstance().getUserInfo(conversation.getTargetId());
        mCore.setConversationTitle(
                user == null
                        ? conversation.getTargetId()
                        : RongUserInfoManager.getInstance().getUserDisplayName(user));
        mCore.setPortraitUrl(
                user == null || user.getPortraitUri() == null
                        ? ""
                        : user.getPortraitUri().toString());
        buildConversationContent();
    }

    @Override
    public void onGroupInfoUpdate(Group groups) {
        // do nothing
    }

    @Override
    public void onGroupMemberUpdate(GroupUserInfo groupMembers) {
        // do nothing
    }
}
