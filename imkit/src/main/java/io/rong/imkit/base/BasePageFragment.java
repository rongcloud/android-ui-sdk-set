package io.rong.imkit.base;

import android.content.Context;
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
public abstract class BasePageFragment<P extends BasePage, VM extends ViewModel>
        extends BaseViewModelFragment<VM> {
    private P page;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments() == null ? new Bundle() : getArguments();
        this.page = onCreatePage(bundle);
    }

    @Nullable
    @Override
    public final View onCreateView(
            @NonNull Context context,
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return page.onCreateView(context, inflater, container, getArguments());
    }

    /**
     * 创建页面
     *
     * @param bundle Bundle
     * @return 返回页面
     */
    @NonNull
    protected abstract P onCreatePage(@NonNull Bundle bundle);

    /**
     * View 创建之前
     *
     * @param page P
     * @param viewModel VM
     */
    protected void onBeforeViewReady(@NonNull P page, @NonNull VM viewModel) {}

    /**
     * View 创建之后
     *
     * @param page P
     * @param viewModel VM
     */
    protected abstract void onViewReady(@NonNull P page, @NonNull VM viewModel);

    @NonNull
    protected P getPage() {
        return page;
    }

    @Override
    protected final void onBeforeViewReady(@NonNull VM viewModel) {
        onBeforeViewReady(this.page, viewModel);
    }

    @Override
    protected final void onViewReady(@NonNull VM viewModel) {
        onViewReady(this.page, viewModel);
    }
}
