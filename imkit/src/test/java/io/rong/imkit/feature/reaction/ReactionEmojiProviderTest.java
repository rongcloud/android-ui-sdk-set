package io.rong.imkit.feature.reaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Test;

/**
 * 回归测试：回应表情的 reactionId 必须是 emoji unicode 字符本身，而非英文短码。
 *
 * <p>历史 bug：默认列表用 "grinning"/"heart" 这类英文短码当 reactionId 发往对端，iOS 直接把短码当字符渲染，显示成英文字母。 修复后 reactionId
 * == unicode，跨端按字符渲染。
 *
 * <p>说明：单测环境下 {@code AndroidEmoji} 未初始化，{@link ReactionEmojiProvider} 走精简兜底列表；以下断言对兜底列表与
 * 派生列表同样成立（两者都满足 reactionId==unicode、无短码、唯一）。
 */
public class ReactionEmojiProviderTest {

    @After
    public void tearDown() {
        // 清除自定义列表，避免用例间相互影响。
        ReactionEmojiProvider.setEmojiList(null);
    }

    @Test
    public void reactionIdEqualsUnicodeForEveryEmoji() {
        List<ReactionEmoji> list = ReactionEmojiProvider.getEmojiList();
        assertFalse("default emoji list should not be empty", list.isEmpty());
        for (ReactionEmoji emoji : list) {
            assertEquals(
                    "reactionId must equal its unicode (cross-end wire identifier)",
                    emoji.getUnicode(),
                    emoji.getReactionId());
        }
    }

    @Test
    public void noReactionIdIsAnAsciiShortcode() {
        // 短码形如 grinning / heart_eyes / thumbsup —— 纯 ASCII 字母+下划线。emoji 字符不会匹配。
        for (ReactionEmoji emoji : ReactionEmojiProvider.getEmojiList()) {
            String id = emoji.getReactionId();
            assertFalse(
                    "reactionId must not be an English shortcode: " + id,
                    id.matches("^[A-Za-z_]+$"));
        }
    }

    @Test
    public void reactionIdsAreUnique() {
        List<ReactionEmoji> list = ReactionEmojiProvider.getEmojiList();
        Set<String> seen = new HashSet<>();
        for (ReactionEmoji emoji : list) {
            assertTrue(
                    "duplicate reactionId would collide on the wire: " + emoji.getReactionId(),
                    seen.add(emoji.getReactionId()));
        }
    }

    @Test
    public void findUnicodeByIdResolvesUnicodeReactionId() {
        // 本端展示：传 unicode reactionId 应原样取回 unicode（命中）。
        assertEquals("😀", ReactionEmojiProvider.findUnicodeById("😀"));
        // 未知 reactionId（如对端自定义 emoji）走兜底，返回原值，仍是可渲染的字符。
        assertEquals("🦄", ReactionEmojiProvider.findUnicodeById("🦄"));
    }

    @Test
    public void customListOverridesDefault() {
        // 业务方自定义列表应优先于派生/兜底列表。
        List<ReactionEmoji> custom = new ArrayList<>();
        custom.add(new ReactionEmoji("🦄", "🦄"));
        ReactionEmojiProvider.setEmojiList(custom);

        List<ReactionEmoji> list = ReactionEmojiProvider.getEmojiList();
        assertEquals(1, list.size());
        assertEquals("🦄", list.get(0).getReactionId());
        assertEquals("🦄", ReactionEmojiProvider.findUnicodeById("🦄"));
    }
}
