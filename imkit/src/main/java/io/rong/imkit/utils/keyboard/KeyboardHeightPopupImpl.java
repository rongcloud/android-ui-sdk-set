package io.rong.imkit.utils.keyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.utils.AndroidConstant;
import io.rong.imkit.utils.NavigationBarUtil;
import java.lang.ref.WeakReference;

/**
 * @author gusd
 */
public class KeyboardHeightPopupImpl extends PopupWindow implements KeyboardHeightPresenter {
    private static final String TAG = "KeyboardHeightPopupImpl";

    /** The view that is used to calculate the keyboard height */
    private final View popupView;

    /** The parent view */
    private final View parentView;

    /** The root activity that uses this KeyboardHeightProvider */
    private final Activity activity;

    private static final int KEYBOARD_OPEN_THRESHOLD = 100;

    private int mMinKeyboardHeight = 0;

    private int preKeyboardHeight = 0;

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;
    private boolean isStart = false;

    private KeyboardChangeAction mKeyboardChangeAction;

    @SuppressLint("InflateParams")
    public KeyboardHeightPopupImpl(Activity activity) {
        super(activity);
        RLog.d(TAG, "KeyboardHeightPopupImpl: ");
        this.activity = activity;
        LayoutInflater inflater =
                (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this.popupView = inflater.inflate(R.layout.rc_keyboard_popupwindow, null, false);
        setContentView(popupView);

        setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        parentView = activity.findViewById(android.R.id.content);

        setWidth(0);
        setHeight(WindowManager.LayoutParams.MATCH_PARENT);
    }

    private void retryShowPopup(final View parent, final int gravity, final int x, final int y) {
        // FIXME: 2021/10/11 在部分机型上 popupWindow 立刻显示会和键盘重合，500ms 之后重新测量一次
        if (!isStart) {
            return;
        }
        dismiss();
        popupView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
        KeyboardHeightPopupImpl.super.showAtLocation(parent, gravity, x, y);
    }

    @Override
    public void showAtLocation(final View parent, final int gravity, final int x, final int y) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        try {
            super.showAtLocation(parent, gravity, x, y);
        } catch (Exception e) {
        }
        popupView.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        try {
                            retryShowPopup(parent, gravity, x, y);
                        } catch (Exception e) {
                            RLog.e(TAG, e.getMessage());
                            popupView.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            if (activity.isFinishing() || activity.isDestroyed()) {
                                                return;
                                            }
                                            try {
                                                retryShowPopup(parent, gravity, x, y);
                                            } catch (Exception exception) {
                                            }
                                        }
                                    },
                                    1000);
                        }
                    }
                },
                500);
    }

    /**
     * Start the KeyboardHeightProvider, this must be called after the onResume of the Activity.
     * PopupWindows are not allowed to be registered before the onResume has finished of the
     * Activity.
     */
    @Override
    public void start() {
        if (!isShowing()) {
            isStart = true;
            if (mGlobalLayoutListener == null) {
                mGlobalLayoutListener =
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                // 分屏模式下不响应
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
                                        || !activity.isInMultiWindowMode()) {
                                    handleOnGlobalLayout();
                                }
                            }
                        };
            }
            popupView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
            setBackgroundDrawable(new ColorDrawable(0));
            showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0);
        }
    }

    /** Close the keyboard height provider, this provider will not be used anymore. */
    @Override
    public void stop() {
        isStart = false;
        popupView.removeCallbacks(mKeyboardChangeAction);
        // 移除mGlobalLayoutListener，防止内存泄露、减少FD句柄占用
        popupView.getViewTreeObserver().removeOnGlobalLayoutListener(mGlobalLayoutListener);
        dismiss();
    }

    /**
     * Set the keyboard height observer to this provider. The observer will be notified when the
     * keyboard height has changed. For example when the keyboard is opened or closed.
     *
     * @param observer The observer to be added to this provider.
     */
    @Override
    public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
        if (observer != null) {
            mKeyboardChangeAction = new KeyboardHeightPopupImpl.KeyboardChangeAction(observer);
        }
    }

    /**
     * Popup window itself is as big as the window of the Activity. The keyboard can then be
     * calculated by extracting the popup view bottom from the activity window height.
     */
    private void handleOnGlobalLayout() {

        Point screenSize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(screenSize);

        Rect rect = new Rect();
        popupView.getWindowVisibleDisplayFrame(rect);

        // REMIND, you may like to change this using the fullscreen size of the phone
        // and also using the status bar and navigation bar heights of the phone to calculate
        // the keyboard height. But this worked fine on a Nexus.
        int orientation = getScreenOrientation();
        int keyboardHeight = screenSize.y - rect.bottom;

        // 调整键盘高度，排除导航栏的影响
        keyboardHeight = keyboardHeight - getNavigationBarHeight();
        if (keyboardHeight == 0) {
            notifyKeyboardHeightChanged(0, orientation);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            notifyKeyboardHeightChanged(keyboardHeight, orientation);
        } else {
            notifyKeyboardHeightChanged(keyboardHeight, orientation);
        }
    }

    private int getNavigationBarHeight() {
        // Android 15适配的虚拟导航栏, 添加这个判定是为了防止在Android 15以下的手机出现问题(相当于不修改Android 15以下的版本的逻辑)
        if (Build.VERSION.SDK_INT < AndroidConstant.ANDROID_VANILLA_ICE_CREAM) {
            return 0;
        }
        return NavigationBarUtil.getNavigationBarHeight(activity.getBaseContext());
    }

    private int getScreenOrientation() {
        return activity.getResources().getConfiguration().orientation;
    }

    private void notifyKeyboardHeightChanged(int height, int orientation) {
        if (preKeyboardHeight == height) {
            return;
        }
        preKeyboardHeight = height;

        if (height < mMinKeyboardHeight) {
            mMinKeyboardHeight = height;
        }

        popupView.removeCallbacks(mKeyboardChangeAction);
        mKeyboardChangeAction.orientation = orientation;
        mKeyboardChangeAction.isOpen = height >= KEYBOARD_OPEN_THRESHOLD;
        mKeyboardChangeAction.keyboardHeight = height - mMinKeyboardHeight;

        popupView.postDelayed(mKeyboardChangeAction, 100);
    }

    private static class KeyboardChangeAction implements Runnable {
        private final WeakReference<KeyboardHeightObserver> observer;
        public int orientation;
        public boolean isOpen;
        public int keyboardHeight;

        KeyboardChangeAction(KeyboardHeightObserver observer) {
            this.observer = new WeakReference<>(observer);
        }

        @Override
        public void run() {
            KeyboardHeightObserver keyboardHeightObserver = observer.get();
            if (keyboardHeightObserver != null) {
                keyboardHeightObserver.onKeyboardHeightChanged(orientation, isOpen, keyboardHeight);
            }
        }
    }
}
