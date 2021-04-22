package io.rong.imkit.widget.adapter;

public interface IViewProviderListener<T> {

    /**
     * @param clickType 区分点击事件的标记位
     * @param data      传递的数据源
     */
    void onViewClick(int clickType, T data);


    /**
     * @param clickType 区分点击事件的标记位
     * @param data      传递的数据源
     */
    boolean onViewLongClick(int clickType, T data);
    
}
