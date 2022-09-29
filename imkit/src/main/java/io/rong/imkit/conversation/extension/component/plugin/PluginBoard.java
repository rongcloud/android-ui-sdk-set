package io.rong.imkit.conversation.extension.component.plugin;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imlib.model.Conversation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginBoard {
    private final String TAG = this.getClass().getSimpleName();
    private ViewGroup mViewContainer;
    private ViewGroup mRoot;
    private List<IPluginModule> mPluginModules = new ArrayList<>();
    private Fragment mFragment;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private int mPluginCountPerPage;
    private int currentPage = 0;
    private ViewPager2 mViewPager;
    private View mCustomPager;
    private PluginPagerAdapter mPagerAdapter;
    private LinearLayout mIndicator;

    private static final int DEFAULT_SHOW_COLUMN = 4;
    // FIXME: 2021/10/15 优先确保显示两行按钮时不会产生滑动
    private static final int DEFAULT_SHOW_ROW = 2;

    public PluginBoard(
            Fragment fragment,
            ViewGroup parent,
            Conversation.ConversationType type,
            String targetId) {
        mFragment = fragment;
        mRoot = parent;
        mConversationType = type;
        mTargetId = targetId;
        initView(fragment.getContext(), mRoot);
    }

    public ViewGroup getView() {
        if (mCustomPager != null) {
            mCustomPager.setVisibility(View.GONE);
        }
        return mViewContainer;
    }

    private void initView(Context context, ViewGroup viewGroup) {
        mViewContainer =
                (ViewGroup)
                        LayoutInflater.from(context)
                                .inflate(R.layout.rc_ext_plugin_pager, viewGroup, false);
        mViewContainer.addOnAttachStateChangeListener(
                new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(View v) {
                        mRoot.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        ConstraintLayout.LayoutParams layoutParams =
                                                (ConstraintLayout.LayoutParams)
                                                        mIndicator.getLayoutParams();
                                        Pair<Integer, Integer> cellSize =
                                                calculateCellSize(
                                                        mRoot,
                                                        DEFAULT_SHOW_COLUMN,
                                                        DEFAULT_SHOW_ROW,
                                                        0,
                                                        mIndicator.getHeight()
                                                                + layoutParams.topMargin
                                                                + layoutParams.bottomMargin,
                                                        0,
                                                        0);
                                        for (GridView gridView : mPagerAdapter.pageSet) {
                                            ((PluginItemAdapter) gridView.getAdapter())
                                                    .updateLayoutByCellSize(cellSize);
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onViewDetachedFromWindow(View v) {}
                });
        try {
            mPluginCountPerPage =
                    context.getResources()
                            .getInteger(
                                    context.getResources()
                                            .getIdentifier(
                                                    "rc_extension_plugin_count_per_page",
                                                    "integer",
                                                    context.getPackageName()));
        } catch (Exception e) {
            mPluginCountPerPage = 8;
        }

        mViewPager = mViewContainer.findViewById(R.id.rc_view_pager);
        mIndicator = mViewContainer.findViewById(R.id.rc_indicator);
        mViewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        onIndicatorChanged(currentPage, position);
                        currentPage = position;
                    }
                });
        initPlugins(mConversationType);
    }

    private Pair<Integer, Integer> calculateCellSize(
            ViewGroup parent,
            int numColumns,
            int numRows,
            int paddingTop,
            int paddingBottom,
            int paddingLeft,
            int paddingRight) {
        int parentHeight = parent.getHeight();
        int parentWidth = parent.getWidth();

        int cellWidth = (parentWidth - paddingLeft - paddingRight) / numColumns;
        int cellHeight = (parentHeight - paddingBottom - paddingTop) / numRows;
        // 为保证在美观，宽高比最大不超过 1.2
        return Pair.create(cellWidth, Math.min(cellHeight, (int) (cellWidth * 1.2)));
    }

    private void initIndicator(Context context, int pages, LinearLayout indicator) {
        for (int i = 0; i < pages; i++) {
            ImageView imageView =
                    (ImageView)
                            LayoutInflater.from(context).inflate(R.layout.rc_ext_indicator, null);
            imageView.setImageResource(R.drawable.rc_ext_indicator);
            indicator.addView(imageView);
            if (pages <= 1) {
                indicator.setVisibility(View.INVISIBLE);
            } else {
                indicator.setVisibility(View.VISIBLE);
            }
        }
    }

    private void initPlugins(Conversation.ConversationType conversationType) {
        // size() 大于 0 ，代表初始化过，直接返回
        if (mPluginModules != null && !mPluginModules.isEmpty()) {
            return;
        }
        mPluginModules =
                RongExtensionManager.getInstance()
                        .getExtensionConfig()
                        .getPluginModules(mConversationType, mTargetId);
        int pages = 0;
        int count = mPluginModules.size();
        if (count > 0) {
            int rem = count % mPluginCountPerPage;
            if (rem > 0) {
                rem = 1;
            }
            pages = count / mPluginCountPerPage + rem;
        }
        mPagerAdapter = new PluginPagerAdapter(pages, count);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);
        initIndicator(mFragment.getContext(), pages, mIndicator);
        onIndicatorChanged(-1, 0);
        mPagerAdapter.notifyDataSetChanged();
    }

    public void removePlugin(IPluginModule pluginModule) {
        mPluginModules.remove(pluginModule);
        if (mPagerAdapter != null && mViewPager != null) {
            int count = mPluginModules.size();
            if (count > 0) {
                int rem = count % mPluginCountPerPage;
                if (rem > 0) {
                    rem = 1;
                }
                int pages = count / mPluginCountPerPage + rem;
                mPagerAdapter.setPages(pages);
                mPagerAdapter.setItems(count);
                mPagerAdapter.notifyDataSetChanged();
                removeIndicator(pages, mIndicator);
            }
        }
    }

    public void addPlugin(IPluginModule pluginModule) {
        if (pluginModule == null) {
            RLog.d(TAG, "addPlugin pluginModule is null");
            return;
        }
        mPluginModules.add(pluginModule);
        int count = mPluginModules.size();
        if (mPagerAdapter != null && count > 0 && mIndicator != null) {
            int rem = count % mPluginCountPerPage;
            if (rem > 0) {
                rem = 1;
            }
            int pages = count / mPluginCountPerPage + rem;
            mPagerAdapter.setPages(pages);
            mPagerAdapter.setItems(count);
            mPagerAdapter.notifyDataSetChanged();
            mIndicator.removeAllViews();
            initIndicator(mFragment.getContext(), pages, mIndicator);
        }
    }

    private void removeIndicator(int totalPages, LinearLayout indicator) {
        int index = indicator.getChildCount();
        if (index > totalPages && index - 1 >= 0) {
            indicator.removeViewAt(index - 1);
            onIndicatorChanged(index, index - 1);
            if (totalPages <= 1) {
                indicator.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void onIndicatorChanged(int pre, int cur) {
        int count = mIndicator.getChildCount();
        if (count > 0 && pre < count && cur < count) {
            if (pre >= 0) {
                ImageView preView = (ImageView) mIndicator.getChildAt(pre);
                preView.setImageResource(R.drawable.rc_ext_indicator);
            }
            if (cur >= 0) {
                ImageView curView = (ImageView) mIndicator.getChildAt(cur);
                curView.setImageResource(R.drawable.rc_ext_indicator_hover);
            }
        }
    }

    public void addPager(View v) {
        mCustomPager = v;
        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mViewContainer.addView(v, params);
    }

    public View getPager() {
        return mCustomPager;
    }

    public void removePager(View view) {
        if (mCustomPager != null && mCustomPager == view) {
            mViewContainer.removeView(view);
            mCustomPager = null;
        }
    }

    public int getPluginPosition(IPluginModule pluginModule) {
        return mPluginModules.indexOf(pluginModule);
    }

    public IPluginModule getPluginModule(int position) {
        if (position >= 0 && position < mPluginModules.size()) {
            return mPluginModules.get(position);
        } else {
            return null;
        }
    }

    public List<IPluginModule> getPluginModules() {
        return mPluginModules;
    }

    private class PluginPagerAdapter extends RecyclerView.Adapter<PluginPagerViewHolder> {
        int pages;
        int items;
        Set<GridView> pageSet;

        public PluginPagerAdapter(int pages, int items) {
            this.pages = pages;
            this.items = items;
            pageSet = new HashSet<>();
        }

        @NonNull
        @Override
        public PluginPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            GridView gridView =
                    (GridView)
                            LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.rc_ext_plugin_grid_view, parent, false);
            pageSet.add(gridView);
            return new PluginPagerViewHolder(gridView);
        }

        @Override
        public void onBindViewHolder(@NonNull PluginPagerViewHolder holder, int position) {
            GridView gridView = holder.gridView;
            gridView.setNumColumns(DEFAULT_SHOW_COLUMN);
            gridView.setAdapter(new PluginItemAdapter(position * mPluginCountPerPage, items));
        }

        @Override
        public int getItemCount() {
            return pages;
        }

        public void setPages(int value) {
            this.pages = value;
        }

        public void setItems(int value) {
            this.items = value;
        }
    }

    private class PluginPagerViewHolder extends RecyclerView.ViewHolder {
        GridView gridView;

        public PluginPagerViewHolder(@NonNull View itemView) {
            super(itemView);
            this.gridView = (GridView) itemView;
        }
    }

    private class PluginItemAdapter extends BaseAdapter {
        int count;
        int index;
        Pair<Integer, Integer> cellSize;

        public PluginItemAdapter(int index, int count) {
            this.count = Math.min(mPluginCountPerPage, count - index);
            this.index = index;
            cellSize =
                    new Pair<>(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView =
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.rc_ext_plugin_item, null);

                holder.imageView = convertView.findViewById(R.id.rc_ext_plugin_icon);
                holder.textView = convertView.findViewById(R.id.rc_ext_plugin_title);
                convertView.setTag(holder);
            }
            AbsListView.LayoutParams layoutParams =
                    (AbsListView.LayoutParams) convertView.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new AbsListView.LayoutParams(cellSize.first, cellSize.second);
            } else {
                layoutParams.width = cellSize.first;
                layoutParams.height = cellSize.second;
            }
            convertView.setLayoutParams(layoutParams);

            convertView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            IPluginModule plugin =
                                    mPluginModules.get(
                                            currentPage * mPluginCountPerPage + position);
                            if (mFragment instanceof ConversationFragment) {
                                plugin.onClick(
                                        mFragment,
                                        ((ConversationFragment) mFragment).getRongExtension(),
                                        currentPage * mPluginCountPerPage + position);
                            }
                        }
                    });
            holder = (ViewHolder) convertView.getTag();
            IPluginModule plugin = mPluginModules.get(position + index);
            if (plugin == null) {
                return convertView;
            }
            holder.imageView.setImageDrawable(plugin.obtainDrawable(context));
            holder.textView.setText(plugin.obtainTitle(context));
            return convertView;
        }

        public void updateLayoutByCellSize(Pair<Integer, Integer> cellSize) {
            if (this.cellSize == null || !this.cellSize.equals(cellSize)) {
                this.cellSize = cellSize;
                notifyDataSetChanged();
            }
        }

        class ViewHolder {
            ImageView imageView;
            TextView textView;
        }
    }
}
