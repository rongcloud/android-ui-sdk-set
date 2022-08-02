package io.rong.imkit.feature.recallEdit;

import io.rong.common.CountDownTimer;

/**
 * 用于撤回消息，保持重新编辑状态倒计时的类
 */
public class RecallEditCountDownTimer {
    private static final int COUNTDOWN_INTERVAL = 1000;
    private CountDownTimer mCountDownTimer;
    private String mMessageId;
    private RecallEditCountDownTimerListener mListener;

    public RecallEditCountDownTimer(String messageId, RecallEditCountDownTimerListener listener, long millisInFuture) {
        mMessageId = messageId;
        mListener = listener;
        mCountDownTimer = new CountDownTimer(millisInFuture, COUNTDOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mListener != null) {
                    mListener.onTick(Math.round(millisUntilFinished / 1000f), mMessageId);
                }
            }

            @Override
            public void onFinish() {
                if (mListener != null) {
                    mListener.onFinish(mMessageId);
                }
            }
        };
    }

    public void start() {
        if (mCountDownTimer != null && !mCountDownTimer.isStart()) {
            mCountDownTimer.start();
        }
    }

    public void cancel() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    public void setListener(RecallEditCountDownTimerListener listener) {
        this.mListener = listener;
    }
}
