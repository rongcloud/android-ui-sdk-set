package io.rong.imkit.utils.keyboard;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import io.rong.common.rlog.RLog;

/** @author gusd */
public class KeyboardHeightFloatImpl
        implements KeyboardHeightPresenter, View.OnLayoutChangeListener {

    private static final String TAG = "KeyboardHeightFloatImpl";
    private final WindowManager windowManager;
    private final View view;
    private final Activity activity;

    private KeyboardHeightObserver keyboardHeightObserver;

    public KeyboardHeightFloatImpl(Activity activity) {
        Log.d(TAG, "KeyboardHeightFloatImpl: ");
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        this.view = new View(activity);
    }

    @Override
    public void start() {
        try {
            ViewParent parent = view.getParent();
            view.addOnLayoutChangeListener(this);
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(view);
                windowManager.addView(view, createLayoutParams());
            }
        } catch (Exception e) {
            RLog.e(TAG, "start" + e.getMessage());
            view.removeOnLayoutChangeListener(this);
        }
    }

    @Override
    public void stop() {
        try {
            view.removeOnLayoutChangeListener(this);
            if (view.isAttachedToWindow()) {
                windowManager.removeViewImmediate(view);
            }
        } catch (Exception e) {
            RLog.e(TAG, "stop" + e.getMessage());
        }
    }

    @Override
    public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
        keyboardHeightObserver = observer;
    }

    private static WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();

        int type;
        if (Build.VERSION.SDK_INT < 24) {
            type = WindowManager.LayoutParams.TYPE_TOAST;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.type = type;
        params.flags =
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        params.format = PixelFormat.TRANSLUCENT;
        params.width = 0;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.START;
        return params;
    }

    @Override
    public void onLayoutChange(
            View v,
            int left,
            int top,
            int right,
            int bottom,
            int oldLeft,
            int oldTop,
            int oldRight,
            int oldBottom) {
        int orientation = getScreenOrientation();
        int oldHeight = oldBottom - oldTop;
        int height = bottom - top;
        if (oldBottom == 0) {
            notifyKeyboardHeightChanged(0, orientation, false);
        } else {
            notifyKeyboardHeightChanged(
                    Math.abs(height - oldHeight), orientation, bottom < oldBottom);
        }
    }

    private int getScreenOrientation() {
        return activity.getResources().getConfiguration().orientation;
    }

    private void notifyKeyboardHeightChanged(int height, int orientation, boolean isOpen) {
        KeyboardHeightObserver keyboardHeightObserver = this.keyboardHeightObserver;
        if (keyboardHeightObserver != null) {
            keyboardHeightObserver.onKeyboardHeightChanged(orientation, isOpen, height);
        }
    }
}
