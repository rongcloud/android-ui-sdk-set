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
