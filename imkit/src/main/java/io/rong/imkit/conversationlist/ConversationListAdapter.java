package io.rong.imkit.conversationlist;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.widget.adapter.BaseAdapter;
import io.rong.imkit.widget.adapter.ViewHolder;
import java.util.ArrayList;
import java.util.List;

public class ConversationListAdapter extends BaseAdapter<BaseUiConversation> {

    public ConversationListAdapter() {
        super();
        mProviderManager = RongConfigCenter.conversationListConfig().getProviderManager();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void setDataCollection(List<BaseUiConversation> data) {
        if (data == null) {
            data = new ArrayList<>();
        }
        super.setDataCollection(data);
        notifyDataSetChanged();
    }
}
