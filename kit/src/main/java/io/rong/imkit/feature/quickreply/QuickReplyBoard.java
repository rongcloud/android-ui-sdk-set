package io.rong.imkit.feature.quickreply;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;

public class QuickReplyBoard {
    private ListView mListView;
    private List<String> mPhraseList;
    private View mRootView;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;


    public QuickReplyBoard(@NonNull Context context, ViewGroup parent, List<String> phraseList) {
        mPhraseList = phraseList;
        initView(context, parent);
    }

    private void initView(Context context, ViewGroup parent) {
        mRootView = LayoutInflater.from(context).inflate(R.layout.rc_ext_quick_reply_list, parent, false);
        mListView = mRootView.findViewById(R.id.rc_list);
        PhrasesAdapter adapter = new PhrasesAdapter();
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String text = mPhraseList.get(position);
                TextMessage textMessage = TextMessage.obtain(text);
                IMCenter.getInstance().sendMessage(Message.obtain(mTargetId, mConversationType, textMessage), null, null, null);
            }
        });
    }

    public void setAttachedConversation(RongExtension extension) {
        if(extension != null) {
            mConversationType = extension.getConversationType();
            mTargetId = extension.getTargetId();
        }
    }

    public View getRootView() {
        return mRootView;
    }

    private class PhrasesAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mPhraseList.size();
        }

        @Override
        public Object getItem(int position) {
            return mPhraseList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_ext_quick_reply_list_item, null);
                int height = (int) parent.getContext().getResources().getDimension(R.dimen.rc_extension_board_height) / 5;
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
                convertView.setLayoutParams(layoutParams);
            }
            TextView tvPhrases = convertView.findViewById(R.id.rc_phrases_tv);
            tvPhrases.setText(mPhraseList.get(position));
            return convertView;
        }
    }
}
