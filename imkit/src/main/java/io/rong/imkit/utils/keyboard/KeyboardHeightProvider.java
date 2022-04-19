package io.rong.imkit.utils.keyboard;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import io.rong.common.RLog;
import java.lang.reflect.Method;

/**
 * The keyboard height provider, this class uses a PopupWindow to calculate the window height when
 * the floating keyboard is opened and closed.
 */
public class KeyboardHeightProvider {

    private static final String TAG = "KeyboardHeightProvider";
    private KeyboardHeightPresenter floatKeyboardPresenter = null;
    private final KeyboardHeightPresenter keyboardPresenter;
    private Boolean openStatus = false;
    private int keyboardHeight = 0;
    private final KeyboardHeightObserver internalObserver =
            new KeyboardHeightObserver() {
                @Override
                public void onKeyboardHeightChanged(
                        int orientation, boolean isOpen, int keyboardHeight) {
                    if (observer != null) {
                        if (openStatus == null
                                || isOpen != openStatus
                                || keyboardHeight != KeyboardHeightProvider.this.keyboardHeight) {
                            openStatus = isOpen;
                            KeyboardHeightProvider.this.keyboardHeight = keyboardHeight;
                            observer.onKeyboardHeightChanged(orientation, isOpen, keyboardHeight);
                        }
                    }
                }
            };
    private KeyboardHeightObserver observer;

    /**
     * Construct a new KeyboardHeightProvider
     *
     * @param activity The parent activity
     */
    public KeyboardHeightProvider(Activity activity) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && checkOverLayPermission(activity)) {
            // android 10 以下如果存在悬浮窗，且悬浮窗的 flag 包含 FLAG_ALT_FOCUSABLE_IM 时，通过 PopupWindow
            // 的方式测量键盘高度会失效，
            // 此时采用添加悬浮窗的形式来测量
            this.floatKeyboardPresenter = new KeyboardHeightFloatImpl(activity);
            this.floatKeyboardPresenter.setKeyboardHeightObserver(internalObserver);
        }
        this.keyboardPresenter = new KeyboardHeightPopupImpl(activity);
        this.keyboardPresenter.setKeyboardHeightObserver(internalObserver);
    }

    public void start() {
        this.keyboardPresenter.start();
        if (floatKeyboardPresenter != null) {
            this.floatKeyboardPresenter.start();
        }
    }

    public void stop() {
        this.keyboardPresenter.stop();
        if (floatKeyboardPresenter != null) {
            this.floatKeyboardPresenter.stop();
        }
    }

    public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
        this.observer = observer;
    }

    private boolean checkOverLayPermission(Context context) {
        boolean result = true;
        boolean booleanValue;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Method method = Settings.class.getDeclaredMethod("canDrawOverlays", Context.class);
                if (method == null) {
                    return true;
                }
                booleanValue = (Boolean) method.invoke(null, new Object[] {context});
                RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + booleanValue);
                return booleanValue;
            } catch (NullPointerException e) {

                return true;
            } catch (Exception e) {
                RLog.e(
                        TAG,
                        String.format(
                                "getDeclaredMethod:canDrawOverlays! Error:%s, etype:%s",
                                e.getMessage(), e.getClass().getCanonicalName()));
                return true;
            }
        } else if (Build.BRAND.toLowerCase().contains("xiaomi")) {
            Method method;
            Object systemService = context.getSystemService(Context.APP_OPS_SERVICE);
            try {
                method =
                        Class.forName("android.app.AppOpsManager")
                                .getMethod("checkOp", Integer.TYPE, Integer.TYPE, String.class);
            } catch (NoSuchMethodException e) {
                RLog.e(
                        TAG,
                        String.format(
                                "NoSuchMethodException method:checkOp! Error:%s", e.getMessage()));
                method = null;
            } catch (ClassNotFoundException e) {
                RLog.e(TAG, "canDrawOverlays", e);
                method = null;
            }
            if (method != null) {
                try {
                    Integer tmp =
                            (Integer)
                                    method.invoke(
                                            systemService,
                                            new Object[] {
                                                24,
                                                context.getApplicationInfo().uid,
                                                context.getPackageName()
                                            });
                    result = tmp != null && tmp == 0;
                } catch (Exception e) {
                    RLog.e(
                            TAG,
                            String.format(
                                    "call checkOp failed: %s etype:%s",
                                    e.getMessage(), e.getClass().getCanonicalName()));
                }
            }
            RLog.i(TAG, "isFloatWindowOpAllowed allowed: " + result);
            return result;
        }
        return true;
    }
}
