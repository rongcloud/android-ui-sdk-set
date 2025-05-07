package io.rong.imkit.usermanage.group.follows;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.ContactListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.group.memberselect.impl.GroupAddFollowsActivity;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CommonDialog;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 群组关注人列表页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupFollowsFragment extends BaseViewModelFragment<GroupFollowsViewModel> {

    protected HeadComponent headComponent;
    protected ContactListComponent memberListComponent;
    protected SettingItemView groupAddMember;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupFollowsViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupFollowsViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_follows, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        groupAddMember = view.findViewById(R.id.siv_group_add_member);
        memberListComponent = view.findViewById(R.id.rc_group_list_component);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupFollowsViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        ConversationIdentifier conversationIdentifier =
                getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);

        groupAddMember.setOnClickListener(
                v -> {
                    List<String> followsUserIds = new ArrayList<>();
                    List<ContactModel> contactModels =
                            viewModel.getAllGroupFollowsLiveData().getValue();
                    if (contactModels != null) {
                        for (ContactModel contactModel : contactModels) {
                            if (contactModel.getBean() instanceof GroupMemberInfo) {
                                followsUserIds.add(
                                        ((GroupMemberInfo) contactModel.getBean()).getUserId());
                            }
                        }
                    }
                    startActivity(
                            GroupAddFollowsActivity.newIntent(
                                    getContext(), conversationIdentifier, followsUserIds));
                });

        memberListComponent.setOnItemRemoveClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        onRemoveGroupFollow(groupMemberInfo);
                    }
                });

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getAllGroupFollowsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                memberListComponent.setVisibility(View.VISIBLE);
                                memberListComponent.setContactList(contactModels);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                memberListComponent.setVisibility(View.GONE);
                            }
                        });
    }

    /**
     * 移除群组关注人
     *
     * @param groupMemberInfo 群组成员信息
     */
    protected void onRemoveGroupFollow(GroupMemberInfo groupMemberInfo) {
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
                .setContentMessage(getString(R.string.rc_remove_follow_hint, name))
                .setDialogButtonClickListener(
                        (v, bundle) -> {
                            getViewModel()
                                    .removeGroupFollows(
                                            Arrays.asList(groupMemberInfo.getUserId()),
                                            isSuccess -> {
                                                if (isSuccess) {
                                                    getViewModel().refreshGroupFollows();
                                                }
                                                ToastUtils.show(
                                                        getContext(),
                                                        isSuccess
                                                                ? getString(
                                                                        R.string.rc_remove_success)
                                                                : getString(
                                                                        R.string.rc_remove_failed),
                                                        Toast.LENGTH_SHORT);
                                            });
                        })
                .build()
                .show(getParentFragmentManager(), null);
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

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupFollows();
    }
}
