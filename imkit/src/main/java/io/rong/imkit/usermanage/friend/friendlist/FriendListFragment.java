package io.rong.imkit.usermanage.friend.friendlist;

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
import io.rong.imkit.usermanage.friend.add.AddFriendListActivity;
import io.rong.imkit.usermanage.friend.search.FriendSearchActivity;
import io.rong.imkit.usermanage.friend.user.profile.UserProfileActivity;
import io.rong.imlib.model.FriendInfo;

/**
 * 好友列表页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class FriendListFragment extends BaseViewModelFragment<FriendListViewModel> {

    protected ContactListComponent contactListComponent;
    protected SearchComponent searchComponent;
    protected HeadComponent headComponent;

    @NonNull
    @Override
    protected FriendListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(FriendListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_friend_list, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        contactListComponent = view.findViewById(R.id.rc_contact_list_component);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull FriendListViewModel viewModel) {
        headComponent.setRightClickListener(
                v -> {
                    // 跳转到创建群组页面
                    startActivity(AddFriendListActivity.newIntent(getActivity()));
                });

        searchComponent.setSearchClickListener(
                v -> startActivity(FriendSearchActivity.newIntent(getContext())));

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getAllContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && contactListComponent != null) {
                                contactListComponent.setContactList(contactModels);
                            }
                        });
        // 设置联系人列表点击事件
        contactListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel != null && contactModel.getBean() instanceof FriendInfo) {
                        FriendInfo friendInfo = (FriendInfo) contactModel.getBean();
                        startActivity(
                                UserProfileActivity.newIntent(
                                        getActivity(), friendInfo.getUserId()));
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        getViewModel().getAllFriends();
    }
}
