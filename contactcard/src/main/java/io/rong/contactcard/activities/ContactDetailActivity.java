package io.rong.contactcard.activities;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import io.rong.contactcard.R;
import io.rong.contactcard.message.ContactMessage;
import io.rong.imkit.IMCenter;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.TextMessage;
import java.util.List;

/** Created by Beyond on 2016/11/24. */
public class ContactDetailActivity extends RongBaseNoActionbarActivity
        implements RongUserInfoManager.UserDataObserver {

    private static final int DIP_VALUE_160 = 160;
    private static final int DIP_VALUE_90 = 90;
    private ImageView mTargetPortrait;
    private TextView mTargetName;
    private TextView mContactName;
    private EditText mMessage;
    private TextView mSend;
    private TextView mCancel;
    private ImageView mArrow;
    private ViewAnimator mViewAnimator;
    private GridView mGridView;

    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private UserInfo mContactFriend;
    private Group group;
    private List<UserInfo> mGroupMember;
    private boolean mGroupMemberShown = false;
    private static final int ENCRYPTED_LENGTH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rc_ac_contact_detail);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        initView();
        initData();
    }

    @Override
    protected void onDestroy() {
        RongUserInfoManager.getInstance().removeUserDataObserver(this);

        super.onDestroy();
    }

    private void initView() {
        mTargetPortrait = (ImageView) findViewById(R.id.target_portrait);
        mTargetName = (TextView) findViewById(R.id.target_name);
        mArrow = (ImageView) findViewById(R.id.target_group_arrow);
        mContactName = (TextView) findViewById(R.id.contact_name);
        mMessage = (EditText) findViewById(R.id.message);
        mSend = (TextView) findViewById(R.id.send);
        mCancel = (TextView) findViewById(R.id.cancel);
        mViewAnimator = (ViewAnimator) findViewById(R.id.va_detail);
        mGridView = (GridView) findViewById(R.id.gridview);

        mCancel.requestFocus();
        this.setFinishOnTouchOutside(false);
    }

    private void initData() {
        RongUserInfoManager.getInstance().addUserDataObserver(this);
        mTargetId = getIntent().getStringExtra("targetId");
        mConversationType =
                (Conversation.ConversationType)
                        getIntent().getSerializableExtra("conversationType");
        mContactFriend = getIntent().getParcelableExtra("contact");

        switch (mConversationType) {
            case PRIVATE:
                UserInfo mine = RongUserInfoManager.getInstance().getUserInfo(mTargetId);
                onUserUpdate(mine);
                break;
            case ENCRYPTED:
                String userId = null;
                String[] str = mTargetId.split(";;;");
                if (str.length >= ENCRYPTED_LENGTH) {
                    userId = str[1];
                }
                mine = RongUserInfoManager.getInstance().getUserInfo(userId);
                onUserUpdate(mine);
                break;
            case GROUP:
                group = RongUserInfoManager.getInstance().getGroupInfo(mTargetId);
                onGroupUpdate(group);

                RongMentionManager.IGroupMembersProvider groupMembersProvider =
                        RongMentionManager.getInstance().getGroupMembersProvider();
                if (groupMembersProvider != null) {
                    groupMembersProvider.getGroupMembers(
                            mTargetId,
                            new RongMentionManager.IGroupMemberCallback() {
                                @Override
                                public void onGetGroupMembersResult(final List<UserInfo> members) {
                                    mGroupMember = members;
                                    if (mGroupMember != null) {
                                        if (group != null) {
                                            mTargetName.setText(
                                                    String.format(
                                                            getResources()
                                                                    .getString(
                                                                            R.string
                                                                                    .rc_contact_group_member_count),
                                                            group.getName(),
                                                            mGroupMember.size()));
                                        }
                                        mGridView.setAdapter(
                                                new GridAdapter(
                                                        ContactDetailActivity.this, mGroupMember));
                                    }
                                }
                            });
                    mArrow.setVisibility(View.VISIBLE);
                }

                mArrow.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (!mGroupMemberShown) {
                                    hideInputKeyBoard();
                                    mViewAnimator.setDisplayedChild(1);
                                    if (mGroupMember != null && mGroupMember.size() > 4) {

                                        mGridView.setLayoutParams(
                                                new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        RongUtils.dip2px(DIP_VALUE_160)));
                                    } else {
                                        mGridView.setLayoutParams(
                                                new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                                        RongUtils.dip2px(DIP_VALUE_90)));
                                    }

                                    ObjectAnimator animator =
                                            ObjectAnimator.ofFloat(mArrow, "rotation", 0f, 180f);
                                    animator.setDuration(500);
                                    animator.start();
                                    mGroupMemberShown = true;
                                } else {
                                    mViewAnimator.setDisplayedChild(0);
                                    mGridView.setLayoutParams(
                                            new LinearLayout.LayoutParams(
                                                    LinearLayout.LayoutParams.MATCH_PARENT, 0));
                                    ObjectAnimator animator =
                                            ObjectAnimator.ofFloat(mArrow, "rotation", 180f, 0f);
                                    animator.setDuration(500);
                                    animator.start();
                                    mGroupMemberShown = false;
                                }
                            }
                        });
                break;
            default:
                break;
        }

        if (mContactFriend != null) {
            mContactName.setText(
                    getString(R.string.rc_plugins_contact) + ": " + mContactFriend.getName());
        }

        mMessage.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        // default implementation ignored
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // default implementation ignored
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        if (s != null) {
                            int start = mMessage.getSelectionStart();
                            int end = mMessage.getSelectionEnd();
                            mMessage.removeTextChangedListener(this);
                            mMessage.setText(AndroidEmoji.ensure(s.toString()));
                            mMessage.addTextChangedListener(this);
                            mMessage.setSelection(start, end);
                        }
                    }
                });

        mSend.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        UserInfo sendUserInfo =
                                RongUserInfoManager.getInstance()
                                        .getUserInfo(RongIMClient.getInstance().getCurrentUserId());
                        String sendUserName = sendUserInfo == null ? "" : sendUserInfo.getName();
                        String friendPortrait =
                                mContactFriend.getPortraitUri() == null
                                        ? ""
                                        : mContactFriend.getPortraitUri().toString();
                        ContactMessage contactMessage =
                                ContactMessage.obtain(
                                        mContactFriend.getUserId(),
                                        mContactFriend.getName(),
                                        friendPortrait,
                                        RongIMClient.getInstance().getCurrentUserId(),
                                        sendUserName,
                                        "");
                        String pushContent =
                                String.format(
                                        v.getContext()
                                                .getResources()
                                                .getString(R.string.rc_recommend_clause_to_me),
                                        sendUserName,
                                        contactMessage.getName());
                        IMCenter.getInstance()
                                .sendMessage(
                                        Message.obtain(
                                                mTargetId, mConversationType, contactMessage),
                                        pushContent,
                                        null,
                                        new IRongCallback.ISendMessageCallback() {
                                            @Override
                                            public void onAttached(Message message) {
                                                // do nothing
                                            }

                                            @Override
                                            public void onSuccess(Message message) {
                                                // do nothing
                                            }

                                            @Override
                                            public void onError(
                                                    Message message,
                                                    RongIMClient.ErrorCode errorCode) {
                                                // do nothing
                                            }
                                        });

                        String message = mMessage.getText().toString().trim();
                        if (!("".equals(message))) {
                            TextMessage mTextMessage = TextMessage.obtain(message);
                            IMCenter.getInstance()
                                    .sendMessage(
                                            Message.obtain(
                                                    mTargetId, mConversationType, mTextMessage),
                                            null,
                                            null,
                                            new IRongCallback.ISendMessageCallback() {
                                                @Override
                                                public void onAttached(Message message) {
                                                    // do nothing
                                                }

                                                @Override
                                                public void onSuccess(Message message) {
                                                    // do nothing
                                                }

                                                @Override
                                                public void onError(
                                                        Message message,
                                                        RongIMClient.ErrorCode errorCode) {
                                                    // do nothing
                                                }
                                            });
                        } else {
                            hideInputKeyBoard();
                        }
                        finish();
                    }
                });

        mCancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideInputKeyBoard();
                        finish();
                    }
                });
    }

    @Override
    public void onUserUpdate(UserInfo info) {
        if (info != null) {
            if (info.getPortraitUri() != null) {
                Glide.with(this)
                        .load(info.getPortraitUri())
                        .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                        .into(mTargetPortrait);
            }
            RongUserInfoManager.getInstance().getUserDisplayName(info);
        }
    }

    @Override
    public void onGroupUpdate(Group group) {
        if (group != null) {
            this.group = group;
            if (group.getPortraitUri() != null) {
                Glide.with(this).load(group.getPortraitUri()).into(mTargetPortrait);
            }
            if (group.getName() != null) {
                if (mGroupMember != null && mGroupMember.size() > 0) {
                    mTargetName.setText(
                            String.format(
                                    getResources()
                                            .getString(R.string.rc_contact_group_member_count),
                                    group.getName(),
                                    mGroupMember.size()));
                } else {
                    mTargetName.setText(group.getName());
                }
            }
        }
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        // do nothing
    }

    private static class GridAdapter extends BaseAdapter {

        private List<UserInfo> list;
        Context context;

        GridAdapter(Context context, List<UserInfo> list) {
            this.list = list;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView =
                        LayoutInflater.from(context)
                                .inflate(
                                        R.layout.rc_gridview_item_contact_group_members,
                                        parent,
                                        false);
                viewHolder = new ViewHolder();
                viewHolder.portrait = (ImageView) convertView.findViewById(R.id.iv_avatar);
                viewHolder.name = (TextView) convertView.findViewById(R.id.tv_username);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            UserInfo member = list.get(position);
            if (member != null) {
                Glide.with(convertView).load(member.getPortraitUri()).into(viewHolder.portrait);
                viewHolder.name.setText(member.getName());
            }

            return convertView;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }

    private static class ViewHolder {
        ImageView portrait;
        TextView name;
    }

    private void hideInputKeyBoard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mMessage.getWindowToken(), 0);
    }
}
