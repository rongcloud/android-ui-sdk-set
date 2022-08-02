package io.rong.imkit.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * <p>
 * My GitHub : https://github.com/af913337456/
 * <p>
 * My Blog   : http://www.cnblogs.com/linguanh/
 * <p>
 * second time edited by LinGuanHong on 2017/4/26.
 */

public class CircleProgressView extends View {


    private Paint paintBgCircle;


    private Paint paintCircle;

    private Paint paintProgressCircle;


    private static final float startAngle = -90f;//开始角度

    private float sweepAngle = 0;//结束

    private static final int progressCirclePadding = 3;//进度圆与背景圆的间距


    private boolean fillIn = false;//进度圆是否填充

    private static final int animDuration = 2000;


    private CircleProgressViewAnim mCircleProgressViewAnim;//动画效果


    public CircleProgressView(Context context) {
        super(context);
        init();
    }

    public CircleProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    private void init() {

        mCircleProgressViewAnim = new CircleProgressViewAnim();
        mCircleProgressViewAnim.setDuration(animDuration);
        //progressCirclePadding = dip2px(getContext(), 3);

        paintBgCircle = new Paint();
        paintBgCircle.setAntiAlias(true);
        paintBgCircle.setStyle(Paint.Style.STROKE);
        paintBgCircle.setColor(0xCCFFFFFF);


        paintCircle = new Paint();
        paintCircle.setAntiAlias(true);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setColor(Color.GRAY);


        paintProgressCircle = new Paint();
        paintProgressCircle.setAntiAlias(true);
        paintProgressCircle.setStyle(Paint.Style.FILL);
        paintProgressCircle.setColor(0xCCFFFFFF);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredWidth() / 2, getMeasuredWidth() / 2, paintBgCircle);
        //canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredWidth() / 2, getMeasuredWidth() / 2 - progressCirclePadding / 2, paintCircle);
        RectF f = new RectF(progressCirclePadding, progressCirclePadding, getMeasuredWidth() - progressCirclePadding, getMeasuredWidth() - progressCirclePadding);
        canvas.drawArc(f, startAngle, sweepAngle, true, paintProgressCircle);
        if (!fillIn)
            canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredWidth() / 2, getMeasuredWidth() / 2 - progressCirclePadding * 2, paintCircle);


    }


    public void startAnimAutomatic(boolean fillIn) {
        this.fillIn = fillIn;
        if (mCircleProgressViewAnim != null)
            clearAnimation();
        startAnimation(mCircleProgressViewAnim);
    }

    public void stopAnimAutomatic() {
        if (mCircleProgressViewAnim != null)
            clearAnimation();
    }


    public void setProgress(int progress, boolean fillIn) {
        this.fillIn = fillIn;
        sweepAngle = (float) (360 / 100.0 * progress);
        invalidate();
    }


    private class CircleProgressViewAnim extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            if (interpolatedTime < 1.0f) {
                sweepAngle = 360 * interpolatedTime;
                invalidate();
            } else {
                startAnimAutomatic(fillIn);
            }

        }
    }
}