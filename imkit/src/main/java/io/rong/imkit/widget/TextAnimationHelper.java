package io.rong.imkit.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 文本展示动画帮助类 - 专门处理RecyclerView中ViewHolder复用的情况 */
public class TextAnimationHelper {

    private static final long DEFAULT_ANIMATION_DURATION = 450;
    private static final float OVERSHOOT_TENSION = 0.3f;

    /** 正在运行的动画集合 */
    private static final Map<String, AnimatorSet> sRunningAnimations = new HashMap<>();

    /** 待执行动画的消息UID集合 */
    private static final Set<String> sAnimationPendingUids = new HashSet<>();

    // ==================== 公共API ====================

    /** 添加需要展示动画的消息UID */
    public static void addPendingAnimation(String messageUid) {
        if (!TextUtils.isEmpty(messageUid)) {
            sAnimationPendingUids.add(messageUid);
        }
    }

    /** 根据消息UID智能启动展示动画 如果是第一次显示则使用动画，否则直接显示完整内容 */
    public static void startWithUidCheck(
            View containerView,
            TextView textView,
            String text,
            String messageUid,
            boolean isLeftToRight) {
        startWithUidCheck(
                containerView,
                textView,
                text,
                messageUid,
                isLeftToRight,
                DEFAULT_ANIMATION_DURATION);
    }

    /** 检查指定消息UID是否需要执行动画 */
    public static boolean needsAnimation(String messageUid) {
        return !TextUtils.isEmpty(messageUid) && sAnimationPendingUids.contains(messageUid);
    }

    /** 检查指定消息UID是否正在执行动画 */
    private static boolean isRunningAnimation(String messageUid) {
        return !TextUtils.isEmpty(messageUid) && sRunningAnimations.containsKey(messageUid);
    }

    /** 立即完成动画，显示完整内容 */
    private static void completeAnimation(
            View containerView, TextView textView, String text, String messageUid) {
        if (containerView == null || textView == null) return;

        stopAnimation(messageUid);

        // 设置最终状态
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        containerView.setVisibility(View.VISIBLE);

        // 恢复布局参数
        ViewGroup.LayoutParams params = containerView.getLayoutParams();
        if (params.height == 0) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            containerView.setLayoutParams(params);
        }

        containerView.setClipBounds(null);
    }

    /** 清除所有缓存 */
    public static void clearAllCache() {
        // 停止所有动画
        for (AnimatorSet animatorSet : sRunningAnimations.values()) {
            animatorSet.cancel();
        }
        sRunningAnimations.clear();
        sAnimationPendingUids.clear();
    }

    // ==================== 核心动画逻辑 ====================

    /** 带时长的智能动画启动方法 */
    private static void startWithUidCheck(
            View containerView,
            TextView textView,
            String text,
            String messageUid,
            boolean isLeftToRight,
            long duration) {
        // 验证输入参数
        if (containerView == null || textView == null || text == null) return;

        // 检查动画是否正在运行
        if (!TextUtils.isEmpty(messageUid) && sRunningAnimations.containsKey(messageUid)) return;

        // 检查是否应该启动动画
        boolean shouldStartAnimation =
                !TextUtils.isEmpty(messageUid) && sAnimationPendingUids.contains(messageUid);

        if (shouldStartAnimation) {
            sAnimationPendingUids.remove(messageUid);
            startExpandAnimation(
                    containerView, textView, text, messageUid, isLeftToRight, duration);
        } else {
            // 直接显示内容（无动画）
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
            containerView.setVisibility(View.VISIBLE);

            // 恢复布局参数
            ViewGroup.LayoutParams params = containerView.getLayoutParams();
            if (params.height == 0) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                containerView.setLayoutParams(params);
            }

            containerView.setClipBounds(null);
        }
    }

    /** 启动展开动画 */
    private static void startExpandAnimation(
            View containerView,
            TextView textView,
            String text,
            String messageUid,
            boolean isLeftToRight,
            long duration) {
        stopAnimation(messageUid);

        // 设置初始状态
        textView.setText(text);
        textView.setVisibility(View.VISIBLE);
        containerView.setVisibility(View.VISIBLE);

        containerView
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                containerView
                                        .getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);

                                int width = containerView.getWidth();
                                int height = containerView.getHeight();

                                if (width <= 0 || height <= 0) {
                                    // 如果尺寸无效，直接显示内容
                                    textView.setText(text);
                                    textView.setVisibility(View.VISIBLE);
                                    containerView.setVisibility(View.VISIBLE);
                                    containerView.setClipBounds(null);
                                    return;
                                }

                                createAndStartAnimation(
                                        containerView,
                                        messageUid,
                                        width,
                                        height,
                                        isLeftToRight,
                                        duration);
                            }
                        });
    }

    /** 创建并启动动画 */
    private static void createAndStartAnimation(
            View containerView,
            String messageUid,
            int finalWidth,
            int finalHeight,
            boolean isLeftToRight,
            long duration) {
        // 保存并设置初始布局参数
        ViewGroup.LayoutParams originalParams = containerView.getLayoutParams();
        int originalHeight = originalParams.height;
        originalParams.height = 0;
        containerView.setLayoutParams(originalParams);

        // 创建展开动画器
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(duration);
        animator.setInterpolator(new OvershootInterpolator(OVERSHOOT_TENSION));

        // 设置动画更新监听器
        animator.addUpdateListener(
                animation -> {
                    float progress = (Float) animation.getAnimatedValue();

                    // 更新容器高度
                    ViewGroup.LayoutParams params = containerView.getLayoutParams();
                    params.height = (int) (finalHeight * progress);
                    containerView.setLayoutParams(params);

                    // 计算剪切区域
                    int currentWidth = (int) (finalWidth * Math.min(1, progress * 1.3));
                    int currentHeight = (int) (finalHeight * progress);

                    Rect clipBounds;
                    if (isLeftToRight) {
                        // 从左上角开始展开
                        clipBounds = new Rect(0, 0, currentWidth, currentHeight);
                    } else {
                        // 从右上角开始展开
                        int leftPos = finalWidth - currentWidth;
                        clipBounds = new Rect(leftPos, 0, finalWidth, currentHeight);
                    }
                    containerView.setClipBounds(clipBounds);
                });

        // 设置动画结束监听器
        animator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        restoreContainerState(containerView, originalHeight, messageUid);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        restoreContainerState(containerView, originalHeight, messageUid);
                    }
                });

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(animator);

        if (!TextUtils.isEmpty(messageUid)) {
            sRunningAnimations.put(messageUid, animatorSet);
        }
        animatorSet.start();
    }

    /** 停止指定消息UID的动画 */
    private static void stopAnimation(String messageUid) {
        if (TextUtils.isEmpty(messageUid)) return;

        AnimatorSet animatorSet = sRunningAnimations.remove(messageUid);
        if (animatorSet != null) {
            animatorSet.cancel();
        }
    }

    /** 恢复容器状态 */
    private static void restoreContainerState(
            View containerView, int originalHeight, String messageUid) {
        ViewGroup.LayoutParams params = containerView.getLayoutParams();
        params.height = originalHeight;
        containerView.setLayoutParams(params);

        containerView.setClipBounds(null);

        // 清理动画运行状态
        if (!TextUtils.isEmpty(messageUid)) {
            sRunningAnimations.remove(messageUid);
        }
    }
}
