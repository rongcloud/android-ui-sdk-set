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
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;
import java.util.List;

public class QuickReplyBoard {
    private final AdapterView.OnItemClickListener mListener;
    private ListView mListView;
    private List<String> mPhraseList;
    private View mRootView;
    private ConversationIdentifier mConversationIdentifier;

    public QuickReplyBoard(
            @NonNull Context context,
            ViewGroup parent,
            List<String> phraseList,
            AdapterView.OnItemClickListener listener) {
        mPhraseList = phraseList;
        mListener = listener;
        initView(context, parent);
    }

    private void initView(Context context, ViewGroup parent) {
        // 根据主题选择不同的布局
        int layoutResId =
                IMKitThemeManager.dynamicResource(
                        R.layout.rc_ext_quick_reply_list_v2, R.layout.rc_ext_quick_reply_list);
        mRootView = LayoutInflater.from(context).inflate(layoutResId, parent, false);
        mListView = mRootView.findViewById(R.id.rc_list);
        PhrasesAdapter adapter = new PhrasesAdapter();
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(
                            AdapterView<?> parent, View view, int position, long id) {
                        sendMessage(mPhraseList.get(position));
                        if (mListener != null) {
                            mListener.onItemClick(parent, view, position, id);
                        }
                    }
                });
    }

    private void sendMessage(String text) {
        Message message = Message.obtain(mConversationIdentifier, TextMessage.obtain(text));
        List<IExtensionEventWatcher> watchers =
                RongExtensionManager.getInstance().getExtensionEventWatcher();
        if (!watchers.isEmpty()) {
            for (IExtensionEventWatcher watcher : watchers) {
                watcher.onSendToggleClick(message);
            }
        }
        IMCenter.getInstance().sendMessage(message, null, null, null);
    }

    public void setAttachedConversation(RongExtension extension) {
        if (extension != null) {
            mConversationIdentifier = extension.getConversationIdentifier();
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
                Context context = parent.getContext();
                // 根据主题选择不同的列表项布局
                int itemLayoutResId =
                        IMKitThemeManager.dynamicResource(
                                R.layout.rc_ext_quick_reply_list_item_v2,
                                R.layout.rc_ext_quick_reply_list_item);
                convertView = LayoutInflater.from(context).inflate(itemLayoutResId, parent, false);

                // V1 版本需要动态设置高度
                if (IMKitThemeManager.isTraditionTheme()) {
                    int height =
                            (int)
                                            context.getResources()
                                                    .getDimension(R.dimen.rc_extension_board_height)
                                    / 5;
                    RelativeLayout.LayoutParams layoutParams =
                            new RelativeLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, height);
                    convertView.setLayoutParams(layoutParams);
                }
            }
            TextView tvPhrases = convertView.findViewById(R.id.rc_phrases_tv);
            tvPhrases.setText(mPhraseList.get(position));
            return convertView;
        }
    }
}
