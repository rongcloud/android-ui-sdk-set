package io.rong.imkit.feature.recallEdit;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.rong.imlib.model.Message;

public class RecallEditManager {
    private Map<String, ConcurrentHashMap<Integer, RecallEditCountDownTimer>> timerMap;

    private RecallEditManager() {
        timerMap = new HashMap<>();
    }

    private static class RecallEditManagerHolder {
        private static RecallEditManager instance = new RecallEditManager();
    }

    public static RecallEditManager getInstance() {
        return RecallEditManagerHolder.instance;
    }

    /**
     * 开启倒计时
     *
     * @param message        消息
     * @param millisInFuture 倒计时时间
     * @param callBack       结果回调
     */
    public void startCountDown(final Message message, long millisInFuture, final RecallEditCountDownCallBack callBack) {
        String key = message.getConversationType().getName() + message.getTargetId();
        ConcurrentHashMap<Integer, RecallEditCountDownTimer> recallEditTimerMap = timerMap.get(key);
        if (recallEditTimerMap != null) {
            RecallEditCountDownTimer timer = recallEditTimerMap.get(message.getMessageId());
            if (timer != null) {
                timer.setListener(new RecallEditTimerListener(message, callBack));
                return;
            }
        }
        RecallEditCountDownTimer countDownTimer = new RecallEditCountDownTimer(String.valueOf(message.getMessageId()), new RecallEditTimerListener(message, callBack), millisInFuture);
        if (recallEditTimerMap == null) {
            ConcurrentHashMap<Integer, RecallEditCountDownTimer> timers = new ConcurrentHashMap<>();
            timers.put(message.getMessageId(), countDownTimer);
            timerMap.put(key, timers);
        } else {
            recallEditTimerMap.put(message.getMessageId(), countDownTimer);
        }
        countDownTimer.start();
    }

    /**
     * 取消会话中所有的倒计时
     *
     * @param key 由会话类型加上目标 id 组合而成，ConversationType.getName()+mTargetId
     */
    public void cancelCountDownInConversation(String key) {
        ConcurrentHashMap<Integer, RecallEditCountDownTimer> timers = timerMap.get(key);
        if (timers != null && timers.size() > 0) {
            Set<Map.Entry<Integer, RecallEditCountDownTimer>> entrySet = timers.entrySet();
            for (Map.Entry<Integer, RecallEditCountDownTimer> entry : entrySet) {
                RecallEditCountDownTimer timer = entry.getValue();
                if (timer != null) {
                    timer.cancel();
                }
            }
            timerMap.remove(key);
        }
    }

    /**
     * 清除指定消息 id 对应的倒计时
     *
     * @param messageId 消息 id
     */
    public void cancelCountDown(String messageId) {
        Set<Map.Entry<String, ConcurrentHashMap<Integer, RecallEditCountDownTimer>>> timerEntrySet = timerMap.entrySet();
        for (Map.Entry<String, ConcurrentHashMap<Integer, RecallEditCountDownTimer>> timerEntry : timerEntrySet) {
            ConcurrentHashMap<Integer, RecallEditCountDownTimer> timers = timerEntry.getValue();
            if (timers != null && timers.size() > 0) {
                RecallEditCountDownTimer timer = timers.get(Integer.valueOf(messageId));
                if (timer != null) {
                    timer.cancel();
                    timers.remove(Integer.valueOf(messageId));
                }
            }
        }
    }

    private class RecallEditTimerListener implements RecallEditCountDownTimerListener {
        private Message message;
        private RecallEditCountDownCallBack callBack;

        public RecallEditTimerListener(Message message, RecallEditCountDownCallBack callBack) {
            this.message = message;
            this.callBack = callBack;
        }

        @Override
        public void onTick(long untilFinished, String messageId) {

        }

        @Override
        public void onFinish(String messageId) {
            Map<Integer, RecallEditCountDownTimer> value = timerMap.get(message.getConversationType().getName() + messageId);
            if (value != null && value.get(message.getMessageId()) != null) {
                value.remove(message.getMessageId());
            }
            if (callBack != null) {
                callBack.onFinish(messageId);
            }
        }
    }

}
