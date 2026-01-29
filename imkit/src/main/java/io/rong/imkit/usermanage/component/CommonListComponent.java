package io.rong.imkit.usermanage.component;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseComponent;
import io.rong.imkit.usermanage.interfaces.OnPagedDataLoader;
import io.rong.imkit.widget.refresh.SmartRefreshLayout;
import io.rong.imkit.widget.refresh.wrapper.RongRefreshHeader;

public final class CommonListComponent extends BaseComponent {

    private SmartRefreshLayout refreshLayout;
    private RecyclerView recyclerView;

    private OnPagedDataLoader onPagedDataLoader;

    private boolean enableLoadMore;
    private boolean enableRefresh;

    public CommonListComponent(@NonNull Context context) {
        super(context);
    }

    public CommonListComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CommonListComponent(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View onCreateView(
            Context context, LayoutInflater from, @NonNull ViewGroup parent, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a =
                    context.getTheme()
                            .obtainStyledAttributes(attrs, R.styleable.CommonListComponent, 0, 0);
            try {
                enableLoadMore =
                        a.getBoolean(R.styleable.CommonListComponent_enable_load_more, false);
                enableRefresh = a.getBoolean(R.styleable.CommonListComponent_enable_refresh, false);
            } finally {
                if (a != null) {
                    a.recycle();
                }
            }
        }

        View view = from.inflate(R.layout.rc_list_component, parent, false);
        refreshLayout = view.findViewById(R.id.rc_refresh);
        refreshLayout.setNestedScrollingEnabled(false);
        refreshLayout.setRefreshHeader(new RongRefreshHeader(context));
        refreshLayout.setRefreshFooter(new RongRefreshHeader(context));
        refreshLayout.setOnLoadMoreListener(
                refreshLayout -> {
                    if (onPagedDataLoader != null) {
                        onPagedDataLoader.loadNext(
                                aBoolean -> {
                                    refreshLayout.finishLoadMore();
                                    if (!onPagedDataLoader.hasNext()) {
                                        refreshLayout.setEnableLoadMore(false);
                                    }
                                });
                    }
                });
        refreshLayout.setOnRefreshListener(
                refreshLayout -> {
                    if (onPagedDataLoader != null) {
                        onPagedDataLoader.loadPrevious(
                                aBoolean -> {
                                    refreshLayout.finishRefresh();
                                    if (!onPagedDataLoader.hasPrevious()) {
                                        refreshLayout.setEnableRefresh(false);
                                    }
                                });
                    }
                });
        refreshLayout.setEnableRefresh(enableRefresh);
        refreshLayout.setEnableLoadMore(enableLoadMore);
        recyclerView = view.findViewById(R.id.rc_list);
        recyclerView.setItemAnimator(null);
        return view;
    }

    /**
     * 设置适配器
     *
     * @param adapter 适配器
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    /**
     * 设置分页数据加载器
     *
     * @param onPageLoader 分页数据加载器
     */
    public void setOnPageDataLoader(OnPagedDataLoader onPageLoader) {
        this.onPagedDataLoader = onPageLoader;
    }
}
