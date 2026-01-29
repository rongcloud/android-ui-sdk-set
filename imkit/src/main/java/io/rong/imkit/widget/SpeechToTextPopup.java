package io.rong.imkit.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import io.rong.imkit.R;

/** 语音文本操作弹窗 - 提供复制和取消转文字功能 */
public class SpeechToTextPopup extends PopupWindow {

    private Context mContext;
    private View mContentView;
    private View mBackgroundMask;
    private TextView mCopyButton;
    private TextView mCancelButton;
    private OnActionClickListener mListener;

    /** 操作点击监听器 */
    public interface OnActionClickListener {
        /** 复制文本 */
        void onCopyTextClick();

        /** 取消语音转文字 */
        void onCancelSpeechToTextClick();
    }

    public SpeechToTextPopup(Context context) {
        super(context);
        mContext = context;
        initContentView();
        initPopupWindow();
    }

    /** 初始化内容视图 */
    private void initContentView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mContentView = inflater.inflate(R.layout.rc_speech_to_text_popup, null);

        mBackgroundMask = mContentView.findViewById(R.id.rc_speech_to_text_popup_background_mask);
        mCopyButton = mContentView.findViewById(R.id.rc_speech_to_text_popup_copy);
        mCancelButton = mContentView.findViewById(R.id.rc_speech_to_text_popup_cancel);

        // 设置复制按钮点击事件
        mCopyButton.setOnClickListener(
                v -> {
                    if (mListener != null) {
                        mListener.onCopyTextClick();
                    }
                    dismissWithAnimation();
                });

        // 设置取消按钮点击事件
        mCancelButton.setOnClickListener(
                v -> {
                    if (mListener != null) {
                        mListener.onCancelSpeechToTextClick();
                    }
                    dismissWithAnimation();
                });

        // 点击背景遮罩关闭弹窗
        mBackgroundMask.setOnClickListener(v -> dismissWithAnimation());

        setContentView(mContentView);
    }

    /** 初始化弹窗属性 */
    private void initPopupWindow() {
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        setFocusable(true);
        setOutsideTouchable(false); // 禁用外部点击，使用背景遮罩处理
        setBackgroundDrawable(new ColorDrawable(0x00000000)); // 透明背景
        setAnimationStyle(0); // 禁用默认动画，使用自定义动画
    }

    /** 设置操作点击监听器 */
    public void setOnActionClickListener(OnActionClickListener listener) {
        mListener = listener;
    }

    /** 在指定View上方显示弹窗 */
    public void showAboveView(View anchorView) {
        if (anchorView == null) return;

        // 显示弹窗
        showAtLocation(anchorView, Gravity.NO_GRAVITY, 0, 0);

        // 启动进入动画
        playEnterAnimation();
    }

    /** 播放进入动画 */
    private void playEnterAnimation() {
        if (mBackgroundMask == null) return;

        // 背景遮罩渐入动画
        mBackgroundMask.setAlpha(0f);
        ObjectAnimator.ofFloat(mBackgroundMask, "alpha", 0f, 1f).setDuration(200).start();

        // 内容容器缩放动画
        View contentContainer =
                mContentView.findViewById(R.id.rc_speech_to_text_popup_content_container);
        if (contentContainer != null) {
            contentContainer.setScaleX(0.8f);
            contentContainer.setScaleY(0.8f);
            contentContainer.setAlpha(0f);

            ObjectAnimator.ofFloat(contentContainer, "scaleX", 0.8f, 1f).setDuration(200).start();
            ObjectAnimator.ofFloat(contentContainer, "scaleY", 0.8f, 1f).setDuration(200).start();
            ObjectAnimator.ofFloat(contentContainer, "alpha", 0f, 1f).setDuration(200).start();
        }
    }

    /** 带动画的关闭弹窗 */
    private void dismissWithAnimation() {
        if (mBackgroundMask == null) {
            dismiss();
            return;
        }

        // 背景遮罩渐出动画
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mBackgroundMask, "alpha", 1f, 0f);
        fadeOut.setDuration(150);
        fadeOut.addListener(
                new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        dismiss();
                    }
                });

        // 内容容器缩放动画
        View contentContainer =
                mContentView.findViewById(R.id.rc_speech_to_text_popup_content_container);
        if (contentContainer != null) {
            ObjectAnimator.ofFloat(contentContainer, "scaleX", 1f, 0.8f).setDuration(150).start();
            ObjectAnimator.ofFloat(contentContainer, "scaleY", 1f, 0.8f).setDuration(150).start();
            ObjectAnimator.ofFloat(contentContainer, "alpha", 1f, 0f).setDuration(150).start();
        }

        fadeOut.start();
    }

    /** dp转px工具方法 */
    private int dp2px(float dp) {
        float density = mContext.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
