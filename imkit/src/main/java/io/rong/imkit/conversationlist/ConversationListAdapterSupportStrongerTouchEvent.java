package io.rong.imkit.conversationlist;

import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.widget.adapter.ViewHolder;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话列表增强Adapter，能够解决会话列表频繁刷新时不能响应点击和长按事件的问题
 *
 * @author chenjialong
 */
public class ConversationListAdapterSupportStrongerTouchEvent extends ConversationListAdapter {

    protected Handler mainHandler = new Handler(Looper.getMainLooper());
    protected boolean isItemClickEventScheduled = false;

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (isStrongerSupportConversationListItemClick()) {
            holder.itemView.setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                isItemClickEventScheduled = true;
                            }
                            return false;
                        }
                    });
        }
    }

    // 是否增强会话列表点击事件能力
    protected boolean isStrongerSupportConversationListItemClick() {
        return true;
    }

    protected long notifyDataSetChangedDelayMillis() {
        return 500;
    }

    @Override
    public void setDataCollection(List<BaseUiConversation> data) {
        if (data == null) {
            data = new ArrayList<>();
        }

        if (isItemClickEventScheduled) {
            List<BaseUiConversation> finalData = data;
            mainHandler.postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            isItemClickEventScheduled = false;
                            ConversationListAdapterSupportStrongerTouchEvent.super
                                    .setDataCollection(finalData);
                            notifyDataSetChanged();
                        }
                    },
                    notifyDataSetChangedDelayMillis());
        } else {
            super.setDataCollection(data);
            notifyDataSetChanged();
        }
    }
}
