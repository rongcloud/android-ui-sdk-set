package io.rong.imkit.usermanage.adapter.vh;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.handler.AppSettingsHandler;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.model.OnlineStatusFriendInfo;
import io.rong.imkit.usermanage.group.mention.GroupMentionFragment;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.utils.TextViewUtils;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

public class ContactSelectableViewHolder extends RecyclerView.ViewHolder {

    private final TextView contactNameTextView;
    private final TextView rightText;
    private final ImageView contactPortraitImageView;
    private final ImageView contactSelectImageView;
    private final ImageView rightArrow;
    private final ImageView tvRemove;
    private final View divider;
    private final boolean showSelectButton;
    private final boolean showItemRightArrow;
    private final boolean showItemRightText;
    private final boolean showItemSelectAutoUpdate;
    private final boolean showItemRemoveButton;
    private ContactModel<FriendInfo> data;

    public ContactSelectableViewHolder(
            @NonNull View itemView,
            OnActionClickListener<ContactModel> onItemClickListener,
            OnActionClickListener<ContactModel> onItemRemoveClickListener,
            boolean showSelectButton,
            boolean showItemRightArrow,
            boolean showItemRightText,
            boolean showItemSelectAutoUpdate,
            boolean showItemRemoveButton) {
        super(itemView);
        contactPortraitImageView = itemView.findViewById(R.id.iv_contact_portrait);
        contactNameTextView = itemView.findViewById(R.id.tv_contact_name);
        rightText = itemView.findViewById(R.id.tv_right_text);
        contactSelectImageView = itemView.findViewById(R.id.iv_contact_select);
        rightArrow = itemView.findViewById(R.id.iv_right_arrow);
        tvRemove = itemView.findViewById(R.id.tv_remove);
        divider = itemView.findViewById(R.id.divider);

        this.showSelectButton = showSelectButton;
        this.showItemRightArrow = showItemRightArrow;
        this.showItemRightText = showItemRightText;
        this.showItemSelectAutoUpdate = showItemSelectAutoUpdate;
        this.showItemRemoveButton = showItemRemoveButton;

        itemView.setOnClickListener(
                v -> {
                    if (data != null) {
                        // 更新选中状态(选中/未选中)
                        if (showSelectButton && showItemSelectAutoUpdate) {
                            updateCheckType();
                        }
                        if (onItemClickListener != null) {
                            onItemClickListener.onActionClickWithConfirm(
                                    data,
                                    (OnActionClickListener.OnConfirmClickListener<Boolean>)
                                            isUpdate -> {
                                                if (showSelectButton && isUpdate) {
                                                    updateCheckType();
                                                }
                                            });
                        }
                    }
                });

        tvRemove.setOnClickListener(
                v -> {
                    if (onItemRemoveClickListener != null) {
                        onItemRemoveClickListener.onActionClick(data);
                    }
                });
    }

    private void updateCheckType() {
        ContactModel.CheckType checkType = data.getCheckType();
        if (checkType == ContactModel.CheckType.CHECKED
                || checkType == ContactModel.CheckType.UNCHECKED) {
            checkType =
                    (checkType == ContactModel.CheckType.CHECKED)
                            ? ContactModel.CheckType.UNCHECKED
                            : ContactModel.CheckType.CHECKED;
            // 更新视图
            updateCheck(contactSelectImageView, checkType);
        }
    }

