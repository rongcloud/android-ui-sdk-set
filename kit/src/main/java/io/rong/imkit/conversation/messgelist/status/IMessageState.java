package io.rong.imkit.conversation.messgelist.status;

import android.os.Bundle;

import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.model.Message;

/**
 * 处理，消息列表，历史消息展示条，新消息展示条相关逻辑
 */
public interface IMessageState {
    IMessageState historyState = new HistoryState();
    IMessageState normalState = new NormalState();

    /**
     *  处理历史消息
     *
     */
    void init(MessageViewModel viewModel, Bundle bundle);

    /**
     *  处理上拉加载更多
     */
    void onLoadMore(MessageViewModel viewModel);

    /**
     * 处理下拉刷新
     */
    void onRefresh(MessageViewModel viewModel);

    void onReceived(MessageViewModel viewModel, UiMessage message, int left, boolean hasPackage, boolean offline);

    void onNewMessageBarClick(MessageViewModel viewModel);

    void onNewMentionMessageBarClick(MessageViewModel viewModel);

    void onScrollToBottom(MessageViewModel viewModel);

    void onHistoryBarClick(MessageViewModel viewModel);
}
