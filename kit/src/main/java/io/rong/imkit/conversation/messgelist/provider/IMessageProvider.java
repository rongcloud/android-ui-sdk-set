package io.rong.imkit.conversation.messgelist.provider;

import io.rong.imkit.model.UiMessage;
import io.rong.imkit.widget.adapter.IViewProvider;
import io.rong.imlib.model.MessageContent;

public interface IMessageProvider<T extends MessageContent> extends IConversationSummaryProvider<T>, IViewProvider<UiMessage> {
}
