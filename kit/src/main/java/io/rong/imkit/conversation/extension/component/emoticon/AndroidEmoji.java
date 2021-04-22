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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AndroidEmoji {
    private static float density;
    private static Context mContext;
    private static final int MAX_DISPLAY_EMOJI = 600;

    public static void init(Context context) {
        sEmojiMap = new HashMap<>();
        sEmojiList = new ArrayList<>();
        mContext = context.getApplicationContext();

        int[] codes = context.getResources().getIntArray(context.getResources().getIdentifier("rc_emoji_code", "array", context.getPackageName()));
        TypedArray array = context.getResources().obtainTypedArray(context.getResources().getIdentifier("rc_emoji_res", "array", context.getPackageName()));
        TypedArray strArray = context.getResources().obtainTypedArray(context.getResources().getIdentifier("rc_emoji_description", "array", context.getPackageName()));
        if (codes.length != array.length()) {
            throw new RuntimeException("Emoji resource init fail.");
        }

        int i = -1;
        while (++i < codes.length) {
            EmojiInfo emoji = new EmojiInfo(codes[i], array.getResourceId(i, -1), strArray.getResourceId(i, -1));
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
         * A constant indicating that the bottom of this span should be aligned
         * with the bottom of the surrounding text, i.e., at the same level as the
         * lowest descender in the text.
         */
        public static final int ALIGN_BOTTOM = 0;


        /**
         * Your subclass must implement this method to provide the bitmap
         * to be drawn.  The dimensions of the bitmap must be the same
         * from each call to the next.
         */
        public Drawable getDrawable() {
            return mDrawable;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text,
                           int start, int end,
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
        public void draw(Canvas canvas, CharSequence text,
                         int start, int end, float x,
                         int top, int y, int bottom, @NonNull Paint paint) {
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

            if (wr != null)
                d = wr.get();

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
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji characters

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
        if (input == null) {
            return null;
        }

        // extract the single chars that will be operated on
        final char[] chars = input.toCharArray();
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji characters
        final SpannableStringBuilder ssb = new SpannableStringBuilder(input);

        int codePoint;
        boolean isSurrogatePair;
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
                ssb.setSpan(new EmojiImageSpan(codePoint), isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return ssb;
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
//                    codePoint = Character.toCodePoint(chars[length - 2], chars[length - 1]);
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
        // extract the single chars that will be operated on
        final char[] chars = spannable.toString().toCharArray();
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji characters

        int codePoint;
        boolean isSurrogatePair;
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
                spannable.setSpan(new EmojiImageSpan(codePoint, textSize), isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
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
                            resultSpanStr.append(mContext.getResources().getString(emojiInfo.strId));
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

    private static StringBuilder appendSpanStr(boolean isSurrogatePair, StringBuilder resultSpanStr, char[] chars, int index) {
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


}
