package io.rong.imkit.widget;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

public class LinkTextViewMovementMethod extends LinkMovementMethod {

    private long mLastActionDownTime;
    private ILinkClickListener mListener;

    public LinkTextViewMovementMethod(ILinkClickListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            ClickableSpan[] links = buffer.getSpans(0, buffer.length(), ClickableSpan.class);
            if (links.length == 0) {
                return Touch.onTouchEvent(widget, buffer, event);
            }

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off;
            try {
                off = layout.getOffsetForHorizontal(line, x);
            } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
                // This happens for bidi text on Android 7-8.
                // See
                // https://android.googlesource.com/platform/frameworks/base/+/821e9bd5cc2be4b3210cb0226e40ba0f42b51aed
                return true;
            }

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    long actionUpTime = System.currentTimeMillis();
                    if (actionUpTime - mLastActionDownTime
                            > ViewConfiguration.getLongPressTimeout()) {
                        return true;
                    }
                    String url = null;
                    if (link[0] instanceof URLSpan) url = ((URLSpan) link[0]).getURL();
                    if (mListener != null && mListener.onLinkClick(url)) return true;
                    else link[0].onClick(widget);
                } else {
                    mLastActionDownTime = System.currentTimeMillis();
                }
                return true;
            } else {
                Touch.onTouchEvent(widget, buffer, event);
                return false;
            }
        }
        return Touch.onTouchEvent(widget, buffer, event);
    }
}
