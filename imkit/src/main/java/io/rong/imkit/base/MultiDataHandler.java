package io.rong.imkit.base;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreEnum;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 业务逻辑处理-包含数据回调
 *
 * @author rongcloud
 * @since 5.10.4
 */
public abstract class MultiDataHandler extends BaseHandler {

    private final Map<DataKey<?>, List<OnDataChangeListener<?>>> listenersMap =
            new ConcurrentHashMap<>();

    @Override
    public void stop() {
        super.stop();
        this.listenersMap.clear();
    }

    /**
     * 添加数据变化监听器
     *
     * @param dataKey 包含key和对应Class类型的对象
     * @param onDataChangeListener 数据变化监听器
     * @param <T> 数据类型
     */
    public final <T> void addDataChangeListener(
            @NonNull DataKey<T> dataKey, @NonNull OnDataChangeListener<T> onDataChangeListener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            listenersMap
                    .computeIfAbsent(dataKey, k -> new CopyOnWriteArrayList<>())
                    .add(onDataChangeListener);
        } else {
            List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
            if (listeners == null) {
                listeners = new CopyOnWriteArrayList<>();
                listenersMap.put(dataKey, listeners);
            }
            listeners.add(onDataChangeListener);
        }
    }

    /**
     * 替换数据变化监听器
     *
     * @param dataKey 包含key和对应Class类型的对象
     * @param onDataChangeListener 数据变化监听器
     * @param <T> 数据类型
     */
    public final <T> void replaceDataChangeListener(
            @NonNull DataKey<T> dataKey, @NonNull OnDataChangeListener<T> onDataChangeListener) {
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            listeners.clear();
        }
        addDataChangeListener(dataKey, onDataChangeListener);
    }

    /**
     * 通知指定类型的数据变化
     *
     * @param dataKey 包含key和对应Class类型的对象
     * @param data 数据
     */
    protected final <T> void notifyDataChange(@NonNull DataKey<T> dataKey, @NonNull T data) {
        if (!isAlive()) {
            return;
        }
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            for (OnDataChangeListener<?> listener : listeners) {
                try {
                    ((OnDataChangeListener<T>) listener).onDataChange(data);
                } catch (ClassCastException e) {
                    Log.e("MultiDataHandler", "notifyDataChange: ", e);
                    throw e;
                }
            }
        }
    }

    /**
     * 通知指定类型的数据错误
     *
     * @param dataKey 包含key和对应Class类型的对象
     * @param coreErrorCode 错误
     */
    protected final <T> void notifyDataError(
            @NonNull DataKey<T> dataKey, @NonNull IRongCoreEnum.CoreErrorCode coreErrorCode) {
        notifyDataError(dataKey, coreErrorCode, "");
    }

    /**
     * 通知指定类型的数据错误
     *
     * @param dataKey 包含key和对应Class类型的对象
     * @param coreErrorCode 错误
     * @param errorMsg 错误信息
     */
    protected final <T> void notifyDataError(
            @NonNull DataKey<T> dataKey,
            @NonNull IRongCoreEnum.CoreErrorCode coreErrorCode,
            @NonNull String errorMsg) {
        if (!isAlive()) {
            return;
        }
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            for (OnDataChangeListener<?> listener : listeners) {
                try {
                    listener.onDataError(coreErrorCode, errorMsg);
                } catch (ClassCastException e) {
                    Log.e("MultiDataHandler", "notifyDataError: ", e);
                    throw e;
                }
            }
        }
    }

    /**
     * 通知指定类型的数据错误
     *
     * @param dataKey 包含key和对应Class类型的对象
     * @param coreErrorCode 错误
     * @param errorMsgs 错误信息
     */
    protected final <T> void notifyDataError(
            @NonNull DataKey<T> dataKey,
            @NonNull IRongCoreEnum.CoreErrorCode coreErrorCode,
            @NonNull List<String> errorMsgs) {
        if (!isAlive()) {
            return;
        }
        List<OnDataChangeListener<?>> listeners = listenersMap.get(dataKey);
        if (listeners != null) {
            for (OnDataChangeListener<?> listener : listeners) {
                try {
                    if (listener instanceof OnDataChangeEnhancedListener) {
                        ((OnDataChangeEnhancedListener) listener)
                                .onDataError(coreErrorCode, errorMsgs);
                    }
                } catch (ClassCastException e) {
                    Log.e("MultiDataHandler", "notifyDataErrors: ", e);
                    throw e;
                }
            }
        }
    }

    // 静态内部类 DataKey，用于封装key和Class类型
    protected static class DataKey<T> {

        private final String key;
        private final Class<T> type;

        public static <T> DataKey<T> obtain(String key, Class<T> type) {
            return new DataKey<>(key, type);
        }

        private DataKey(@NonNull String key, @NonNull Class<T> type) {
            this.key = key;
            this.type = type;
        }

        @NonNull
        public String getKey() {
            return key;
        }

        @NonNull
        public Class<T> getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DataKey<?> that = (DataKey<?>) o;
            return key.equals(that.key) && type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, type);
        }
    }
}
