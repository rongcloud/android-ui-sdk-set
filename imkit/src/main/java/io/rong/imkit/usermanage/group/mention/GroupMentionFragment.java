package io.rong.imkit.usermanage.group.mention;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.userinfo.model.ExtendedUserInfo;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.component.ContactListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.SearchComponent;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.GroupMemberInfo;
import io.rong.imlib.model.GroupMemberRole;
import io.rong.imlib.model.UserInfo;
import io.rong.imlib.model.UserProfile;
import java.util.Collections;
import java.util.List;

/**
 * 群管理员列表页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupMentionFragment extends BaseViewModelFragment<GroupMentionViewModel> {

    protected HeadComponent headComponent;
    protected SearchComponent searchComponent;
    protected ContactListComponent memberListComponent;
    private TextView emptyView;
    private View mentionAllHeaderView;
    private ConversationIdentifier conversationIdentifier;

    @NonNull
    @Override
    protected GroupMentionViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupMentionViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_mention_list, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        searchComponent = view.findViewById(R.id.rc_search_component);
        memberListComponent = view.findViewById(R.id.rc_group_list_component);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupMentionViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        searchComponent.setSearchQueryListener(viewModel::queryGroupMembers);
        onBindContactListComponent(memberListComponent, viewModel);
    }

    protected void onBindContactListComponent(
            @NonNull ContactListComponent memberListComponent,
            @NonNull GroupMentionViewModel viewModel) {
        ConversationIdentifier conversationIdentifier =
                getArguments().getParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER);
        this.conversationIdentifier = conversationIdentifier;
        if (this.conversationIdentifier == null) {
            return;
        }

        memberListComponent.setOnPageDataLoader(viewModel);
        memberListComponent.setEnableLoadMore(true);

        viewModel
                .getMentionAllRoleLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        role -> updateMentionAllHeader(role, this.conversationIdentifier));

        viewModel
                .getFilteredContactsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        contactModels -> {
                            if (contactModels != null && !contactModels.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                memberListComponent.setVisibility(View.VISIBLE);
                                memberListComponent.post(
                                        () -> memberListComponent.setContactList(contactModels));
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                memberListComponent.setVisibility(View.GONE);
                            }
                        });

        memberListComponent.setOnItemClickListener(
                contactModel -> {
                    if (contactModel.getBean() instanceof GroupMemberInfo) {
                        GroupMemberInfo groupMemberInfo = (GroupMemberInfo) contactModel.getBean();
                        onMention(conversationIdentifier.getTargetId(), groupMemberInfo);
                    }
                });
    }

    private void updateMentionAllHeader(
            @Nullable GroupMemberRole role, @Nullable ConversationIdentifier conversation) {
        boolean shouldShow = enableMentionAll(conversation, role);
        if (mentionAllHeaderView == null && shouldShow) {
            mentionAllHeaderView =
                    LayoutInflater.from(getContext())
                            .inflate(
                                    R.layout.rc_item_group_mention_all, memberListComponent, false);
            mentionAllHeaderView.setOnClickListener(v -> onMentionAll());
            memberListComponent.addHeaderView(mentionAllHeaderView);
        }
        if (mentionAllHeaderView != null) {
            mentionAllHeaderView.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 是否展示 @All 功能，子类可覆盖
     *
     * @return true 显示，false 不显示
     */
    private boolean enableMentionAll(
            @NonNull ConversationIdentifier conversationIdentifier,
            @Nullable GroupMemberRole currentUserRole) {
        return true;
    }

    private void onMentionAll() {
        finishActivity();
        RongMentionManager.getInstance()
                .mentionMember(
                        new UserInfo(
                                RongMentionManager.MENTION_ALL_USER_ID,
                                getString(R.string.rc_group_mention_all_members),
                                null));
    }

    /**
     * 移除群管理员
     *
     * @param groupId 群组 ID
     * @param groupMemberInfo 群成员信息
     */
    protected void onMention(String groupId, GroupMemberInfo groupMemberInfo) {
        if (TextUtils.equals(groupMemberInfo.getUserId(), RongMentionManager.MENTION_ALL_USER_ID)) {
            onMentionAll();
            return;
        }

        RongCoreClient.getInstance()
                .getUserProfiles(
                        Collections.singletonList(groupMemberInfo.getUserId()),
                        new IRongCoreCallback.ResultCallback<List<UserProfile>>() {
                            @Override
                            public void onSuccess(List<UserProfile> userProfiles) {
                                finishActivity();
                                if (userProfiles != null && !userProfiles.isEmpty()) {
                                    ExtendedUserInfo userInfo =
                                            ExtendedUserInfo.obtain(userProfiles.get(0));
                                    if (!TextUtils.isEmpty(groupMemberInfo.getNickname())) {
                                        userInfo.setName(groupMemberInfo.getNickname());
                                    }
                                    RongMentionManager.getInstance().mentionMember(userInfo);
                                }
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {}
                        });
    }

    @Override
    public void onStart() {
        super.onStart();
        getViewModel().refreshGroupManagerList();
    }
}
