package io.rong.imkit.usermanage.friend.apply;

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
import io.rong.imkit.base.adapter.ViewHolder;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.model.UiFriendApplicationInfo;
import io.rong.imkit.picture.tools.ScreenUtils;
import io.rong.imkit.usermanage.ViewModelFactory;
import io.rong.imkit.usermanage.adapter.ApplyFriendAdapter;
import io.rong.imkit.usermanage.component.HeadComponent;
import io.rong.imkit.usermanage.component.ListComponent;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imkit.utils.RTLUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imkit.widget.CommonDialog;
import io.rong.imkit.widget.refresh.api.RefreshLayout;
import io.rong.imkit.widget.refresh.listener.OnLoadMoreListener;
import io.rong.imkit.widget.refresh.listener.OnRefreshListener;

/**
 * 申请好友列表页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class ApplyFriendListFragment extends BaseViewModelFragment<ApplyFriendViewModel> {

    protected View rootView;
    protected HeadComponent headComponent;
    protected ListComponent listComponent;
    protected PopupWindow popupWindow;
    private TextView emptyView;

    protected ApplyFriendAdapter applyFriendAdapter = new ApplyFriendAdapter();

    protected int status = 0;
    private OnRefreshListener onRefreshListener =
            new OnRefreshListener() {
                @Override
                public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                    status = 1;
                    getViewModel().loadFriendApplications(false);
                }
            };
    protected OnLoadMoreListener onLoadMoreListener =
            new OnLoadMoreListener() {
                @Override
                public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                    status = 2;
                    getViewModel().loadFriendApplications(true);
                }
            };

    @NonNull
    @Override
    protected ApplyFriendViewModel onCreateViewModel(Bundle bundle) {
        ApplyFriendViewModel applyFriendViewModel =
                new ViewModelProvider(this, new ViewModelFactory(bundle))
                        .get(ApplyFriendViewModel.class);
        return applyFriendViewModel;
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle args) {
        rootView = inflater.inflate(R.layout.rc_page_friend_list_apply, container, false);
        headComponent = rootView.findViewById(R.id.tb_bar);
        headComponent.setRightTextDrawable(
                IMKitThemeManager.getAttrResId(context, R.attr.rc_navigation_bar_btn_more_img));

        listComponent = rootView.findViewById(R.id.rc_list_component);
        listComponent.setOnRefreshListener(onRefreshListener);
        listComponent.setOnLoadMoreListener(onLoadMoreListener);
        emptyView = rootView.findViewById(R.id.rc_empty_tv);

        listComponent.setAdapter(applyFriendAdapter);
        return rootView;
    }

    /**
     * View 创建之后
     *
     * @param viewModel VM
     */
    @Override
    protected void onViewReady(@NonNull ApplyFriendViewModel viewModel) {
        headComponent.setRightClickListener(v -> showPopupWindow(v));

        applyFriendAdapter.setOnBtnClickListener(
                new ApplyFriendAdapter.OnBtnClickListener() {
                    public void onAcceptClick(
                            ViewHolder holder, UiFriendApplicationInfo item, int position) {
                        onFriendApplyAcceptClick(item);
                    }

                    @Override
                    public void onRejectClick(
                            ViewHolder holder, UiFriendApplicationInfo item, int position) {
                        onFriendApplyRejectClick(item);
                    }
                });

        viewModel
                .getFriendApplicationsLiveData()
                .observe(
                        getViewLifecycleOwner(),
                        data -> {
                            // 更新UI
                            if (listComponent != null) {
                                if (status == 1) {
                                    listComponent.finishRefresh();
                                } else if (status == 2) {
                                    listComponent.finishLoadMore();
                                }
                            }
                            status = 0;
                            boolean hasData = data != null && !data.isEmpty();
                            emptyView.setVisibility(hasData ? View.GONE : View.VISIBLE);
                            listComponent.setVisibility(hasData ? View.VISIBLE : View.GONE);
                            applyFriendAdapter.setData(data);
                        });
        viewModel.loadFriendApplications(false);
    }

    /**
     * 点击联系人列表项接受
     *
     * @param uiFriendApplicationInfo 联系人信息
     */
    protected void onFriendApplyAcceptClick(UiFriendApplicationInfo uiFriendApplicationInfo) {
        getViewModel()
                .acceptFriendApplication(
                        uiFriendApplicationInfo.getInfo().getUserId(),
                        aBoolean -> {
                            if (aBoolean) {
                                ToastUtils.show(
                                        getContext(),
                                        getString(R.string.rc_send_apply_success),
                                        Toast.LENGTH_SHORT);
                                getViewModel().loadFriendApplications(false);
                            }
                        });
    }

    /**
     * 点击联系人列表项拒绝
     *
     * @param uiFriendApplicationInfo 联系人信息
     */
    protected void onFriendApplyRejectClick(UiFriendApplicationInfo uiFriendApplicationInfo) {
        showDialog(uiFriendApplicationInfo.getInfo().getUserId());
    }

    /**
     * 显示弹窗
     *
     * @param anchor 锚点
     */
    protected void showPopupWindow(View anchor) {
        if (popupWindow == null) {
            View view =
                    LayoutInflater.from(anchor.getContext())
                            .inflate(R.layout.rc_pop_apply_list, null);
            TextView allView = view.findViewById(R.id.tv_all);
            TextView receivedView = view.findViewById(R.id.tv_received_apply);
            TextView sendView = view.findViewById(R.id.tv_send_apply);
            popupWindow =
                    new PopupWindow(
                            view,
                            ScreenUtils.dip2px(anchor.getContext(), 136),
                            WindowManager.LayoutParams.WRAP_CONTENT);
            popupWindow.setBackgroundDrawable(new ColorDrawable()); // 必须设置Background才能触发外部触摸事件
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            allView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getViewModel().loadFriendApplications(0);
                            popupWindow.dismiss();
                        }
                    });
            receivedView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getViewModel().loadFriendApplications(1);
                            popupWindow.dismiss();
                        }
                    });
            sendView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            getViewModel().loadFriendApplications(2);
                            popupWindow.dismiss();
                        }
                    });
        }
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
            return;
        }
        // 设置 PopupWindow 的显示和消失监听
        popupWindow.setOnDismissListener(() -> setWindowAlpha(anchor.getContext(), 1.0f));

        // 计算 x 偏移量，根据布局方向确定 PopupWindow 的位置
        int screenWidth = ScreenUtils.getScreenWidth(anchor.getContext());
        int popupWidth = ScreenUtils.dip2px(anchor.getContext(), 136);
        int margin = ScreenUtils.dip2px(anchor.getContext(), 16);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int anchorX = location[0];
        int xOffset;

        // 检测是否为 RTL 模式
        if (RTLUtils.isRtl(anchor.getContext())) {
            // RTL 模式：从左侧显示，距离屏幕左侧 16dp
            xOffset = -(anchorX - margin);
        } else {
            // LTR 模式：从右侧显示，距离屏幕右侧 16dp
            xOffset = screenWidth - anchorX - popupWidth - margin;
        }

        popupWindow.showAsDropDown(anchor, xOffset, 0);

        setWindowAlpha(anchor.getContext(), 0.5f);
    }

    protected void showDialog(String userId) {
        // 弹出删除好友确认对话框
        CommonDialog dialog =
                new CommonDialog.Builder()
                        .setContentMessage(getString(R.string.rc_reject_request))
                        .setDialogButtonClickListener(
                                new CommonDialog.OnDialogButtonClickListener() {
                                    @Override
                                    public void onPositiveClick(View v, Bundle bundle) {
                                        getViewModel()
                                                .refuseFriendApplication(
                                                        userId,
                                                        new OnDataChangeListener<Boolean>() {
                                                            @Override
                                                            public void onDataChange(
                                                                    Boolean aBoolean) {
                                                                if (aBoolean) {
                                                                    ToastUtils.show(
                                                                            getContext(),
                                                                            getString(
                                                                                    R.string
                                                                                            .rc_reject_success),
                                                                            Toast.LENGTH_SHORT);
                                                                    getViewModel()
                                                                            .loadFriendApplications(
                                                                                    false);
                                                                }
                                                            }
                                                        });
                                    }

                                    @Override
                                    public void onNegativeClick(View v, Bundle bundle) {}
                                })
                        .build();
        dialog.show(getParentFragmentManager(), null);
    }

    private void setWindowAlpha(Context context, float alpha) {
        Window window = ((Activity) context).getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.alpha = alpha;
        window.setAttributes(layoutParams);
    }
}
