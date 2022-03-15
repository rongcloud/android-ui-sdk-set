package io.rong.imkit.conversation.messgelist.provider;

/** /~chinese 消息展示配置类，可在 BaseMessageItemProvider 的 子类构造方法中，配置参数 */

/**
 * /~english The message shows the configuration class, which can be configured in the subclass
 * constructor of BaseMessageItemProvider.
 */
public class MessageItemProviderConfig {
    /**
     * /~chinese 是否显示头像。
     *
     * @return 是否显示头像。
     */

    /** /~english Whether to display the portrait. */
    public boolean showPortrait = true;

    /**
     * /~chinese 是否横向居中显示。
     *
     * @return 是否横向居中显示。
     */

    /** /~english Whether it is displayed horizontally and centered. */
    public boolean centerInHorizontal = false;
    /**
     * /~chinese 是否显示未发送成功警告。
     *
     * @return 是否显示未发生成功警告。
     */

    /** /~english Whether to show that the success warning is not sent. */
    public boolean showWarning = true;

    /**
     * /~chinese 是否显示发送进度。
     *
     * @return 是否显示发送进度。
     */

    /** /~english Whether to display the progress of sending. */
    public boolean showProgress = true;

    /**
     * /~chinese 会话界面是否在消息上面显示昵称。
     *
     * @return 是否显示
     */

    /**
     * /~english Whether the conversation interface displays a nickname on top of the message.
     *
     * @return Whether the conversation interface displays a nickname on top of the message.
     */
    public boolean showSummaryWithName = true;

    /**
     * /~chinese 单聊会话中是否在消息旁边显示已读回执状态。 默认不显示
     *
     * @return 是否显示
     */

    /**
     * /~english Whether the read receipt status is displayed next to the message in a single chat
     * conversation. Do not display by default.
     *
     * @return Whether the read receipt status is displayed next to the message in a single chat
     *     conversation. Do not display by default.
     */
    public boolean showReadState = false;

    /**
     * /~chinese
     *
     * @return 是否需要展示气泡
     */

    /**
     * /~english Whether to show bubbles
     *
     * @return Whether to show bubbles
     */
    public boolean showContentBubble = true;
}
