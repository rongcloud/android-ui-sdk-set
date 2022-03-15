package io.rong.imkit.conversation.messgelist.viewmodel;

import io.rong.imkit.model.UiMessage;

/** /~chinese 会话列表处理接口，当用户需要实现自定义 */

/** /~english Conversation list processing interface, when users shall implement custom */
public interface IMessageViewModelProcessor {
    boolean onViewClick(MessageViewModel viewModel, int clickType, UiMessage data);

    boolean onViewLongClick(MessageViewModel viewModel, int clickType, UiMessage data);
}
