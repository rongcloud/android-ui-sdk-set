package io.rong.imkit.feature.reference;

final class ReferenceContentLayoutResolver {

    private static final int DEFAULT_MAX_LINES = 1;
    private static final int STATUS_MAX_LINES = 2;
    private static final int RICH_MAX_LINES = 3;

    private ReferenceContentLayoutResolver() {}

    static int resolveMaxLines(boolean statusText, boolean richContent) {
        if (statusText) {
            return STATUS_MAX_LINES;
        }
        return richContent ? RICH_MAX_LINES : DEFAULT_MAX_LINES;
    }

    static boolean resolveSingleLine(boolean statusText, boolean richContent) {
        return !statusText && !richContent;
    }

    static boolean resolveEndEllipsize(boolean statusText, boolean richContent) {
        return !statusText;
    }
}
