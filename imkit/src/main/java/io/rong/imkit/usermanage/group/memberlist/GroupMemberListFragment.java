package io.rong.imkit.usermanage.group.memberlist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.ContactListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.usermanage.friend.my.profile.MyProfileActivity;
import io.rong.imkit.usermanage.friend.user.profile.UserProfileActivity;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.Objects;

/**
 * 功能描述: 群组联系人页面
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class GroupMemberListFragment extends BaseViewModelFragment<GroupMemberListViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent memberListComponent;

    @NonNull
    @Override
    protected GroupMemberListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupMemberListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_member_list, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        memberListComponent = view.findViewById(R.id.rc_member_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupMemberListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        searchComponent.setSearchQueryListener(viewModel::queryContacts);

        memberListComponent.setOnPageDataLoader(viewModel.getOnPageDataLoader());
        memberListComponent.setEnableLoadMore(true);
        // 设置联系人列表点击事件
        memberListComponent.setOnContactClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        String currentUserId = RongCoreClient.getInstance().getCurrentUserId();
                        if (Objects.equals(groupMemberInfo.getUserId(), currentUserId)) {
                            startActivity(MyProfileActivity.newIntent(getContext()));
                        } else {
                            startActivity(
                                    UserProfileActivity.newIntent(
                                            getContext(), groupMemberInfo.getUserId()));
                        }
                    }
                });

        // 监听 ViewModel 中的联系人总数变化
        viewModel
                .getGroupInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupInfo -> {
                            if (groupInfo != null) {
                                headComponent.setTitleText(
                                        getString(
                                                R.string.rc_group_members_label,
                                                groupInfo.getMembersCount()));
                            }
                        });

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && memberListComponent != null) {
                                memberListComponent.setContactList(contactModels);
                            }
                        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupMembers();
    }
}
