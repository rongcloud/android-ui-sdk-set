package io.rong.imkit.widget.adapter;


import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class BaseAdapter<T> extends RecyclerView.Adapter<ViewHolder> {
    private final String TAG = BaseAdapter.class.getSimpleName();
    protected IViewProviderListener<T> mListener;
    protected List<T> mDataList = new ArrayList<>();
    protected OnItemClickListener mOnItemClickListener;
    protected ProviderManager<T> mProviderManager = new ProviderManager<>();
    private final int EMPTY_ITEM_VIEW_TYPE = -200;
    private static final int BASE_ITEM_TYPE_HEADER = -300;
    private static final int BASE_ITEM_TYPE_FOOTER = -400;
    private View mEmptyView;
    private @LayoutRes
    int mEmptyId;
    private SparseArrayCompat<View> mHeaderViews = new SparseArrayCompat<>();
    private SparseArrayCompat<View> mFootViews = new SparseArrayCompat<>();

    public BaseAdapter() {

    }

    public BaseAdapter(IViewProviderListener<T> listener, ProviderManager<T> providerManager) {
        mListener = listener;
        mProviderManager = providerManager;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderViewPos(position)) {
            return mHeaderViews.keyAt(position);
        } else if (isFooterViewPos(position)) {
            return mFootViews.keyAt(position - (getHeadersCount() + (isEmpty() ? 1 : getRealItemCount())));
        } else if (isEmpty()) {
            return EMPTY_ITEM_VIEW_TYPE;
        } else {
            int listPosition = position - getHeadersCount();
            //没有空布局返回真实数值
            if (mProviderManager != null) {
                return mProviderManager.getItemViewType(mDataList.get(listPosition), listPosition);
            } else {
                throw new IllegalArgumentException(
                        "adapter did not set providerManager");
            }
        }

    }


    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mHeaderViews.get(viewType) != null) {
            return ViewHolder.createViewHolder(parent.getContext(), mHeaderViews.get(viewType));

        } else if (mFootViews.get(viewType) != null) {
            return ViewHolder.createViewHolder(parent.getContext(), mFootViews.get(viewType));
        } else if (viewType == EMPTY_ITEM_VIEW_TYPE) {
            ViewHolder holder;
            if (mEmptyView != null) {
                holder = ViewHolder.createViewHolder(parent.getContext(), mEmptyView);
            } else {
                holder = ViewHolder.createViewHolder(parent.getContext(), parent, mEmptyId);
            }
            return holder;
        } else {
            IViewProvider<T> provider = mProviderManager.getProvider(viewType);
            return provider.onCreateViewHolder(parent, viewType);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if (isHeaderViewPos(position)) {
            return;
        } else if (isFooterViewPos(position)) {
            return;
        } else if (isEmpty()) {
            return;
        } else {
            final int listPosition = position - getHeadersCount();
            IViewProvider<T> provider = mProviderManager.getProvider(mDataList.get(listPosition));
            provider.bindViewHolder(holder, mDataList.get(listPosition), listPosition, mDataList, mListener);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v, holder, listPosition);
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemClickListener != null) {
                        return mOnItemClickListener.onItemLongClick(v, holder, listPosition);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (isEmpty()) {
            return getHeadersCount() + getFootersCount() + 1;
        } else {
            //没有空布局返回真实数值
            return getHeadersCount() + getFootersCount() + getRealItemCount();
        }

    }

    public void setDataCollection(List<T> data) {
        if (data != null) {
            this.mDataList.clear();
            this.mDataList.addAll(data);
        }
    }

    public void add(T t) {
        mDataList.add(t);
    }

    public void remove(T t) {
        mDataList.remove(t);
    }

    public List<T> getData() {
        return mDataList;
    }

    public T getItem(int position) {
        return mDataList.get(position);
    }

    public void setItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, ViewHolder holder, int position);

        boolean onItemLongClick(View view, ViewHolder holder, int position);

    }

    private boolean isHeaderViewPos(int position) {
        return position < getHeadersCount();
    }

    private boolean isFooterViewPos(int position) {
        return position >= getHeadersCount() + (isEmpty() ? 1 : getRealItemCount());
    }

    protected boolean isEmpty() {
        return (mEmptyView != null || mEmptyId != 0) && getRealItemCount() == 0;
    }

    public void addHeaderView(View view) {
        mHeaderViews.put(mHeaderViews.size() + BASE_ITEM_TYPE_HEADER, view);
    }

    public void addFootView(View view) {
        mFootViews.put(mFootViews.size() + BASE_ITEM_TYPE_FOOTER, view);
    }

    public void setEmptyView(View view) {
        mEmptyView = view;
    }

    public void setEmptyView(@LayoutRes int emptyId) {
        mEmptyId = emptyId;
    }

    public int getHeadersCount() {
        return mHeaderViews.size();
    }

    public int getFootersCount() {
        return mFootViews.size();
    }

    private int getRealItemCount() {
        return mDataList.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        WrapperUtils.onAttachedToRecyclerView(this, recyclerView, new WrapperUtils.SpanSizeCallback() {
            @Override
            public int getSpanSize(GridLayoutManager layoutManager, GridLayoutManager.SpanSizeLookup oldLookup, int position) {
                int viewType = getItemViewType(position);
                if (mHeaderViews.get(viewType) != null) {
                    return layoutManager.getSpanCount();
                } else if (mFootViews.get(viewType) != null) {
                    return layoutManager.getSpanCount();
                } else if (isEmpty()) {
                    return layoutManager.getSpanCount();
                }
                if (oldLookup != null) {
                    return oldLookup.getSpanSize(position);
                }
                return 1;
            }
        });


    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        int position = holder.getLayoutPosition();
        if (isHeaderViewPos(position) || isFooterViewPos(position)) {
            WrapperUtils.setFullSpan(holder);
        } else if (isEmpty()) {
            WrapperUtils.setFullSpan(holder);
        }

    }


}
