package io.rong.imkit.utils;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.rong.imlib.common.ExecutorFactory;

/**
 * toast 工具类，SDK 内部 toast 通过该类弹出。
 *
 * <p>App 可以通过 {@link #setInterceptor(ToastInterceptor)} 设置拦截器来拦截 SDK toast
 *
 * @author rongcloud
 */
public final class ToastUtils {
    private static ToastInterceptor interceptor;
    private static Toast lastToast;

    /**
     * 弹出 toast，受 {@link #setInterceptor(ToastInterceptor)} 影响
     *
     * @param context context
     * @param text toast 文本
     * @param duration toast 显示时间, {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     * @discussion 在非 UI 线程会切换到 UI 线程弹 toast
     */
    public static void show(@Nullable Context context, @Nullable CharSequence text, int duration) {
        if (context == null || text == null) {
            return;
        }

        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        showOnMainThread(context, text, duration);
                    }
                });
    }

    /**
     * 在 UI 线程运行。UI 线程直接运行，非 UI 线程切换到 UI 线程运行
     *
     * @param runnable runnable
     */
    private static void runOnUiThread(@Nullable Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (ExecutorFactory.isMainThread()) {
            runnable.run();
            return;
        }
        ExecutorFactory.getInstance().getMainHandler().post(runnable);
    }

    /**
     * 在UI 线程弹 toast
     *
     * @param context context
     * @param text toast 文本
     * @param duration toast 显示时间, {@link Toast#LENGTH_SHORT} or {@link Toast#LENGTH_LONG}
     */
    private static void showOnMainThread(
            @NonNull Context context, @NonNull CharSequence text, int duration) {
        // 如果被拦截了，直接返回
        if (interceptor != null && !interceptor.willToast(context, text, duration)) {
            return;
        }

        // 9.0 以上直接用调用即可防止重复的显示的问题，且如果复用 Toast 会出现无法再出弹出对话框问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Toast.makeText(context, text, duration).show();
            return;
        }

        if (lastToast != null) {
            lastToast.setText(text);
        } else {
            lastToast = Toast.makeText(context, text, duration);
        }
        lastToast.show();
    }

    /**
     * 设置 toast 拦截器
     *
     * @param interceptor 拦截器 {@link ToastInterceptor}
     * @discussion 如果想取消拦截，重新设置为 null 即可
     */
    public static void setInterceptor(@Nullable ToastInterceptor interceptor) {
        ToastUtils.interceptor = interceptor;
    }

    /** SDK IMKit toast 拦截器 */
    public interface ToastInterceptor {
        /**
         * 是否拦截
         *
         * @param context context
         * @param text toast 文本
         * @param duration toast 显示时间
         * @return true：不拦截，由 SDK 弹 toast。false：拦截 SDK toast，由 App 自行弹 toast。
         * @discussion 在 UI 线程调用
         */
        boolean willToast(@NonNull Context context, @NonNull CharSequence text, int duration);
    }
}
