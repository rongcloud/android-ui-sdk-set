package io.rong.imkit.usermanage.friend.search;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.base.adapter.CommonAdapter;
import io.rong.imkit.base.adapter.MultiItemTypeAdapter;
import io.rong.imkit.base.adapter.ViewHolder;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.ListComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.usermanage.friend.user.profile.UserProfileActivity;
import io.rong.imlib.model.FriendInfo;

/**
 * 好友搜索页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class FriendSearchFragment extends BaseViewModelFragment<FriendSearchViewModel> {

    protected ListComponent listComponent;
    protected SearchComponent searchComponent;
    protected HeadComponent headComponent;

    protected CommonAdapter<FriendInfo> adapter =
            new CommonAdapter<FriendInfo>(R.layout.rc_contact_item) {
                @Override
                public void bindData(ViewHolder holder, FriendInfo friendInfo, int position) {
                    holder.setText(R.id.tv_contact_name, friendInfo.getName());
                    RongConfigCenter.featureConfig()
                            .getKitImageEngine()
                            .loadUserPortrait(
                                    holder.itemView.getContext(),
                                    friendInfo.getPortraitUri(),
                                    holder.<ImageView>getView(R.id.iv_contact_portrait));
                }
            };

    @NonNull
    @Override
    protected FriendSearchViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(FriendSearchViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_friend_search, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        listComponent = view.findViewById(R.id.rc_list_component);
        listComponent = view.findViewById(R.id.rc_list_component);
        listComponent.setEnableLoadMore(false);
        listComponent.setEnableRefresh(false);
        listComponent.setAdapter(adapter);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull FriendSearchViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        searchComponent.setSearchQueryListener(viewModel::queryContacts);

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getFriendInfoLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        data -> {
                            adapter.setData(data);
                        });
        // 设置联系人列表点击事件
        adapter.setOnItemClickListener(
                new MultiItemTypeAdapter.OnItemClickListener<FriendInfo>() {
                    @Override
                    public void onItemClick(
                            View view,
                            RecyclerView.ViewHolder holder,
                            FriendInfo friendInfo,
                            int position) {
                        onFriendItemClick(friendInfo);
                    }

                    @Override
                    public boolean onItemLongClick(
                            View view,
                            RecyclerView.ViewHolder holder,
                            FriendInfo friendInfo,
                            int position) {
                        return false;
                    }
                });
    }

    /**
     * 点击联系人列表项
     *
     * @param friendInfo 联系人信息
     */
    protected void onFriendItemClick(FriendInfo friendInfo) {
        startActivity(UserProfileActivity.newIntent(getActivity(), friendInfo.getUserId()));
    }
}
