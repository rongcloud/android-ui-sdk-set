package io.rong.imkit.usermanage.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupMembersAdapter
        extends RecyclerView.Adapter<GroupMembersAdapter.GroupInfoViewHolder> {

    private List<GroupMemberInfo> groupInfoList;
    private final Context context;
    private final int groupDisplayLimit;
    private boolean allowGroupRemoval = false;
    private boolean allowGroupAddition = false;
    private OnGroupActionListener groupActionListener;

    public GroupMembersAdapter(Context context, int groupDisplayLimit) {
        this.context = context;
        this.groupDisplayLimit = groupDisplayLimit;
    }

    @NonNull
    @Override
    public GroupInfoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(context).inflate(R.layout.rc_item_group_member, parent, false);
        return new GroupInfoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupInfoViewHolder holder, int position) {
        if (isSpecialActionPosition(position)) {
            setupSpecialActionItem(holder, position);
        } else {
            setupGroupInfoItem(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        int extraItems =
                (allowGroupRemoval && allowGroupAddition)
                        ? 2
                        : (allowGroupRemoval || allowGroupAddition) ? 1 : 0;
        return (groupInfoList != null ? groupInfoList.size() : 0) + extraItems;
    }

    private boolean isSpecialActionPosition(int position) {
        return groupInfoList == null || position >= groupInfoList.size();
    }

    private void setupSpecialActionItem(@NonNull GroupInfoViewHolder holder, int position) {
        // 清除 ImageView 上可能正在进行的 Glide 加载请求，防止异步加载覆盖特殊操作图标
        Glide.with(context).clear(holder.avatarImageView);

        if (position == getItemCount() - 1 && allowGroupRemoval) {
            holder.groupNameTextView.setText("");
            holder.groupNameTextView.setVisibility(View.GONE);
            holder.avatarImageView.setImageResource(
                    IMKitThemeManager.getAttrResId(context, R.attr.rc_group_member_remove_img));
            holder.itemView.setOnClickListener(
                    v -> {
                        if (groupActionListener != null) {
                            groupActionListener.removeMemberClick();
                        }
                    });
        } else if (allowGroupAddition) {
            holder.groupNameTextView.setText("");
            holder.groupNameTextView.setVisibility(View.GONE);
            holder.avatarImageView.setImageResource(
                    IMKitThemeManager.getAttrResId(context, R.attr.rc_group_member_add_img));
            holder.itemView.setOnClickListener(
                    v -> {
                        if (groupActionListener != null) {
                            groupActionListener.addMemberClick();
                        }
                    });
        }
    }

    private void setupGroupInfoItem(@NonNull GroupInfoViewHolder holder, int position) {
        GroupMemberInfo groupMemberInfo = groupInfoList.get(position);
        holder.groupNameTextView.setText(
                !TextUtils.isEmpty(groupMemberInfo.getNickname())
                        ? groupMemberInfo.getNickname()
                        : groupMemberInfo.getName());
        holder.groupNameTextView.setVisibility(View.VISIBLE);

        RongCoreClient.getInstance()
                .getFriendsInfo(
                        Collections.singletonList(groupMemberInfo.getUserId()),
                        new FriendsInfoCallback(holder));

        String portraitUri = groupMemberInfo.getPortraitUri();
        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadUserPortrait(
                        holder.avatarImageView.getContext(), portraitUri, holder.avatarImageView);

        holder.itemView.setOnClickListener(
                v -> {
                    if (groupActionListener != null) {
                        groupActionListener.onGroupClicked(groupMemberInfo);
                    }
                });
    }

    private static class FriendsInfoCallback
            extends IRongCoreCallback.ResultCallback<List<FriendInfo>> {
        private final WeakReference<GroupInfoViewHolder> holderRef;

        public FriendsInfoCallback(GroupInfoViewHolder holder) {
            this.holderRef = new WeakReference<>(holder);
        }

        @Override
        public void onSuccess(List<FriendInfo> friendInfos) {
            GroupInfoViewHolder viewHolder = holderRef.get();
            if (viewHolder != null && friendInfos != null && !friendInfos.isEmpty()) {
                FriendInfo friendInfo = friendInfos.get(0);
                if (friendInfo != null && !TextUtils.isEmpty(friendInfo.getRemark())) {
                    if (viewHolder.groupNameTextView != null) {
                        viewHolder.groupNameTextView.setText(friendInfo.getRemark());
                    }
                }
            }
        }

        @Override
        public void onError(IRongCoreEnum.CoreErrorCode e) {
            GroupInfoViewHolder viewHolder = holderRef.get();
            if (viewHolder != null) {
                RLog.e("GroupMembersAdapter", "getFriendsInfo error: " + e.getMessage());
            }
        }
    }

    public void setAllowGroupRemoval(boolean allowGroupRemoval) {
        this.allowGroupRemoval = allowGroupRemoval;
        notifyDataSetChanged();
    }

    public void setAllowGroupAddition(boolean allowGroupAddition) {
        this.allowGroupAddition = allowGroupAddition;
        notifyDataSetChanged();
    }

    public void updateGroupInfoList(List<GroupMemberInfo> groupInfoList) {
        groupInfoList = new CopyOnWriteArrayList<>(groupInfoList);
        if (groupDisplayLimit > 0
                && groupInfoList != null
                && groupInfoList.size() > groupDisplayLimit) {
            this.groupInfoList = groupInfoList.subList(0, groupDisplayLimit);
        } else {
            this.groupInfoList = groupInfoList;
        }
        notifyDataSetChanged();
    }

    public void setOnGroupActionListener(OnGroupActionListener listener) {
        this.groupActionListener = listener;
    }

    public interface OnGroupActionListener {
        /** 添加群成员的点击事件 */
        default void addMemberClick() {}

        /** 删除群成员的点击事件 */
        default void removeMemberClick() {}

        /**
         * 群成员的点击事件
         *
         * @param groupInfo 群成员信息
         */
        void onGroupClicked(GroupMemberInfo groupInfo);
    }

    public static class GroupInfoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImageView;
        private final TextView groupNameTextView;

        public GroupInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.iv_group_member_avatar);
            groupNameTextView = itemView.findViewById(R.id.tv_group_member_name);
        }
    }
}
