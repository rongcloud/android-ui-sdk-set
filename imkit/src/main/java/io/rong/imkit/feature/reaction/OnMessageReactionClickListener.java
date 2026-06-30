package io.rong.imkit.feature.reaction;

import io.rong.imkit.model.UiMessage;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.MessageReaction;
import io.rong.imlib.model.MessageReactionUser;

/**
 * 消息回应点击事件监听器。
 *
 * <p>开发者可通过此接口拦截回应相关的点击事件，自定义业务逻辑。
 *
 * @since 5.42.0
 */
public interface OnMessageReactionClickListener {

    /**
     * 点击消息上的回应时回调。
     *
     * @param reaction 被点击的回应。
     * @param message 回应所属的消息。
     * @return true 表示开发者已处理本次点击，IMKit 不再执行默认添加或移除逻辑；false 表示继续执行默认逻辑。
     */
    default boolean onMessageReactionClicked(MessageReaction reaction, UiMessage message) {
        return false;
    }

    /**
     * 点击消息回应详情入口时回调。
     *
     * @param reaction 被点击的回应。
     * @param message 回应所属的消息。
     * @return true 表示开发者已处理本次点击，IMKit 不再展示默认详情页；false 表示继续展示默认详情页。
     */
    default boolean onMessageReactionDetailClicked(MessageReaction reaction, UiMessage message) {
        return false;
    }

    /**
     * 点击消息回应详情页中的用户时回调。
     *
     * @param user 被点击的回应用户。
     * @param message 回应所属的消息。
     * @param reaction 被点击用户所属的回应。
     * @return true 表示开发者已处理本次点击；false 表示继续保持 IMKit 默认无操作。
     */
    default boolean onMessageReactionUserClicked(
            MessageReactionUser user, UiMessage message, MessageReaction reaction) {
        return false;
    }

    /**
     * 消息回应点击触发的添加或移除操作失败时回调。
     *
     * @param errorCode 失败错误码。
     * @param message 操作失败的消息。
     * @param reactionId 操作失败的回应 ID。
     * @return true 表示开发者已处理本次错误，IMKit 不再展示默认错误提示；false 表示继续展示默认错误提示。
     */
    default boolean onMessageReactionClickError(
            IRongCoreEnum.CoreErrorCode errorCode, UiMessage message, String reactionId) {
        return false;
    }
}
