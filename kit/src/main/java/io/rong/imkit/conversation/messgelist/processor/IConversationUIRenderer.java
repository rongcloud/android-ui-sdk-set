package io.rong.imkit.conversation.messgelist.processor;

import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imlib.model.Conversation;

/**
 * 会话 UI 渲染器。
 */
public interface IConversationUIRenderer {
    /**
     * 进入会话页面后，初始化各 UI 渲染器。
     * @param fragment  会话 fragment
     * @param extension 会话扩展栏
     * @param conversationType 会话类型
     * @param targetId 会话 id
     */
    void init(ConversationFragment fragment, RongExtension extension, Conversation.ConversationType conversationType, String targetId);

    /**
     * 会话页面渲染事件回调。
     * @param event 渲染事件
     * @return 事件是否被消费。true 事件被本渲染器消费，会话页面不再处理； false 未被消费，会话页面默认处理。
     */
    boolean handlePageEvent(PageEvent event);

    /**
     * 按返回键时的回调
     * @return 事件是否被消费
     */
    boolean onBackPressed();

    /**
     * 退出会话页面时回调。
     */
    void onDestroy();
}
