package io.rong.imkit.conversation.messgelist.status;

import android.os.Bundle;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;

/** 处理，消息列表，历史消息展示条，新消息展示条相关逻辑 */
public interface IMessageState {
    /** 处理历史消息 */
    /**
     * 处理历史消息
     *
     * @param viewModel {@link MessageViewModel}
     * @param bundle Bundle
     */
    void init(MessageViewModel viewModel, Bundle bundle);

    /**
     * 处理上拉加载更多
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onLoadMore(MessageViewModel viewModel);

    /**
     * 处理下拉刷新
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onRefresh(MessageViewModel viewModel);

    /**
     * 收到消息回调
     *
     * @param viewModel {@link MessageViewModel}
     * @param message 消息
     * @param left 剩余未拉取消息数目
     * @param hasPackage 标识是否还有剩余的消息包
     * @param offline 是否离线
     */
    void onReceived(
            MessageViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline);

    /**
     * 新消息按钮点击
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onNewMessageBarClick(MessageViewModel viewModel);

    /**
     * 新的 {@code @} 消息点击
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onNewMentionMessageBarClick(MessageViewModel viewModel);

    /**
     * 滑动到底部
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onScrollToBottom(MessageViewModel viewModel);

    /**
     * 历史消息 bar 点击事件
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onHistoryBarClick(MessageViewModel viewModel);

    /**
     * 清除消息
     *
     * @param viewModel {@link MessageViewModel}
     */
    void onClearMessage(MessageViewModel viewModel);

    /**
     * 是否是正常会话状态
     *
     * @param viewModel {@link MessageViewModel}
     * @return 是否是正常会话状态
     */
    boolean isNormalState(MessageViewModel viewModel);
}
