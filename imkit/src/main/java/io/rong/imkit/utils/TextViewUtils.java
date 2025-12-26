package io.rong.imkit.utils;

import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.widget.TextViewCompat;

/** 处理 textMessage 和 ReferenceMessage 的文本内容 */
public class TextViewUtils {

    /** 字符串校验限制，大于等于 150 字符开线程校验，否则在当前线程校验 */
    private static final int CONTENT_LIMIT_LENGTH = 150;

    /**
     * @param content 替换内容
     * @param callBack 异步请求回调
     * @return 内容的 spannable
     */
    public static SpannableStringBuilder getSpannable(
            String content, final RegularCallBack callBack) {
        return getSpannable(content, true, callBack);
    }

    /**
     * @param content 替换内容
     * @param callBack 异步请求回调
     * @return 内容的 spannable
     */
    public static SpannableStringBuilder getSpannable(
            String content, boolean regular, final RegularCallBack callBack) {
        if (content == null) {
            return new SpannableStringBuilder("");
        }
        // 处理RTL
        String adapterContent = RTLUtils.adapterAitInRTL(content);
        SpannableStringBuilder emojiSpannable = new SpannableStringBuilder(adapterContent);
        if (!regular) {
            return emojiSpannable;
        }
        if (emojiSpannable.length() < CONTENT_LIMIT_LENGTH) {
            regularContent(emojiSpannable);
        } else {
            final SpannableStringBuilder spannableStringBuilder =
                    new SpannableStringBuilder(emojiSpannable);
            ExecutorHelper.getInstance()
                    .compressExecutor()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    regularContent(spannableStringBuilder);
                                    callBack.finish(spannableStringBuilder);
                                }
                            });
        }
        return emojiSpannable;
    }

    /**
     * @param content 替换内容
     * @param callBack 异步请求回调
     * @return 内容的 spannable
     */
    public static SpannableStringBuilder getRichSpannable(
            String content, final RegularCallBack callBack, @ColorInt int foregroundColor) {
        if (content == null) {
            return new SpannableStringBuilder("");
        }
        SpannableStringBuilder emojiSpannable = new SpannableStringBuilder(content);
        emojiSpannable.setSpan(
                new ForegroundColorSpan(foregroundColor),
                0,
                content.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (emojiSpannable.length() < CONTENT_LIMIT_LENGTH) {
            regularContent(emojiSpannable);
        } else {
            ExecutorHelper.getInstance()
                    .compressExecutor()
                    .execute(
                            new Runnable() {
                                @Override
                                public void run() {
                                    regularContent(emojiSpannable);
                                    callBack.finish(emojiSpannable);
                                }
                            });
        }
        return emojiSpannable;
    }

    private static void regularContent(SpannableStringBuilder spannable) {
        Linkify.addLinks(
                spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES | Linkify.PHONE_NUMBERS);

        // 使用自定义的URL识别逻辑，处理所有http和https链接
        addCustomUrlLinks(spannable);

        // 添加自定义手机号码识别逻辑，确保在所有设备上都能识别
        addCustomPhoneLinks(spannable);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            spannable.removeSpan(span);
            span = new URLSpanUnderlineSameColor(span.getURL());
            if (end < start) {
                continue;
            }
            spannable.setSpan(span, start, end, 0);
        }
    }

    /** 添加自定义URL识别逻辑，处理所有http和https链接 */
    private static void addCustomUrlLinks(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        // 匹配http或https开头的URL，包括包含URL编码字符的URL
        String urlPattern = "(https?://[^\\s]+)";
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(
                        urlPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String url = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            // 清理URL末尾的标点符号
            while (end > start && isUrlEndChar(text.charAt(end - 1))) {
                end--;
            }

            // 重新获取清理后的URL
            url = text.substring(start, end);

            // 移除这个位置可能存在的任何URLSpan
            URLSpan[] existingSpans = spannable.getSpans(start, end, URLSpan.class);
            for (URLSpan existingSpan : existingSpans) {
                spannable.removeSpan(existingSpan);
            }

            // 添加新的URLSpan
            URLSpanUnderlineSameColor urlSpan = new URLSpanUnderlineSameColor(url);
            spannable.setSpan(urlSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /** 判断字符是否为URL结束字符 */
    private static boolean isUrlEndChar(char c) {
        return c == '.' || c == ',' || c == '!' || c == '?' || c == ';' || c == ':' || c == '"'
                || c == '\'' || c == ')';
    }

    /** 添加自定义手机号码识别逻辑，确保在所有设备上都能识别 */
    private static void addCustomPhoneLinks(SpannableStringBuilder spannable) {
        String text = spannable.toString();
        // 匹配中国大陆手机号码：1开头的11位数字
        String phonePattern = "(1[3-9]\\d{9})";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(phonePattern);
        java.util.regex.Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String phone = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);

            // 检查这个位置是否已经有URLSpan（避免重复）
            URLSpan[] existingSpans = spannable.getSpans(start, end, URLSpan.class);
            if (existingSpans.length == 0) {
                // 添加手机号码链接
                URLSpanUnderlineSameColor phoneSpan = new URLSpanUnderlineSameColor("tel:" + phone);
                spannable.setSpan(phoneSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public interface RegularCallBack {
        void finish(SpannableStringBuilder spannable);
    }

    /** 自定义链接样式：保留下划线，同时保持与普通文字一致的颜色。 通过先记录调用前的颜色，再覆盖为默认文字色，避免主题默认的链接色。 */
    public static class URLSpanUnderlineSameColor extends URLSpan {
        public URLSpanUnderlineSameColor(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            int originalColor = ds.getColor();
            super.updateDrawState(ds);
            ds.setUnderlineText(true);
            ds.setColor(originalColor);
        }
    }

    /**
     * 设置 TextView 的setCompoundDrawablesRelative
     *
     * @param textView TextView
     * @param gravity 仅支持设置：Gravity.START，Gravity.TOP，Gravity.END，Gravity.BOTTOM。
     * @param resId 资源ID
     */
    public static void setCompoundDrawables(TextView textView, int gravity, int resId) {
        Drawable drawable = textView.getResources().getDrawable(resId);
        int w = drawable.getIntrinsicWidth();
        drawable.setBounds(0, 0, w, w);
        Drawable[] drawables = textView.getCompoundDrawablesRelative();
        if (Gravity.START == gravity) {
            drawables[0] = drawable;
        } else if (Gravity.TOP == gravity) {
            drawables[1] = drawable;
        } else if (Gravity.END == gravity) {
            drawables[2] = drawable;
        } else if (Gravity.BOTTOM == gravity) {
            drawables[3] = drawable;
        }
        textView.setCompoundDrawablesRelative(
                drawables[0], drawables[1], drawables[2], drawables[3]);
        textView.setCompoundDrawablePadding(w / 2);
    }

    /**
     * 设置 TextView 的 Drawable 自动翻转，适用于带有方向性的 Drawable
     *
     * @param textView TextView
     */
    public static void enableDrawableAutoMirror(TextView textView) {
        if (textView == null) {
            return;
        }
        Drawable[] drawables = TextViewCompat.getCompoundDrawablesRelative(textView);
        if (drawables != null) {
            for (Drawable drawable : drawables) {
                if (drawable != null) {
                    drawable.setAutoMirrored(true);
                }
            }
        }
    }
}
