package io.rong.imkit.config;

import android.content.Context;
import android.view.View;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;

public interface ConversationClickListener {

    /**
     * /~chinese 当点击用户头像后执行。
     *
     * @param context 上下文。
     * @param conversationType 会话类型。
     * @param user 被点击的用户的信息。
     * @param targetId 会话 id
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Execute when the user portrait is clicked.
     *
     * @param context Context
     * @param conversationType -Conversation type
     * @param user Information about the user who is clicked on.
     * @param targetId Conversation Id
     * @return If the user handles the logical processing after clicking, return true, otherwise
     *     return false. False is handled by using the default processing method of Fuxing Cloud..
     */
    boolean onUserPortraitClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo user,
            String targetId);

    /**
     * /~chinese 当长按用户头像后执行。
     *
     * @param context 上下文。
     * @param conversationType 会话类型。
     * @param user 被点击的用户的信息。
     * @param targetId 会话 id
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Execute when the user portrait is held.
     *
     * @param context Context
     * @param conversationType -Conversation type
     * @param user Information about the user who is clicked on.
     * @param targetId Conversation Id
     * @return If the user handles the logical processing after clicking, return true, otherwise
     *     return false. False is handled by using the default processing method of Fuxing Cloud..
     */
    boolean onUserPortraitLongClick(
            Context context,
            Conversation.ConversationType conversationType,
            UserInfo user,
            String targetId);

    /**
     * /~chinese 当点击消息时执行。
     *
     * @param context 上下文。
     * @param view 触发点击的 View。
     * @param message 被点击的消息的实体信息。
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
     */

    /**
     * /~english Executes when the message is clicked.
     *
     * @param context Context
     * @param view The View that triggers the click.
     * @param message The physical information of the message that is clicked.
     * @return If the user handles the logical processing after clicking, return true, otherwise
     *     return false. False is handled by using the default processing method of RongCloud.
     */
    boolean onMessageClick(Context context, View view, Message message);

    /**
     * /~chinese 当长按消息时执行。
     *
     * @param context 上下文。
     * @param view 触发点击的 View。
     * @param message 被长按的消息的实体信息。
     * @return 如果用户自己处理了长按后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Executes when the holdes the message.
     *
     * @param context Context
     * @param view The View that triggers the click.
     * @param message The entity information of the message that has been pressed for a long time.
     * @return If the user handles the logical processing after holding, return true, otherwise
     *     return false. False is handled by using the default processing method of RongCloud.
     */
    boolean onMessageLongClick(Context context, View view, Message message);

    /**
     * /~chinese 当点击链接消息时执行。
     *
     * @param context 上下文。
     * @param link 被点击的链接。
     * @param message 被点击的消息的实体信息
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
     */

    /**
     * /~english Executes when the link message is clicked.
     *
     * @param context Context
     * @param link The link that is clicked
     * @param message Physical information of the message clicked
     * @return If the user handles the logical processing after clicking, return true, otherwise
     *     return false. False is handled by using the default processing method of RongCloud.
     */
    boolean onMessageLinkClick(Context context, String link, Message message);

    /**
     * /~chinese 当点击已读回执状态时执行
     *
     * @param context 上下文。
     * @param message 被点击消息的实体信息。
     * @return 如果用户自己处理了长按后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */

    /**
     * /~english Execute when the read receipt status is clicked
     *
     * @param context Context
     * @param message The physical information of the clicked message.
     * @return If the user handles the logical processing after holding, return true, otherwise
     *     return false. False is handled by using the default processing method of RongCloud.
     */
    boolean onReadReceiptStateClick(Context context, Message message);
}
