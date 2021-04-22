package io.rong.imkit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import io.rong.imkit.R;

public class FileRectangleProgress extends View {

    public static final int PI_RADIUS = 180; // pi弧度对应的角度

    private int mProgress;                   // 进度，取值范围: 0-100
    private int mCorner;                     // 圆角，如果是矩形，取一半的话可以是圆
    private int mStartAngle;                 // 百分比进度的起始值，0-n，其中0度与x轴方向一致
    private int mBackgroundColor;            // 覆盖部分，也就是除进度外部分的颜色

    private int width;
    private int height;
    private PointF mCenter;                   // View的中心
    private PointF mStart;                   // 起始点角度在圆上对应的横坐标
    private float mRadius;                   // View的外切圆的半径
    private RectF mBackground;               // 被裁剪的底层圆角矩形
    private Path mClipArcPath = new Path();  // 要裁剪掉的扇形部分 B
    private Path mClipBgPath = new Path();   // 整个View的背景 A，绘制部分为: A-B
    private RectF mEnclosingRectF;           // 这是整个View的外切圆的外切矩形，忽略padding的话它比View的尺寸大
    private Paint mPaint = new Paint();

    public FileRectangleProgress(Context context) {
        super(context);
    }

    public FileRectangleProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FileRectangleProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setProgress(int progress) {
        mProgress = progress;
        invalidate();
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleProgress);
        mProgress = typedArray.getInt(R.styleable.CircleProgress_circleProgress, 0);
        mCorner = typedArray.getDimensionPixelOffset(R.styleable.CircleProgress_circleCorner, 0);
        mStartAngle = typedArray.getInt(R.styleable.CircleProgress_startAngle, 315);
        mBackgroundColor = typedArray.getColor(R.styleable.CircleProgress_backgroundColor,
                Color.argb(90, 90, 90, 90));
        typedArray.recycle();

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mBackgroundColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        float rw = (width - getPaddingStart() - getPaddingEnd()) / 2f;
        float rh = (height - getPaddingTop() - getPaddingBottom()) / 2f;
        mRadius = (float) Math.sqrt(rw * rw + rh * rh);
        mCenter = new PointF(getPaddingStart() + rw, getPaddingTop() + rh);
        mStart = new PointF((float) (mCenter.x + mRadius * Math.cos(mStartAngle * Math.PI / PI_RADIUS)),
                (float) (mCenter.y + mRadius * Math.sin(mStartAngle * Math.PI / PI_RADIUS)));
        mBackground = new RectF(getPaddingStart(),
                getPaddingTop(),
                width - getPaddingEnd(),
                height - getPaddingBottom());
        mEnclosingRectF = new RectF(mCenter.x - mRadius, mCenter.y - mRadius,
                mCenter.x + mRadius, mCenter.y + mRadius);
        mClipBgPath.reset();
        mClipBgPath.addRoundRect(mBackground, mCorner, mCorner, Path.Direction.CW);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.clipPath(mClipBgPath);
        canvas.clipPath(getSectorClip(360 * mProgress / 100f + mStartAngle), Region.Op.DIFFERENCE);
        canvas.drawRoundRect(mBackground, mCorner, mCorner, mPaint);
        canvas.restore();
    }

    private Path getSectorClip(float sweepAngle) {
        mClipArcPath.reset();
        mClipArcPath.moveTo(mCenter.x, mCenter.y);
        mClipArcPath.lineTo(mStart.x, mStart.y);
        mClipArcPath.lineTo((float) (mCenter.x + mRadius * Math.cos(sweepAngle * Math.PI / PI_RADIUS)),
                (float) (mCenter.y + mRadius * Math.sin(sweepAngle * Math.PI / PI_RADIUS)));
        mClipArcPath.close();
        mClipArcPath.addArc(mEnclosingRectF, mStartAngle, sweepAngle - mStartAngle);
        return mClipArcPath;
    }
}
