package io.rong.imkit.conversation.messgelist.provider;

/**
 * 消息展示配置类，可在 BaseMessageItemProvider 的 子类构造方法中，配置参数
 */
public class MessageItemProviderConfig {
    /**
     * 是否显示头像。
     *
     * @return 是否显示头像。
     */
    public boolean showPortrait = true;

    /**
     * 是否横向居中显示。
     *
     * @return 是否横向居中显示。
     */
    public boolean centerInHorizontal = false;
    /**
     * 是否显示未发送成功警告。
     *
     * @return 是否显示未发生成功警告。
     */
    public boolean showWarning = true;

    /**
     * 是否显示发送进度。
     *
     * @return 是否显示发送进度。
     */
    public boolean showProgress = true;

    /**
     * 会话界面是否在消息上面显示昵称。
     *
     * @return 是否显示
     */
    public boolean showSummaryWithName = true;

    /**
     * 单聊会话中是否在消息旁边显示已读回执状态。
     * 默认不显示
     *
     * @return 是否显示
     */
    public boolean showReadState = false;

    /**
     * @return 是否需要展示气泡
     */
    public boolean showContentBubble = true;
}
