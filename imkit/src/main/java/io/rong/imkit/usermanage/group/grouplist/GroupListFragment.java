package io.rong.imkit.usermanage.group.grouplist;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.adapter.GroupListAdapter;
import io.rong.imkit.usermanage.component.CommonListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.usermanage.group.groupsearch.GroupSearchActivity;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupInfo;

/**
 * 群组列表页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupListFragment extends BaseViewModelFragment<GroupListViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected CommonListComponent groupListComponent;
    protected GroupListAdapter groupListAdapter;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupListViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupListViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_list, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        groupListComponent = view.findViewById(R.id.rc_group_list_component);
        groupListAdapter = new GroupListAdapter();
        groupListComponent.setAdapter(groupListAdapter);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupListViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchClickListener(
                v -> startActivity(GroupSearchActivity.newIntent(getContext())));

        groupListComponent.setOnPageDataLoader(viewModel.getOnPageDataLoader());
        groupListAdapter.setOnItemClickListener(this::onGroupItemClick);
        groupListAdapter.setOnItemLongClickListener(this::onGroupItemLongClick);

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getAllGroupInfoListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                groupListComponent.setVisibility(View.VISIBLE);
                                groupListAdapter.setData(contactModels);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                groupListComponent.setVisibility(View.GONE);
                            }
                        });
    }

    /**
     * 点击群组
     *
     * @param groupInfo 群组信息
     */
    protected void onGroupItemClick(GroupInfo groupInfo) {
        if (groupInfo != null) {
            RouteUtils.routeToConversationActivity(
                    getActivity(),
                    ConversationIdentifier.obtain(
                            Conversation.ConversationType.GROUP, groupInfo.getGroupId(), ""));
        }
    }

    /**
     * 长按群组
     *
     * @param groupInfo 群组信息
     */
    protected void onGroupItemLongClick(GroupInfo groupInfo) {}
}
