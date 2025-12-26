package io.rong.imkit.widget.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.rong.imkit.R;
import java.util.List;

/** 会话列表长按操作弹窗，使用 PopupWindow 实现，支持图标显示 */
public class ConversationLongClickPopup {

    private Context mContext;
    private List<OptionItem> mOptionItems;
    private OnOptionItemClickListener mItemClickListener;
    private View mAnchorView; // 锚点视图，用于定位弹窗
    private PopupWindow mPopupWindow;
    private DialogInterface.OnDismissListener mOnDismissListener;
    private ImageView mTriangleIndicator; // 三角形指示器
    private View mContentContainer; // 主内容容器
    private HighlightMaskView mMaskView; // 遮罩视图，用于高亮 AnchorView

    /** 选项数据类 */
    public static class OptionItem {
        public final String title;
        public final int iconResId; // drawable 资源 ID

        public OptionItem(String title, int iconResId) {
            this.title = title;
            this.iconResId = iconResId;
        }
    }

    /** 选项点击监听器 */
    public interface OnOptionItemClickListener {
        /**
         * 当选项被点击时回调
         *
         * @param item 被点击的选项对象
         * @param position 选项在列表中的位置
         */
        void onOptionItemClick(OptionItem item, int position);
    }

    public static ConversationLongClickPopup newInstance(
            final Context context, List<OptionItem> items) {
        return new ConversationLongClickPopup(context, items);
    }

    public ConversationLongClickPopup(final Context context, List<OptionItem> items) {
        mContext = context;
        mOptionItems = items;
        initPopupWindow();
    }

