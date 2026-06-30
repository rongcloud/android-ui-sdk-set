package io.rong.imkit.feature.reaction;

import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 回应表情数据提供者。
 *
 * <p>默认回应表情库与输入框表情面板（{@link AndroidEmoji}）共用同一份数据，保证数量与排序一致：回应默认表情 直接派生自 {@code rc_emoji.xml} 定义的
 * emoji，按相同顺序排列。当 {@link AndroidEmoji} 尚未初始化时，回退到一份精简内置 列表兜底（保证选择器不为空、单测可独立运行）。业务方仍可通过 {@link
 * #setEmojiList(List)} 完全自定义。
 *
 * @since 5.42.0
 */
public class ReactionEmojiProvider {

    /** 业务方自定义的回应表情库；非 null 时优先于派生列表。 */
    private static List<ReactionEmoji> sCustomList;

    /** 从 {@link AndroidEmoji} 派生的缓存列表；仅在 AndroidEmoji 就绪后才会被填充并缓存。 */
    private static List<ReactionEmoji> sDerivedList;

    public static List<ReactionEmoji> getEmojiList() {
        if (sCustomList != null) {
            return Collections.unmodifiableList(sCustomList);
        }
        if (sDerivedList != null) {
            return Collections.unmodifiableList(sDerivedList);
        }
        List<ReactionEmoji> derived = buildFromInputEmoji();
        if (!derived.isEmpty()) {
            // AndroidEmoji 已就绪，缓存派生结果，与输入框表情保持一致。
            sDerivedList = derived;
            return Collections.unmodifiableList(sDerivedList);
        }
        // AndroidEmoji 尚未初始化：返回兜底列表但不缓存，待其就绪后下次重建。
        return Collections.unmodifiableList(createFallbackList());
    }

    /**
     * 自定义回应表情库。
     *
     * <p><b>跨端约束</b>：每个 {@link ReactionEmoji} 的 {@code reactionId} 必须填 emoji 的 unicode 字符（如 {@code
     * "😀"}），不能用私有英文短码。{@code reactionId} 是跨端（Android/iOS/Web）传输的回应标识，对端会直接按字符渲染；
     * 若填短码，对端会显示成英文字母。对于带变体选择符的符号（如 {@code "❤️"}），需与 {@code AndroidEmoji.replaceEmojiMap}
     * 保持一致的全限定（含 {@code \\uFE0F}）写法。
     */
    public static void setEmojiList(List<ReactionEmoji> list) {
        sCustomList = list == null ? null : new ArrayList<>(list);
    }

    public static String findUnicodeById(String reactionId) {
        for (ReactionEmoji emoji : getEmojiList()) {
            if (emoji.getReactionId().equals(reactionId)) {
                return emoji.getUnicode();
            }
        }
        return reactionId;
    }

    /**
     * 从输入框表情（{@link AndroidEmoji}）派生回应表情库：数量、顺序与输入框完全一致。 去重保序，每个 emoji 的 {@code reactionId} 即其
     * unicode 字符本身（跨端按字符渲染）。
     */
    private static List<ReactionEmoji> buildFromInputEmoji() {
        List<ReactionEmoji> list = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String unicode : AndroidEmoji.getEmojiUnicodeList()) {
            if (unicode != null && !unicode.isEmpty() && seen.add(unicode)) {
                list.add(new ReactionEmoji(unicode, unicode));
            }
        }
        return list;
    }

    /**
     * 兜底表情列表：仅在 {@link AndroidEmoji} 未初始化时使用，保证回应选择器不为空。
     *
     * <p>reactionId 与 unicode 一致；带变体选择符的符号（❤️/☺️/☹️）沿用与 {@code AndroidEmoji.replaceEmojiMap}
     * 一致的全限定（含 {@code \\uFE0F}）写法。
     */
    private static List<ReactionEmoji> createFallbackList() {
        List<ReactionEmoji> list = new ArrayList<>();
        add(list, "😀");
        add(list, "😂");
        add(list, "❤️");
        add(list, "👍");
        add(list, "👎");
        add(list, "😢");
        add(list, "😱");
        add(list, "😍");
        add(list, "😡");
        add(list, "🙏");
        add(list, "🎉");
        add(list, "👌");
        add(list, "🔥");
        add(list, "👏");
        add(list, "😊");
        add(list, "☺️");
        add(list, "😉");
        add(list, "😘");
        return list;
    }

    private static void add(List<ReactionEmoji> list, String unicode) {
        list.add(new ReactionEmoji(unicode, unicode));
    }
}
