package io.rong.imkit.feature.mention;

import io.rong.imlib.model.Conversation;

public interface IMentionedInputListener {

    /**
     * 当启动@功能，即在rc_config.xml中设置rc_enable_mentioned_message 为true后，该方法用于设置在群组或者讨论组中，输入@时的监听。
     * 如果{@link IMentionedInputListener#onMentionedInput(Conversation.ConversationType, String)}返回true, 则您自己处理显示@成员
     * 的选择界面；如果返回false, 则会显示融云SDK默认@成员选择界面。
     *
     * @param conversationType 会话类型
     * @param targetId         会话 id
     * @return 返回true, 则您自己处理显示 @ 成员的选择界面；如果返回false, 则会显示融云SDK默认@成员选择界面。
     */
    boolean onMentionedInput(Conversation.ConversationType conversationType, String targetId);
}
