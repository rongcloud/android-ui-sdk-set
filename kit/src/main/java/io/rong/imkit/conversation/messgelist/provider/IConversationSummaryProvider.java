package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import io.rong.imlib.model.MessageContent;

public interface IConversationSummaryProvider<T extends MessageContent> {
    /**
     * /~chinese 是否为本模板处理的消息内容。
     *
     * @param messageContent 待处理的消息内容
     * @return 是否处理。true 代表是本模板需要处理的消息，上层会继续调用模板的 {@link #getSummarySpannable(Context,
     *     MessageContent)} ()} 获取资源。 false 代表不是本模板需要处理的消息。
     */

    /**
     * /~english Whether it is the message content processed by this template
     *
     * @param messageContent Message content to be processed
     * @return Whether to deal with it or not. true represents the message that shall be processed
     *     by this template, and the upper layer will continue to call the template's
     *     getSummarySpannable(Context, MessageContent) ()} to obtain resources. false represents a
     *     message that shall not be processed by this template.
     */
    boolean isSummaryType(MessageContent messageContent);

    /**
     * /~chiense 在会话列表页某条会话最后一条消息为该类型消息时，会话里需要展示的内容。 比如: 图片消息在会话里需要展示为"图片"，那返回对应的字符串资源即可。
     *
     * @param context 上下文
     * @param t 消息内容
     * @return 会话里需要展示的字符串资源
     */

    /**
     * /~english The content that shall be displayed in a conversation when the last message in a
     * conversation is of that type on the conversation list page. E.g. The image message shall be
     * displayed as "image" in the conversation, and the corresponding string resource is returned.
     *
     * @param context Context
     * @param t message content
     * @return The string resources that shall be displayed in the conversation
     */
    Spannable getSummarySpannable(Context context, T t);

    /**
     * /~chinese 是否需要在会话里拼接发送者名称。 此配置只对群组会话有效。
     *
     * @return
     */

    /**
     * /~english Whether the sender name shall be concatenated in the conversation. This
     * configuration is valid only for group conversations.
     *
     * @return
     */
    boolean showSummaryWithName();
}