    public void bind(ContactModel contactModel) {
        this.data = contactModel;

        Object contactModelBean = contactModel.getBean();
        String name = "";
        String portraitUrl = null;
        String roleText = "";

        if (contactModelBean instanceof OnlineStatusFriendInfo
                || contactModelBean instanceof FriendInfo) {
            FriendInfo friendInfo =
                    contactModelBean instanceof OnlineStatusFriendInfo
                            ? ((OnlineStatusFriendInfo) contactModelBean).getFriendInfo()
                            : (FriendInfo) contactModelBean;
            name =
                    !TextUtils.isEmpty(friendInfo.getRemark())
                            ? friendInfo.getRemark()
                            : friendInfo.getName();
            portraitUrl = friendInfo.getPortraitUri();
            RongConfigCenter.featureConfig()
                    .getKitImageEngine()
                    .loadUserPortrait(
                            contactPortraitImageView.getContext(),
                            portraitUrl,
                            contactPortraitImageView);
        } else if (contactModelBean instanceof GroupMemberInfo) {
            GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModelBean;
            name =
                    !TextUtils.isEmpty(groupMemberInfo.getNickname())
                            ? groupMemberInfo.getNickname()
                            : groupMemberInfo.getName();
            // 使用静态内部类回调来获取朋友信息，避免内存泄露
            if (!GroupMentionFragment.class.getSimpleName().equals(contactModel.getExtra())) {
                RongCoreClient.getInstance()
                        .getFriendsInfo(
                                Collections.singletonList(groupMemberInfo.getUserId()),
                                new FriendsInfoCallback(this));
            }

            portraitUrl = groupMemberInfo.getPortraitUri();
            roleText = getRoleText(groupMemberInfo.getRole(), rightText);
            RongConfigCenter.featureConfig()
                    .getKitImageEngine()
                    .loadUserPortrait(
                            contactPortraitImageView.getContext(),
                            portraitUrl,
                            contactPortraitImageView);
        }

        contactNameTextView.setText(name);
        // 如果是OnlineStatusFriendInfo，则设置在线状态
        if (AppSettingsHandler.getInstance().isOnlineStatusEnable()
                && contactModelBean instanceof OnlineStatusFriendInfo) {
            int statusResID =
                    IMKitThemeManager.getAttrResId(
                            contactNameTextView.getContext(),
                            ((OnlineStatusFriendInfo) contactModelBean).isOnline()
                                    ? R.attr.rc_user_online_status_img
                                    : R.attr.rc_user_offline_status_img);
            TextViewUtils.setCompoundDrawables(contactNameTextView, Gravity.START, statusResID);
        }
        rightText.setText(roleText);
        rightText.setVisibility(showItemRightText ? View.VISIBLE : View.GONE);
        contactSelectImageView.setVisibility(showSelectButton ? View.VISIBLE : View.GONE);

        if (showSelectButton) {
            updateCheck(contactSelectImageView, contactModel.getCheckType());
        }

        rightArrow.setVisibility(showItemRightArrow ? View.VISIBLE : View.GONE);
        tvRemove.setVisibility(showItemRemoveButton ? View.VISIBLE : View.GONE);
    }

    public void setShowItemRemoveButton(boolean isShow) {
        if (tvRemove != null) {
            tvRemove.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    public void setDividerVisibility(boolean isVisible) {
        if (divider != null) {
            divider.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    private static class FriendsInfoCallback
            extends IRongCoreCallback.ResultCallback<List<FriendInfo>> {
        private final WeakReference<ContactSelectableViewHolder> holderRef;

        public FriendsInfoCallback(ContactSelectableViewHolder holder) {
            this.holderRef = new WeakReference<>(holder);
        }

        @Override
        public void onSuccess(List<FriendInfo> friendInfos) {
            ContactSelectableViewHolder viewHolder = holderRef.get();
            if (viewHolder != null && friendInfos != null && !friendInfos.isEmpty()) {
                FriendInfo friendInfo = friendInfos.get(0);
                if (friendInfo != null
                        && !TextUtils.isEmpty(friendInfo.getRemark())
                        && viewHolder.contactNameTextView != null) {
                    viewHolder.contactNameTextView.setText(friendInfo.getRemark());
                }
            }
        }

        @Override
        public void onError(IRongCoreEnum.CoreErrorCode e) {
            ContactSelectableViewHolder viewHolder = holderRef.get();
            if (viewHolder != null) {
                RLog.e("SelectableContactViewHolder", "getFriendsInfo error: " + e.getMessage());
            }
        }
    }

    private String getRoleText(GroupMemberRole role, View view) {
        switch (role) {
            case Manager:
                return view.getContext().getString(R.string.rc_admin);
            case Owner:
                return view.getContext().getString(R.string.rc_group_owner);
            default:
                return "";
        }
    }

    private void updateCheck(ImageView checkBox, ContactModel.CheckType checkType) {
        switch (checkType) {
            case NONE:
                checkBox.setVisibility(View.GONE);
                break;
            case UNCHECKED:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(
                        IMKitThemeManager.getAttrResId(
                                checkBox.getContext(), R.attr.rc_group_member_unselect_img));
                break;
            case CHECKED:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(
                        IMKitThemeManager.getAttrResId(
                                checkBox.getContext(), R.attr.rc_group_member_select_img));
                break;
            case DISABLE:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(
                        IMKitThemeManager.getAttrResId(
                                checkBox.getContext(), R.attr.rc_group_member_disable_select_img));
                break;
            default:
                break;
        }
    }
}
