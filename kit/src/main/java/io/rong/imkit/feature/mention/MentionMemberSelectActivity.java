package io.rong.imkit.feature.mention;


import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.CharacterParser;
import io.rong.imkit.widget.SideBar;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.discussion.model.Discussion;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

public class MentionMemberSelectActivity extends RongBaseNoActionbarActivity {
    private ListView mListView;
    private List<MemberInfo> mAllMemberList;
    private MembersAdapter mAdapter;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rc_mention_select_activity);

        EditText searchBar = findViewById(R.id.rc_edit_text);
        mListView = findViewById(R.id.rc_list);
        SideBar mSideBar = findViewById(R.id.rc_sidebar);
        TextView letterPopup = findViewById(R.id.rc_popup_bg);
        mSideBar.setTextView(letterPopup);

        mAdapter = new MembersAdapter();
        mListView.setAdapter(mAdapter);
        mAllMemberList = new ArrayList<>();

        String targetId = getIntent().getStringExtra("targetId");
        Conversation.ConversationType conversationType = Conversation.ConversationType.setValue(getIntent().getIntExtra("conversationType", 0));

        RongMentionManager.IGroupMembersProvider groupMembersProvider = RongMentionManager.getInstance().getGroupMembersProvider();
        if (conversationType.equals(Conversation.ConversationType.GROUP) && groupMembersProvider != null) {
            groupMembersProvider.getGroupMembers(targetId, new RongMentionManager.IGroupMemberCallback() {
                @Override
                public void onGetGroupMembersResult(final List<UserInfo> members) {
                    if (members != null && members.size() > 0) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 0; i < members.size(); i++) {
                                    UserInfo userInfo = members.get(i);
                                    if (userInfo != null && !userInfo.getUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                                        MemberInfo memberInfo = new MemberInfo(userInfo);
                                        String sortString = "#";
                                        //汉字转换成拼音
                                        String pinyin = CharacterParser.getInstance().getSelling(userInfo.getName());

                                        if (pinyin != null) {
                                            if (pinyin.length() > 0) {
                                                sortString = pinyin.substring(0, 1).toUpperCase();
                                            }
                                        }
                                        // 正则表达式，判断首字母是否是英文字母
                                        if (sortString.matches("[A-Z]")) {
                                            memberInfo.setLetter(sortString.toUpperCase());
                                        } else {
                                            memberInfo.setLetter("#");
                                        }
                                        mAllMemberList.add(memberInfo);
                                    }
                                }
                                Collections.sort(mAllMemberList, PinyinComparator.getInstance());
                                mAdapter.setData(mAllMemberList);
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            });
        } else if (conversationType.equals(Conversation.ConversationType.DISCUSSION)) {
            RongIMClient.getInstance().getDiscussion(targetId, new RongIMClient.ResultCallback<Discussion>() {
                @Override
                public void onSuccess(Discussion discussion) {
                    List<String> memeberIds = discussion.getMemberIdList();
                    for (String id : memeberIds) {
                        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(id);
                        if (userInfo != null && !userInfo.getUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                            MemberInfo memberInfo = new MemberInfo(userInfo);
                            String sortString = "#";
                            //汉字转换成拼音
                            String pinyin = CharacterParser.getInstance().getSelling(userInfo.getName());
                            if (pinyin != null) {
                                if (pinyin.length() > 0) {
                                    sortString = pinyin.substring(0, 1).toUpperCase();
                                }
                            }

                            // 正则表达式，判断首字母是否是英文字母
                            if (sortString.matches("[A-Z]")) {
                                memberInfo.setLetter(sortString.toUpperCase());
                            } else {
                                memberInfo.setLetter("#");
                            }
                            mAllMemberList.add(memberInfo);
                        }
                    }

                    Collections.sort(mAllMemberList, PinyinComparator.getInstance());
                    mAdapter.setData(mAllMemberList);
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                finish();
                MemberInfo item = mAdapter.getItem(position);
                if (item != null && item.userInfo != null) {
                    RongMentionManager.getInstance().mentionMember(item.userInfo);
                }
            }
        });

        //设置右侧触摸监听
        mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String s) {
                //该字母首次出现的位置
                int position = mAdapter.getPositionForSection(s.charAt(0));
                if (position != -1) {
                    mListView.setSelection(position);
                }
            }
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //当输入框里面的值为空，更新为原来的列表，否则为过滤数据列表
                List<MemberInfo> filterDataList = new ArrayList<>();

                if (TextUtils.isEmpty(s.toString())) {
                    filterDataList = mAllMemberList;
                } else {
                    filterDataList.clear();
                    for (MemberInfo member : mAllMemberList) {
                        String name = member.userInfo.getName();
                        if (name != null) {
                            if (name.contains(s) || CharacterParser.getInstance().getSelling(name).startsWith(s.toString())) {
                                filterDataList.add(member);
                            }
                        }
                    }
                }
                // 根据a-z进行排序
                Collections.sort(filterDataList, PinyinComparator.getInstance());
                mAdapter.setData(filterDataList);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        findViewById(R.id.rc_btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    class MembersAdapter extends BaseAdapter implements SectionIndexer {
        private List<MemberInfo> mList = new ArrayList<>();

        public void setData(List<MemberInfo> list) {
            mList = list;
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public MemberInfo getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_mention_list_item, null);
                viewHolder.name = convertView.findViewById(R.id.rc_user_name);
                viewHolder.portrait = convertView.findViewById(R.id.rc_user_portrait);
                viewHolder.letter = convertView.findViewById(R.id.letter);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            UserInfo userInfo = mList.get(position).userInfo;
            if (userInfo != null) {
                viewHolder.name.setText(userInfo.getName());
                Glide.with(convertView).load(userInfo.getPortraitUri()).into(viewHolder.portrait);
            }

            //根据position获取分类的首字母的Char ascii值
            int section = getSectionForPosition(position);
            //如果当前位置等于该分类首字母的Char的位置 ，则认为是第一次出现
            if (position == getPositionForSection(section)) {
                viewHolder.letter.setVisibility(View.VISIBLE);
                viewHolder.letter.setText(mList.get(position).getLetter());
            } else {
                viewHolder.letter.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public Object[] getSections() {
            return new Object[0];
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            for (int i = 0; i < getCount(); i++) {
                String sortStr = mList.get(i).getLetter();
                char firstChar = sortStr.toUpperCase().charAt(0);
                if (firstChar == sectionIndex) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mList.get(position).getLetter().charAt(0);
        }
    }

    class ViewHolder {
        ImageView portrait;
        TextView name;
        TextView letter;
    }

    private class MemberInfo {
        UserInfo userInfo;
        String letter;

        MemberInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }

        public void setLetter(String letter) {
            this.letter = letter;
        }

        public String getLetter() {
            return letter;
        }
    }

    public static class PinyinComparator implements Comparator<MemberInfo> {


        public static PinyinComparator instance = null;

        public static PinyinComparator getInstance() {
            if (instance == null) {
                instance = new PinyinComparator();
            }
            return instance;
        }

        public int compare(MemberInfo o1, MemberInfo o2) {
            if (o1.getLetter().equals("@") || o2.getLetter().equals("#")) {
                return -1;
            } else if (o1.getLetter().equals("#") || o2.getLetter().equals("@")) {
                return 1;
            } else {
                return o1.getLetter().compareTo(o2.getLetter());
            }
        }

    }

}
