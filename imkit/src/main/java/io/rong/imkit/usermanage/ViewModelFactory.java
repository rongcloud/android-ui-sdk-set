package io.rong.imkit.usermanage;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.base.BaseViewModel;
import java.lang.reflect.Constructor;

/**
 * 功能描述: ViewModelFactory
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class ViewModelFactory implements ViewModelProvider.Factory {

    @Nullable private final Object[] params;

    public ViewModelFactory(@Nullable Object... params) {
        this.params = params;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (BaseViewModel.class.isAssignableFrom(modelClass)) {
            try {
                Bundle bundle = getParam(Bundle.class, 0);
                Constructor<T> constructor = modelClass.getConstructor(Bundle.class);
                return constructor.newInstance(bundle);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to create ViewModel: " + modelClass.getSimpleName(), e);
            }
        }
        try {
            return modelClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ViewModel, A> T createViewModel(Class<T> modelClass, Class<A> argType) {
        A arg = getParam(argType, 0);
        try {
            Constructor<T> constructor = modelClass.getConstructor(argType);
            return constructor.newInstance(arg);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create ViewModel: " + modelClass.getSimpleName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T getParam(Class<T> type, int index) {
        if (params != null && params.length > index && type.isInstance(params[index])) {
            return (T) params[index];
        }
        return null;
    }
}
