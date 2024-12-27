package io.rong.imkit.usermanage.group.add;

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
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.IRongCoreEnum;
import java.util.List;

/**
 * 功能描述: 创建增加群联系人页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class AddGroupMembersFragment extends BaseViewModelFragment<AddGroupMembersViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private TextView emptyView;

    @NonNull
    @Override
    protected AddGroupMembersViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(AddGroupMembersViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_add_member, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        contactListComponent = view.findViewById(R.id.rc_contact_list_component);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull AddGroupMembersViewModel viewModel) {
        onBindHeadComponent(headComponent, viewModel);
        onBindSearchComponent(searchComponent, viewModel);
        onBindContactListComponent(contactListComponent, viewModel);
    }

    protected void onBindHeadComponent(
            @NonNull HeadComponent headComponent, @NonNull AddGroupMembersViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(v -> handleConfirmSelection(viewModel));

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

    private void handleConfirmSelection(@NonNull AddGroupMembersViewModel viewModel) {
        viewModel.inviteUsersToGroup(
                errorCode ->
                        onInviteUsersToGroupResult(
                                viewModel.getGroupId(), viewModel.getSelectUserIds(), errorCode));
    }

    protected void onInviteUsersToGroupResult(
            String groupId, List<String> selectedIdList, IRongCoreEnum.CoreErrorCode errorCode) {
        if (errorCode == IRongCoreEnum.CoreErrorCode.SUCCESS) {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.rc_invite_join_group_success),
                    Toast.LENGTH_SHORT);
            finishActivity();
        } else if (errorCode == IRongCoreEnum.CoreErrorCode.RC_GROUP_NEED_INVITEE_ACCEPT) {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.rc_invite_join_group_send),
                    Toast.LENGTH_SHORT);
            finishActivity();
        } else if (errorCode
                == IRongCoreEnum.CoreErrorCode.RC_GROUP_JOIN_GROUP_NEED_MANAGER_ACCEPT) {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.rc_invite_join_group_send_need_manange),
                    Toast.LENGTH_SHORT);
            finishActivity();
        } else {
            ToastUtils.show(
                    getActivity(),
                    getString(R.string.rc_invite_join_group_failed),
                    Toast.LENGTH_SHORT);
        }
    }

    protected void onBindSearchComponent(
            @NonNull SearchComponent searchComponent, @NonNull AddGroupMembersViewModel viewModel) {
        searchComponent.setSearchQueryListener(viewModel::queryContacts);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent contactListComponent,
            @NonNull AddGroupMembersViewModel viewModel) {
        int maxCount =
                Math.max(
                        1,
                        Math.min(
                                30,
                                getArguments().getInt(KitConstants.KEY_MAX_MEMBER_COUNT_ADD, 30)));
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

        // 设置联系人列表点击事件
        contactListComponent.setOnItemClickListener(
                new OnActionClickListener<ContactModel>() {
                    @Override
                    public void onActionClick(ContactModel contactModel) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            ContactModel contactModel, OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                contactModel, listener);
                        if (listener != null) {
                            handleContactSelection(
                                    viewModel,
                                    contactModel,
                                    maxCount,
                                    (OnConfirmClickListener<Boolean>) listener);
                        }
                    }
                });
    }

    private void handleContactSelection(
            @NonNull AddGroupMembersViewModel viewModel,
            ContactModel contactModel,
            int maxCount,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
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

            ContactModel.CheckType updateCheckType =
                    (contactModel.getCheckType() == ContactModel.CheckType.CHECKED)
                            ? ContactModel.CheckType.UNCHECKED
                            : ContactModel.CheckType.CHECKED;
            listener.onActionClick(true);
            contactModel.setCheckType(updateCheckType);
            viewModel.updateContact(contactModel);
        }
    }
}
