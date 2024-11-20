package io.rong.imkit.usermanage.friend.select;

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
import io.rong.imkit.usermanage.group.create.GroupCreateActivity;
import io.rong.imkit.utils.KitConstants;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.model.FriendInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * 功能描述: 选择联系人页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class FriendSelectFragment extends BaseViewModelFragment<FriendSelectViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent contactListComponent;
    private int maxCount;
    private TextView tvEmptyContacts;

    @NonNull
    @Override
    protected FriendSelectViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(FriendSelectViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_friend_select, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        contactListComponent = view.findViewById(R.id.rc_contact_list_component);
        tvEmptyContacts = view.findViewById(R.id.tv_empty_contacts);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull FriendSelectViewModel viewModel) {
        maxCount =
                Math.max(
                        1,
                        Math.min(
                                100,
                                getArguments()
                                        .getInt(KitConstants.KEY_MAX_FRIEND_SELECT_COUNT, 30)));
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(
                v -> {
                    List<ContactModel> value = viewModel.getSelectedContactsLiveData().getValue();
                    if (value == null || value.isEmpty()) {
                        return;
                    }
                    List<String> inviteeUserIds = new ArrayList<>();
                    for (int i = 0; i < value.size(); i++) {
                        Object bean = value.get(i).getBean();
                        if (bean instanceof FriendInfo) {
                            inviteeUserIds.add(((FriendInfo) bean).getUserId());
                        }
                    }
                    startActivity(GroupCreateActivity.newIntent(getContext(), inviteeUserIds));
                });
        headComponent.setRightTextViewEnable(false);
        viewModel
                .getSelectedContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels ->
                                headComponent.setRightTextViewEnable(
                                        contactModels != null && !contactModels.isEmpty()));

        searchComponent.setSearchQueryListener(viewModel::queryContacts);

        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactListComponent != null) {
                                contactListComponent.setContactList(contactModels);
                            }
                        });

        viewModel
                .getAllContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels == null || contactModels.isEmpty()) {
                                contactListComponent.setVisibility(View.GONE);
                                tvEmptyContacts.setVisibility(View.VISIBLE);
                            } else {
                                contactListComponent.setVisibility(View.VISIBLE);
                                tvEmptyContacts.setVisibility(View.GONE);
                            }
                        });

        // 设置联系人列表点击事件
        contactListComponent.setOnContactClickListener(
                contactModel -> {
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
                    if (newCheckType == ContactModel.CheckType.CHECKED) {
                        newCheckType = ContactModel.CheckType.UNCHECKED;
                    } else {
                        newCheckType = ContactModel.CheckType.CHECKED;
                    }

                    // 更新 ContactModel 并通知 ViewModel
                    contactModel.setCheckType(newCheckType);
                    viewModel.updateContact(contactModel);
                });
    }
}