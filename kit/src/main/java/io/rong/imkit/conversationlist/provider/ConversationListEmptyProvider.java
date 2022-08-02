package io.rong.imkit.conversationlist.provider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.widget.adapter.IViewProvider;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;

public class ConversationListEmptyProvider implements IViewProvider<BaseUiConversation> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_conversationlist_empty_view, parent, false);
        return ViewHolder.createViewHolder(parent.getContext(), view);
    }

    @Override
    public boolean isItemViewType(BaseUiConversation item) {
        return false;
    }

    @Override
    public void bindViewHolder(ViewHolder holder, BaseUiConversation baseUiConversation, int position, List<BaseUiConversation> list, IViewProviderListener<BaseUiConversation> listener) {

    }

}
