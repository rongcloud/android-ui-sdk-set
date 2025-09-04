package io.rong.imkit.utils;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

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
            span = new URLSpanNoUnderline(span.getURL());
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
            URLSpanNoUnderline urlSpan = new URLSpanNoUnderline(url);
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
                URLSpanNoUnderline phoneSpan = new URLSpanNoUnderline("tel:" + phone);
                spannable.setSpan(phoneSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public interface RegularCallBack {
        void finish(SpannableStringBuilder spannable);
    }

    /** 取消超链接下划线的UrlSpan */
    public static class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
        }
    }
}
