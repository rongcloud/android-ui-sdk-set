package io.rong.sight.record;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * 445263848@qq.com.
 */
public class FocusView extends View {
    private int focusView_size;
    private int x;
    private int y;
    private int length;
    private Paint mPaint;

    public FocusView(Context context, int size) {
        super(context);
        focusView_size = size;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF00CC00);
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Paint.Style.STROKE);
    }

    private FocusView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        x = y = (int) (focusView_size / 2.0);
        length = (int) (focusView_size / 2.0) - 2;
        setMeasuredDimension(focusView_size, focusView_size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(x - length, y - length, x + length, y + length, mPaint);
        canvas.drawLine(2, getHeight() / 2, focusView_size / 10, getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() - 2, getHeight() / 2, getWidth() - focusView_size / 10, getHeight() / 2, mPaint);
        canvas.drawLine(getWidth() / 2, 2, getWidth() / 2, focusView_size / 10, mPaint);
        canvas.drawLine(getWidth() / 2, getHeight() - 2, getWidth() / 2, getHeight() - focusView_size / 10, mPaint);
    }
}
