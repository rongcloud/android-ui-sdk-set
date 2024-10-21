package io.rong.imkit.usermanage.interfaces;

import io.rong.imlib.IRongCoreEnum;

/**
 * 数据监听 Listener
 *
 * @author rongcloud
 * @since 5.10.4
 */
public interface OnDataChangeListener<T> {

    /**
     * 回调执行结果
     *
     * @param t 回调执行结果的数据
     */
    void onDataChange(T t);

    /**
     * 回调执行错误
     *
     * @param coreErrorCode 错误
     * @param errorMsg 错误信息
     */
    default void onDataError(IRongCoreEnum.CoreErrorCode coreErrorCode, String errorMsg) {}
}
