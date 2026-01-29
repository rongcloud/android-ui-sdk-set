package io.rong.imkit.feature.expose;

public interface OnItemExposeListener<T> {
    /**
     * item 可见性回调 回调此方法时 视觉上一定是可见的（无论可见多少）
     *
     * @param visible true，逻辑上可见，即宽/高 >50%
     * @param position item在列表中的位置
     * @param data 数据
     */
    void onItemViewVisible(boolean visible, int position, T data);
}
