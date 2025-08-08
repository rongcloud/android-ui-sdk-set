package io.rong.imkit.usermanage.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.GroupApplicationDirection;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationStatus;
import io.rong.imlib.model.GroupApplicationType;
import io.rong.imlib.model.GroupInfo;
import io.rong.imlib.model.GroupMemberInfo;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

/**
 * 群组申请列表适配器
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationsAdapter
        extends RecyclerView.Adapter<GroupApplicationsAdapter.GroupApplicationViewHolder> {

    private final List<GroupApplicationInfo> data = new ArrayList<>();
    private OnActionClickListener<GroupApplicationInfo> onAcceptClickListener;
    private OnActionClickListener<GroupApplicationInfo> onRejectClickListener;

    private final WeakHashMap<String, GroupInfo> groupInfoCacheMap = new WeakHashMap<>();

    /**
     * 设置接受按钮点击事件
     *
     * @param listener 点击事件监听
     */
    public void setOnAcceptClickListener(OnActionClickListener<GroupApplicationInfo> listener) {
        this.onAcceptClickListener = listener;
    }

    /**
     * 设置拒绝按钮点击事件
     *
     * @param listener 点击事件监听
     */
    public void setOnRejectClickListener(OnActionClickListener<GroupApplicationInfo> listener) {
        this.onRejectClickListener = listener;
    }

    public void setData(List<GroupApplicationInfo> newData) {
        if (newData != null) {
            data.clear();
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.rc_item_group_application, parent, false);
        return new GroupApplicationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupApplicationViewHolder holder, int position) {
        GroupApplicationInfo groupApplicationInfo = data.get(position);
        RongConfigCenter.featureConfig()
                .getKitImageEngine()
                .loadUserPortrait(
                        holder.getContext(), getPortraitUri(groupApplicationInfo), holder.ivHead);

        // 设置标题和内容
        holder.tvTitle.setText(
                getTitleFromApplicationInfo(groupApplicationInfo, holder.tvTitle.getContext()));

        String groupId = groupApplicationInfo.getGroupId();
        // 从缓存中加载群组名称
        if (groupInfoCacheMap.containsKey(groupId)) {
            holder.tvContent.setText(groupInfoCacheMap.get(groupId).getGroupName());
        } else {
            holder.tvContent.setText(""); // 清空旧内容，避免复用问题
            new GroupInfoLoader(holder, groupId, groupInfoCacheMap).execute();
        }

        // 根据状态显示按钮
        switch (groupApplicationInfo.getStatus()) {
            case InviteeUnHandled:
                if (groupApplicationInfo.getDirection()
                        == GroupApplicationDirection.ApplicationSent) {
                    updateButtonVisibility(holder, R.string.rc_manager_pending, true);
                } else if (groupApplicationInfo.getDirection()
                        == GroupApplicationDirection.InvitationSent) {
                    updateButtonVisibility(holder, R.string.rc_invitee_pending, true);
                } else if (groupApplicationInfo.getDirection()
                        == GroupApplicationDirection.ApplicationReceived) {
                    updateButtonVisibility(holder, R.string.rc_invitee_pending, true);
                } else {
                    updateButtonVisibility(holder, 0, false);
                }
                break;
            case ManagerUnHandled:
                if (groupApplicationInfo.getDirection() == GroupApplicationDirection.ApplicationSent
                        || groupApplicationInfo.getDirection()
                                == GroupApplicationDirection.InvitationSent) {
                    updateButtonVisibility(holder, R.string.rc_manager_pending, true);
                } else {
                    updateButtonVisibility(holder, 0, false);
                }
                break;
            case ManagerRefused:
                updateButtonVisibility(holder, R.string.rc_manager_rejected, true);
                break;
            case InviteeRefused:
                updateButtonVisibility(holder, R.string.rc_invitee_rejected, true);
                break;
            case Joined:
                updateButtonVisibility(holder, R.string.rc_joined, true);
                break;
            case Expired:
                updateButtonVisibility(holder, R.string.rc_expired, true);
                break;
        }

        // 接受按钮点击事件
        holder.tvAccept.setOnClickListener(
                v -> {
                    if (onAcceptClickListener != null) {
                        onAcceptClickListener.onActionClickWithConfirm(
                                groupApplicationInfo,
                                coreErrorCode -> {
                                    if (coreErrorCode instanceof IRongCoreEnum.CoreErrorCode) {
                                        if (coreErrorCode == IRongCoreEnum.CoreErrorCode.SUCCESS
                                                || coreErrorCode
                                                        == IRongCoreEnum.CoreErrorCode
                                                                .RC_GROUP_NEED_INVITEE_ACCEPT) {
                                            @StringRes int resid = R.string.rc_joined;
                                            if (coreErrorCode
                                                    == IRongCoreEnum.CoreErrorCode.SUCCESS) {
                                                resid = R.string.rc_joined;
                                            } else if (coreErrorCode
                                                    == IRongCoreEnum.CoreErrorCode
                                                            .RC_GROUP_NEED_INVITEE_ACCEPT) {
                                                resid = R.string.rc_invitee_pending;
                                            } else {
                                                resid = R.string.rc_set_failed;
                                            }
                                            holder.tvResult.setVisibility(View.VISIBLE);
                                            holder.tvResult.setText(resid);
                                            holder.tvReject.setVisibility(View.GONE);
                                            holder.tvAccept.setVisibility(View.GONE);
                                        }
                                    }
                                });
                    }
                });

        // 拒绝按钮点击事件
        holder.tvReject.setOnClickListener(
                v -> {
                    if (onRejectClickListener != null) {
                        onRejectClickListener.onActionClickWithConfirm(
                                groupApplicationInfo,
                                isSuccess -> {
                                    if (isSuccess instanceof Boolean && (Boolean) isSuccess) {
                                        GroupApplicationStatus status =
                                                groupApplicationInfo.getStatus();
                                        @StringRes int resid = R.string.rc_rejected;
                                        if (status == GroupApplicationStatus.InviteeUnHandled) {
                                            resid = R.string.rc_invitee_rejected;
                                        } else if (status
                                                == GroupApplicationStatus.ManagerUnHandled) {
                                            resid = R.string.rc_manager_rejected;
                                        }
                                        holder.tvResult.setVisibility(View.VISIBLE);
                                        holder.tvResult.setText(resid);
                                        holder.tvReject.setVisibility(View.GONE);
                                        holder.tvAccept.setVisibility(View.GONE);
                                    }
                                });
                    }
                });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void updateButtonVisibility(
            GroupApplicationViewHolder holder,
            @StringRes int resultTextResId,
            boolean isShowResult) {
        if (resultTextResId != 0) {
            holder.tvResult.setText(resultTextResId);
        }
        holder.tvResult.setVisibility(isShowResult ? View.VISIBLE : View.GONE);
        holder.tvReject.setVisibility(isShowResult ? View.GONE : View.VISIBLE);
        holder.tvAccept.setVisibility(isShowResult ? View.GONE : View.VISIBLE);
    }

    private String getPortraitUri(GroupApplicationInfo info) {
        GroupApplicationDirection direction = info.getDirection();
        if (direction == GroupApplicationDirection.InvitationReceived
                || direction == GroupApplicationDirection.InvitationSent) {
            return info.getInviterInfo().getPortraitUri();
        }
        if (direction == GroupApplicationDirection.ApplicationSent) {
            return info.getJoinMemberInfo().getPortraitUri();
        }
        return info.getInviterInfo() != null
                ? info.getInviterInfo().getPortraitUri()
                : info.getJoinMemberInfo().getPortraitUri();
    }

    private String getTitleFromApplicationInfo(GroupApplicationInfo info, Context context) {
        GroupApplicationDirection direction = info.getDirection();
        GroupApplicationType type = info.getType();
        String inviterName = getGroupMemberName(info.getInviterInfo());
        String joinMemberName = getGroupMemberName(info.getJoinMemberInfo());

        switch (direction) {
            case InvitationReceived:
                return type == GroupApplicationType.Invitation
                        ? context.getString(
                                R.string.rc_group_invitation_received_inviter, inviterName)
                        : context.getString(
                                R.string.rc_group_application_received_invitation,
                                inviterName,
                                joinMemberName);
            case ApplicationReceived:
                return type == GroupApplicationType.Invitation
                        ? context.getString(
                                R.string.rc_group_application_received_invitation,
                                inviterName,
                                joinMemberName)
                        : context.getString(
                                R.string.rc_group_application_received_request, joinMemberName);
            case ApplicationSent:
                return context.getString(R.string.rc_group_application_sent);
            case InvitationSent:
                return context.getString(R.string.rc_group_invitation_sent, joinMemberName);
            default:
                return "";
        }
    }

    private String getGroupMemberName(GroupMemberInfo info) {
        if (info == null) {
            return "";
        }
        return TextUtils.isEmpty(info.getNickname()) ? info.getName() : info.getNickname();
    }

    static class GroupApplicationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHead;
        TextView tvTitle, tvContent, tvResult, tvReject, tvAccept;
        LinearLayout llBtn;

        public GroupApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHead = itemView.findViewById(R.id.iv_head);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvResult = itemView.findViewById(R.id.tv_result);
            tvReject = itemView.findViewById(R.id.tv_reject);
            tvAccept = itemView.findViewById(R.id.tv_accept);
            llBtn = itemView.findViewById(R.id.ll_btn);
        }

        Context getContext() {
            return itemView.getContext();
        }
    }

    private static class GroupInfoLoader {
        private final WeakReference<GroupApplicationViewHolder> weakHolder;
        private final String groupId;
        private final WeakHashMap<String, GroupInfo> cache;

        GroupInfoLoader(
                GroupApplicationViewHolder holder,
                String groupId,
                WeakHashMap<String, GroupInfo> cache) {
            this.weakHolder = new WeakReference<>(holder);
            this.groupId = groupId;
            this.cache = cache;
        }

        void execute() {
            RongCoreClient.getInstance()
                    .getGroupsInfo(
                            Arrays.asList(groupId),
                            new IRongCoreCallback.ResultCallback<List<GroupInfo>>() {
                                @Override
                                public void onSuccess(List<GroupInfo> groupInfos) {
                                    if (groupInfos != null && !groupInfos.isEmpty()) {
                                        GroupInfo groupInfo = groupInfos.get(0);
                                        cache.put(groupId, groupInfo);

                                        GroupApplicationViewHolder holder = weakHolder.get();
                                        if (holder != null) {
                                            holder.tvContent.setText(groupInfo.getGroupName());
                                        }
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    // 错误处理
                                    RLog.w(
                                            "GroupApplicationsAdapter",
                                            "GroupInfoLoader get group info error: " + e);
                                }
                            });
        }
    }
}
