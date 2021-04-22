package io.rong.imkit.conversation;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;

import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseActivity;
import io.rong.imkit.model.TypingInfo;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.TitleBar;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

public class RongConversationActivity extends RongBaseActivity {
    protected String mTargetId;
    protected Conversation.ConversationType mConversationType;
    protected ConversationFragment mConversationFragment;
    private ConversationViewModel conversationViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            mTargetId = getIntent().getStringExtra(RouteUtils.TARGET_ID);
            String type = getIntent().getStringExtra(RouteUtils.CONVERSATION_TYPE);
            if (!TextUtils.isEmpty(type)) {
                mConversationType = Conversation.ConversationType.valueOf(type.toUpperCase(Locale.US));
            } else {
                return;
            }
        }
        setContentView(R.layout.rc_conversation_activity);
        setTitle();
        mConversationFragment = (ConversationFragment) getSupportFragmentManager().findFragmentById(R.id.conversation);
        mTitleBar.setOnBackClickListener(new TitleBar.OnBackClickListener() {
            @Override
            public void onBackClick() {
                if (mConversationFragment != null && !mConversationFragment.onBackPressed()) {
                    finish();
                }
            }
        });
        mTitleBar.getRightView().setVisibility(View.GONE);
        initViewModel();
    }

    private void setTitle() {
        if (!TextUtils.isEmpty(mTargetId) && mConversationType.equals(Conversation.ConversationType.GROUP)) {
            Group group = RongUserInfoManager.getInstance().getGroupInfo(mTargetId);
            mTitleBar.setTitle(group == null ? mTargetId : group.getName());
        } else {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(mTargetId);
            mTitleBar.setTitle(userInfo == null ? mTargetId : userInfo.getName());
        }
        if (mConversationType.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                || mConversationType.equals(Conversation.ConversationType.CHATROOM)) {
            mTitleBar.setRightVisible(false);
        }
    }

    private void initViewModel() {
        conversationViewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        conversationViewModel.getTypingStatusInfo().observe(this, new Observer<TypingInfo>() {
            @Override
            public void onChanged(TypingInfo typingInfo) {
                if (typingInfo == null) {
                    return;
                }
                if (typingInfo.conversationType == mConversationType && mTargetId.equals(typingInfo.targetId)) {
                    if (typingInfo.typingList == null) {
                        mTitleBar.getMiddleView().setVisibility(View.VISIBLE);
                        mTitleBar.getTypingView().setVisibility(View.GONE);
                    } else {
                        mTitleBar.getMiddleView().setVisibility(View.GONE);
                        mTitleBar.getTypingView().setVisibility(View.VISIBLE);
                        TypingInfo.TypingUserInfo typing = typingInfo.typingList.get(typingInfo.typingList.size() - 1);
                        if (typing.type == TypingInfo.TypingUserInfo.Type.text) {
                            mTitleBar.setTyping(R.string.rc_conversation_remote_side_is_typing);
                        } else if (typing.type == TypingInfo.TypingUserInfo.Type.voice) {
                            mTitleBar.setTyping(R.string.rc_conversation_remote_side_speaking);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == event.getKeyCode()) {
            if (mConversationFragment != null && !mConversationFragment.onBackPressed()) {
                finish();
            }
        }
        return false;
    }
}
