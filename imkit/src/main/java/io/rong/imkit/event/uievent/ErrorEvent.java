package io.rong.imkit.event.uievent;

import androidx.annotation.NonNull;

/**
 * 页面级别错误事件
 *
 * @param <E> 错误类型
 * @author rongcloud
 */
public class ErrorEvent<E> implements PageEvent {
    private final E error;
    private final String message;

    /**
     * 创建错误事件
     *
     * @param error 错误类型
     * @param message 错误信息
     * @param <E> 错误类型
     * @return 错误事件
     */
    public static <E> ErrorEvent<E> obtain(@NonNull E error, String message) {
        return new ErrorEvent<>(error, message);
    }

    /**
     * 创建错误事件
     *
     * @param error 错误类型
     * @param <E> 错误类型
     * @return 错误事件
     */
    public static <E> ErrorEvent<E> obtain(@NonNull E error) {
        return new ErrorEvent<>(error);
    }

    private ErrorEvent(E error) {
        this.error = error;
        this.message = "";
    }

    private ErrorEvent(E error, String message) {
        this.error = error;
        this.message = message;
    }

    /**
     * 获取错误类型
     *
     * @return 错误类型
     */
    public E getError() {
        return error;
    }

    /**
     * 获取错误信息
     *
     * @return 错误信息
     */
    public String getMessage() {
        return message;
    }
}
