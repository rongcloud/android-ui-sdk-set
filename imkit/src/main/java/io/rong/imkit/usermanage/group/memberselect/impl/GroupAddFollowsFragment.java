package io.rong.imkit.usermanage.group.memberselect.impl;

import android.widget.Toast;
import androidx.annotation.NonNull;
import io.rong.imkit.R;
import io.rong.imkit.usermanage.group.memberselect.GroupMemberSelectionFragment;
import io.rong.imkit.usermanage.group.memberselect.GroupMemberSelectionViewModel;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.List;

/**
 * 选择群联系人页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupAddFollowsFragment extends GroupMemberSelectionFragment {

    @Override
    protected void handleConfirmSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ConversationIdentifier conversationIdentifier,
            List<GroupMemberInfo> selectGroupMemberInfoList) {
        viewModel.addGroupFollows(
                isSuccess -> {
                    ToastUtils.show(
                            getActivity(),
                            getString(isSuccess ? R.string.rc_add_success : R.string.rc_add_failed),
                            Toast.LENGTH_SHORT);
                    if (isSuccess) {
                        finishActivity();
                    }
                });
    }
}
