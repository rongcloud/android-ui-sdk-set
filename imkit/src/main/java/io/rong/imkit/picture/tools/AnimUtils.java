package io.rong.imkit.picture.tools;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class AnimUtils {
    private static final int DURATION = 450;

    public static void zoom(View view, boolean isZoomAnim) {
        if (isZoomAnim) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.12f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.12f));
            set.setDuration(DURATION);
            set.start();
        }
    }

    public static void disZoom(View view, boolean isZoomAnim) {
        if (isZoomAnim) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(
                    ObjectAnimator.ofFloat(view, "scaleX", 1.12f, 1f),
                    ObjectAnimator.ofFloat(view, "scaleY", 1.12f, 1f));
            set.setDuration(DURATION);
            set.start();
        }
    }

    /**
     * 箭头旋转动画
     *
     * @param arrow ImageView
     * @param flag 是否向上
     */
    public static void rotateArrow(ImageView arrow, boolean flag) {
        float pivotX = arrow.getWidth() / 2f;
        float pivotY = arrow.getHeight() / 2f;
        // flag为true则向上
        float fromDegrees = 180f;
        float toDegrees = flag ? 360f : 0f;
        // 旋转动画效果   参数值 旋转的开始角度  旋转的结束角度  pivotX x轴伸缩值
        RotateAnimation animation = new RotateAnimation(fromDegrees, toDegrees, pivotX, pivotY);
        // 该方法用于设置动画的持续时间，以毫秒为单位
        animation.setDuration(350);
        // 启动动画
        arrow.startAnimation(animation);
    }
}
