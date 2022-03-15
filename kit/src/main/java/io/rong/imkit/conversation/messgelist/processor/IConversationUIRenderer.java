package io.rong.imkit.conversation.messgelist.processor;

import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imlib.model.Conversation;

/** /~chinese 会话 UI 渲染器。 */

/** /~english conversation UI renderer. */
public interface IConversationUIRenderer {
    /**
     * /~chinese 进入会话页面后，初始化各 UI 渲染器。
     *
     * @param fragment 会话 fragment
     * @param extension 会话扩展栏
     * @param conversationType 会话类型
     * @param targetId 会话 id
     */

    /**
     * /~english After entering the conversation page, initialize each UI renderer.
     *
     * @param fragment Conversation fragment
     * @param extension Conversation extension bar
     * @param conversationType Conversation type
     * @param targetId Conversation Id
     */
    void init(
            ConversationFragment fragment,
            RongExtension extension,
            Conversation.ConversationType conversationType,
            String targetId);

    /**
     * /~chinese 会话页面渲染事件回调。
     *
     * @param event 渲染事件
     * @return 事件是否被消费。true 事件被本渲染器消费，会话页面不再处理； false 未被消费，会话页面默认处理。
     */

    /**
     * /~english Conversation page rendering event callback.
     *
     * @param event Render event
     * @return Whether the event is consumed. The true event is consumed by this renderer, and the
     *     conversation page is no longer processed; false indicates no consumption and the
     *     conversation page is handled by default.
     */
    boolean handlePageEvent(PageEvent event);

    /**
     * /~chinese 按返回键时的回调
     *
     * @return 事件是否被消费
     */

    /**
     * /~english Callback when you press the return key
     *
     * @return Whether the event is consumed
     */
    boolean onBackPressed();

    /** /~chinese 退出会话页面时回调。 */

    /** /~english Callback when you exit the conversation page. */
    void onDestroy();
}
