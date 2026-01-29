package io.rong.imkit.usermanage.group.transfer;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.ContactListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.usermanage.group.manage.GroupManagementFragment;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CommonDialog;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import java.util.Arrays;
import java.util.List;

/**
 * 移交群主页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupTransferFragment extends BaseViewModelFragment<GroupTransferViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;

    @NonNull
    @Override
    protected GroupTransferViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupTransferViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_transfer, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        contactListComponent = view.findViewById(R.id.rc_contact_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupTransferViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchQueryListener(viewModel::queryGroupMembers);
        contactListComponent.setOnPageDataLoader(viewModel);
        contactListComponent.setEnableLoadMore(true);
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null) {
                                contactListComponent.post(
                                        () -> contactListComponent.setContactList(contactModels));
                            }
                        });

        contactListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel != null && contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        if (groupMemberInfo.getRole() == GroupMemberRole.Owner) {
                            return;
                        }
                        onGroupOwnerTransfer(groupMemberInfo);
                    }
                });
    }

    /**
     * 群主转移
     *
     * @param groupMemberInfo 被转移的群成员信息
     */
    protected void onGroupOwnerTransfer(GroupMemberInfo groupMemberInfo) {
        RongCoreClient.getInstance()
                .getFriendsInfo(
                        Arrays.asList(groupMemberInfo.getUserId()),
                        new IRongCoreCallback.ResultCallback<List<FriendInfo>>() {
                            @Override
                            public void onSuccess(List<FriendInfo> friendInfos) {
                                transferGroupOwner(friendInfos, groupMemberInfo);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                transferGroupOwner(null, groupMemberInfo);
                            }
                        });
    }

    private void transferGroupOwner(List<FriendInfo> friendInfos, GroupMemberInfo groupMemberInfo) {
        String name = concatenateUserDisplayNames(friendInfos, groupMemberInfo);
        // 弹出删除好友确认对话框
        new CommonDialog.Builder()
                .setTitleText(R.string.rc_prompt)
                .setContentMessage(getString(R.string.rc_group_transfer_hint, name))
                .setPositiveTextColor(
                        IMKitThemeManager.getColorFromAttrId(getContext(), R.attr.rc_primary_color))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            getViewModel()
                                    .transferGroupOwner(
                                            groupMemberInfo,
                                            isSuccess ->
                                                    onGroupOwnerTransferResult(
                                                            getViewModel().getGroupId(),
                                                            groupMemberInfo,
                                                            isSuccess));
                        })
                .build()
                .show(getParentFragmentManager(), null);
    }

    /**
     * 群主转移结果
     *
     * @param groupId 群组 ID
     * @param groupMemberInfo 被转移的群成员信息
     * @param isSuccess 是否成功
     */
    protected void onGroupOwnerTransferResult(
            String groupId, GroupMemberInfo groupMemberInfo, boolean isSuccess) {
        if (isSuccess) {
            ToastUtils.show(
                    getContext(),
                    getString(R.string.rc_group_transfer_success),
                    Toast.LENGTH_SHORT);
            sendFinishActivityBroadcast(GroupManagementFragment.class);
            finishActivity();
        } else {
            ToastUtils.show(
                    getContext(), getString(R.string.rc_group_transfer_failed), Toast.LENGTH_SHORT);
        }
    }

    private String concatenateUserDisplayNames(
            List<FriendInfo> friendInfos, GroupMemberInfo groupMemberInfo) {
        String displayName =
                TextUtils.isEmpty(groupMemberInfo.getNickname())
                        ? groupMemberInfo.getName()
                        : groupMemberInfo.getNickname();
        if (friendInfos != null && !friendInfos.isEmpty()) {
            String remark = friendInfos.get(0).getRemark();
            if (!TextUtils.isEmpty(remark)) {
                displayName = remark;
            }
        }
        return displayName;
    }
}
