package io.rong.imkit.conversation.extension.component.emoticon;

/** Created by weiqinxiao on 16/8/5. */
public interface IEmojiItemClickListener {
    /**
     * Emoji 点击事件
     *
     * @param emoji Emoji
     */
    void onEmojiClick(String emoji);

    /** Emoji 删除回调 */
    void onDeleteClick();
}
