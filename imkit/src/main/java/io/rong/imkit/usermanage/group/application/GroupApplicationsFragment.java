package io.rong.imkit.usermanage.group.application;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseViewModelFragment;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.adapter.GroupApplicationsAdapter;
import io.rong.imkit.usermanage.component.CommonListComponent;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.interfaces.OnActionClickListener;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.GroupApplicationDirection;
import io.rong.imlib.model.GroupApplicationInfo;
import io.rong.imlib.model.GroupApplicationStatus;

/**
 * 群组申请页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationsFragment extends BaseViewModelFragment<GroupApplicationsViewModel> {

    protected HeadComponent headComponent;
    protected CommonListComponent commonListComponent;
    protected GroupApplicationsAdapter groupApplicationsAdapter;
    private TextView emptyView;

    @NonNull
    @Override
    protected GroupApplicationsViewModel onCreateViewModel(Bundle bundle) {
        return new ViewModelProvider(this, new ViewModelFactory(bundle))
                .get(GroupApplicationsViewModel.class);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        View view = inflater.inflate(R.layout.rc_page_group_applications, container, false);
        headComponent = view.findViewById(R.id.rc_head_component);
        commonListComponent = view.findViewById(R.id.rc_common_list_component);
        groupApplicationsAdapter = new GroupApplicationsAdapter();
        commonListComponent.setAdapter(groupApplicationsAdapter);
        emptyView = view.findViewById(R.id.rc_empty_tv);
        return view;
    }

    @Override
    protected void onViewReady(@NonNull GroupApplicationsViewModel viewModel) {
        headComponent.setLeftClickListener(v -> finishActivity());
        headComponent.setRightClickListener(this::onOptionsMenuClick);

        commonListComponent.setOnPageDataLoader(viewModel.getOnPageDataLoader());
        // 监听 ViewModel 中的联系人列表变化
        viewModel
                .getGroupApplicationInfoListLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        groupApplicationInfos -> {
                            if (groupApplicationInfos != null && !groupApplicationInfos.isEmpty()) {
                                emptyView.setVisibility(View.GONE);
                                commonListComponent.setVisibility(View.VISIBLE);
                                groupApplicationsAdapter.setData(groupApplicationInfos);
                            } else {
                                emptyView.setVisibility(View.VISIBLE);
                                commonListComponent.setVisibility(View.GONE);
                            }
                        });

        groupApplicationsAdapter.setOnAcceptClickListener(
                new OnActionClickListener<GroupApplicationInfo>() {
                    @Override
                    public void onActionClick(GroupApplicationInfo groupApplicationInfo) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            GroupApplicationInfo groupApplicationInfo,
                            OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                groupApplicationInfo, listener);
                        if (listener != null) {
                            onApplicationAccept(
                                    groupApplicationInfo,
                                    (OnConfirmClickListener<IRongCoreEnum.CoreErrorCode>) listener);
                        }
                    }
                });

        groupApplicationsAdapter.setOnRejectClickListener(
                new OnActionClickListener<GroupApplicationInfo>() {
                    @Override
                    public void onActionClick(GroupApplicationInfo groupApplicationInfo) {}

                    @Override
                    public <E> void onActionClickWithConfirm(
                            GroupApplicationInfo groupApplicationInfo,
                            OnActionClickListener.OnConfirmClickListener<E> listener) {
                        OnActionClickListener.super.onActionClickWithConfirm(
                                groupApplicationInfo, listener);
                        if (listener != null) {
                            onApplicationReject(
                                    groupApplicationInfo,
                                    (OnConfirmClickListener<Boolean>) listener);
                        }
                    }
                });
    }

    /**
     * 处理申请
     *
     * @param groupApplicationInfo 群组申请信息
     * @param listener 确认点击监听
     */
    protected void onApplicationAccept(
            GroupApplicationInfo groupApplicationInfo,
            @NonNull
                    OnActionClickListener.OnConfirmClickListener<IRongCoreEnum.CoreErrorCode>
                            listener) {
        GroupApplicationDirection direction = groupApplicationInfo.getDirection();
        GroupApplicationStatus status = groupApplicationInfo.getStatus();
        if (status == GroupApplicationStatus.InviteeUnHandled
                || status == GroupApplicationStatus.ManagerUnHandled) {
            if (direction == GroupApplicationDirection.InvitationReceived) {
                getViewModel()
                        .acceptGroupInvite(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                isSuccess -> {
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.rc_invite_confirm_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    listener.onActionClick(IRongCoreEnum.CoreErrorCode.SUCCESS);
                                });
            } else if (direction == GroupApplicationDirection.ApplicationReceived) {
                getViewModel()
                        .acceptGroupApplication(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                groupApplicationInfo.getJoinMemberInfo().getUserId(),
                                coreErrorCode -> {
                                    boolean isSuccess =
                                            coreErrorCode == IRongCoreEnum.CoreErrorCode.SUCCESS
                                                    || coreErrorCode
                                                            == IRongCoreEnum.CoreErrorCode
                                                                    .RC_GROUP_NEED_INVITEE_ACCEPT;
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.rc_invite_confirm_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    if (isSuccess) {
                                        listener.onActionClick(coreErrorCode);
                                    }
                                });
            }
        }
    }

    /**
     * 拒绝申请
     *
     * @param groupApplicationInfo 群组申请信息
     * @param listener 确认点击监听
     */
    protected void onApplicationReject(
            GroupApplicationInfo groupApplicationInfo,
            @NonNull OnActionClickListener.OnConfirmClickListener<Boolean> listener) {
        GroupApplicationDirection direction = groupApplicationInfo.getDirection();
        GroupApplicationStatus status = groupApplicationInfo.getStatus();
        if (status == GroupApplicationStatus.InviteeUnHandled
                || status == GroupApplicationStatus.ManagerUnHandled) {
            if (direction == GroupApplicationDirection.InvitationReceived) {
                getViewModel()
                        .refuseGroupInvite(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                groupApplicationInfo.getReason(),
                                isSuccess -> {
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.rc_invite_reject_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    listener.onActionClick(isSuccess);
                                });
            } else if (direction == GroupApplicationDirection.ApplicationReceived) {
                getViewModel()
                        .refuseGroupApplication(
                                groupApplicationInfo.getGroupId(),
                                groupApplicationInfo.getInviterInfo().getUserId(),
                                groupApplicationInfo.getJoinMemberInfo().getUserId(),
                                groupApplicationInfo.getReason(),
                                isSuccess -> {
                                    if (!isSuccess) {
                                        ToastUtils.show(
                                                getContext(),
                                                getString(R.string.rc_invite_reject_failed),
                                                Toast.LENGTH_SHORT);
                                    }
                                    listener.onActionClick(isSuccess);
                                });
            }
        }
    }

    /**
     * 点击菜单按钮
     *
     * @param anchor 锚点 View
     */
    protected void onOptionsMenuClick(View anchor) {
        // 加载自定义布局
        View rootView =
                LayoutInflater.from(anchor.getContext())
                        .inflate(R.layout.rc_pop_group_applications_category, null);

        // 配置 PopupWindow
        final PopupWindow popupWindow =
                new PopupWindow(
                        rootView,
                        ScreenUtils.dip2px(anchor.getContext(), 150),
                        WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setBackgroundDrawable(new ColorDrawable());
        popupWindow.setOutsideTouchable(true); // 必须设置以启用触摸外部关闭
        popupWindow.setFocusable(true);

        // 初始化菜单项
        TextView menuAllRequests = rootView.findViewById(R.id.menu_item_all_requests);
        TextView menuPendingConfirmation =
                rootView.findViewById(R.id.menu_item_pending_confirmation);
        TextView menuProcessedRequests = rootView.findViewById(R.id.menu_item_processed_requests);
        TextView menuExpiredRequests = rootView.findViewById(R.id.menu_item_expired_requests);
        // 全部请求
        menuAllRequests.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.rc_all_requests);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.ApplicationSent,
                                GroupApplicationDirection.InvitationSent,
                                GroupApplicationDirection.ApplicationReceived,
                                GroupApplicationDirection.InvitationReceived
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {
                                GroupApplicationStatus.ManagerUnHandled,
                                GroupApplicationStatus.ManagerRefused,
                                GroupApplicationStatus.Joined,
                                GroupApplicationStatus.Expired,
                                GroupApplicationStatus.InviteeRefused,
                                GroupApplicationStatus.InviteeUnHandled
                            };
                    getViewModel().getGroupApplications(directions, status);
                });
        // 待确认
        menuPendingConfirmation.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.rc_pending_confirmation);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.ApplicationSent,
                                GroupApplicationDirection.InvitationSent,
                                GroupApplicationDirection.ApplicationReceived,
                                GroupApplicationDirection.InvitationReceived
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {
                                GroupApplicationStatus.ManagerUnHandled,
                                GroupApplicationStatus.InviteeUnHandled
                            };
                    getViewModel().getGroupApplications(directions, status);
                });
        // 已处理
        menuProcessedRequests.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.rc_completed);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.ApplicationSent,
                                GroupApplicationDirection.InvitationSent,
                                GroupApplicationDirection.ApplicationReceived,
                                GroupApplicationDirection.InvitationReceived
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {
                                GroupApplicationStatus.Joined,
                                GroupApplicationStatus.ManagerRefused,
                                GroupApplicationStatus.InviteeRefused,
                            };
                    getViewModel().getGroupApplications(directions, status);
                });
        menuExpiredRequests.setOnClickListener(
                v -> {
                    headComponent.setTitleText(R.string.rc_expired);
                    popupWindow.dismiss();
                    GroupApplicationDirection[] directions =
                            new GroupApplicationDirection[] {
                                GroupApplicationDirection.ApplicationSent,
                                GroupApplicationDirection.InvitationSent,
                                GroupApplicationDirection.ApplicationReceived,
                                GroupApplicationDirection.InvitationReceived
                            };
                    GroupApplicationStatus[] status =
                            new GroupApplicationStatus[] {GroupApplicationStatus.Expired};
                    getViewModel().getGroupApplications(directions, status);
                });

        // 设置 PopupWindow 的显示和消失监听
        popupWindow.setOnDismissListener(() -> setWindowAlpha(anchor.getContext(), 1.0f));
        popupWindow.showAsDropDown(anchor, 0, 0);
        setWindowAlpha(anchor.getContext(), 0.5f);
    }

    private void setWindowAlpha(Context context, float alpha) {
        Window window = ((Activity) context).getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.alpha = alpha;
        window.setAttributes(layoutParams);
    }
}
