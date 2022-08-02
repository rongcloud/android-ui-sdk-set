package io.rong.sticker.model;

/**
 * Created by luoyanlong on 2018/08/20.
 */
public class FullResponse<T> extends BaseResponse {

    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
