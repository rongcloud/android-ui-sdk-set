package io.rong.imkit.manager;

import java.util.List;

/**
 * 在线状态订阅数据源接口
 *
 * @since 5.32.0
 */
public interface OnlineStatusDataSource {

    /**
     * 返回一个用户列表，该列表按优先级排序的，靠后代表权重低。 注意：
     *
     * <p>1. 当订阅总数超过订阅上限时，会截取前 OnLineStatusManager#DEFAULT_SUBSCRIBE_NUMBER
     * 个用户列表，未在列表中但已订阅的用户会被取消订阅，列表中未订阅的用户会进行订阅。
     *
     * <p>2. 如果想做到未在列表中的用户ID不会在超过订阅上限时被淘汰，那么把它放到列表靠前的位置即可。
     *
     * <p>3. SDK 会缓存上次返回的数据，调用 OnLineStatusManager#clearPriorityUserList 清空缓存。
     *
     * @return 用户ID列表
     */
    List<String> onPriorityUserList();
}
