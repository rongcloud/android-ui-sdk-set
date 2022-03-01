package io.rong.imkit.utils.keyboard;

/** The observer that will be notified when the height of the keyboard has changed */
public interface KeyboardHeightObserver {

    void onKeyboardHeightChanged(int orientation, boolean isOpen, int keyboardHeight);
}
