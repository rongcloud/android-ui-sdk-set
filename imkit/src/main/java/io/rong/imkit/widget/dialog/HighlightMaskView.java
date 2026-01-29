package io.rong.imkit.widget.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

/** 遮罩 View，用于显示高亮区域（AnchorView）和阴影遮罩效果 在高亮区域（AnchorView）不显示遮罩，其他区域显示半透明遮罩 */
class HighlightMaskView extends View {

    private Paint mMaskPaint; // 遮罩画笔
    private Paint mClearPaint; // 清除画笔，用于挖空高亮区域
    private Rect mHighlightRect; // 高亮区域的矩形
    private int mMaskColor = 0x99000000; // 遮罩颜色，默认半透明黑色
    private float mCornerRadius = 0; // 高亮区域的圆角半径

    public HighlightMaskView(Context context) {
        super(context);
        init();
    }

    private void init() {
        // 关闭硬件加速，以便使用 PorterDuffXfermode
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // 初始化遮罩画笔
        mMaskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mMaskPaint.setStyle(Paint.Style.FILL);
        mMaskPaint.setColor(mMaskColor);

        // 初始化清除画笔，用于挖空高亮区域
        mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mClearPaint.setStyle(Paint.Style.FILL);
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    /**
     * 设置高亮区域
     *
     * @param rect 高亮区域的矩形
     */
    public void setHighlightRect(Rect rect) {
        this.mHighlightRect = rect;
        invalidate(); // 重绘
    }

    /**
     * 设置高亮区域的圆角半径
     *
     * @param cornerRadius 圆角半径（px）
     */
    public void setCornerRadius(float cornerRadius) {
        this.mCornerRadius = cornerRadius;
        invalidate();
    }

    /**
     * 设置遮罩颜色
     *
     * @param color 遮罩颜色
     */
    public void setMaskColor(int color) {
        this.mMaskColor = color;
        mMaskPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 绘制全屏遮罩
        canvas.drawRect(0, 0, getWidth(), getHeight(), mMaskPaint);

        // 如果有高亮区域，则挖空该区域
        if (mHighlightRect != null) {
            if (mCornerRadius > 0) {
                // 绘制圆角矩形高亮区域
                RectF rectF = new RectF(mHighlightRect);
                canvas.drawRoundRect(rectF, mCornerRadius, mCornerRadius, mClearPaint);
            } else {
                // 绘制矩形高亮区域
                canvas.drawRect(mHighlightRect, mClearPaint);
            }
        }
    }
}
