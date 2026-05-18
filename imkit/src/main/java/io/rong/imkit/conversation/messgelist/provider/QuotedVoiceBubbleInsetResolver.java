package io.rong.imkit.conversation.messgelist.provider;

import android.view.ViewGroup;

public final class QuotedVoiceBubbleInsetResolver {

    private QuotedVoiceBubbleInsetResolver() {}

    public static int resolveEndInsetPx(
            boolean hasQuoteCard,
            boolean isSender,
            boolean hasExternalStatusDecoration,
            int decorationWidthPx,
            int decorationStartMarginPx,
            int decorationEndMarginPx) {
        if (!hasQuoteCard || isSender || !hasExternalStatusDecoration) {
            return 0;
        }
        return Math.max(0, decorationWidthPx)
                + Math.max(0, decorationStartMarginPx)
                + Math.max(0, decorationEndMarginPx);
    }

    public static int resolveQuoteCardWidthPx(
            boolean hasQuoteCard, boolean isSender, int messageBubbleWidthPx) {
        if (!hasQuoteCard || isSender || messageBubbleWidthPx <= 0) {
            return ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        // 对于接收方的语音消息，引用卡片宽度不受语音气泡限制
        // 让引用卡片根据内容自适应宽度，wrapper 会自动扩展到较宽的那个
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    public static int resolveMeasuredEndInsetPx(
            boolean hasQuoteCard, boolean isSender, int contentWidthPx, int messageBubbleWidthPx) {
        if (!hasQuoteCard || isSender) {
            return 0;
        }
        return Math.max(0, contentWidthPx - messageBubbleWidthPx);
    }
}
