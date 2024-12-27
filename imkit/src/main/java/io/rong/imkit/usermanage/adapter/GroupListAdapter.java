package io.rong.imkit.usermanage.adapter;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imlib.model.GroupInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 群组列表适配器
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupListAdapter extends RecyclerView.Adapter<GroupListAdapter.GroupListViewHolder> {

    private List<GroupInfo> data = new ArrayList<>();

    private OnActionClickListener<GroupInfo> onItemClickListener;
    private OnActionClickListener<GroupInfo> onItemLongClickListener;

    private String highlightedText = null;

    public void setData(List<GroupInfo> newData) {
        if (newData == null) {
            newData = new CopyOnWriteArrayList<>();
        }
        this.data = new CopyOnWriteArrayList<>(newData);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnActionClickListener<GroupInfo> listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnActionClickListener<GroupInfo> listener) {
        this.onItemLongClickListener = listener;
    }

    /**
     * 设置高亮文本
     *
     * @param text 高亮文本
     */
    public void setHighlightedText(String text) {
        highlightedText = text;
    }

    @NonNull
    @Override
    public GroupListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.rc_item_group_info, parent, false);
        return new GroupListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupListViewHolder holder, int position) {
        GroupInfo groupInfo = data.get(position);

        // 加载群头像
        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadGroupPortrait(
                        holder.itemView.getContext(), groupInfo.getPortraitUri(), holder.ivHead);

        String groupName =
                TextUtils.isEmpty(groupInfo.getRemark())
                        ? groupInfo.getGroupName()
                        : groupInfo.getRemark();
        // 设置群名称
        holder.tvTitle.setText(
                getHighlightedText(holder.tvTitle.getContext(), groupName, highlightedText));

        // 添加点击事件 (示例，可自定义行为)
        holder.itemView.setOnClickListener(
                v -> {
                    if (onItemClickListener != null) {
                        onItemClickListener.onActionClick(groupInfo);
                    }
                });
        holder.itemView.setOnLongClickListener(
                v -> {
                    if (onItemLongClickListener != null) {
                        onItemLongClickListener.onActionClick(groupInfo);
                    }
                    return false;
                });
    }

    private SpannableStringBuilder getHighlightedText(
            Context context, String fullText, String searchText) {
        if (fullText == null || TextUtils.isEmpty(searchText)) {
            return new SpannableStringBuilder(fullText);
        }
        // 高亮颜色获取
        int highlightColor = ContextCompat.getColor(context, R.color.rc_read_receipt_status);

        // 不区分大小写的搜索
        SpannableStringBuilder spannable = new SpannableStringBuilder(fullText);
        Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fullText);

        // 只匹配到第一个符合的文本
        if (matcher.find()) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();

            // 设置高亮颜色
            spannable.setSpan(
                    new ForegroundColorSpan(highlightColor),
                    startIndex,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannable;
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class GroupListViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead;
        TextView tvTitle;

        public GroupListViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHead = itemView.findViewById(R.id.iv_head);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }
}
