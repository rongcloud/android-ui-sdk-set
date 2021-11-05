package io.rong.imkit.utils.keyboard;

import android.app.Activity;

import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;

import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import android.view.WindowManager.LayoutParams;

import android.widget.PopupWindow;

import java.lang.ref.WeakReference;

import io.rong.imkit.R;


/**
 * The keyboard height provider, this class uses a PopupWindow
 * to calculate the window height when the floating keyboard is opened and closed.
 */
public class KeyboardHeightProvider extends PopupWindow {

    private static final String TAG = "KeyboardHeightProvider";

    /**
     * The cached landscape height of the keyboard
     */
    private int keyboardLandscapeHeight;

    /**
     * The cached portrait height of the keyboard
     */
    private int keyboardPortraitHeight;

    /**
     * The view that is used to calculate the keyboard height
     */
    private View popupView;

    /**
     * The parent view
     */
    private View parentView;

    /**
     * The root activity that uses this KeyboardHeightProvider
     */
    private Activity activity;

    private static int KEYBOARD_OPEN_THRESHOLD = 100;

    private int mMinKeyboardHeight = 0;

    private int preKeyboardHeight = 0;
    private KeyboardChangeAction mKeyboardChangeAction;
    private OnGlobalLayoutListener mGlobalLayoutListener;


    /**
     * Construct a new KeyboardHeightProvider
     *
     * @param activity The parent activity
     */
    public KeyboardHeightProvider(Activity activity) {
        super(activity);
        this.activity = activity;

        LayoutInflater inflator = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        this.popupView = inflator.inflate(R.layout.rc_keyboard_popupwindow, null, false);
        setContentView(popupView);

        setSoftInputMode(LayoutParams.SOFT_INPUT_ADJUST_RESIZE | LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        parentView = activity.findViewById(android.R.id.content);

        setWidth(0);
        setHeight(LayoutParams.MATCH_PARENT);
    }


    @Override
    public void showAtLocation(final View parent, final int gravity, final int x, final int y) {
        super.showAtLocation(parent, gravity, x, y);
        popupView.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    retryShowPopup(parent, gravity, x, y);
                } catch (Exception e) {
                    e.printStackTrace();
                    popupView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            retryShowPopup(parent, gravity, x, y);
                        }
                    }, 1000);
                }
            }
        }, 500);
    }

    private void retryShowPopup(final View parent, final int gravity, final int x, final int y) {
        // FIXME: 2021/10/11 在部分机型上 popupWindow 立刻显示会和键盘重合，500ms 之后重新测量一次
        dismiss();
        popupView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
        KeyboardHeightProvider.super.showAtLocation(parent, gravity, x, y);
    }

    /**
     * Start the KeyboardHeightProvider, this must be called after the onResume of the Activity.
     * PopupWindows are not allowed to be registered before the onResume has finished
     * of the Activity.
     */
    public void start() {
        if (!isShowing()) {
            if (mGlobalLayoutListener == null) {
                mGlobalLayoutListener = new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // 分屏模式下不响应
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !activity.isInMultiWindowMode()) {
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

    /**
     * Close the keyboard height provider,
     * this provider will not be used anymore.
     */
    public void stop() {
        dismiss();
    }

    /**
     * Set the keyboard height observer to this provider. The
     * observer will be notified when the keyboard height has changed.
     * For example when the keyboard is opened or closed.
     *
     * @param observer The observer to be added to this provider.
     */
    public void setKeyboardHeightObserver(KeyboardHeightObserver observer) {
        if (observer != null) {
            mKeyboardChangeAction = new KeyboardChangeAction(observer);
        }
    }

    /**
     * Popup window itself is as big as the window of the Activity.
     * The keyboard can then be calculated by extracting the popup view bottom
     * from the activity window height.
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
        if (keyboardHeight == 0) {
            notifyKeyboardHeightChanged(0, orientation);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.keyboardPortraitHeight = keyboardHeight;
            notifyKeyboardHeightChanged(keyboardPortraitHeight, orientation);
        } else {
            this.keyboardLandscapeHeight = keyboardHeight;
            notifyKeyboardHeightChanged(keyboardLandscapeHeight, orientation);
        }
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
        mKeyboardChangeAction.height = height;
        mKeyboardChangeAction.orientation = orientation;
        mKeyboardChangeAction.isOpen = height >= KEYBOARD_OPEN_THRESHOLD;
        mKeyboardChangeAction.keyboardHeight = height - mMinKeyboardHeight;

        popupView.postDelayed(mKeyboardChangeAction, 100);

    }


    private static class KeyboardChangeAction implements Runnable {
        private final WeakReference<KeyboardHeightObserver> observer;
        public int height;
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
                keyboardHeightObserver.onKeyboardHeightChanged(height, orientation, isOpen, keyboardHeight);
            }
        }
    }
}
