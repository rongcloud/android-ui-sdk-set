package io.rong.imkit.feature.reference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.RichContentMessage;

/** 引用控件 Created by JL on 2018/3/28. */
@SuppressLint("ViewConstructor")
public class ReferenceView extends FrameLayout {
    private Context mContext;
    private View mReferenceView;
    private TextView mReferenceSenderName;
    private TextView mReferenceContent;
    private ReferenceCancelListener mCancelListener;

    public ReferenceView(Context context, ViewGroup parent, UiMessage message) {
        super(context);
        mContext = context;
        initView(context, parent);
        initData(message);
    }

    private void initView(Context context, ViewGroup parent) {
        mReferenceView =
                LayoutInflater.from(context)
                        .inflate(R.layout.rc_reference_ext_attach_view, parent, false);

        ImageView cancelButton = mReferenceView.findViewById(R.id.rc_reference_cancel);
        mReferenceSenderName = mReferenceView.findViewById(R.id.rc_reference_sender_name);
        mReferenceContent = mReferenceView.findViewById(R.id.rc_reference_content);
        cancelButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCancelListener != null) {
                            mCancelListener.onCanceled();
                        }
                    }
                });
    }

    private void initData(UiMessage uiMessage) {
        Message message = uiMessage.getMessage();
        if (message != null) {
            Spannable content =
                    RongConfigCenter.conversationConfig()
                            .getMessageSummary(mContext, message.getContent());
            MessageContent messageContent = message.getContent();
            String fileTile;
            SpannableStringBuilder ssb;
            if (messageContent instanceof FileMessage) {
                ssb = new SpannableStringBuilder(content);
            } else if (messageContent instanceof RichContentMessage) {
                fileTile = ((RichContentMessage) messageContent).getTitle();
                ssb = new SpannableStringBuilder(content + fileTile);
            } else {
                ssb = new SpannableStringBuilder(StringUtils.getStringNoBlank(content.toString()));
            }
            mReferenceSenderName.setText(getDisplayName(uiMessage));
            mReferenceContent.setText(ssb);
        }
    }

    public View getReferenceView() {
        return mReferenceView;
    }

    public void setReferenceCancelListener(ReferenceCancelListener referenceCancelListener) {
        this.mCancelListener = referenceCancelListener;
    }

    private String getDisplayName(UiMessage uiMessage) {
        String groupMemberName = "";
        if (uiMessage
                .getMessage()
                .getConversationType()
                .equals(Conversation.ConversationType.GROUP)) {
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance()
                            .getGroupUserInfo(
                                    uiMessage.getMessage().getTargetId(),
                                    uiMessage.getMessage().getSenderUserId());
            groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
        }

        UserInfo userInfo =
                getUserInfo(uiMessage.getMessage().getSenderUserId(), uiMessage.getContent());
        return RongUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName)
                + "：";
    }

    private UserInfo getUserInfo(String userId, MessageContent messageContent) {
        boolean isInfoManagement =
                RongUserInfoManager.getInstance().getDataSourceType()
                        == RongUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && messageContent != null
                && messageContent.getUserInfo() != null
                && messageContent.getUserInfo().getUserId() != null
                && messageContent.getUserInfo().getUserId().equals(userId)) {
            return messageContent.getUserInfo();
        }
        return RongUserInfoManager.getInstance().getUserInfo(userId);
    }

    public interface ReferenceCancelListener {
        void onCanceled();
    }
}
