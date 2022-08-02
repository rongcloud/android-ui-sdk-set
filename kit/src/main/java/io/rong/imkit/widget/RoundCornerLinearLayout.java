package io.rong.imkit.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class RoundCornerLinearLayout extends LinearLayout {

    private Path path = new Path();
    private RectF rect = new RectF();

    public RoundCornerLinearLayout(@NonNull Context context) {
        super(context);
    }

    public RoundCornerLinearLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        path.reset();
        rect.set(0, 0, w, h);
        float radius = 15;
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        path.close();

    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }
}
