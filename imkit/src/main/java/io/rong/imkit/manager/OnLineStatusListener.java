package io.rong.imkit.manager;

import io.rong.imlib.model.SubscribeUserOnlineStatus;
import java.util.Map;

/**
 * 在线状态变更监听接口
 *
 * @since 5.32.0
 */
public interface OnLineStatusListener {
    /**
     * 状态变更方法
     *
     * @param list 变更列表
     */
    void onOnlineStatusUpdate(Map<String, SubscribeUserOnlineStatus> list);
}
