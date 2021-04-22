package io.rong.imkit.model;

public interface ResultCallback<T> {
    void onSuccess(T t);
    void onError(RErrorCode errorCode);
}
