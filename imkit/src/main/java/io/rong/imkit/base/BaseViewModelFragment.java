package io.rong.imkit.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

/**
 * Fragment 页面基类
 *
 * @author rongcloud
 * @since 5.10.4
 */
public abstract class BaseViewModelFragment<VM extends ViewModel> extends BaseFragment
        implements BasePage {
    private VM viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments() == null ? new Bundle() : getArguments();
        this.viewModel = onCreateViewModel(bundle);
    }

    @Nullable
    @Override
    public final View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        onBeforeViewReady(this.viewModel);
        View view = onCreateView(requireActivity(), inflater, container, getArguments());
        onViewReady(this.viewModel);
        return view;
    }

    /**
     * 创建 ViewModel
     *
     * @param bundle Bundle
     * @return 返回 ViewModel
     */
    @NonNull
    protected abstract VM onCreateViewModel(@NonNull Bundle bundle);

    /**
     * View 创建之前
     *
     * @param viewModel VM
     */
    protected void onBeforeViewReady(@NonNull VM viewModel) {}

    /**
     * View 创建之后
     *
     * @param viewModel VM
     */
    protected abstract void onViewReady(@NonNull VM viewModel);

    @NonNull
    protected VM getViewModel() {
        return viewModel;
    }
}
