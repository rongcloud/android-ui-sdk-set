package io.rong.imkit.feature.recallEdit;

public interface RecallEditCountDownTimerListener {
    /**
     * @param untilFinished 剩余时间，单位：秒
     * @param messageId     消息Uid
     */
    void onTick(long untilFinished, String messageId);

    /**
     * @param messageId 消息Uid
     */
    void onFinish(String messageId);
}
