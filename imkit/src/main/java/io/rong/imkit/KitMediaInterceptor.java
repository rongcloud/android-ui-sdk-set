package io.rong.imkit;

import android.webkit.WebView;
import java.util.Map;

public interface KitMediaInterceptor {

    /**
     * 使用Glide加载图片前的拦截，可用于添加header
     *
     * <p>使用场景：
     *
     * <p>1.大图加载页面（PicturePagerActivity）
     *
     * <p>2.SDK内置默认加载头像类GlideKitImageEngine，
     * 加载loadConversationListPortrait、loadConversationPortrait时会使用该方法
     *
     * <p>注意：该方法回调在主线程，不能做耗时操作；必须做耗时操作切换到子线程执行.
     *
     * <p>注意：调用callback.onComplete(headers)返回header，SDK才会使用该header继续图片加载
     *
     * @param url 图片地址
     * @param headers 准备本次请求前的header
     * @param callback 调用callback.onComplete(headers)返回header
     */
    void onGlidePrepareLoad(
            String url, Map<String, String> headers, Callback<Map<String, String>> callback);

    /**
     * 拦截WebView资源加载
     *
     * <p>使用场景：
     *
     * <p>1.CombineWebViewActivity加载资源时，优先走此接口，如果返回WebResourceResponse则不会去请求资源
     *
     * <p>注意：该方法WebView回调在子线程.
     *
     * @param view WebVIew
     * @param url 图片地址
     * @return 是否拦截此请求，true代表不会去请求资源，false代表会去请求资源
     */
    boolean shouldInterceptRequest(WebView view, String url);

    interface Callback<T> {
        void onComplete(T t);
    }
}
