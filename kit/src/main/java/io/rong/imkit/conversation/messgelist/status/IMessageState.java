package io.rong.imkit.conversation.messgelist.status;

import android.os.Bundle;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;

/** /~chinese 处理，消息列表，历史消息展示条，新消息展示条相关逻辑 */

/**
 * /~english Processing, message list, historical message display bar, new message display bar
 * related logic
 */
public interface IMessageState {
    IMessageState historyState = new HistoryState();
    IMessageState normalState = new NormalState();

    /** /~chinese 处理历史消息 */

    /** /~english Processing historical messages */
    void init(MessageViewModel viewModel, Bundle bundle);

    /** /~chinese 处理上拉加载更多 */

    /** /~english Handle pull-up loading more */
    void onLoadMore(MessageViewModel viewModel);

    /** /~chinese 处理下拉刷新 */

    /** /~english Handle drop-down refresh */
    void onRefresh(MessageViewModel viewModel);

    void onReceived(
            MessageViewModel viewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline);

    void onNewMessageBarClick(MessageViewModel viewModel);

    void onNewMentionMessageBarClick(MessageViewModel viewModel);

    void onScrollToBottom(MessageViewModel viewModel);

    void onHistoryBarClick(MessageViewModel viewModel);
}
