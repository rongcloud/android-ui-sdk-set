package io.rong.imkit.feature.expose;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import io.rong.imkit.widget.adapter.BaseAdapter;

public class ItemExposeManager<T> {

    private OnItemExposeListener<T> mItemOnExposeListener;

    private RecyclerView mRecyclerView;
    private BaseAdapter<T> mAdapter;

    /** 曝光阈值比例，范围 0.0 ~ 1.0 0.1 表示可见10%就触发曝光回调 0.5 表示可见50%就触发曝光回调 默认值为 0.5（50%） */
    private float mExposeThresholdRatio = 0.1f;

    public ItemExposeManager() {}

    /**
     * 设置RecyclerView的item可见状态的监听
     *
     * @param recyclerView recyclerView
     * @param onExposeListener 列表中的item可见性的回调
     */
    public void attach(
            RecyclerView recyclerView,
            BaseAdapter<T> adapter,
            OnItemExposeListener<T> onExposeListener) {
        if (recyclerView == null
                || recyclerView.getVisibility() != View.VISIBLE
                || adapter == null
                || onExposeListener == null) {
            return;
        }
        mItemOnExposeListener = onExposeListener;
        mRecyclerView = recyclerView;
        mAdapter = adapter;

        // 检测recyclerView的滚动事件
        mRecyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(
                            @NonNull RecyclerView recyclerView, int newState) {
                        // 关注：SCROLL_STATE_IDLE:停止滚动；  SCROLL_STATE_DRAGGING: 用户慢慢拖动
                        // 关注：SCROLL_STATE_SETTLING：惯性滚动
                        if (newState == RecyclerView.SCROLL_STATE_IDLE
                                || newState == RecyclerView.SCROLL_STATE_DRAGGING
                                || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                            handleCurrentVisibleItems();
                        }
                    }

                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        // 包括刚进入列表时统计当前屏幕可见views
                        handleCurrentVisibleItems();
                    }
                });
    }

    public void release() {
        mItemOnExposeListener = null;
        mRecyclerView = null;
        mAdapter = null;
    }

    /** 处理 当前屏幕上mRecyclerView可见的item view */
    public void handleCurrentVisibleItems() {
        // View.getGlobalVisibleRect(new Rect())，true表示view视觉可见，无论可见多少。
        if (mRecyclerView == null
                || mRecyclerView.getVisibility() != View.VISIBLE
                || !mRecyclerView.isShown()
                || !mRecyclerView.getGlobalVisibleRect(new Rect())) {
            return;
        }
        // 保险起见，为了不让统计影响正常业务，这里做下try-catch
        try {
            int[] range = new int[2];
            int orientation = -1;
            RecyclerView.LayoutManager manager = mRecyclerView.getLayoutManager();
            if (manager instanceof GridLayoutManager) {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) manager;
                range = findRangeGrid(gridLayoutManager);
                orientation = gridLayoutManager.getOrientation();
            } else if (manager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) manager;
                range = findRangeLinear(linearLayoutManager);
                orientation = linearLayoutManager.getOrientation();
            } else if (manager instanceof StaggeredGridLayoutManager) {
                StaggeredGridLayoutManager staggeredGridLayoutManager =
                        (StaggeredGridLayoutManager) manager;
                range = findRangeStaggeredGrid(staggeredGridLayoutManager);
                orientation = staggeredGridLayoutManager.getOrientation();
            }
            if (range.length < 2) {
                return;
            }
            // 注意，这里 会处理此刻 滑动过程中 所有可见的view
            for (int i = range[0]; i <= range[1]; i++) {
                if (manager != null) {
                    View view = manager.findViewByPosition(i);
                    setCallbackForLogicVisibleView(view, i, orientation);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 为 逻辑上可见的view设置 可见性回调 说明：逻辑上可见--可见且可见高度（宽度）>view高度（宽度）的50%
     *
     * @param view 可见item的view
     * @param position 可见item的position
     * @param orientation recyclerView的方向
     */
    private void setCallbackForLogicVisibleView(View view, int position, int orientation) {
        if (view == null
                || view.getVisibility() != View.VISIBLE
                || !view.isShown()
                || !view.getGlobalVisibleRect(new Rect())) {
            return;
        }
        if (mAdapter == null || mItemOnExposeListener == null) {
            return;
        }
        Rect rect = new Rect();

        boolean cover = view.getGlobalVisibleRect(rect);

        // 曝光阈值比例，范围 0.0 ~ 1.0
        float exposeThresholdRatio =
                (mExposeThresholdRatio < 0.0f || mExposeThresholdRatio > 1.0f)
                        ? 0.2f
                        : mExposeThresholdRatio;
        // item逻辑上可见：可见且可见高度（宽度）大于view高度（宽度）的阈值比例才触发曝光
        boolean visibleHeightEnough =
                orientation == OrientationHelper.VERTICAL
                        && rect.height() > view.getMeasuredHeight() * exposeThresholdRatio;
        boolean visibleWidthEnough =
                orientation == OrientationHelper.HORIZONTAL
                        && rect.width() > view.getMeasuredWidth() * exposeThresholdRatio;
        boolean isItemViewVisibleInLogic = visibleHeightEnough || visibleWidthEnough;

        T itemData = mAdapter.getItem(position);
        boolean visible = cover && isItemViewVisibleInLogic;
        mItemOnExposeListener.onItemViewVisible(visible, position, itemData);
    }

    private int[] findRangeLinear(LinearLayoutManager manager) {
        int[] range = new int[2];
        range[0] = manager.findFirstVisibleItemPosition();
        range[1] = manager.findLastVisibleItemPosition();
        return range;
    }

    private int[] findRangeGrid(GridLayoutManager manager) {
        int[] range = new int[2];
        range[0] = manager.findFirstVisibleItemPosition();
        range[1] = manager.findLastVisibleItemPosition();
        return range;
    }

    private int[] findRangeStaggeredGrid(StaggeredGridLayoutManager manager) {
        int[] startPos = new int[manager.getSpanCount()];
        int[] endPos = new int[manager.getSpanCount()];
        manager.findFirstVisibleItemPositions(startPos);
        manager.findLastVisibleItemPositions(endPos);
        return findRange(startPos, endPos);
    }

    private int[] findRange(int[] startPos, int[] endPos) {
        int start = startPos[0];
        int end = endPos[0];
        for (int i = 1; i < startPos.length; i++) {
            if (start > startPos[i]) {
                start = startPos[i];
            }
        }
        for (int i = 1; i < endPos.length; i++) {
            if (end < endPos[i]) {
                end = endPos[i];
            }
        }
        return new int[] {start, end};
    }
}
