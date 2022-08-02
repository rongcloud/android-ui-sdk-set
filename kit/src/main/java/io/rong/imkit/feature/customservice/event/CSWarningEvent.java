package io.rong.imkit.feature.customservice.event;

import android.view.View;

import io.rong.imkit.event.uievent.PageEvent;

public class CSWarningEvent implements PageEvent {
    public String mCSMessage;
    public View.OnClickListener mClickListener;

    /**
     * 客服提示事件
     * @param mCSMessage 提示的内容
     * @param mClickListener 页面处理该事件时的点击回调
     */
    public CSWarningEvent(String mCSMessage, View.OnClickListener mClickListener) {
        this.mCSMessage = mCSMessage;
        this.mClickListener = mClickListener;
    }
}
