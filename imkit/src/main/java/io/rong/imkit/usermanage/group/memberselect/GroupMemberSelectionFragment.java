package io.rong.imkit.usermanage.group.memberselect;

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
import io.rong.imkit.model.ContactModel;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.ContactListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建增加群联系人页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public abstract class GroupMemberSelectionFragment
        extends BaseViewModelFragment<GroupMemberSelectionViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupMemberSelectionViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupMemberSelectionViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_member_selection, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        contactListComponent = view.findViewById(R.id.rc_contact_list_component);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupMemberSelectionViewModel viewModel) {
        onBindHeadComponent(headComponent, viewModel);
        onBindSearchComponent(searchComponent, viewModel);
        onBindContactListComponent(contactListComponent, viewModel);
    }

    protected void onBindHeadComponent(
            @NonNull HeadComponent headComponent,
            @NonNull GroupMemberSelectionViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());

        ConversationIdentifier conversationIdentifier =
                getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        headComponent.setRightClickListener(
                v -> {
                    // 获取选中联系人
                    List<ContactModel> contactModels =
                            viewModel.getSelectedContactsLiveData().getValue();
                    List<GroupMemberInfo> groupMemberInfoList = new ArrayList<>();

                    // 遍历联系人，拼接群管理者名称
                    if (contactModels != null && !contactModels.isEmpty()) {
                        for (ContactModel contactModel : contactModels) {
                            if (contactModel.getBean() instanceof GroupMemberInfo) {
                                groupMemberInfoList.add((GroupMemberInfo) contactModel.getBean());
                            }
                        }
                    }
                    handleConfirmSelection(viewModel, conversationIdentifier, groupMemberInfoList);
                });

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
            @NonNull GroupMemberSelectionViewModel viewModel) {
        searchComponent.setSearchQueryListener(viewModel::queryContacts);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent contactListComponent,
            @NonNull GroupMemberSelectionViewModel viewModel) {
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
                                    (OnConfirmClickListener<Boolean>) listener);
                        }
                    }
                });
    }

    /**
     * 处理联系人选择
     *
     * @param viewModel 群成员选择 ViewModel
     * @param contactModel 联系人信息
     * @param listener 确认点击监听器
     */
    protected void handleContactSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ContactModel contactModel,
            OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        if (contactModel.getCheckType() != ContactModel.CheckType.DISABLE) {
            ContactModel.CheckType newCheckType =
                    (contactModel.getCheckType() == ContactModel.CheckType.CHECKED)
                            ? ContactModel.CheckType.UNCHECKED
                            : ContactModel.CheckType.CHECKED;
            listener.onActionClick(true);
            contactModel.setCheckType(newCheckType);
            viewModel.updateContact(contactModel);
        }
    }

    /**
     * 处理确认选择
     *
     * @param viewModel 群成员选择 ViewModel
     * @param conversationIdentifier 会话标识
     * @param selectGroupMemberInfoList 选中的群成员信息列表
     */
    protected abstract void handleConfirmSelection(
            @NonNull GroupMemberSelectionViewModel viewModel,
            ConversationIdentifier conversationIdentifier,
            List<GroupMemberInfo> selectGroupMemberInfoList);
}
