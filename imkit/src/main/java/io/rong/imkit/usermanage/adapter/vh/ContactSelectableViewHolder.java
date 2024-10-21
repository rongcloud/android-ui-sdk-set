package io.rong.imkit.usermanage.adapter.vh;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.interfaces.OnContactClickListener;
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
    private final OnContactClickListener listener;
    private final ImageView rightArrow;
    private final boolean showSelectButton;
    private final boolean showItemRightArrow;
    private final boolean showItemRightText;
    private final boolean showItemSelectAutoUpdate;
    private ContactModel<FriendInfo> data;

    public ContactSelectableViewHolder(
            @NonNull View itemView,
            OnContactClickListener listener,
            boolean showSelectButton,
            boolean showItemRightArrow,
            boolean showItemRightText,
            boolean showItemSelectAutoUpdate) {
        super(itemView);
        this.listener = listener;
        contactPortraitImageView = itemView.findViewById(R.id.iv_contact_portrait);
        contactNameTextView = itemView.findViewById(R.id.tv_contact_name);
        rightText = itemView.findViewById(R.id.tv_right_text);
        contactSelectImageView = itemView.findViewById(R.id.iv_contact_select);
        rightArrow = itemView.findViewById(R.id.iv_right_arrow);
        this.showSelectButton = showSelectButton;
        this.showItemRightArrow = showItemRightArrow;
        this.showItemRightText = showItemRightText;
        this.showItemSelectAutoUpdate = showItemSelectAutoUpdate;

        itemView.setOnClickListener(
                v -> {
                    if (data != null) {
                        // 更新选中状态(选中/未选中)
                        if (showSelectButton && showItemSelectAutoUpdate) {
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
                        listener.onContactClick(data);
                    }
                });
    }

    public void bind(ContactModel contactModel) {
        this.data = contactModel;

        Object contactModelBean = contactModel.getBean();
        String name = "";
        String portraitUrl = null;
        String roleText = "";

        if (contactModelBean instanceof FriendInfo) {
            FriendInfo friendInfo = (FriendInfo) contactModelBean;
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
            RongCoreClient.getInstance()
                    .getFriendsInfo(
                            Collections.singletonList(groupMemberInfo.getUserId()),
                            new FriendsInfoCallback(this));

            portraitUrl = groupMemberInfo.getPortraitUri();
            roleText = getRoleText(groupMemberInfo.getRole(), rightText);
            RongConfigCenter.featureConfig()
                    .getKitImageEngine()
                    .loadGroupPortrait(
                            contactPortraitImageView.getContext(),
                            portraitUrl,
                            contactPortraitImageView);
        }

        contactNameTextView.setText(name);

        rightText.setText(roleText);
        rightText.setVisibility(showItemRightText ? View.VISIBLE : View.GONE);
        contactSelectImageView.setVisibility(showSelectButton ? View.VISIBLE : View.GONE);

        if (showSelectButton) {
            updateCheck(contactSelectImageView, contactModel.getCheckType());
        }

        rightArrow.setVisibility(showItemRightArrow ? View.VISIBLE : View.GONE);
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
                checkBox.setImageResource(R.drawable.rc_checkbox_none);
                break;
            case CHECKED:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(R.drawable.rc_checkbox_select);
                break;
            case DISABLE:
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setImageResource(R.drawable.rc_checkbox_disable);
                break;
            default:
                break;
        }
    }
}
