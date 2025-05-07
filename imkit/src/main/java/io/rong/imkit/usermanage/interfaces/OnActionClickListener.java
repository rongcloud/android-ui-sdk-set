package io.rong.imkit.usermanage.interfaces;

/**
 * 动作点击事件监听
 *
 * @author rongcloud
 * @since 5.12.2
 */
public interface OnActionClickListener<T> {

    /**
     * 动作点击事件
     *
     * @param t 动作信息
     */
    void onActionClick(T t);

    /**
     * 动作点击事件
     *
     * @param t 动作信息
     * @param listener 动作点击事件监听
     */
    default <E> void onActionClickWithConfirm(T t, OnConfirmClickListener<E> listener) {
        onActionClick(t);
    }

    interface OnConfirmClickListener<T> {
        void onActionClick(T t);
    }
}
