package io.rong.imkit.base;

import androidx.annotation.NonNull;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreEnum;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 业务逻辑处理-包含数据回调
 *
 * @author rongcloud
 * @since 5.10.4
 */
public abstract class SingleDataHandler<T> extends BaseHandler {

    private final List<OnDataChangeListener<T>> onDataChangeListeners =
            new CopyOnWriteArrayList<>();

    @Override
    public void stop() {
        super.stop();
        this.onDataChangeListeners.clear();
    }

    /**
     * 设置数据改变 回调
     *
     * @param onDataChangeListener OnDataChangeListener<T>
     */
    public final void addDataChangeListener(@NonNull OnDataChangeListener<T> onDataChangeListener) {
        if (!this.onDataChangeListeners.contains(onDataChangeListener)) {
            this.onDataChangeListeners.add(onDataChangeListener);
        }
    }

    /**
     * 替换数据改变回调
     *
     * @param onDataChangeListener OnDataChangeListener<T>
     */
    public final void replaceDataChangeListener(
            @NonNull OnDataChangeListener<T> onDataChangeListener) {
        this.onDataChangeListeners.clear();
        addDataChangeListener(onDataChangeListener);
    }

    /**
     * 通知刷新
     *
     * @param t 数据Data
     */
    protected final void notifyDataChange(@NonNull T t) {
        if (!isAlive()) {
            return;
        }
        for (OnDataChangeListener<T> listener : onDataChangeListeners) {
            listener.onDataChange(t);
        }
    }

    /**
     * 通知错误
     *
     * @param coreErrorCode 错误
     */
    protected final void notifyDataError(@NonNull IRongCoreEnum.CoreErrorCode coreErrorCode) {
        notifyDataError(coreErrorCode, "");
    }

    /**
     * 通知错误
     *
     * @param coreErrorCode 错误
     * @param errorMsg 错误信息
     */
    protected final void notifyDataError(
            @NonNull IRongCoreEnum.CoreErrorCode coreErrorCode, @NonNull String errorMsg) {
        if (!isAlive()) {
            return;
        }
        for (OnDataChangeListener<T> listener : onDataChangeListeners) {
            listener.onDataError(coreErrorCode, errorMsg);
        }
    }
}
