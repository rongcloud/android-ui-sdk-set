package io.rong.sight.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.TimeUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.SightMessage;
import io.rong.sight.R;
import java.util.ArrayList;
import java.util.List;

public class SightListActivity extends RongBaseNoActionbarActivity
        implements RongUserInfoManager.UserDataObserver {
    private String targetId;
    private Conversation.ConversationType conversationType;
    private SightListAdapter sightListAdapter;
    private static final int DEFAULT_FILE_COUNT = 100;
    private boolean isDestruct;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_activity_sight_list);
        Intent intent = getIntent();
        targetId = intent.getStringExtra("targetId");
        isDestruct = intent.getBooleanExtra("isDestruct", false);
        conversationType =
                Conversation.ConversationType.setValue(intent.getIntExtra("conversationType", 0));
        ListView fileListView = findViewById(R.id.sightList);
        sightListAdapter = new SightListAdapter();
        fileListView.setAdapter(sightListAdapter);

        RongIMClient.getInstance()
                .getHistoryMessages(
                        conversationType,
                        targetId,
                        "RC:SightMsg",
                        -1,
                        DEFAULT_FILE_COUNT,
                        new RongIMClient.ResultCallback<List<Message>>() {
                            @Override
                            public void onSuccess(final List<Message> messages) {
                                if (messages != null && messages.size() > 0) {
                                    setListAdapterData(messages, sightListAdapter);
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                // default implementation ignored
                            }
                        });
        findViewById(R.id.imgbtn_nav_back)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
        initUserInfoChangeListener();
    }

    private void initUserInfoChangeListener() {
        RongUserInfoManager.getInstance().addUserDataObserver(this);
    }

    @Override
    protected void onDestroy() {
        RongUserInfoManager.getInstance().removeUserDataObserver(this);
        super.onDestroy();
    }

    private void setListAdapterData(List<Message> messages, SightListAdapter sightListAdapter) {
        List<ItemData> itemDataList = new ArrayList<>();
        for (Message message : messages) {
            if (isDestruct != message.getContent().isDestruct()) {
                continue;
            }
            ItemData data = new ItemData();
            data.message = message;
            data.senderName = getSenderName(message.getSenderUserId());
            itemDataList.add(data);
        }
        sightListAdapter.setFileData(itemDataList);
        sightListAdapter.notifyDataSetChanged();
    }

    public void updateUserInfo(UserInfo userInfo) {
        boolean needUpdate = false;
        for (ItemData itemData : sightListAdapter.getData()) {
            if (itemData.message.getSenderUserId().equals(userInfo.getUserId())) {
                itemData.senderName = getSenderName(itemData.message.getSenderUserId());
                needUpdate = true;
            }
        }
        if (needUpdate) {
            sightListAdapter.notifyDataSetChanged();
        }
    }

    public void updateGroupUserInfo(GroupUserInfo groupMember) {
        boolean needUpdate = false;
        if (groupMember != null
                && conversationType.equals(Conversation.ConversationType.GROUP)
                && targetId.equals(groupMember.getGroupId())) {
            for (ItemData itemData : sightListAdapter.getData()) {
                if (itemData.message.getSenderUserId().equals(groupMember.getUserId())) {
                    itemData.senderName = getSenderName(itemData.message.getSenderUserId());
                    needUpdate = true;
                }
            }
            if (needUpdate) {
                sightListAdapter.notifyDataSetChanged();
            }
        }
    }

    private String getSenderName(String senderUserId) {
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(senderUserId);
        String groupMemberName = "";
        if (conversationType.equals(Conversation.ConversationType.GROUP)) {
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance().getGroupUserInfo(targetId, senderUserId);
            groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
        }
        return RongUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
    }

    @Override
    public void onUserUpdate(UserInfo user) {
        updateUserInfo(user);
    }

    @Override
    public void onGroupUpdate(Group group) {
        // default implementation ignored
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {
        updateGroupUserInfo(groupUserInfo);
    }

    private class SightListAdapter extends BaseAdapter {
        List<ItemData> fileData = new ArrayList<>();

        public void setFileData(List<ItemData> fileData) {
            this.fileData.addAll(fileData);
        }

        public List<ItemData> getData() {
            return fileData;
        }

        @Override
        public int getCount() {
            return fileData.size();
        }

        @Override
        public Object getItem(int position) {
            return fileData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.rc_sight_list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.itemIcon = convertView.findViewById(R.id.rc_portrait);
                viewHolder.itemTitle = convertView.findViewById(R.id.rc_title);
                viewHolder.itemDetail = convertView.findViewById(R.id.rc_detail);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            ItemData itemData = fileData.get(position);
            final Message message = itemData.message;
            final SightMessage sightMessage = (SightMessage) message.getContent();
            viewHolder.itemTitle.setText(sightMessage.getName());
            String time = TimeUtils.formatData(SightListActivity.this, message.getSentTime());
            String size = convertFileSize(sightMessage.getSize());
            String detail = String.format("%s %s %s", itemData.senderName, time, size);
            viewHolder.itemDetail.setText(detail);
            viewHolder.itemIcon.setImageResource(
                    IMKitThemeManager.getAttrResId(
                            SightListActivity.this, R.attr.rc_ic_sight_video));
            convertView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent =
                                    new Intent(SightListActivity.this, SightPlayerActivity.class);
                            intent.putExtra("Message", message);
                            intent.putExtra("SightMessage", sightMessage);
                            intent.putExtra("fromList", true);
                            startActivity(intent);
                        }
                    });
            return convertView;
        }
    }

    private static class ViewHolder {
        ImageView itemIcon;
        TextView itemTitle;
        TextView itemDetail;
    }

    private static class ItemData {
        Message message;
        String senderName;
    }

    private String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size < kb) {
            return String.format("%.2f B", (float) size);
        } else if (size < mb) return String.format("%.2f KB", (float) size / kb);
        else if (size < gb) return String.format("%.2f MB", (float) size / mb);
        else return String.format("%.2f G", (float) size / gb);
    }
}
