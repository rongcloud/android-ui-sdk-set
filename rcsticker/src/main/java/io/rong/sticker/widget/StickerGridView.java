package io.rong.sticker.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridView;

import io.rong.sticker.businesslogic.StickerSendMessageTask;
import io.rong.sticker.model.Sticker;

/**
 * Created by luoyanlong on 2018/08/20.
 */
public class StickerGridView extends GridView {

    private GestureDetector gestureDetector;
    private boolean isInLongPress;
    private Rect rect = new Rect();
    private StickerGridItemView focusChild;
    private StickerPopupWindow popupWindow;

    public StickerGridView(Context context) {
        super(context);
        init();
    }

    public StickerGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), new GestureTap());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                getParent().requestDisallowInterceptTouchEvent(isInLongPress);
                checkFocusChild(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isInLongPress = false;
                if (focusChild != null) {
                    focusChild.setPressed(false);
                }
                focusChild = null;
                hidePopupWindow();
                break;
        }
        return true;
    }

    private class GestureTap extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            for (int i = 0; i < getChildCount(); i++) {
                StickerGridItemView child = (StickerGridItemView) getChildAt(i);
                child.getHitRect(rect);
                if (rect.contains((int) e.getX(), (int) e.getY())) {
                    sendMessage(child.getSticker());
                    break;
                }
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            isInLongPress = true;
            if (focusChild != null) {
                showPopupWindow(focusChild);
            }
        }
    }

    private void checkFocusChild(MotionEvent ev) {
        for (int i = 0; i < getChildCount(); i++) {
            StickerGridItemView child = (StickerGridItemView) getChildAt(i);
            child.getHitRect(rect);
            if (rect.contains((int) ev.getX(), (int) ev.getY())) {
                if (focusChild != child) {
                    onFocusChildChanged(focusChild, child);
                    focusChild = child;
                }
                break;
            }
        }
    }

    private void onFocusChildChanged(StickerGridItemView focusChild, StickerGridItemView newFocusChild) {
        if (focusChild != null) {
            focusChild.setPressed(false);
        }
        if (newFocusChild != null) {
            newFocusChild.setPressed(true);
        }
        if (isInLongPress) {
            showPopupWindow(newFocusChild);
        }
    }

    private void sendMessage(Sticker sticker) {
        StickerSendMessageTask.sendMessage(sticker);
    }

    private int getChildIndex(View view) {
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i) == view) {
                return i;
            }
        }
        return -1;
    }

    private void showPopupWindow(StickerGridItemView view) {
        hidePopupWindow();
        popupWindow = new StickerPopupWindow(getContext());
        int index = getChildIndex(view);
        int viewColumnIndex = getChildColumnIndex(index);
        StickerPopupWindow.Background bg;
        if (viewColumnIndex == 0) {
            bg = StickerPopupWindow.Background.LEFT;
        } else if (viewColumnIndex < getNumColumns() - 1) {
            bg = StickerPopupWindow.Background.MIDDLE;
        } else {
            bg = StickerPopupWindow.Background.RIGHT;
        }
        popupWindow.show(view, view.getSticker(), bg);
    }

    private void hidePopupWindow() {
        if (popupWindow != null) {
            popupWindow.dismiss();
        }
    }

    private int getChildColumnIndex(int index) {
        return index % getNumColumns();
    }
}
