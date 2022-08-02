package io.rong.imkit.conversation;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.BaseAdapter;
import io.rong.imkit.widget.adapter.IViewProviderListener;

public class MessageListAdapter extends BaseAdapter<UiMessage> {

    public MessageListAdapter(IViewProviderListener<UiMessage> listener) {
        super(listener, RongConfigCenter.conversationConfig().getMessageListProvider());
    }

    @Override
    public void setDataCollection(List<UiMessage> data) {
        if (data == null) {
            data = new ArrayList<>();
        }
        //当有空布局的时候，需要全部刷新
        if ((mDataList.size() == 0 && data.size() > 0) ||
                (mDataList.size() > 0 && data.size() == 0)) {
            super.setDataCollection(data);
            notifyDataSetChanged();
        } else {
            mDiffCallback.setNewList(data);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(mDiffCallback, false);
            super.setDataCollection(data);
            diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
                @Override
                public void onInserted(int position, int count) {
                    notifyItemRangeInserted(getHeadersCount() + position, count);
                }

                @Override
                public void onRemoved(int position, int count) {
                    notifyItemRangeRemoved(getHeadersCount() + position, count);
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    notifyItemMoved(getHeadersCount() + fromPosition, getHeadersCount() + toPosition);
                }

                @Override
                public void onChanged(int position, int count, @Nullable Object payload) {
                    notifyItemRangeChanged(getHeadersCount() + position, count, null);
                }
            });
        }
    }

    MessageDiffCallBack mDiffCallback = new MessageDiffCallBack();


    private class MessageDiffCallBack extends DiffUtil.Callback {
        private List<UiMessage> newList;

        @Override
        public int getOldListSize() {
            if (mDataList != null) {
                return mDataList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getNewListSize() {
            if (newList != null) {
                return newList.size();
            } else {
                return 0;
            }
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mDataList.get(oldItemPosition).getMessageId() == newList.get(newItemPosition).getMessageId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UiMessage newItem = newList.get(newItemPosition);
            if (newItem.isChange()) {
                return false;
            }
            return true;
        }

        public void setNewList(List<UiMessage> newList) {
            this.newList = newList;
        }
    }

    ;
}