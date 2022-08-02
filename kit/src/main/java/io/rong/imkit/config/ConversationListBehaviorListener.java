package io.rong.imkit.config;

import android.content.Context;
import android.view.View;

import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imlib.model.Conversation;

/**
 * 会话列表界面操作的监听器。
 */
public interface ConversationListBehaviorListener {

    /**
     * 当点击会话头像后执行。
     *
     * @param context          上下文。
     * @param conversationType 会话类型。
     * @param targetId         被点击的用户id。
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */
    boolean onConversationPortraitClick(Context context, Conversation.ConversationType conversationType, String targetId);

    /**
     * 当长按会话头像后执行。
     *
     * @param context          上下文。
     * @param conversationType 会话类型。
     * @param targetId         被点击的用户id。
     * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
     */
    boolean onConversationPortraitLongClick(Context context, Conversation.ConversationType conversationType, String targetId);

    /**
     * 长按会话列表中的 item 时执行。
     *
     * @param context      上下文。
     * @param view         触发点击的 View。
     * @param conversation 长按时的会话条目。
     * @return 如果用户自己处理了长按会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
     */
    boolean onConversationLongClick(Context context, View view, BaseUiConversation conversation);

    /**
     * 点击会话列表中的 item 时执行。
     *
     * @param context      上下文。
     * @param view         触发点击的 View。
     * @param conversation 会话条目。
     * @return 如果用户自己处理了点击会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
     */
    boolean onConversationClick(Context context, View view, BaseUiConversation conversation);
}
