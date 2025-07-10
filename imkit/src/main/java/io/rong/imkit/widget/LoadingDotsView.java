package io.rong.imkit.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** 加载中的圆点动画View 显示3个圆点依次向前移动的动画效果 */
public class LoadingDotsView extends View {

    private static final int DOT_COUNT = 3;
    private static final int DOT_RADIUS = 2; // dp
    private static final int DOT_SPACING = 8; // dp
    private static final long ANIMATION_DURATION = 600; // ms
    private static final long ANIMATION_DELAY = 200; // ms between dots

    private Paint mDotPaint;
    private float mDotRadius;
    private float mDotSpacing;
    private float[] mDotAlphas;
    private AnimatorSet mAnimatorSet;
    private boolean mIsAnimating;

    public LoadingDotsView(Context context) {
        this(context, null);
    }

    public LoadingDotsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingDotsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        mDotRadius = DOT_RADIUS * density;
        mDotSpacing = DOT_SPACING * density;

        mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaint.setColor(0xFF666666); // 深灰色
        mDotPaint.setStyle(Paint.Style.FILL);

        mDotAlphas = new float[DOT_COUNT];

        // 初始化状态
        resetDots();
    }

    private void resetDots() {
        for (int i = 0; i < DOT_COUNT; i++) {
            mDotAlphas[i] = 0.3f; // 默认半透明
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = (int) (mDotRadius * 2 * DOT_COUNT + mDotSpacing * (DOT_COUNT - 1));
        int height = (int) (mDotRadius * 2);

        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!mIsAnimating) {
            return;
        }

        float centerY = getHeight() / 2f;
        float startX = mDotRadius;

        for (int i = 0; i < DOT_COUNT; i++) {
            float centerX = startX + i * (mDotRadius * 2 + mDotSpacing);

            // 设置透明度
            int alpha = (int) (255 * mDotAlphas[i]);
            mDotPaint.setAlpha(alpha);

            canvas.drawCircle(centerX, centerY, mDotRadius, mDotPaint);
        }
    }

    /** 开始动画 */
    public void startAnimation() {
        if (mIsAnimating) {
            return;
        }

        mIsAnimating = true;
        startAnimationInternal();
    }

    /** 内部动画开始方法，用于循环播放 */
    private void startAnimationInternal() {
        if (!mIsAnimating) {
            return;
        }

        resetDots();

        mAnimatorSet = new AnimatorSet();
        AnimatorSet.Builder builder = null;

        for (int i = 0; i < DOT_COUNT; i++) {
            AnimatorSet dotAnimator = createDotAnimator(i);

            if (builder == null) {
                builder = mAnimatorSet.play(dotAnimator);
            } else {
                builder.with(dotAnimator);
            }
        }

        mAnimatorSet.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mIsAnimating) {
                            // 循环播放，先重置然后重新创建动画
                            mAnimatorSet = null;
                            startAnimationInternal();
                        }
                    }
                });

        mAnimatorSet.start();
        invalidate();
    }

    /** 停止动画 */
    private void stopAnimation() {
        mIsAnimating = false;
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }
        resetDots();
        invalidate();
    }

    /** 创建单个圆点的动画 */
    private AnimatorSet createDotAnimator(int index) {
        // 透明度动画：0.3 -> 1.0 -> 0.3
        ObjectAnimator alphaAnimator =
                ObjectAnimator.ofFloat(this, "dotAlpha" + index, 0.3f, 1.0f, 0.3f);
        alphaAnimator.setDuration(ANIMATION_DURATION);

        // 设置延迟和插值器
        long delay = index * ANIMATION_DELAY;
        alphaAnimator.setStartDelay(delay);
        alphaAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet dotSet = new AnimatorSet();
        dotSet.play(alphaAnimator);

        return dotSet;
    }

    // 动态属性设置方法（用于ObjectAnimator）
    public void setDotAlpha0(float alpha) {
        mDotAlphas[0] = alpha;
        invalidate();
    }

    public void setDotAlpha1(float alpha) {
        mDotAlphas[1] = alpha;
        invalidate();
    }

    public void setDotAlpha2(float alpha) {
        mDotAlphas[2] = alpha;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility != VISIBLE) {
            stopAnimation();
        }
    }
}
