package io.rong.imkit.usermanage.friend.add;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.usermanage.friend.my.profile.MyProfileActivity;
import io.rong.imkit.usermanage.friend.user.profile.UserProfileActivity;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.UserProfile;

/**
 * 功能描述: 创建群组页面
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class AddFriendListFragment extends BaseViewModelFragment<AddFriendListViewModel> {
    protected SearchComponent searchComponent;
    protected HeadComponent headComponent;
    protected TextView hintView;
    String mQuery;

    @NonNull
    @Override
    protected AddFriendListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(AddFriendListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_friend_list_add, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        hintView = view.findViewById(R.id.tv_hint);
        searchComponent.setSearchHint(R.string.rc_app_id);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull AddFriendListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchQueryListener(
                new SearchComponent.OnSearchQueryListener() {
                    @Override
                    public void onSearch(String query) {}

                    @Override
                    public void onClickSearch(String query) {
                        mQuery = query;
                        getViewModel().findUser(query);
                    }
                });
        viewModel
                .getUserProfileLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        new Observer<UserProfile>() {
                            @Override
                            public void onChanged(UserProfile userProfiles) {
                                onUserProfileSearchResult(userProfiles);
                            }
                        });
    }

    /**
     * 搜索用户信息结果
     *
     * @param userProfiles 用户信息
     */
    protected void onUserProfileSearchResult(UserProfile userProfiles) {
        if (userProfiles != null) {
            if (userProfiles.getUserId().equals(RongCoreClient.getInstance().getCurrentUserId())) {
                startActivity(MyProfileActivity.newIntent(getActivity()));
            } else {
                startActivity(
                        UserProfileActivity.newIntent(getActivity(), userProfiles.getUserId()));
            }

        } else {
            hintView.setVisibility(TextUtils.isEmpty(mQuery) ? View.GONE : View.VISIBLE);
        }
    }
}
