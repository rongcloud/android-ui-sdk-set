package io.rong.imkit.usermanage.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseComponent;
import io.rong.imkit.widget.refresh.SmartRefreshLayout;
import io.rong.imkit.widget.refresh.api.RefreshFooter;
import io.rong.imkit.widget.refresh.api.RefreshHeader;
import io.rong.imkit.widget.refresh.listener.OnLoadMoreListener;
import io.rong.imkit.widget.refresh.listener.OnRefreshListener;
import io.rong.imkit.widget.refresh.wrapper.RongRefreshHeader;

public final class ListComponent extends BaseComponent {

    public ListComponent(@NonNull Context context) {
        super(context);
    }

    public ListComponent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ListComponent(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private SmartRefreshLayout refreshLayout;
    private RecyclerView recyclerView;

    @Override
    protected View onCreateView(
            Context context, LayoutInflater from, @NonNull ViewGroup parent, AttributeSet attrs) {
        View view = from.inflate(R.layout.rc_list_component, parent, false);
        refreshLayout = view.findViewById(R.id.rc_refresh);
        refreshLayout.setNestedScrollingEnabled(false);
        refreshLayout.setRefreshHeader(new RongRefreshHeader(context));
        refreshLayout.setRefreshFooter(new RongRefreshHeader(context));
        recyclerView = view.findViewById(R.id.rc_list);
        recyclerView.setItemAnimator(null);
        return view;
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        recyclerView.setAdapter(adapter);
    }

    public void finishRefresh() {
        refreshLayout.finishRefresh();
    }

    public void finishLoadMore() {
        refreshLayout.finishLoadMore();
    }

    public void setEnableRefresh(boolean enable) {
        refreshLayout.setEnableRefresh(enable);
        refreshLayout.setEnableLoadMore(enable);
    }

    public void setEnableLoadMore(boolean enable) {
        refreshLayout.setEnableLoadMore(enable);
    }

    public void setRefreshHeader(@NonNull RefreshHeader header) {
        refreshLayout.setRefreshHeader(header);
    }

    public void setRefreshFooter(@NonNull RefreshFooter footer) {
        refreshLayout.setRefreshFooter(footer);
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        refreshLayout.setOnRefreshListener(listener);
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        refreshLayout.setOnLoadMoreListener(listener);
    }

    public SmartRefreshLayout getRefreshLayout() {
        return refreshLayout;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }
}
