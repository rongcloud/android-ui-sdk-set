package io.rong.imkit.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.feature.forward.ForwardManager;
import io.rong.imkit.feature.forward.IHistoryDataResultCallback;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.widget.RongSwipeRefreshLayout;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.UserInfo;

public class ForwardSelectConversationActivity extends RongBaseNoActionbarActivity implements
        View.OnClickListener, RongSwipeRefreshLayout.OnLoadListener {

    private static final String TAG = ForwardSelectConversationActivity.class.getSimpleName();

    private TextView btOK;
    private ListAdapter mAdapter;
    private RongSwipeRefreshLayout mRefreshLayout;

    private ArrayList<Conversation> selectedMember = new ArrayList<>();
    private static final Conversation.ConversationType[] defConversationType = {
            Conversation.ConversationType.PRIVATE,
            Conversation.ConversationType.GROUP
    };
    private long timestamp = 0;
    private int pageSize = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rc_activity_forward_select);
        btOK = findViewById(R.id.rc_btn_ok);
        TextView btCancel = findViewById(R.id.rc_btn_cancel);
        mRefreshLayout = findViewById(R.id.rc_refresh);
        ListView listView = findViewById(R.id.rc_list);

        btOK.setEnabled(false);
        btOK.setOnClickListener(this);
        btCancel.setOnClickListener(this);
        mRefreshLayout.setCanRefresh(false);
        mRefreshLayout.setCanLoading(true);
        mRefreshLayout.setOnLoadListener(this);

        mAdapter = new ListAdapter(this);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new ForwardItemClickListener());
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        RongUserInfoManager.getInstance().getAllUsersLiveData().observe(this, new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                if (users != null && users.size() > 0) {
                    for (Conversation item : mAdapter.allMembers) {
                        for (User user : users) {
                            if (user.id.equals(item.getSenderUserId())) {
                                if (user.name != null) {
                                    item.setSenderUserName(user.name);
                                }
                                item.setPortraitUrl(user.portraitUrl);
                                break;
                            }
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        });

        RongUserInfoManager.getInstance().getAllGroupsLiveData().observe(this, new Observer<List<io.rong.imkit.userinfo.db.model.Group>>() {
            @Override
            public void onChanged(List<io.rong.imkit.userinfo.db.model.Group> groups) {
                if (groups != null && groups.size() > 0) {
                    for (Conversation item : mAdapter.allMembers) {
                        for (io.rong.imkit.userinfo.db.model.Group group : groups) {
                            if (group.id.equals(item.getSenderUserId())) {
                                if (group.name != null) {
                                    item.setSenderUserName(group.name);
                                }
                                item.setPortraitUrl(group.portraitUrl);
                                break;
                            }
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        getConversationList(false);
    }


    private void getConversationList(boolean isLoadMore) {
        getConversationList(defConversationType, new IHistoryDataResultCallback<List<Conversation>>() {
            @Override
            public void onResult(List<Conversation> data) {
                if (data != null && data.size() > 0) {
                    for (Conversation conversation : data) {
                        if (conversation.getConversationType().equals(Conversation.ConversationType.PRIVATE) || conversation.getConversationType().equals(Conversation.ConversationType.ENCRYPTED)) {
                            UserInfo user = RongUserInfoManager.getInstance().getUserInfo(conversation.getTargetId());
                            if (user != null) {
                                conversation.setConversationTitle(user.getName());
                                conversation.setPortraitUrl(user.getPortraitUri() == null ? null : user.getPortraitUri().toString());
                            }
                        } else {
                            Group groupInfo = RongUserInfoManager.getInstance().getGroupInfo(conversation.getTargetId());
                            if (groupInfo != null) {
                                conversation.setConversationTitle(groupInfo.getName());
                                conversation.setPortraitUrl(groupInfo.getPortraitUri() == null ? null : groupInfo.getPortraitUri().toString());
                            }
                        }
                    }
                    mAdapter.setAllMembers(data);
                    mAdapter.notifyDataSetChanged();
                }
                if (data == null) {
                    mRefreshLayout.setLoadMoreFinish(false);
                } else if (data.size() > 0 && data.size() <= pageSize) {
                    mRefreshLayout.setLoadMoreFinish(false);
                } else if (data.size() == 0) {
                    mRefreshLayout.setLoadMoreFinish(false);
                    mRefreshLayout.setCanLoading(false);
                } else {
                    mRefreshLayout.setLoadMoreFinish(false);
                }
            }

            @Override
            public void onError() {
                mRefreshLayout.setLoadMoreFinish(false);
            }
        }, isLoadMore);
    }

    public void getConversationList(Conversation.ConversationType[] conversationTypes, final IHistoryDataResultCallback<List<Conversation>> callback, boolean isLoadMore) {
        long lTimestamp = isLoadMore ? timestamp : 0;
        RongIMClient.getInstance().getConversationListByPage(new RongIMClient.ResultCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> conversations) {
                if (isFinishing()) {
                    return;
                }
                if (callback != null) {
                    if (conversations != null) {
                        timestamp = conversations.get(conversations.size() - 1).getSentTime();
                    }
                    callback.onResult(conversations);
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError();
                }
            }
        }, lTimestamp, pageSize, conversationTypes);
    }

    @Override
    public void onLoad() {
        getConversationList(true);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rc_btn_ok) {
            if (isFinishing()) {
                return;
            }
            ForwardManager.setForwardMessageResult(ForwardSelectConversationActivity.this, selectedMember);
        } else if (id == R.id.rc_btn_cancel) {
            finish();
        }
    }

    private class ForwardItemClickListener implements AdapterView.OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            View v = view.findViewById(R.id.rc_checkbox);
            Conversation member = (Conversation) v.getTag();
            selectedMember.remove(member);
            v.setSelected(!v.isSelected());
            if (v.isSelected()) {
                selectedMember.add(member);
            }

            if (selectedMember.size() > 0) {
                btOK.setEnabled(true);
            } else {
                btOK.setEnabled(false);
            }
        }
    }

    private class ListAdapter extends BaseAdapter {
        private Activity activity;
        private List<Conversation> allMembers = new ArrayList<>();

        ListAdapter(Activity activity) {
            this.activity = activity;
        }

        void setAllMembers(List<Conversation> allMembers) {
            this.allMembers = allMembers;
        }

        @Override
        public int getCount() {
            if (allMembers == null) {
                return 0;
            }
            return allMembers.size();
        }

        @Override
        public Object getItem(int position) {
            if (allMembers == null || allMembers.size() == 0) {
                return null;
            }
            return allMembers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(activity).inflate(R.layout.rc_listitem_forward_select_member, null);
                holder.checkbox = convertView.findViewById(R.id.rc_checkbox);
                holder.portrait = convertView.findViewById(R.id.rc_user_portrait);
                holder.name = convertView.findViewById(R.id.rc_user_name);
                convertView.setTag(holder);
            }
            holder = (ViewHolder) convertView.getTag();
            holder.checkbox.setTag(allMembers.get(position));
            holder.checkbox.setClickable(false);
            holder.checkbox.setImageResource(R.drawable.rc_select_conversation_checkbox);
            holder.checkbox.setEnabled(true);
            Conversation conversation = allMembers.get(position);
            holder.checkbox.setSelected(selectedMember.contains(conversation));
            Glide.with(IMCenter.getInstance().getContext()).load(conversation.getPortraitUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .into(holder.portrait);
            holder.name.setText(conversation.getConversationTitle());
            return convertView;
        }
    }

    private class ViewHolder {
        ImageView checkbox;
        ImageView portrait;
        TextView name;
    }

}
