package io.rong.imkit.usermanage.interfaces;

import io.rong.imlib.IRongCoreEnum;
import java.util.Arrays;
import java.util.List;

/**
 * 数据监听 Listener
 *
 * @author rongcloud
 * @since 5.12.0
 */
public interface OnDataChangeEnhancedListener<T> extends OnDataChangeListener<T> {

    /**
     * 回调执行错误
     *
     * @param coreErrorCode 错误
     * @param errorMsg 错误信息
     */
    default void onDataError(IRongCoreEnum.CoreErrorCode coreErrorCode, String errorMsg) {
        onDataError(coreErrorCode, Arrays.asList(errorMsg));
    }

    /**
     * 回调执行错误
     *
     * @param coreErrorCode 错误
     * @param errorMsgs 错误信息
     */
    default void onDataError(IRongCoreEnum.CoreErrorCode coreErrorCode, List<String> errorMsgs) {}
}
