package io.rong.imkit.widget.adapter;

import android.view.View;

public interface IViewProviderListener<T> {

    /**
     * @param clickType 区分点击事件的标记位
     * @param data 传递的数据源
     */
    void onViewClick(int clickType, T data);

    /**
     * @param view 触发长按的视图
     * @param clickType 区分点击事件的标记位
     * @param data 传递的数据源
     */
    default boolean onViewLongClick(View view, int clickType, T data) {
        return onViewLongClick(clickType, data);
    }

    /**
     * @param clickType 区分点击事件的标记位
     * @param data 传递的数据源
     * @deprecated Use {@link #onViewLongClick(View, int, Object)} instead
     */
    @Deprecated
    boolean onViewLongClick(int clickType, T data);
}
