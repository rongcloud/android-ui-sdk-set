package io.rong.imkit.usermanage.group.remove;

import android.content.Context;
import android.os.Bundle;
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
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.utils.ToastUtils;

/**
 * 移除群成员页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class RemoveGroupMembersFragment extends BaseViewModelFragment<RemoveGroupMembersViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private TextView emptyView;

    @NonNull
    @Override
    protected RemoveGroupMembersViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(RemoveGroupMembersViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_remove_member, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        contactListComponent = view.findViewById(R.id.rc_contact_list_component);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull RemoveGroupMembersViewModel viewModel) {
        onBindHeadComponent(headComponent, viewModel);
        onBindSearchComponent(searchComponent, viewModel);
        onBindContactListComponent(contactListComponent, viewModel);
    }

    protected void onBindHeadComponent(
            @NonNull HeadComponent headComponent, @NonNull RemoveGroupMembersViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        headComponent.setRightClickListener(
                v ->
                        viewModel.kickGroupMembers(
                                isSuccess -> {
                                    if (isSuccess) {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.rc_group_members_kick_success),
                                                Toast.LENGTH_SHORT);
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    } else {
                                        ToastUtils.show(
                                                getActivity(),
                                                getString(R.string.rc_group_members_kick_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                }));

        viewModel
                .getSelectedContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && headComponent != null) {
                                headComponent.setRightTextViewEnable(!contactModels.isEmpty());
                            }
                        });
    }

    protected void onBindSearchComponent(
            @NonNull SearchComponent searchComponent,
            @NonNull RemoveGroupMembersViewModel viewModel) {
        searchComponent.setSearchQueryListener(viewModel::queryGroupMembers);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent contactListComponent,
            @NonNull RemoveGroupMembersViewModel viewModel) {
        contactListComponent.setOnPageDataLoader(viewModel);
        contactListComponent.setEnableLoadMore(true);
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                contactListComponent.setVisibility(View.VISIBLE);
                                contactListComponent.post(
                                        () -> contactListComponent.setContactList(contactModels));
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                contactListComponent.setVisibility(View.GONE);
                            }
                        });

        contactListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
                        ContactModel.CheckType newCheckType =
                                (contactModel.getCheckType() == ContactModel.CheckType.CHECKED)
                                        ? ContactModel.CheckType.UNCHECKED
                                        : ContactModel.CheckType.CHECKED;

                        contactModel.setCheckType(newCheckType);
                        viewModel.updateContact(contactModel);
                    }
                });
    }
}