    private void initPopupWindow() {
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View contentView = inflater.inflate(R.layout.rc_popup_conversation_long_click, null);

        RecyclerView recyclerView =
                contentView.findViewById(R.id.rc_recycler_conversation_long_click);
        mTriangleIndicator = contentView.findViewById(R.id.rc_popup_triangle_indicator);
        mContentContainer = contentView.findViewById(R.id.rc_popup_content_container);

        // 使用水平的 LinearLayoutManager，让每个 item 宽度根据内容自适应
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // 添加 item 间距（仅在 item 之间，不包括首尾）
        recyclerView.addItemDecoration(new HorizontalSpaceItemDecoration(dpToPx(0)));

        OptionsAdapter adapter = new OptionsAdapter(mOptionItems);
        recyclerView.setAdapter(adapter);

        // 创建 PopupWindow
        mPopupWindow =
                new PopupWindow(
                        contentView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true); // focusable

        // 设置背景，使得点击外部可以关闭
        mPopupWindow.setBackgroundDrawable(
                mContext.getResources().getDrawable(android.R.color.transparent));

        // 点击外部可关闭
        mPopupWindow.setOutsideTouchable(true);

        // 设置输入法模式，不影响原有页面的 EditText 焦点
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopupWindow.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        // 设置dismiss监听
        mPopupWindow.setOnDismissListener(
                new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        // 移除遮罩视图
                        removeMaskView();
                        if (mOnDismissListener != null) {
                            mOnDismissListener.onDismiss(null);
                        }
                    }
                });

        // 测量 contentView 以获取实际尺寸
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    }

    /**
     * 设置锚点视图，弹窗将显示在锚点视图上方
     *
     * @param anchorView 锚点视图
     * @return this
     */
    public ConversationLongClickPopup setAnchorView(View anchorView) {
        this.mAnchorView = anchorView;
        return this;
    }

    /**
     * 设置选项点击监听器
     *
     * @param listener 监听器
     * @return this
     */
    public ConversationLongClickPopup setOnOptionItemClickListener(
            OnOptionItemClickListener listener) {
        this.mItemClickListener = listener;
        return this;
    }

    /**
     * 设置选项点击监听器（兼容旧版本 API）
     *
     * @param itemListener 监听器
     * @return this
     * @deprecated 请使用 {@link #setOnOptionItemClickListener(OnOptionItemClickListener)}
     */
    @Deprecated
    public ConversationLongClickPopup setOptionsPopupDialogListener(
            final OptionsPopupDialog.OnOptionsItemClickedListener itemListener) {
        if (itemListener != null) {
            this.mItemClickListener =
                    new OnOptionItemClickListener() {
                        @Override
                        public void onOptionItemClick(OptionItem item, int position) {
                            itemListener.onOptionsItemClicked(position);
                        }
                    };
        }
        return this;
    }

    /**
     * 设置 dismiss 监听器
     *
     * @param listener 监听器
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        this.mOnDismissListener = listener;
    }

    /** 显示弹窗 */
    public void show() {
        if (mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            if (activity.isFinishing()) {
                return;
            }
        }

        if (mPopupWindow == null) {
            return;
        }

        if (mAnchorView != null) {
            // 获取 anchorView 在屏幕中的位置
            int[] location = new int[2];
            mAnchorView.getLocationOnScreen(location);

            View contentView = mPopupWindow.getContentView();
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = contentView.getMeasuredHeight();
            int popupWidth = contentView.getMeasuredWidth();

            // 获取状态栏高度和顶部安全距离
            int statusBarHeight = getStatusBarHeight();
            int topMargin = statusBarHeight + dpToPx(40); // 状态栏 + 40dp 边距

            // 与锚点视图的间距，确保三角形不与 anchorView 重叠
            // 使用负值让 Popup 和 anchorView 之间留出间距
            int verticalSpacing = -dpToPx(7); // 留出 2dp 的间距

            // 获取屏幕宽度和边距
            int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
            int horizontalMargin = dpToPx(10); // 左右边距

            // 计算锚点的中心位置
            int anchorCenterX = location[0] + mAnchorView.getWidth() / 2;

            // 优先让弹窗水平居中对齐锚点，然后调整以适应屏幕
            int xPos = anchorCenterX - popupWidth / 2;

            // 确保不超出屏幕左右边界
            if (xPos < horizontalMargin) {
                xPos = horizontalMargin;
            } else if (xPos + popupWidth > screenWidth - horizontalMargin) {
                xPos = screenWidth - popupWidth - horizontalMargin;
            }

            // 计算垂直位置
            int yPos = location[1] - popupHeight + verticalSpacing;
            boolean isShowOnTop = true; // 默认显示在上方

            // 检查上方是否有足够空间（需要留出顶部安全距离）
            if (yPos < topMargin) {
                // 上方空间不够，显示在下方
                yPos = location[1] + mAnchorView.getHeight();
                isShowOnTop = false;
            }

            // 调整三角形指示器的位置和方向，传入锚点的原始位置
            adjustTriangleIndicator(
                    location[0], mAnchorView.getWidth(), xPos, popupWidth, isShowOnTop);

            // 添加高亮遮罩视图
            addMaskView();

            // 使用 showAtLocation 在指定位置显示
            mPopupWindow.showAtLocation(mAnchorView, Gravity.NO_GRAVITY, xPos, yPos);
        } else {
            // 如果没有锚点视图，隐藏三角形指示器，在屏幕中心显示
            if (mTriangleIndicator != null) {
                mTriangleIndicator.setVisibility(View.GONE);
            }
            if (mContext instanceof Activity) {
                // 添加遮罩视图（没有高亮区域）
                addMaskView();

                View decorView = ((Activity) mContext).getWindow().getDecorView();
                mPopupWindow.showAtLocation(decorView, Gravity.CENTER, 0, 0);
            }
        }
    }

    /** 关闭弹窗 */
    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    /** 添加遮罩视图，并设置高亮区域为 AnchorView */
    private void addMaskView() {
        if (!(mContext instanceof Activity)) {
            return;
        }

        Activity activity = (Activity) mContext;
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        // 创建遮罩视图
        mMaskView = new HighlightMaskView(mContext);
        mMaskView.setMaskColor(0x99000000); // 半透明黑色遮罩

        // 如果有锚点视图，设置高亮区域
        if (mAnchorView != null) {
            // 获取 AnchorView 在屏幕中的位置
            int[] location = new int[2];
            mAnchorView.getLocationOnScreen(location);

            Rect highlightRect =
                    new Rect(
                            location[0],
                            location[1],
                            location[0] + mAnchorView.getWidth(),
                            location[1] + mAnchorView.getHeight());

            mMaskView.setHighlightRect(highlightRect);

            // 设置圆角（假设会话列表项有圆角，这里设置为8dp）
            mMaskView.setCornerRadius(dpToPx(0));
        }

        // 添加遮罩视图到 DecorView
        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        decorView.addView(mMaskView, params);

        // 设置点击遮罩关闭弹窗
        mMaskView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
    }

    /** 移除遮罩视图 */
    private void removeMaskView() {
        if (mMaskView != null && mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            decorView.removeView(mMaskView);
            mMaskView = null;
        }
    }

    /**
     * 判断弹窗是否正在显示
     *
     * @return true 如果正在显示
     */
    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    /**
     * 获取状态栏高度
     *
     * @return 状态栏高度（px）
     */
    private int getStatusBarHeight() {
        int statusBarHeight = 0;
        int resourceId =
                mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    /**
     * dp 转 px
     *
     * @param dp dp 值
     * @return px 值
     */
    private int dpToPx(int dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 调整三角形指示器的位置和方向
     *
     * @param anchorX 锚点视图的屏幕 X 坐标
     * @param anchorWidth 锚点视图的宽度
     * @param popupX 弹窗的 X 坐标
     * @param popupWidth 弹窗的宽度
     * @param isShowOnTop 是否显示在锚点上方
     */
    private void adjustTriangleIndicator(
            int anchorX, int anchorWidth, int popupX, int popupWidth, boolean isShowOnTop) {
        if (mTriangleIndicator == null || mContentContainer == null) {
            return;
        }

        // 显示三角形
        mTriangleIndicator.setVisibility(View.VISIBLE);

        // 计算锚点视图的中心位置
        int anchorCenterX = anchorX + anchorWidth / 2;

        // 计算三角形相对于弹窗的偏移位置（箭头中心对准锚点中心）
        int triangleX = anchorCenterX - popupX - dpToPx(8); // 8dp 是三角形宽度的一半

        // 确保三角形不完全超出弹窗边界（允许箭头在边缘，但至少要有一半在弹窗内）
        int minX = -dpToPx(4); // 允许箭头左侧超出一半
        int maxX = popupWidth - dpToPx(12); // 允许箭头右侧超出一半

        // 只在箭头会完全超出时才进行限制
        if (triangleX < minX) {
            triangleX = minX;
        } else if (triangleX > maxX) {
            triangleX = maxX;
        }

        // 调整主内容容器的 margin，为三角形腾出空间
        ViewGroup.MarginLayoutParams contentParams =
                (ViewGroup.MarginLayoutParams) mContentContainer.getLayoutParams();

        // 设置三角形的水平位置
        android.widget.FrameLayout.LayoutParams triangleParams =
                (android.widget.FrameLayout.LayoutParams) mTriangleIndicator.getLayoutParams();
        triangleParams.leftMargin = triangleX;

        // 根据显示位置调整三角形方向和内容区域的 margin
        if (isShowOnTop) {
            // 弹窗在上方，三角形在底部朝下指向锚点
            mTriangleIndicator.setRotation(180); // 三角形尖端朝下
            triangleParams.gravity = Gravity.BOTTOM | Gravity.START;
            // 底部留出三角形的空间
            contentParams.topMargin = 0;
            contentParams.bottomMargin = dpToPx(7);
        } else {
            // 弹窗在下方，三角形在顶部朝上指向锚点
            mTriangleIndicator.setRotation(0); // 三角形尖端朝上
            triangleParams.gravity = Gravity.TOP | Gravity.START;
            // 顶部留出三角形的空间
            contentParams.topMargin = dpToPx(7);
            contentParams.bottomMargin = 0;
        }

        mTriangleIndicator.setLayoutParams(triangleParams);
        mContentContainer.setLayoutParams(contentParams);
    }

    private class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> {

        private List<OptionItem> items;

        public OptionsAdapter(List<OptionItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.rc_popup_conversation_long_click_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final OptionItem item = items.get(position);
            holder.titleView.setText(item.title);

            // 设置图标
            if (item.iconResId != 0) {
                holder.iconView.setImageResource(item.iconResId);
                holder.iconView.setVisibility(View.VISIBLE);
            } else {
                holder.iconView.setVisibility(View.GONE);
            }

            // 设置点击事件
            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int clickPosition = holder.getAdapterPosition();
                            if (clickPosition != RecyclerView.NO_POSITION
                                    && mItemClickListener != null) {
                                mItemClickListener.onOptionItemClick(item, clickPosition);
                                dismiss();
                            }
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconView;
            TextView titleView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.rc_popup_item_icon);
                titleView = itemView.findViewById(R.id.rc_popup_item_text);
            }
        }
    }

    /** 水平方向的 ItemDecoration，用于在 item 之间添加间距 */
    private static class HorizontalSpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public HorizontalSpaceItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect,
                @NonNull View view,
                @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {
            // 不是最后一个 item，添加右侧间距
            if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
                outRect.right = spacing;
            }
        }
    }
}
