package io.rong.imkit.config;

import android.content.Context;
import android.view.View;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imlib.model.Conversation;

/** /~chinese 会话列表界面操作的监听器。 */

/** /~english Listeners that operate on the conversation list interface. */
public interface ConversationListBehaviorListener {

    /**
     * /~chinese 当点击会话头像后执行。
     *
     * @param context 上下文。
     * @param conversationType 会话类型。
     * @param targetId 被点击的用户id。
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Execute when the conversation portrait is clicked.
     *
     * @param context Context
     * @param conversationType Conversation type
     * @param targetId Id of the user clicked.
     * @return If the user handles the logical processing after clicking, return true, otherwise
     *     return false. False is handled by using the default processing method of Fuxing Cloud.
     */
    boolean onConversationPortraitClick(
            Context context, Conversation.ConversationType conversationType, String targetId);

    /**
     * /~chinese 当长按会话头像后执行。
     *
     * @param context 上下文。
     * @param conversationType 会话类型。
     * @param targetId 被点击的用户id。
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Execute when the conversation portrait is held.
     *
     * @param context Context
     * @param conversationType Conversation type
     * @param targetId Id of the user clicked.
     * @return If the user handles the logical processing after clicking, return true, otherwise
     *     return false. False is handled by using the default processing method of Fuxing Cloud.
     */
    boolean onConversationPortraitLongClick(
            Context context, Conversation.ConversationType conversationType, String targetId);

    /**
     * /~chinese 长按会话列表中的 item 时执行。
     *
     * @param context 上下文。
     * @param view 触发点击的 View。
     * @param conversation 长按时的会话条目。
     * @return 如果用户自己处理了长按会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Executes when the item in the conversation list is held.
     *
     * @param context Context
     * @param view View that triggers the click.
     * @param conversation Conversation entries for holding.
     * @return If the user handles the logical processing after the hold conversation by himself,
     *     return true, otherwise return false,false to follow the default processing mode of
     *     RongCloud.
     */
    boolean onConversationLongClick(Context context, View view, BaseUiConversation conversation);

    /**
     * /~chinese 点击会话列表中的 item 时执行。
     *
     * @param context 上下文。
     * @param view 触发点击的 View。
     * @param conversation 会话条目。
     * @return 如果用户自己处理了点击会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Execute when the item in the conversation list is clicked.
     *
     * @param context Context
     * @param view The View that triggers the click.
     * @param conversation Conversation entry.
     * @return If the user handles the logical processing after clicking conversation by himself,
     *     return true, otherwise return false. False is handled by using the default processing
     *     method of Rong Cloud.
     */
    boolean onConversationClick(Context context, View view, BaseUiConversation conversation);
}
