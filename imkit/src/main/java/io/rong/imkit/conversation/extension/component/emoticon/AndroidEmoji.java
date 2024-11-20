package io.rong.imkit.conversation.extension.component.emoticon;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import io.rong.common.rlog.RLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidEmoji {
    private static final String TAG = "AndroidEmoji";
    private static Map<Integer, EmojiInfo> sEmojiMap;
    private static Map<Integer, String> replaceEmojiMap;
    private static List<EmojiInfo> sEmojiList;

    public static void init(Context context) {
        sEmojiMap = new HashMap<>();
        sEmojiList = new ArrayList<>();
        int[] codes =
                context.getResources()
                        .getIntArray(
                                context.getResources()
                                        .getIdentifier(
                                                "rc_emoji_code",
                                                "array",
                                                context.getPackageName()));
        TypedArray array =
                context.getResources()
                        .obtainTypedArray(
                                context.getResources()
                                        .getIdentifier(
                                                "rc_emoji_res", "array", context.getPackageName()));
        TypedArray strArray =
                context.getResources()
                        .obtainTypedArray(
                                context.getResources()
                                        .getIdentifier(
                                                "rc_emoji_description",
                                                "array",
                                                context.getPackageName()));
        if (codes.length != array.length()) {
            throw new RuntimeException("Emoji resource init fail.");
        }

        int i = -1;
        while (++i < codes.length) {
            EmojiInfo emoji =
                    new EmojiInfo(
                            codes[i], array.getResourceId(i, -1), strArray.getResourceId(i, -1));
            sEmojiMap.put(codes[i], emoji);
            sEmojiList.add(emoji);
        }
        initReplaceEmojiMap();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        array.recycle();
    }

    // iOS端合并转发页面下列emoji异常，需要使用此Map中的emoji字符
    private static void initReplaceEmojiMap() {
        if (replaceEmojiMap == null) {
            replaceEmojiMap = new HashMap<>();
        }
        replaceEmojiMap.put(0x2601, "☁️");
        replaceEmojiMap.put(0x263a, "☺️");
        replaceEmojiMap.put(0x2764, "❤️");
        replaceEmojiMap.put(0x26a1, "⚡️");
        replaceEmojiMap.put(0x2600, "☀️");
        replaceEmojiMap.put(0x2744, "❄️");
        replaceEmojiMap.put(0x2614, "☔️");
        replaceEmojiMap.put(0x270c, "✌️");
        replaceEmojiMap.put(0x261d, "☝️");
        replaceEmojiMap.put(0x2615, "☕️");
        replaceEmojiMap.put(0x270f, "✏️");
    }

    public static List<EmojiInfo> getEmojiList() {
        return sEmojiList;
    }

    public static int getEmojiSize() {
        return sEmojiMap != null ? sEmojiMap.size() : 0;
    }

    public static int getEmojiCode(int index) {
        if (index >= 0 && sEmojiList != null && index < sEmojiList.size()) {
            EmojiInfo info = sEmojiList.get(index);
            return info.code;
        }
        RLog.e(TAG, "getEmojiCode sEmojiList IndexOutOfBounds");
        return 0;
    }

    public static Drawable getEmojiDrawable(Context context, int index) {
        Drawable drawable = null;
        if (index >= 0 && sEmojiList != null && index < sEmojiList.size()) {
            EmojiInfo emoji = sEmojiList.get(index);
            drawable = context.getResources().getDrawable(emoji.resId);
        }
        return drawable;
    }

    private static class EmojiInfo {
        public EmojiInfo(int code, int resId) {
            this.code = code;
            this.resId = resId;
        }

        public EmojiInfo(int code, int resId, int strId) {
            this.code = code;
            this.resId = resId;
            this.strId = strId;
        }

        int code;
        int resId;
        int strId;
    }
}
