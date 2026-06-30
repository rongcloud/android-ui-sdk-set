package io.rong.imkit.feature.reaction;

/**
 * 回应表情数据。
 *
 * @since 5.42.0
 */
public class ReactionEmoji {
    private final String unicode;
    private final String reactionId;

    public ReactionEmoji(String unicode, String reactionId) {
        this.unicode = unicode;
        this.reactionId = reactionId;
    }

    public String getUnicode() {
        return unicode;
    }

    public String getReactionId() {
        return reactionId;
    }
}
