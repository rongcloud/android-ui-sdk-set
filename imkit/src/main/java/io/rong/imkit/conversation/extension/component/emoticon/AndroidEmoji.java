package io.rong.imkit.conversation.extension.component.emoticon;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.util.DisplayMetrics;
import androidx.annotation.NonNull;
import androidx.emoji2.text.EmojiCompat;
import io.rong.common.RLog;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidEmoji {
    private static final String TAG = "AndroidEmoji";
    private static float density;
    private static Context mContext;
    private static final int MAX_DISPLAY_EMOJI = 600;
    private static Boolean useEmoji;

    public static void init(Context context) {
        sEmojiMap = new HashMap<>();
        sEmojiList = new ArrayList<>();
        mContext = context.getApplicationContext();

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

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        density = dm.density;
        array.recycle();
    }

    public static List<EmojiInfo> getEmojiList() {
        return sEmojiList;
    }

    private static Map<Integer, EmojiInfo> sEmojiMap;
    private static List<EmojiInfo> sEmojiList;

    public static class EmojiImageSpan extends ReplacementSpan {
        Drawable mDrawable;

        private EmojiImageSpan(int codePoint) {
            this(codePoint, 0);
        }

        public EmojiImageSpan(int codePoint, float textSize) {
            if (sEmojiMap != null && sEmojiMap.containsKey(codePoint)) {
                EmojiInfo emojiInfo = sEmojiMap.get(codePoint);
                if (emojiInfo == null) {
                    return;
                }
                mDrawable = mContext.getResources().getDrawable(emojiInfo.resId);
                int offset; // 相对 emoji 原图的偏移量
                if (textSize == 0) {
                    offset = -(int) (4 * density);
                } else {
                    // 根据文本尺寸和屏幕密度来推算偏移量，因子 21 为测试调整得到的值
                    offset = (int) (textSize - 21 * density);
                }
                int width = mDrawable.getIntrinsicWidth() + offset;
                int height = mDrawable.getIntrinsicHeight() + offset;
                mDrawable.setBounds(0, 0, width > 0 ? width : 0, height > 0 ? height : 0);
            }
        }

        /**
         * A constant indicating that the bottom of this span should be aligned with the bottom of
         * the surrounding text, i.e., at the same level as the lowest descender in the text.
         */
        public static final int ALIGN_BOTTOM = 0;

        /**
         * Your subclass must implement this method to provide the bitmap to be drawn. The
         * dimensions of the bitmap must be the same from each call to the next.
         */
        public Drawable getDrawable() {
            return mDrawable;
        }

        @Override
        public int getSize(
                @NonNull Paint paint,
                CharSequence text,
                int start,
                int end,
                Paint.FontMetricsInt fm) {
            Drawable d = getCachedDrawable();
            Rect rect = d.getBounds();

            if (fm != null) {
                fm.ascent = -rect.bottom;
                fm.descent = 0;

                fm.top = fm.ascent;
                fm.bottom = 0;
            }

            return rect.right;
        }

        @Override
        public void draw(
                Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                @NonNull Paint paint) {
            Drawable b = getCachedDrawable();
            canvas.save();

            int transY = bottom - b.getBounds().bottom;

            transY -= density;

            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }

        private Drawable getCachedDrawable() {
            WeakReference<Drawable> wr = mDrawableRef;
            Drawable d = null;

            if (wr != null) d = wr.get();

            if (d == null) {
                d = getDrawable();
                mDrawableRef = new WeakReference<>(d);
            }

            return d;
        }

        private WeakReference<Drawable> mDrawableRef;
    }

    public static int getEmojiCount(String input) {
        if (input == null) {
            return 0;
        }

        int count = 0;

        // extract the single chars that will be operated on
        final char[] chars = input.toCharArray();
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji
        // characters

        int codePoint;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
            }

            if (sEmojiMap.containsKey(codePoint)) {
                count++;
            }
        }
        return count;
    }

    public static CharSequence ensure(String input) {
        CharSequence cs;
        if (useEmoji2()) {
            try {
                // 如果EmojiCompat未初始化成功,则使用原Emoji方案处理
                cs = EmojiCompat.get().process(input);
            } catch (Exception e) {
                RLog.i(TAG, "ensure input:" + e.toString());
                cs = input;
            }
        } else {
            cs = input;
        }
        final SpannableStringBuilder ssb = new SpannableStringBuilder(cs);

        int codePoint;
        int start = 0;
        int offset = start;
        int rcEmojiCode = 0;
        while (offset < cs.length()) {
            if (Character.isHighSurrogate(cs.charAt(offset))) {
                // 高代理区不处理
                offset++;
                continue;
            } else if (Character.isLowSurrogate(cs.charAt(offset))) {
                // 当前为低代理区
                if (offset > 0
                        && Character.isSurrogatePair(cs.charAt(offset - 1), cs.charAt(offset))) {
                    codePoint = Character.toCodePoint(cs.charAt(offset - 1), cs.charAt(offset));
                    if (sEmojiMap.containsKey(codePoint) && needReplaceEmoji(cs, offset)) {
                        rcEmojiCode = codePoint;
                        start = offset - 1;
                        // 若当前为字符串结尾，且命中融云表情，则渲染
                        ssb.setSpan(
                                new EmojiImageSpan(rcEmojiCode),
                                start,
                                offset + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                codePoint = cs.charAt(offset);
                // 判断是否为普通表情，且融云内置表情
                if (sEmojiMap.containsKey(codePoint) && needReplaceEmoji(cs, offset)) {
                    ssb.setSpan(
                            new EmojiImageSpan(codePoint),
                            offset,
                            offset + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            offset++;
        }
        return ssb;
    }

    /**
     * 判断是否为 Zero Width Joiner 表情，是则不替换，交由系统处理，否则替换
     *
     * @param cs
     * @param offset
     * @return
     */
    private static boolean needReplaceEmoji(CharSequence cs, int offset) {
        if (offset >= cs.length() - 1) {
            return true;
        }
        if (cs.charAt(offset + 1) == 0x200D) {
            return false;
        }
        return true;
    }

    public static boolean isEmoji(String input) {

        if (input == null) {
            return false;
        }

        final char[] chars = input.toCharArray();

        int codePoint;
        int length = chars.length;

        for (int i = 0; i < length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
            }

            if (sEmojiMap.containsKey(codePoint)) {
                return true;
            }
        }

        //        if (length >= 2) {
        //
        //            if (Character.isLowSurrogate(chars[length - 1])) {
        //
        //                if (Character.isSurrogatePair(chars[length - 2], chars[length - 1])) {
        //                    codePoint = Character.toCodePoint(chars[length - 2], chars[length -
        // 1]);
        //                } else {
        //                    codePoint = (int) chars[length - 1];
        //                }
        //            }
        //
        //            if (sEmojiMap.containsKey(codePoint)) {
        //                return true;
        //            }
        //        }

        return false;
    }

    public static void ensure(Spannable spannable, float textSize) {
        CharSequence cs;
        if (useEmoji2()) {
            try {
                // 如果EmojiCompat未初始化成功,则使用原Emoji方案处理
                cs = EmojiCompat.get().process(spannable);
            } catch (Exception e) {
                RLog.i(TAG, "ensure spannable:" + e.toString());
                cs = spannable;
            }
        } else {
            cs = spannable;
        }

        int codePoint;
        int start = 0;
        int offset = start;
        int rcEmojiCode = 0;
        while (offset < cs.length()) {
            if (Character.isHighSurrogate(cs.charAt(offset))) {
                // 当前为高代理区
                offset++;
                continue;
            } else if (Character.isLowSurrogate(cs.charAt(offset))) {
                // 当前为低代理区
                if (offset > 0
                        && Character.isSurrogatePair(cs.charAt(offset - 1), cs.charAt(offset))) {
                    codePoint = Character.toCodePoint(cs.charAt(offset - 1), cs.charAt(offset));
                    if (sEmojiMap.containsKey(codePoint) && needReplaceEmoji(cs, offset)) {
                        rcEmojiCode = codePoint;
                        start = offset - 1;
                        // 若当前为字符串结尾，且命中融云表情，则渲染
                        spannable.setSpan(
                                new EmojiImageSpan(rcEmojiCode),
                                start,
                                offset + 1,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            } else {
                codePoint = cs.charAt(offset);
                // 判断是否为融云内置表情
                if (sEmojiMap.containsKey(codePoint) && needReplaceEmoji(cs, offset)) {
                    // 若前一个融云表情命中，则使用融云渲染
                    spannable.setSpan(
                            new EmojiImageSpan(codePoint),
                            offset,
                            offset + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            offset++;
        }
    }

    public static void ensure(Spannable spannable) {
        ensure(spannable, 0);
    }

    public static SpannableStringBuilder replaceEmojiWithText(Spannable spannable) {
        if (spannable == null) {
            return null;
        }
        // extract the single chars that will be operated on
        final char[] chars = spannable.toString().toCharArray();

        String resultSpanStr = getReplaceEmojiText(chars, spannable.toString());
        return new SpannableStringBuilder(resultSpanStr);
    }

    public static String replaceEmojiWithText(String input) {
        if (input == null) {
            return null;
        }

        // extract the single chars that will be operated on
        final char[] chars = input.toCharArray();

        return getReplaceEmojiText(chars, input);
    }

    private static String getReplaceEmojiText(final char[] chars, String srcString) {
        int codePoint;
        boolean isSurrogatePair;
        int emojiCount = 0;
        StringBuilder resultSpanStr = new StringBuilder();

        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (sEmojiMap.containsKey(codePoint)) {
                emojiCount++;
                char[] spanchars = srcString.toCharArray();
                if (spanchars != null && spanchars.length > 0) {
                    if (emojiCount > MAX_DISPLAY_EMOJI) {
                        resultSpanStr.append("[");
                        EmojiInfo emojiInfo = sEmojiMap.get(codePoint);
                        if (emojiInfo != null) {
                            resultSpanStr.append(
                                    mContext.getResources().getString(emojiInfo.strId));
                        }
                        resultSpanStr.append("]");
                    } else {
                        resultSpanStr = appendSpanStr(isSurrogatePair, resultSpanStr, chars, i);
                    }
                } else {
                    resultSpanStr = appendSpanStr(isSurrogatePair, resultSpanStr, chars, i);
                }

            } else {
                resultSpanStr = appendSpanStr(isSurrogatePair, resultSpanStr, chars, i);
            }
        }
        return resultSpanStr == null ? null : resultSpanStr.toString();
    }

    private static StringBuilder appendSpanStr(
            boolean isSurrogatePair, StringBuilder resultSpanStr, char[] chars, int index) {
        if (resultSpanStr == null) {
            return null;
        }
        if (isSurrogatePair) {
            if (index - 1 >= 0) {
                resultSpanStr.append(chars[index - 1]);
                resultSpanStr.append(chars[index]);
            }
        } else {
            if (index >= 0) {
                resultSpanStr.append(chars[index]);
            }
        }
        return resultSpanStr;
    }

    public static int getEmojiSize() {
        return sEmojiMap.size();
    }

    public static int getEmojiCode(int index) {
        EmojiInfo info = sEmojiList.get(index);
        return info.code;
    }

    public static Drawable getEmojiDrawable(Context context, int index) {
        Drawable drawable = null;
        if (index >= 0 && index < sEmojiList.size()) {
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

    public static boolean useEmoji2() {
        if (useEmoji != null) {
            return useEmoji;
        }
        try {
            Class<?> aClass = Class.forName("androidx.emoji2.text.EmojiCompat");
            useEmoji = true;
        } catch (ClassNotFoundException e) {
            useEmoji = false;
        }
        return useEmoji;
    }
}
