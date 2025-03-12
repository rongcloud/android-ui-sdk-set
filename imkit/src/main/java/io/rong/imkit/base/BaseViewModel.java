package io.rong.imkit.base;

import android.os.Bundle;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import io.rong.imkit.event.uievent.ErrorEvent;
import io.rong.imkit.usermanage.interfaces.OnDataChangeListener;
import io.rong.imlib.IRongCoreEnum;

/**
 * ViewModel 基类，提供错误事件的 LiveData。
 *
 * @author rongcloud
 * @since 5.10.4
 */
public class BaseViewModel extends ViewModel {

    private final Bundle mArguments;

    public BaseViewModel(Bundle bundle) {
        this.mArguments = bundle;
    }

    protected final Bundle getArguments() {
        return this.mArguments;
    }

    protected final MutableLiveData<ErrorEvent<IRongCoreEnum.CoreErrorCode>> errorEventLiveData =
            new MutableLiveData<>();

    /**
     * 获取错误事件的 LiveData，用于观察错误状态。
     *
     * @return MutableLiveData<FailureResult < ?>>
     */
    public MutableLiveData<ErrorEvent<IRongCoreEnum.CoreErrorCode>> getErrorEventLiveData() {
        return errorEventLiveData;
    }

    /**
     * 发送页面级别事件
     *
     * @param errorEvent 页面级别事件
     */
    protected void postErrorEvent(ErrorEvent<IRongCoreEnum.CoreErrorCode> errorEvent) {
        errorEventLiveData.postValue(errorEvent);
    }

    /**
     * 安全的数据处理器，处理数据变化和错误
     *
     * @param <T> 数据类型
     */
    protected abstract class SafeDataHandler<T> implements OnDataChangeListener<T> {

        @Override
        public void onDataError(IRongCoreEnum.CoreErrorCode coreErrorCode, String errorMsg) {
            postErrorEvent(ErrorEvent.obtain(coreErrorCode, errorMsg));
        }
    }
}
