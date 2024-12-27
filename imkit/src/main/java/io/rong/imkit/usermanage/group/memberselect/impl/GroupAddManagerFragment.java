package io.rong.imkit.usermanage.group.memberselect.impl;

import android.widget.Toast;
import androidx.annotation.NonNull;
import io.rong.imkit.R;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.group.memberselect.GroupMemberSelectionFragment;
import io.rong.imkit.usermanage.group.memberselect.GroupMemberSelectionViewModel;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.List;

/**
 * 创建增加群联系人页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupAddManagerFragment extends GroupMemberSelectionFragment {

    @Override
    protected void handleContactSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ContactModel contactModel,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        int maxCount = getArguments().getInt(KitConstants.KEY_MAX_SELECT_COUNT, 10);
        if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
            List<ContactModel> contactModelList =
                    viewModel.getSelectedContactsLiveData().getValue();
            ContactModel.CheckType newCheckType = contactModel.getCheckType();
            if (newCheckType == ContactModel.CheckType.UNCHECKED
                    && contactModelList != null
                    && contactModelList.size() >= maxCount) {
                ToastUtils.show(
                        getContext(),
                        getString(R.string.rc_max_group_members_selection, maxCount),
                        Toast.LENGTH_SHORT);
                return;
            }
        }
        super.handleContactSelection(viewModel, contactModel, listener);
    }

    @Override
    protected void handleConfirmSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ConversationIdentifier conversationIdentifier,
            List<GroupMemberInfo> selectGroupMemberInfoList) {
        viewModel.addGroupManagers(
                isSuccess ->
                        onAddGroupManagersResult(
                                conversationIdentifier.getTargetId(),
                                selectGroupMemberInfoList,
                                isSuccess));
    }

    /**
     * 添加群管理者结果
     *
     * @param groupId 群组 ID
     * @param selectGroupMemberInfoList 选中的群管理者列表
     * @param isSuccess 是否成功
     */
    protected void onAddGroupManagersResult(
            String groupId, List<GroupMemberInfo> selectGroupMemberInfoList, boolean isSuccess) {
        if (isSuccess) {
            ToastUtils.show(getActivity(), getString(R.string.rc_add_success), Toast.LENGTH_SHORT);
            finishActivity();
        } else {
            ToastUtils.show(getActivity(), getString(R.string.rc_add_failed), Toast.LENGTH_SHORT);
        }
    }
}
